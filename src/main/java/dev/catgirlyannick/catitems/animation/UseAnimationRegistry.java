package dev.catgirlyannick.catitems.animation;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public final class UseAnimationRegistry {
    private final JavaPlugin plugin;
    private final Map<String, UseAnimationDefinition> definitions = new LinkedHashMap<>();
    private List<UseAnimationDefinition> definitionSnapshot = List.of();

    public UseAnimationRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int reload() {
        definitions.clear();
        definitionSnapshot = List.of();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "animations.yml"));
        ConfigurationSection root = yaml.getConfigurationSection("animations");
        if (root == null) {
            throw new IllegalArgumentException("animations.yml does not contain an animations section");
        }
        for (String id : new TreeSet<>(root.getKeys(false))) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            try {
                UseAnimationDefinition definition = parse(id, section);
                definitions.put(definition.id(), definition);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Animation '" + id + "' was skipped: " + exception.getMessage());
            }
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("animations.yml does not contain a valid enabled animation");
        }
        definitionSnapshot = List.copyOf(definitions.values());
        return definitions.size();
    }

    public Optional<UseAnimationDefinition> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        normalized = switch (normalized) {
            case "smoke" -> "smoke_joint";
            case "vape", "vaping", "inhale" -> "inhale_vape";
            case "syringe", "inject" -> "inject_arm";
            case "pill", "tablet", "swallow" -> "swallow_pill";
            case "nasal", "snort" -> "snort_line";
            case "edible", "eat" -> "eat_edible";
            case "drink" -> "drink_bottle";
            case "ritual" -> "ritual_sway";
            default -> normalized;
        };
        UseAnimationDefinition exact = definitions.get(normalized);
        if (exact != null) {
            return Optional.of(exact);
        }
        int separator = normalized.indexOf(':');
        return separator >= 0 ? Optional.ofNullable(definitions.get(normalized.substring(separator + 1))) : Optional.empty();
    }

    public Collection<UseAnimationDefinition> definitions() {
        return definitionSnapshot;
    }

    static UseAnimationDefinition parse(String rawId, ConfigurationSection section) {
        String id = rawId.toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("ID contains unsupported characters");
        }
        int duration = section.getInt("duration-ticks", 30);
        if (duration < 4 || duration > 400) {
            throw new IllegalArgumentException("duration-ticks must be between 4 and 400");
        }
        List<Map<?, ?>> maps = section.getMapList("keyframes");
        if (maps.isEmpty()) {
            throw new IllegalArgumentException("keyframes must not be empty");
        }
        List<AnimationKeyframe> frames = new ArrayList<>();
        for (Map<?, ?> values : maps) {
            int tick = integer(values, "at", -1);
            if (tick < 0 || tick > duration) {
                throw new IllegalArgumentException("keyframe 'at' must be between 0 and duration-ticks");
            }
            AnimationModelPose pose = AnimationModelPose.find(string(values, "item-pose", "rest"))
                    .orElseThrow(() -> new IllegalArgumentException("unknown item-pose at tick " + tick));
            AnimationKeyframe.HandMotion hand = enumValue(AnimationKeyframe.HandMotion.class,
                    string(values, "hand", "none"), "hand", tick);
            AnimationKeyframe.UseAction use = enumValue(AnimationKeyframe.UseAction.class,
                    string(values, "use-action", "none"), "use-action", tick);
            AnimationKeyframe.ArmPose armPose = enumValue(AnimationKeyframe.ArmPose.class,
                    string(values, "arm-pose", "keep"), "arm-pose", tick);
            AnimationKeyframe.BodyPose bodyPose = enumValue(AnimationKeyframe.BodyPose.class,
                    string(values, "body-pose", "keep"), "body-pose", tick);
            Float bodyYaw = optionalFloat(values, "body-yaw", -180.0, 180.0, tick);
            AnimationKeyframe.Easing easing = enumValue(AnimationKeyframe.Easing.class,
                    string(values, "easing", "ease_in_out"), "easing", tick);
            Particle particle = optionalEnum(Particle.class, string(values, "particle", ""), "particle", tick);
            if (particle != null && particle.getDataType() != Void.class) {
                throw new IllegalArgumentException("particle at tick " + tick + " requires unsupported extra data");
            }
            AnimationKeyframe.ParticleAnchor anchor = enumValue(AnimationKeyframe.ParticleAnchor.class,
                    string(values, "particle-anchor", "mouth"), "particle-anchor", tick);
            String sound = soundKey(string(values, "sound", ""), tick);
            frames.add(new AnimationKeyframe(tick, pose, hand, use, armPose, bodyPose, bodyYaw,
                    easing, particle, anchor,
                    clamp(integer(values, "particle-count", 2), 0, 30),
                    clamp(decimal(values, "particle-spread", 0.05), 0.0, 2.0),
                    clamp(decimal(values, "particle-speed", 0.005), 0.0, 2.0),
                    sound,
                    (float) clamp(decimal(values, "volume", 0.65), 0.0, 4.0),
                    (float) clamp(decimal(values, "pitch", 1.0), 0.5, 2.0)));
        }
        frames.sort(Comparator.comparingInt(AnimationKeyframe::tick));
        if (frames.stream().noneMatch(AnimationKeyframe::hasArmMotion)) {
            throw new IllegalArgumentException("at least one keyframe must contain an animated arm-pose");
        }
        return new UseAnimationDefinition(id, duration, section.getBoolean("lock-movement", true), frames);
    }

    private static String string(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value).trim();
    }

    private static int integer(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double decimal(Map<?, ?> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static Float optionalFloat(Map<?, ?> map, String key, double minimum, double maximum, int tick) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be numeric at tick " + tick);
        }
        double numeric = number.doubleValue();
        if (numeric < minimum || numeric > maximum) {
            throw new IllegalArgumentException(key + " is outside its safe range at tick " + tick);
        }
        return number.floatValue();
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, String field, int tick) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown " + field + " at tick " + tick);
        }
    }

    private static <T extends Enum<T>> T optionalEnum(Class<T> type, String value, String field, int tick) {
        if (value.isBlank() || "none".equalsIgnoreCase(value)) {
            return null;
        }
        return enumValue(type, value, field, tick);
    }

    private static String soundKey(String value, int tick) {
        if (value.isBlank() || "none".equalsIgnoreCase(value)) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized.replace('_', '.');
        }
        if (!normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("invalid sound key at tick " + tick);
        }
        return normalized;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
