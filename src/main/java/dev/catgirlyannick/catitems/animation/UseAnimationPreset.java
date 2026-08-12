package dev.catgirlyannick.catitems.animation;

import java.util.Locale;

public enum UseAnimationPreset {
    SMOKE,
    SNORT,
    DRINK,
    EAT,
    INHALE,
    INJECT,
    RITUAL,
    SWALLOW;

    public static UseAnimationPreset parse(String value) {
        if (value == null || value.isBlank()) {
            return SWALLOW;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "VAPE", "VAPOR", "VAPOUR" -> INHALE;
            case "SMOKING" -> SMOKE;
            case "SNIFF" -> SNORT;
            case "SHOT", "SYRINGE" -> INJECT;
            case "FOOD" -> EAT;
            case "PILL", "TABLET", "GENERIC" -> SWALLOW;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield SWALLOW;
                }
            }
        };
    }
}
