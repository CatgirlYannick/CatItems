package dev.catgirlyannick.catitems.animation;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Uses Paper's modern consumable data component through reflection so CatItems
 * still loads on the first 1.21 releases that did not expose this API yet.
 */
final class ArmAnimationBridge {
    private final Object consumableType;
    private final Method getData;
    private final Method hasData;
    private final Method setData;
    private final Method unsetData;
    private final Method newConsumable;
    private final Method buildComponent;
    private final Method consumeSeconds;
    private final Method animation;
    private final Method consumeParticles;
    private final Class<? extends Enum> useAnimationType;
    private final Map<AnimationKeyframe.ArmPose, Object> components;
    private final boolean supported;

    @SuppressWarnings("unchecked")
    ArmAnimationBridge() {
        Object resolvedType = null;
        Method resolvedGet = null;
        Method resolvedHas = null;
        Method resolvedSet = null;
        Method resolvedUnset = null;
        Method resolvedFactory = null;
        Class<?> resolvedBuilder = null;
        Method resolvedBuild = null;
        Method resolvedConsumeSeconds = null;
        Method resolvedAnimationMethod = null;
        Method resolvedConsumeParticles = null;
        Class<? extends Enum> resolvedAnimation = null;
        boolean resolved = false;
        try {
            Class<?> componentType = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
            Class<?> valuedType = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            Class<?> types = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            Class<?> consumable = Class.forName("io.papermc.paper.datacomponent.item.Consumable");
            resolvedBuilder = Class.forName("io.papermc.paper.datacomponent.item.Consumable$Builder");
            Class<?> componentBuilder = Class.forName("io.papermc.paper.datacomponent.DataComponentBuilder");
            resolvedAnimation = (Class<? extends Enum>) Class.forName(
                    "io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation");
            resolvedType = types.getField("CONSUMABLE").get(null);
            resolvedGet = ItemStack.class.getMethod("getData", valuedType);
            resolvedHas = ItemStack.class.getMethod("hasData", componentType);
            resolvedSet = ItemStack.class.getMethod("setData", valuedType, Object.class);
            resolvedUnset = ItemStack.class.getMethod("unsetData", componentType);
            resolvedFactory = consumable.getMethod("consumable");
            resolvedBuild = componentBuilder.getMethod("build");
            resolvedConsumeSeconds = resolvedBuilder.getMethod("consumeSeconds", float.class);
            resolvedAnimationMethod = resolvedBuilder.getMethod("animation", resolvedAnimation);
            resolvedConsumeParticles = resolvedBuilder.getMethod("hasConsumeParticles", boolean.class);
            resolved = true;
        } catch (ReflectiveOperationException ignored) {
            // Paper 1.21 through 1.21.3: the plugin remains loadable and uses its legacy fallback.
        }
        consumableType = resolvedType;
        getData = resolvedGet;
        hasData = resolvedHas;
        setData = resolvedSet;
        unsetData = resolvedUnset;
        newConsumable = resolvedFactory;
        buildComponent = resolvedBuild;
        consumeSeconds = resolvedConsumeSeconds;
        animation = resolvedAnimationMethod;
        consumeParticles = resolvedConsumeParticles;
        useAnimationType = resolvedAnimation;
        supported = resolved;
        components = resolved ? buildComponents() : Map.of();
    }

    boolean supported() {
        return supported;
    }

    ComponentSnapshot snapshot(ItemStack item) {
        if (!supported || item == null) {
            return ComponentSnapshot.NONE;
        }
        try {
            boolean present = (boolean) hasData.invoke(item, consumableType);
            return new ComponentSnapshot(present, present ? getData.invoke(item, consumableType) : null);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw failure("read", exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    void apply(ItemStack item, AnimationKeyframe.ArmPose pose) {
        if (!supported || item == null || !pose.isAnimated()) {
            return;
        }
        Object component = components.get(pose);
        if (component == null) {
            return;
        }
        try {
            setData.invoke(item, consumableType, component);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw failure("apply", exception);
        }
    }

    void restore(ItemStack item, ComponentSnapshot snapshot) {
        if (!supported || item == null || snapshot == null) {
            return;
        }
        try {
            if (snapshot.present()) {
                setData.invoke(item, consumableType, snapshot.value());
            } else {
                unsetData.invoke(item, consumableType);
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw failure("restore", exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<AnimationKeyframe.ArmPose, Object> buildComponents() {
        EnumMap<AnimationKeyframe.ArmPose, Object> result = new EnumMap<>(AnimationKeyframe.ArmPose.class);
        try {
            for (AnimationKeyframe.ArmPose pose : AnimationKeyframe.ArmPose.values()) {
                if (!pose.isAnimated()) {
                    continue;
                }
                Object builder = newConsumable.invoke(null);
                consumeSeconds.invoke(builder, 3600.0F);
                String nativePose = switch (pose) {
                    case FACE -> "TOOT_HORN";
                    case FACE_BOTH -> "SPYGLASS";
                    default -> pose.name().toUpperCase(Locale.ROOT);
                };
                Enum<?> nativeAnimation;
                try {
                    nativeAnimation = Enum.valueOf(useAnimationType, nativePose);
                } catch (IllegalArgumentException unavailableOnThisVersion) {
                    continue;
                }
                animation.invoke(builder, nativeAnimation);
                consumeParticles.invoke(builder, false);
                result.put(pose, buildComponent.invoke(builder));
            }
            return Map.copyOf(result);
        } catch (ReflectiveOperationException exception) {
            throw failure("prepare", exception);
        }
    }

    private IllegalStateException failure(String action, Exception exception) {
        Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : exception;
        return new IllegalStateException("Modern arm animation could not " + action + " its item component", cause);
    }

    record ComponentSnapshot(boolean present, Object value) {
        private static final ComponentSnapshot NONE = new ComponentSnapshot(false, null);
    }
}
