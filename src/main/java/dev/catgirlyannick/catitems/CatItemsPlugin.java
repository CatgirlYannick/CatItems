package dev.catgirlyannick.catitems;

import dev.catgirlyannick.catitems.api.CatItemsApi;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
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
        itemService = new CatItemService(this, registry);
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
                + " items, Minecraft " + Bukkit.getMinecraftVersion() + ", pack format " + format.display() + ".");
    }

    @Override
    public void onDisable() {
        if (itemService != null) {
            Bukkit.getServicesManager().unregister(CatItemsApi.class, itemService);
        }
        if (packManager != null) {
            packManager.stop();
        }
    }

    public int reloadEverything() {
        reloadConfig();
        validateConfig();
        messages.reload(loadMessages());
        int loaded = registry.reload();
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
        saveIfMissing("items/starter.yml");
        saveIfMissing("pack/README.txt");
        for (String texture : STARTER_TEXTURES) {
            saveIfMissing(texture);
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
