package dev.catgirlyannick.catitems.pack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackFormatTest {
    @Test
    void mapsEverySupportedReleaseFamily() {
        assertEquals(34, PackFormat.forMinecraftVersion("1.21").major());
        assertEquals(34, PackFormat.forMinecraftVersion("1.21.1").major());
        assertEquals(42, PackFormat.forMinecraftVersion("1.21.3").major());
        assertEquals(46, PackFormat.forMinecraftVersion("1.21.4").major());
        assertEquals(55, PackFormat.forMinecraftVersion("1.21.5").major());
        assertEquals(63, PackFormat.forMinecraftVersion("1.21.6").major());
        assertEquals(64, PackFormat.forMinecraftVersion("1.21.8").major());
        assertEquals(69, PackFormat.forMinecraftVersion("1.21.10").major());
        assertEquals(75, PackFormat.forMinecraftVersion("1.21.11").major());
    }

    @Test
    void switchesMetadataAtPackFormat65() {
        PackFormat oldFormat = PackFormat.forMinecraftVersion("1.21.8");
        PackFormat newFormat = PackFormat.forMinecraftVersion("1.21.9");

        assertFalse(oldFormat.modernMetadata());
        assertTrue(oldFormat.packMetaJson().contains("\"pack_format\": 64"));
        assertTrue(newFormat.modernMetadata());
        assertTrue(newFormat.packMetaJson().contains("\"min_format\": [69, 0]"));
        assertFalse(newFormat.packMetaJson().contains("pack_format"));
    }

    @Test
    void rejectsVersionsOutsideTheContract() {
        assertThrows(IllegalArgumentException.class, () -> PackFormat.forMinecraftVersion("1.20.6"));
        assertThrows(IllegalArgumentException.class, () -> PackFormat.forMinecraftVersion("1.21.12"));
    }
}
