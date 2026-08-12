package dev.catgirlyannick.catitems.animation;

import java.util.List;

public record UseAnimationDefinition(String id, int durationTicks, boolean lockMovement,
                                     List<AnimationKeyframe> keyframes) {
    public UseAnimationDefinition {
        keyframes = List.copyOf(keyframes);
    }
}
