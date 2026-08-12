package dev.catgirlyannick.catitems.animation;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationModelPoseTest {
    @Test
    void generatedPoseUsesDedicatedModelKeyAndParent() {
        NamespacedKey base = new NamespacedKey("catdrugs", "item/joint");
        NamespacedKey animated = AnimationModelPose.MOUTH.itemModelKey(base);

        assertEquals("catdrugs:item/joint_catanim_mouth", animated.toString());
        String json = AnimationModelPose.MOUTH.modelJson(base);
        assertTrue(json.contains("\"parent\": \"catdrugs:item/joint\""));
        assertTrue(json.contains("firstperson_righthand"));
        assertTrue(json.contains("thirdperson_lefthand"));
    }

    @Test
    void restDoesNotGenerateASecondModel() {
        assertEquals(15, AnimationModelPose.generated().size());
        assertTrue(AnimationModelPose.generated().stream().noneMatch(pose -> pose == AnimationModelPose.REST));
    }
}
