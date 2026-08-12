package dev.catgirlyannick.catitems.pack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PackFormat(int major, int minor, boolean modernMetadata) {
    private static final Pattern VERSION = Pattern.compile("^1\\.21(?:\\.(\\d+))?.*$");

    public static PackFormat forMinecraftVersion(String minecraftVersion) {
        Matcher matcher = VERSION.matcher(minecraftVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported Minecraft version: " + minecraftVersion);
        }
        int patch = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        return switch (patch) {
            case 0, 1 -> new PackFormat(34, 0, false);
            case 2, 3 -> new PackFormat(42, 0, false);
            case 4 -> new PackFormat(46, 0, false);
            case 5 -> new PackFormat(55, 0, false);
            case 6 -> new PackFormat(63, 0, false);
            case 7, 8 -> new PackFormat(64, 0, false);
            case 9, 10 -> new PackFormat(69, 0, true);
            case 11 -> new PackFormat(75, 0, true);
            default -> throw new IllegalArgumentException("CatItems currently supports Paper 1.21 through 1.21.11; found: " + minecraftVersion);
        };
    }

    public String display() {
        return modernMetadata ? major + "." + minor : Integer.toString(major);
    }

    public String packMetaJson() {
        if (modernMetadata) {
            return """
                    {
                      "pack": {
                        "description": "CatItems - automatically generated resource pack",
                        "min_format": [%d, %d],
                        "max_format": [%d, %d]
                      }
                    }
                    """.formatted(major, minor, major, minor);
        }
        return """
                {
                  "pack": {
                    "pack_format": %d,
                    "description": "CatItems - automatically generated resource pack"
                  }
                }
                """.formatted(major);
    }
}
