package dev.catgirlyannick.catitems.animation;

import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum AnimationModelPose {
    REST(null, null),
    LOW(transform(5, -80, 8, 1.4, 0.2, 1.8, 0.27), transform(5, 80, -8, 1.4, 0.2, 1.8, 0.27)),
    APPROACH(transform(6, -91, 19, 2.2, 3.4, 1.3, 0.27), transform(6, 91, -19, 2.2, 3.4, 1.3, 0.27)),
    RAISE(transform(0, -96, 22, 2.4, 4.2, 0.8, 0.29), transform(0, 96, -22, 2.4, 4.2, 0.8, 0.29)),
    CONTACT(transform(8, -106, 34, 3.0, 5.2, 1.0, 0.26), transform(8, 106, -34, 3.0, 5.2, 1.0, 0.26)),
    MOUTH(transform(10, -112, 42, 3.2, 5.8, 1.1, 0.25), transform(10, 112, -42, 3.2, 5.8, 1.1, 0.25)),
    RELEASE(transform(7, -97, 25, 2.5, 3.8, 1.4, 0.27), transform(7, 97, -25, 2.5, 3.8, 1.4, 0.27)),
    TILT_LEFT(transform(-12, -105, 58, 2.8, 5.2, 0.7, 0.27), transform(-12, 105, -58, 2.8, 5.2, 0.7, 0.27)),
    TILT_RIGHT(transform(16, -82, 4, 2.0, 4.7, 0.2, 0.28), transform(16, 82, -4, 2.0, 4.7, 0.2, 0.28)),
    INHALE(transform(-6, -102, 28, 2.9, 5.5, 0.3, 0.24), transform(-6, 102, -28, 2.9, 5.5, 0.3, 0.24)),
    EXTEND(transform(22, -70, -24, 0.1, 2.0, -1.7, 0.30), transform(22, 70, 24, 0.1, 2.0, -1.7, 0.30)),
    FOREARM(transform(63, -30, -49, 1.0, 1.0, -0.2, 0.27), transform(63, 30, 49, 1.0, 1.0, -0.2, 0.27)),
    CHEST(transform(78, -18, -62, 1.1, 0.0, -0.8, 0.25), transform(78, 18, 62, 1.1, 0.0, -0.8, 0.25)),
    AFTERCARE(transform(48, -38, -38, 1.3, 0.6, 0.4, 0.28), transform(48, 38, 38, 1.3, 0.6, 0.4, 0.28)),
    SHAKE_UP(transform(-18, -92, 18, 2.2, 5.0, 0.5, 0.27), transform(-18, 92, -18, 2.2, 5.0, 0.5, 0.27)),
    SHAKE_DOWN(transform(20, -88, 12, 2.1, 3.5, 0.9, 0.27), transform(20, 88, -12, 2.1, 3.5, 0.9, 0.27));

    private static final List<AnimationModelPose> GENERATED = Arrays.stream(values())
            .filter(pose -> pose != REST)
            .toList();

    private final Transform right;
    private final Transform left;

    AnimationModelPose(Transform right, Transform left) {
        this.right = right;
        this.left = left;
    }

    public static Optional<AnimationModelPose> find(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_')));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static List<AnimationModelPose> generated() {
        return GENERATED;
    }

    public NamespacedKey itemModelKey(NamespacedKey base) {
        if (this == REST) {
            return base;
        }
        return new NamespacedKey(base.getNamespace(), base.getKey() + "_catanim_" + name().toLowerCase(Locale.ROOT));
    }

    public String modelJson(NamespacedKey parent) {
        if (this == REST) {
            throw new IllegalStateException("REST uses the original model");
        }
        return """
                {
                  "parent": "%s",
                  "display": {
                    "firstperson_righthand": %s,
                    "firstperson_lefthand": %s,
                    "thirdperson_righthand": %s,
                    "thirdperson_lefthand": %s
                  }
                }
                """.formatted(parent, right.json(), left.json(), right.thirdPersonJson(), left.thirdPersonJson());
    }

    private static Transform transform(double rx, double ry, double rz, double tx, double ty, double tz, double scale) {
        return new Transform(rx, ry, rz, tx, ty, tz, scale);
    }

    private record Transform(double rx, double ry, double rz, double tx, double ty, double tz, double scale) {
        String json() {
            return "{\"rotation\": [%s, %s, %s], \"translation\": [%s, %s, %s], \"scale\": [%s, %s, %s]}"
                    .formatted(n(rx), n(ry), n(rz), n(tx), n(ty), n(tz), n(scale), n(scale), n(scale));
        }

        String thirdPersonJson() {
            double thirdScale = Math.min(0.55, scale + 0.16);
            return "{\"rotation\": [%s, %s, %s], \"translation\": [%s, %s, %s], \"scale\": [%s, %s, %s]}"
                    .formatted(n(rx * 0.45), n(ry), n(rz * 0.55), n(tx * 0.35), n(ty * 0.45), n(tz),
                            n(thirdScale), n(thirdScale), n(thirdScale));
        }

        private String n(double value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
    }
}
