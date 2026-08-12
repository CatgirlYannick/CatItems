package dev.catgirlyannick.catitems.config;

import dev.catgirlyannick.catitems.api.CatItemDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

public final class ItemRegistry {
    private final JavaPlugin plugin;
    private final Map<String, CatItemDefinition> definitions = new LinkedHashMap<>();
    private final File modelDataFile;
    private YamlConfiguration modelData;

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modelDataFile = new File(plugin.getDataFolder(), "data/model-data.yml");
        this.modelData = YamlConfiguration.loadConfiguration(modelDataFile);
    }

    public int reload() {
        File itemDirectory = new File(plugin.getDataFolder(), "items");
        if (!itemDirectory.isDirectory() && !itemDirectory.mkdirs()) {
            throw new IllegalStateException("The item directory could not be created: " + itemDirectory);
        }

        definitions.clear();
        modelData = YamlConfiguration.loadConfiguration(modelDataFile);
        int nextModelData = Math.max(plugin.getConfig().getInt("items.model-data-start", 10000),
                modelData.getInt("next", 10000));

        File[] files = itemDirectory.listFiles((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return 0;
        }
        List<File> sortedFiles = new ArrayList<>(List.of(files));
        sortedFiles.sort(Comparator.comparing(File::getName));
        Set<String> usedMaterialAndModel = new TreeSet<>();

        for (File file : sortedFiles) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String namespace = yaml.getString("namespace", "catitems").toLowerCase(Locale.ROOT);
            ConfigurationSection items = yaml.getConfigurationSection("items");
            if (items == null) {
                plugin.getLogger().warning(file.getName() + " does not contain an 'items' section.");
                continue;
            }
            for (String localId : new TreeSet<>(items.getKeys(false))) {
                ConfigurationSection section = items.getConfigurationSection(localId);
                if (section == null || !section.getBoolean("enabled", true)) {
                    continue;
                }
                try {
                    NamespacedKey id = requireKey(namespace + ":" + localId, "Item-ID");
                    String idString = id.toString();
                    if (definitions.containsKey(idString)) {
                        throw new IllegalArgumentException("Duplicate item ID " + idString);
                    }

                    Material material = Material.matchMaterial(section.getString("material", "PAPER"));
                    if (material == null || !material.isItem()) {
                        throw new IllegalArgumentException("Invalid material");
                    }

                    int customModelData;
                    Object configuredModelData = section.get("custom-model-data", "auto");
                    if (configuredModelData instanceof Number number) {
                        customModelData = number.intValue();
                    } else if ("auto".equalsIgnoreCase(String.valueOf(configuredModelData))) {
                        String storagePath = "models." + idString;
                        customModelData = modelData.getInt(storagePath, -1);
                        if (customModelData < 1) {
                            customModelData = nextModelData++;
                            modelData.set(storagePath, customModelData);
                        }
                    } else {
                        throw new IllegalArgumentException("custom-model-data must be a number or 'auto'");
                    }
                    if (customModelData < 1) {
                        throw new IllegalArgumentException("custom-model-data must be greater than 0");
                    }
                    String collisionKey = material.name() + ":" + customModelData;
                    if (!usedMaterialAndModel.add(collisionKey)) {
                        throw new IllegalArgumentException("Duplicate material and CustomModelData combination: " + collisionKey);
                    }

                    NamespacedKey itemModel = requireKey(section.getString("item-model", idString), "item-model");
                    NamespacedKey texture = requireKey(section.getString("texture", namespace + ":item/" + localId), "texture");
                    CatItemDefinition definition = new CatItemDefinition(
                            id,
                            material,
                            section.getString("display-name", "<white>" + localId + "</white>"),
                            section.getStringList("lore"),
                            customModelData,
                            itemModel,
                            texture,
                            section.getBoolean("glint", false),
                            section.getString("permission", "")
                    );
                    definitions.put(idString, definition);
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Item '" + namespace + ":" + localId + "' in " + file.getName()
                            + " was skipped: " + exception.getMessage());
                }
            }
        }

        modelData.set("next", nextModelData);
        saveModelData();
        return definitions.size();
    }

    public Optional<CatItemDefinition> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        CatItemDefinition exact = definitions.get(normalized);
        if (exact != null) {
            return Optional.of(exact);
        }
        if (!normalized.contains(":")) {
            List<CatItemDefinition> matches = definitions.values().stream()
                    .filter(definition -> definition.id().getKey().equals(normalized))
                    .toList();
            if (matches.size() == 1) {
                return Optional.of(matches.getFirst());
            }
        }
        return Optional.empty();
    }

    public Collection<CatItemDefinition> definitions() {
        return List.copyOf(definitions.values());
    }

    private NamespacedKey requireKey(String value, String field) {
        NamespacedKey key = NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
        if (key == null) {
            throw new IllegalArgumentException(field + " is not a valid NamespacedKey: " + value);
        }
        return key;
    }

    private void saveModelData() {
        File parent = modelDataFile.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("The data directory could not be created: " + parent);
        }
        try {
            modelData.save(modelDataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "model-data.yml could not be saved.", exception);
            throw new IllegalStateException("Stable model IDs could not be saved", exception);
        }
    }
}
