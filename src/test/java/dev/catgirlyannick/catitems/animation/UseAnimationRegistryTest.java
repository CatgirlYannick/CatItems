package dev.catgirlyannick.catitems.animation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseAnimationRegistryTest {
    @Test
    void bundledAnimationsAreValidSelfAuthoredTimelines() {
        InputStream input = getClass().getClassLoader().getResourceAsStream("animations.yml");
        assertNotNull(input);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        ConfigurationSection root = yaml.getConfigurationSection("animations");
        assertNotNull(root);
        assertEquals(5, yaml.getInt("config-version"));
        assertEquals(List.of("drink_bottle", "eat_edible", "inhale_vape", "inject_arm", "ritual_sway",
                        "smoke_joint", "smoke_pipe", "smoke_stimulant", "snort_line", "swallow_pill"),
                root.getKeys(false).stream().sorted().toList());

        for (String id : root.getKeys(false)) {
            UseAnimationDefinition definition = UseAnimationRegistry.parse(id, root.getConfigurationSection(id));
            assertTrue(definition.keyframes().size() >= 7, id);
            assertEquals(AnimationModelPose.REST,
                    definition.keyframes().get(definition.keyframes().size() - 1).pose(), id);
            assertTrue(definition.keyframes().stream().anyMatch(frame -> frame.pose() != AnimationModelPose.REST), id);
            assertTrue(definition.keyframes().stream().anyMatch(AnimationKeyframe::hasArmMotion), id);
            assertTrue(definition.lockMovement(), id);
        }
        UseAnimationDefinition drink = UseAnimationRegistry.parse(
                "drink_bottle", root.getConfigurationSection("drink_bottle"));
        assertEquals("minecraft:item.bottle.fill", drink.keyframes().getFirst().sound());
        assertTrue(drink.keyframes().stream()
                .anyMatch(frame -> frame.armPose() == AnimationKeyframe.ArmPose.DRINK));

        UseAnimationDefinition joint = UseAnimationRegistry.parse(
                "smoke_joint", root.getConfigurationSection("smoke_joint"));
        assertTrue(joint.keyframes().stream().anyMatch(frame -> frame.pose() == AnimationModelPose.APPROACH));
        assertTrue(joint.keyframes().stream().anyMatch(frame -> frame.pose() == AnimationModelPose.CONTACT));
        assertTrue(joint.keyframes().stream().anyMatch(frame -> frame.pose() == AnimationModelPose.RELEASE));
        assertTrue(joint.keyframes().stream().anyMatch(frame -> frame.armPose() == AnimationKeyframe.ArmPose.KEEP));
        long nativeArmStarts = joint.keyframes().stream()
                .filter(frame -> frame.armPose() == AnimationKeyframe.ArmPose.FACE)
                .count();
        assertEquals(2, nativeArmStarts);

        UseAnimationDefinition snort = UseAnimationRegistry.parse(
                "snort_line", root.getConfigurationSection("snort_line"));
        assertTrue(snort.keyframes().stream().anyMatch(frame -> frame.armPose() == AnimationKeyframe.ArmPose.FACE_BOTH));
    }

    @Test
    void identicalOrKeepArmPosesDoNotRestartTheNativeAnimation() {
        assertTrue(!UseAnimationService.shouldChangeArmPose(
                AnimationKeyframe.ArmPose.FACE, AnimationKeyframe.ArmPose.FACE));
        assertTrue(!UseAnimationService.shouldChangeArmPose(
                AnimationKeyframe.ArmPose.FACE, AnimationKeyframe.ArmPose.KEEP));
        assertTrue(UseAnimationService.shouldChangeArmPose(
                AnimationKeyframe.ArmPose.FACE, AnimationKeyframe.ArmPose.REST));
    }

    @Test
    void runtimeTimelineUsesDirectTickBucketsWithoutDroppingFrames() {
        InputStream input = getClass().getClassLoader().getResourceAsStream("animations.yml");
        assertNotNull(input);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        UseAnimationDefinition definition = UseAnimationRegistry.parse(
                "smoke_joint", yaml.getConfigurationSection("animations.smoke_joint"));

        AnimationKeyframe[][] timeline = UseAnimationService.scaleTimeline(
                definition, definition.durationTicks());
        assertEquals(definition.durationTicks() + 1, timeline.length);
        assertEquals(definition.keyframes().size(),
                java.util.Arrays.stream(timeline).mapToInt(frames -> frames.length).sum());
        for (AnimationKeyframe frame : definition.keyframes()) {
            assertTrue(java.util.Arrays.asList(timeline[frame.tick()]).contains(frame));
        }
    }

    @Test
    void generatedModelPoseListIsAnImmutableCachedSnapshot() {
        assertSame(AnimationModelPose.generated(), AnimationModelPose.generated());
        assertTrue(!AnimationModelPose.generated().contains(AnimationModelPose.REST));
    }

}
