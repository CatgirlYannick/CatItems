package dev.catgirlyannick.catitems.pack;

import dev.catgirlyannick.catitems.api.CatItemDefinition;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackBuilder {
    private final JavaPlugin plugin;

    public ResourcePackBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public PackArtifact build(Collection<CatItemDefinition> definitions, String minecraftVersion,
                              boolean modernItemModels) throws IOException {
        PackFormat format = PackFormat.forMinecraftVersion(minecraftVersion);
        Path packDirectory = plugin.getDataFolder().toPath().resolve("pack");
        Path generatedDirectory = plugin.getDataFolder().toPath().resolve("generated-pack");
        Path outputDirectory = plugin.getDataFolder().toPath().resolve("output");
        Path output = outputDirectory.resolve("CatItems-pack.zip");

        deleteDirectory(generatedDirectory);
        Files.createDirectories(generatedDirectory);
        Files.createDirectories(outputDirectory);
        Path sourceAssets = packDirectory.resolve("assets");
        if (Files.isDirectory(sourceAssets)) {
            copyDirectory(sourceAssets, generatedDirectory.resolve("assets"));
        }
        Files.writeString(generatedDirectory.resolve("pack.mcmeta"), format.packMetaJson(), StandardCharsets.UTF_8);

        Map<Material, List<CatItemDefinition>> byMaterial = new LinkedHashMap<>();
        for (CatItemDefinition definition : definitions) {
            ensureTextureExists(sourceAssets, definition);
            writeGeneratedModel(generatedDirectory, definition);
            if (modernItemModels) {
                writeModernItemDefinition(generatedDirectory, definition);
            } else {
                byMaterial.computeIfAbsent(definition.material(), ignored -> new ArrayList<>()).add(definition);
            }
        }
        if (!modernItemModels) {
            writeLegacyOverrides(generatedDirectory, byMaterial);
        }

        zipDirectory(generatedDirectory, output);
        byte[] hash = sha1(output);
        return new PackArtifact(output, hash, hex(hash), Files.size(output), format);
    }

    private void ensureTextureExists(Path sourceAssets, CatItemDefinition definition) throws IOException {
        Path texture = sourceAssets
                .resolve(definition.texture().getNamespace())
                .resolve("textures")
                .resolve(definition.texture().getKey() + ".png")
                .normalize();
        if (!texture.startsWith(sourceAssets.normalize()) || !Files.isRegularFile(texture)) {
            throw new IOException("Texture for " + definition.id() + " is missing: " + texture);
        }
    }

    private void writeGeneratedModel(Path root, CatItemDefinition definition) throws IOException {
        Path modelFile = assetPath(root, definition.itemModel().getNamespace(), "models",
                definition.itemModel().getKey() + ".json");
        if (Files.exists(modelFile)) {
            return;
        }
        Files.createDirectories(modelFile.getParent());
        String json = """
                {
                  "ambientocclusion": false,
                  "gui_light": "front",
                  "textures": {
                    "particle": "%1$s",
                    "icon": "%1$s"
                  },
                  "elements": [
                    {
                      "from": [2, 2, 7],
                      "to": [14, 14, 9],
                      "faces": {
                        "north": {"uv": [0, 0, 16, 16], "texture": "#icon"},
                        "south": {"uv": [16, 0, 0, 16], "texture": "#icon"},
                        "east": {"uv": [0, 0, 2, 16], "texture": "#icon"},
                        "west": {"uv": [14, 0, 16, 16], "texture": "#icon"},
                        "up": {"uv": [0, 0, 16, 2], "texture": "#icon"},
                        "down": {"uv": [0, 14, 16, 16], "texture": "#icon"}
                      }
                    },
                    {
                      "from": [4, 1, 6],
                      "to": [12, 3, 10],
                      "faces": {
                        "north": {"uv": [2, 13, 14, 16], "texture": "#icon"},
                        "south": {"uv": [14, 13, 2, 16], "texture": "#icon"},
                        "east": {"uv": [0, 12, 4, 16], "texture": "#icon"},
                        "west": {"uv": [12, 12, 16, 16], "texture": "#icon"},
                        "up": {"uv": [2, 12, 14, 16], "texture": "#icon"},
                        "down": {"uv": [2, 12, 14, 16], "texture": "#icon"}
                      }
                    }
                  ],
                  "display": {
                    "thirdperson_righthand": {"rotation": [0, 90, -35], "translation": [0, 1.25, -3], "scale": [0.45, 0.45, 0.45]},
                    "thirdperson_lefthand": {"rotation": [0, -90, 35], "translation": [0, 1.25, -3], "scale": [0.45, 0.45, 0.45]},
                    "firstperson_righthand": {"rotation": [0, -90, 20], "translation": [1.1, 2.8, 0.8], "scale": [0.30, 0.30, 0.30]},
                    "firstperson_lefthand": {"rotation": [0, 90, -20], "translation": [1.1, 2.8, 0.8], "scale": [0.30, 0.30, 0.30]},
                    "gui": {"rotation": [28, 225, 0], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72]},
                    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.30, 0.30, 0.30]},
                    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
                    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [0.55, 0.55, 0.55]}
                  }
                }
                """.formatted(definition.texture());
        Files.writeString(modelFile, json, StandardCharsets.UTF_8);
    }

    private void writeModernItemDefinition(Path root, CatItemDefinition definition) throws IOException {
        Path itemFile = assetPath(root, definition.itemModel().getNamespace(), "items",
                definition.itemModel().getKey() + ".json");
        Files.createDirectories(itemFile.getParent());
        String json = """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "%s"
                  }
                }
                """.formatted(definition.itemModel());
        Files.writeString(itemFile, json, StandardCharsets.UTF_8);
    }

    private void writeLegacyOverrides(Path root, Map<Material, List<CatItemDefinition>> byMaterial) throws IOException {
        for (Map.Entry<Material, List<CatItemDefinition>> entry : byMaterial.entrySet()) {
            List<CatItemDefinition> definitions = new ArrayList<>(entry.getValue());
            definitions.sort(Comparator.comparingInt(CatItemDefinition::customModelData));
            String material = entry.getKey().getKey().getKey();
            StringBuilder overrides = new StringBuilder();
            for (int index = 0; index < definitions.size(); index++) {
                CatItemDefinition definition = definitions.get(index);
                if (index > 0) {
                    overrides.append(",\n");
                }
                overrides.append("    {\"predicate\": {\"custom_model_data\": ")
                        .append(definition.customModelData())
                        .append("}, \"model\": \"")
                        .append(definition.itemModel())
                        .append("\"}");
            }
            Path vanillaModel = assetPath(root, "minecraft", "models/item", material + ".json");
            Files.createDirectories(vanillaModel.getParent());
            String json = """
                    {
                      "parent": "minecraft:item/generated",
                      "textures": {"layer0": "minecraft:item/%s"},
                      "overrides": [
                    %s
                      ]
                    }
                    """.formatted(material, overrides);
            Files.writeString(vanillaModel, json, StandardCharsets.UTF_8);
        }
    }

    private Path assetPath(Path root, String namespace, String category, String relative) throws IOException {
        Path assets = root.resolve("assets").normalize();
        Path result = assets.resolve(namespace).resolve(category).resolve(relative).normalize();
        if (!result.startsWith(assets)) {
            throw new IOException("Unsafe asset path: " + result);
        }
        return result;
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted().toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void zipDirectory(Path source, Path output) throws IOException {
        try (OutputStream fileOutput = Files.newOutputStream(output);
             ZipOutputStream zip = new ZipOutputStream(fileOutput, StandardCharsets.UTF_8);
             var paths = Files.walk(source)) {
            for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = source.relativize(file).toString().replace('\\', '/');
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0L);
                zip.putNextEntry(entry);
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private byte[] sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is not supported by this JVM", exception);
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        }
        return result.toString();
    }
}
