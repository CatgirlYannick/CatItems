package dev.catgirlyannick.catitems;

import dev.catgirlyannick.catitems.api.CatItemsApi;
import dev.catgirlyannick.catitems.animation.UseAnimationService;
import dev.catgirlyannick.catitems.animation.UseAnimationRegistry;
import dev.catgirlyannick.catitems.command.CatItemsCommand;
import dev.catgirlyannick.catitems.config.ItemRegistry;
import dev.catgirlyannick.catitems.item.CatItemService;
import dev.catgirlyannick.catitems.listener.ResourcePackListener;
import dev.catgirlyannick.catitems.pack.PackArtifact;
import dev.catgirlyannick.catitems.pack.PackFormat;
import dev.catgirlyannick.catitems.pack.ResourcePackManager;
import dev.catgirlyannick.catitems.service.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class CatItemsPlugin extends JavaPlugin {
    private static final List<String> STARTER_TEXTURES = List.of(
            "pack/assets/catitems/textures/item/ruby.png",
            "pack/assets/catitems/textures/item/sapphire.png",
            "pack/assets/catitems/textures/item/emerald_gear.png",
            "pack/assets/catitems/textures/item/star_token.png"
    );

    private ItemRegistry registry;
    private CatItemService itemService;
    private MessageService messages;
    private ResourcePackManager packManager;
    private UseAnimationService animations;
    private UseAnimationRegistry animationRegistry;

    @Override
    public void onEnable() {
        saveBundledFiles();
        PackFormat format;
        try {
            format = PackFormat.forMinecraftVersion(Bukkit.getMinecraftVersion());
            validateConfig();
        } catch (IllegalArgumentException exception) {
            getLogger().severe(exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        messages = new MessageService(loadMessages());
        registry = new ItemRegistry(this);
        int loaded = registry.reload();
        animationRegistry = new UseAnimationRegistry(this);
        int animationCount = animationRegistry.reload();
        animations = new UseAnimationService(this, registry, animationRegistry);
        if (!animations.supportsArmPoses()) {
            getLogger().warning("Modern arm poses are unavailable on this Paper build; update to Minecraft 1.21.4 or newer.");
        }
        getServer().getPluginManager().registerEvents(animations, this);
        itemService = new CatItemService(this, registry, animations);
        packManager = new ResourcePackManager(this, messages);

        Bukkit.getServicesManager().register(CatItemsApi.class, itemService, this, ServicePriority.Normal);
        ResourcePackListener listener = new ResourcePackListener(this, packManager);
        getServer().getPluginManager().registerEvents(listener, this);

        PluginCommand command = requireNonNull(getCommand("catitems"), "The catitems command is missing from plugin.yml");
        CatItemsCommand executor = new CatItemsCommand(this, itemService, packManager, messages);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        if (getConfig().getBoolean("resource-pack.build-on-start", true)) {
            try {
                PackArtifact artifact = rebuildPack();
                getLogger().info("Resource pack built: " + artifact.file() + " (SHA-1 " + artifact.sha1Hex() + ")");
            } catch (IOException | RuntimeException exception) {
                getLogger().log(Level.SEVERE, "The resource pack could not be built or provided.", exception);
            }
        }
        getLogger().info("CatItems " + getPluginMeta().getVersion() + " is active: " + loaded
                + " items, " + animationCount + " custom animations, Minecraft "
                + Bukkit.getMinecraftVersion() + ", pack format " + format.display() + ".");
    }

    @Override
    public void onDisable() {
        if (itemService != null) {
            Bukkit.getServicesManager().unregister(CatItemsApi.class, itemService);
        }
        if (packManager != null) {
            packManager.stop();
        }
        if (animations != null) {
            animations.shutdown();
        }
    }

    public int reloadEverything() {
        reloadConfig();
        validateConfig();
        messages.reload(loadMessages());
        int loaded = registry.reload();
        itemService.clearPrototypeCache();
        animations.shutdown();
        animationRegistry.reload();
        if (getConfig().getBoolean("resource-pack.build-on-start", true)) {
            try {
                rebuildPack();
            } catch (IOException exception) {
                throw new IllegalStateException("The resource pack could not be built during reload", exception);
            }
        }
        return loaded;
    }

    public PackArtifact rebuildPack() throws IOException {
        return packManager.rebuild(itemService.items(), itemService.supportsModernItemModels());
    }

    private void saveBundledFiles() {
        saveDefaultConfig();
        saveIfMissing("messages.yml");
        saveIfMissing("animations.yml");
        upgradeBundledAnimations();
        saveIfMissing("items/starter.yml");
        saveIfMissing("pack/README.txt");
        for (String texture : STARTER_TEXTURES) {
            saveIfMissing(texture);
        }
    }

    private void upgradeBundledAnimations() {
        File target = new File(getDataFolder(), "animations.yml");
        YamlConfiguration current = YamlConfiguration.loadConfiguration(target);
        int previousVersion = current.getInt("config-version", 1);
        if (previousVersion >= 5) {
            return;
        }
        File backup = new File(getDataFolder(), "animations-v" + previousVersion + "-backup.yml");
        try {
            if (!backup.isFile()) {
                Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            }
            Map<String, Map<String, Object>> customAnimations = customAnimations(current);
            saveResource("animations.yml", true);
            if (!customAnimations.isEmpty()) {
                YamlConfiguration upgraded = YamlConfiguration.loadConfiguration(target);
                customAnimations.forEach((id, values) -> upgraded.set("animations." + id, values));
                upgraded.save(target);
            }
            getLogger().info("Upgraded animations.yml to smooth transition format; previous file saved as "
                    + backup.getName() + " and " + customAnimations.size() + " custom animation(s) were preserved.");
        } catch (IOException exception) {
            throw new IllegalStateException("animations.yml could not be upgraded", exception);
        }
    }

    private Map<String, Map<String, Object>> customAnimations(YamlConfiguration current) throws IOException {
        try (InputStream input = getResource("animations.yml")) {
            if (input == null) {
                throw new IOException("Bundled animations.yml is unavailable");
            }
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            ConfigurationSection currentRoot = current.getConfigurationSection("animations");
            ConfigurationSection bundledRoot = bundled.getConfigurationSection("animations");
            Map<String, Map<String, Object>> custom = new LinkedHashMap<>();
            if (currentRoot == null || bundledRoot == null) {
                return custom;
            }
            for (String id : currentRoot.getKeys(false)) {
                if (bundledRoot.contains(id)) {
                    continue;
                }
                ConfigurationSection section = currentRoot.getConfigurationSection(id);
                if (section != null) {
                    custom.put(id, new LinkedHashMap<>(section.getValues(false)));
                }
            }
            return custom;
        }
    }

    private void saveIfMissing(String path) {
        File target = new File(getDataFolder(), path);
        if (!target.isFile()) {
            saveResource(path, false);
        }
    }

    private YamlConfiguration loadMessages() {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
    }

    private void validateConfig() {
        int start = getConfig().getInt("items.model-data-start", 10000);
        if (start < 1) {
            throw new IllegalArgumentException("items.model-data-start must be greater than 0");
        }
        String delivery = getConfig().getString("resource-pack.delivery", "self-host").toLowerCase(Locale.ROOT);
        if (!List.of("disabled", "self-host", "external").contains(delivery)) {
            throw new IllegalArgumentException("resource-pack.delivery must be disabled, self-host, or external");
        }
        int port = getConfig().getInt("resource-pack.self-host.port", 8164);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("resource-pack.self-host.port must be between 1 and 65535");
        }
    }

    private <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
