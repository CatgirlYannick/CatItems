package dev.catgirlyannick.catitems;

import dev.catgirlyannick.catitems.feature.FeatureCatalog;
import dev.catgirlyannick.catitems.feature.FeatureStatus;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginContractTest {
    @Test
    void pluginManifestDeclaresStandaloneEntryPoint() throws IOException {
        String manifest = resourceText("plugin.yml");
        assertTrue(manifest.contains("name: CatItems"));
        assertTrue(manifest.contains("version: '0.7.0-ALPHA'"));
        assertTrue(manifest.contains("main: dev.catgirlyannick.catitems.CatItemsPlugin"));
        assertTrue(manifest.contains("api-version: '1.21'"));
        assertTrue(manifest.contains("catitems.admin:"));
        assertTrue(!manifest.contains("depend:") && !manifest.contains("ItemsAdder"));
    }

    @Test
    void featureCatalogTracksItemsAdderParityGroups() {
        assertTrue(FeatureCatalog.all().size() >= 60);
        assertTrue(FeatureCatalog.find("custom-items").isPresent());
        assertTrue(FeatureCatalog.find("CUSTOM-BLOCKS").isPresent());
        assertTrue(FeatureCatalog.find("furniture").isPresent());
        assertEquals(FeatureStatus.LIVE, FeatureCatalog.find("use-animations").orElseThrow().status());
        assertTrue(FeatureCatalog.count(FeatureStatus.LIVE) > 0);
        assertTrue(FeatureCatalog.count(FeatureStatus.FOUNDATION) > 0);
        assertTrue(FeatureCatalog.count(FeatureStatus.PLANNED) > 0);
    }

    @Test
    void configDocumentsFutureFeatureSwitches() throws IOException {
        String config = resourceText("config.yml");
        assertTrue(config.contains("features:"));
        assertTrue(config.contains("custom-blocks: false"));
        assertTrue(config.contains("furniture: false"));
        assertTrue(config.contains("worldgen: false"));
        assertTrue(config.contains("animations:"));
        assertTrue(config.contains("max-duration-ticks: 120"));
        assertTrue(resourceText("animations.yml").contains("item-pose: mouth"));
        assertTrue(resourceText("animations.yml").contains("item-pose: approach"));
        assertTrue(resourceText("animations.yml").contains("item-pose: contact"));
        assertTrue(resourceText("animations.yml").contains("item-pose: release"));
        assertTrue(resourceText("animations.yml").contains("arm-pose: drink"));
        assertTrue(resourceText("animations.yml").contains("arm-pose: face"));
        assertTrue(!resourceText("animations.yml").contains("head-yaw"));
        assertTrue(!resourceText("animations.yml").contains("head-pitch"));
    }

    @Test
    void starterRegistryContainsFourIndependentItems() throws IOException {
        String registry = resourceText("items/starter.yml");
        for (String id : List.of("ruby", "sapphire", "emerald_gear", "star_token")) {
            assertTrue(registry.contains("  " + id + ":"), id);
            assertTrue(registry.contains("catitems:item/" + id), id);
        }
    }

    @Test
    void starterTexturesAre64PixelRgbaAssets() throws IOException {
        for (String id : List.of("ruby", "sapphire", "emerald_gear", "star_token")) {
            String path = "pack/assets/catitems/textures/item/" + id + ".png";
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(input, path);
                BufferedImage image = ImageIO.read(input);
                assertNotNull(image, path);
                assertEquals(64, image.getWidth(), path);
                assertEquals(64, image.getHeight(), path);
                assertTrue(image.getColorModel().hasAlpha(), path);
            }
        }
    }

    private String resourceText(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
