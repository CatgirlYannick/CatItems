package dev.catgirlyannick.catitems.animation;

import org.bukkit.Particle;

public record AnimationKeyframe(
        int tick,
        AnimationModelPose pose,
        HandMotion handMotion,
        UseAction useAction,
        ArmPose armPose,
        BodyPose bodyPose,
        Float bodyYaw,
        Easing easing,
        Particle particle,
        ParticleAnchor particleAnchor,
        int particleCount,
        double particleSpread,
        double particleSpeed,
        String sound,
        float volume,
        float pitch
) {
    public enum HandMotion { NONE, MAIN, OFF, BOTH }
    public enum UseAction { NONE, START, STOP }
    public enum ArmPose {
        KEEP, REST, FACE, FACE_BOTH, EAT, DRINK, BLOCK, BOW, TRIDENT, CROSSBOW, SPYGLASS, TOOT_HORN,
        BRUSH, BUNDLE, SPEAR;

        public boolean isAnimated() {
            return this != KEEP && this != REST;
        }
    }
    public enum BodyPose { KEEP, STANDING, CROUCHING, SWIMMING, FALL_FLYING, SPIN_ATTACK }
    public enum Easing { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
    public enum ParticleAnchor { MOUTH, HAND, EYE, FEET }

    public boolean hasBodyMotion() {
        return bodyPose != BodyPose.KEEP || bodyYaw != null;
    }

    public boolean hasArmMotion() {
        return armPose.isAnimated();
    }
}
