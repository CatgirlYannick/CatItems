package dev.catgirlyannick.catitems.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UseAnimationPresetTest {
    @Test
    void acceptsPublicPresetIdsAndUsefulAliases() {
        assertEquals(UseAnimationPreset.SMOKE, UseAnimationPreset.parse("smoke"));
        assertEquals(UseAnimationPreset.INHALE, UseAnimationPreset.parse("vape"));
        assertEquals(UseAnimationPreset.INJECT, UseAnimationPreset.parse("syringe"));
        assertEquals(UseAnimationPreset.SWALLOW, UseAnimationPreset.parse("unknown-addon-value"));
    }
}
