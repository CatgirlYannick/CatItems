package dev.catgirlyannick.catitems.animation;

import dev.catgirlyannick.catitems.api.CatItemDefinition;
import dev.catgirlyannick.catitems.config.ItemRegistry;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

public final class UseAnimationService implements Listener {
    private final JavaPlugin plugin;
    private final ItemRegistry items;
    private final UseAnimationRegistry animations;
    private final NamespacedKey identityKey;
    private final Method setItemModel;
    private final Method getItemModel;
    private final ArmAnimationBridge armAnimations;
    private final Map<UUID, RunningAnimation> active = new HashMap<>();
    private BukkitTask ticker;

    public UseAnimationService(JavaPlugin plugin, ItemRegistry items, UseAnimationRegistry animations) {
        this.plugin = plugin;
        this.items = items;
        this.animations = animations;
        this.identityKey = new NamespacedKey(plugin, "item_id");
        this.setItemModel = method("setItemModel", NamespacedKey.class);
        this.getItemModel = method("getItemModel");
        this.armAnimations = new ArmAnimationBridge();
    }

    public boolean play(Player player, String animationId, int requestedDurationTicks) {
        if (!plugin.getConfig().getBoolean("animations.enabled", true) || player == null || !player.isOnline()) {
            return false;
        }
        UseAnimationDefinition definition = animations.find(animationId).orElse(null);
        if (definition == null) {
            return false;
        }
        int minimum = clamp(plugin.getConfig().getInt("animations.min-duration-ticks", 12), 4, 200);
        int maximum = clamp(plugin.getConfig().getInt("animations.max-duration-ticks", 120), minimum, 400);
        int duration = clamp(requestedDurationTicks > 0 ? requestedDurationTicks : definition.durationTicks(), minimum, maximum);
        stop(player);

        ItemSnapshot snapshot = snapshot(player.getInventory().getItemInMainHand(),
                player.getInventory().getHeldItemSlot());
        Location start = player.getLocation();
        RunningAnimation running = new RunningAnimation(player, snapshot, definition.lockMovement(), start,
                player.getBodyYaw(), player.getPose(), player.hasFixedPose(), player.isSneaking(), duration,
                scaleTimeline(definition, duration), motionTimeline(definition, duration));
        active.put(player.getUniqueId(), running);
        ensureTicker();
        return true;
    }

    public void stop(Player player) {
        if (player == null) {
            return;
        }
        RunningAnimation previous = active.remove(player.getUniqueId());
        if (previous != null) {
            restore(player, previous);
        }
        stopTickerWhenIdle();
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        for (Map.Entry<UUID, RunningAnimation> entry : active.entrySet()) {
            restore(entry.getValue().player, entry.getValue());
        }
        active.clear();
    }

    public int registeredAnimations() {
        return animations.definitions().size();
    }

    public Collection<String> animationIds() {
        return animations.definitions().stream().map(UseAnimationDefinition::id).sorted().toList();
    }

    public boolean supportsArmPoses() {
        return armAnimations.supported();
    }

    public Optional<Integer> duration(String id) {
        return animations.find(id).map(UseAnimationDefinition::durationTicks);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        RunningAnimation running = active.get(event.getPlayer().getUniqueId());
        Location destination = event.getTo();
        if (running == null || !running.lockMovement || destination == null
                || event instanceof PlayerTeleportEvent || !moved(event.getFrom(), destination)) {
            return;
        }
        Location locked = running.anchor.clone();
        locked.setYaw(destination.getYaw());
        locked.setPitch(destination.getPitch());
        event.setTo(locked);
    }

    private void apply(Player player, RunningAnimation running, AnimationKeyframe frame) {
        applyPose(player, running, frame.pose());
        switch (frame.handMotion()) {
            case MAIN -> player.swingMainHand();
            case OFF -> player.swingOffHand();
            case BOTH -> {
                player.swingMainHand();
                player.swingOffHand();
            }
            case NONE -> { }
        }
        applyArmPose(player, running, frame.armPose());
        switch (frame.useAction()) {
            case START -> {
                if (frame.armPose() == AnimationKeyframe.ArmPose.KEEP) {
                    player.startUsingItem(EquipmentSlot.HAND);
                    running.startedUse = true;
                }
            }
            case STOP -> {
                if (running.startedUse) {
                    player.clearActiveItem();
                    running.startedUse = false;
                }
            }
            case NONE -> { }
        }
        if (frame.sound() != null) {
            player.playSound(player.getLocation(), frame.sound(), frame.volume(), frame.pitch());
        }
        if (frame.particle() != null && frame.particleCount() > 0) {
            Location location = particleLocation(player, frame.particleAnchor());
            double spread = frame.particleSpread();
            player.getWorld().spawnParticle(frame.particle(), location, frame.particleCount(),
                    spread, spread, spread, frame.particleSpeed());
        }
    }

    private void applyBodyMotion(Player player, RunningAnimation running) {
        BodyMotion motion = running.motion[running.elapsed];
        if (player.getPose() != motion.pose || !player.hasFixedPose()) {
            player.setPose(motion.pose, true);
        }
        float targetYaw = normalizeYaw(running.anchorBodyYaw + motion.bodyYaw);
        if (Math.abs(player.getBodyYaw() - targetYaw) > 0.01F) {
            player.setBodyYaw(targetYaw);
        }
    }

    private void applyArmPose(Player player, RunningAnimation running, AnimationKeyframe.ArmPose pose) {
        if (!shouldChangeArmPose(running.armPose, pose)) {
            return;
        }
        if (running.startedUse) {
            player.clearActiveItem();
            running.startedUse = false;
        }
        ItemStack current = animatedItem(player, running.snapshot);
        if (current == null) {
            return;
        }
        if (pose == AnimationKeyframe.ArmPose.REST) {
            armAnimations.restore(current, running.snapshot.originalConsumable);
        } else {
            armAnimations.apply(current, pose);
        }
        running.armPose = pose;
        player.getInventory().setItem(running.snapshot.slot, current);
        if (pose.isAnimated() && armAnimations.supported()
                && player.getInventory().getHeldItemSlot() == running.snapshot.slot) {
            player.startUsingItem(EquipmentSlot.HAND);
            running.startedUse = true;
        }
    }

    private void maintainArmPose(Player player, RunningAnimation running) {
        if (!armAnimations.supported() || !running.armPose.isAnimated() || running.snapshot == null
                || player.getInventory().getHeldItemSlot() != running.snapshot.slot
                || player.hasActiveItem() || animatedItem(player, running.snapshot) == null) {
            return;
        }
        player.startUsingItem(EquipmentSlot.HAND);
        running.startedUse = true;
    }

    private Location particleLocation(Player player, AnimationKeyframe.ParticleAnchor anchor) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        return switch (anchor) {
            case EYE -> eye;
            case MOUTH -> eye.clone().add(direction.multiply(0.42)).add(0.0, -0.16, 0.0);
            case HAND -> eye.clone().add(direction.multiply(0.65)).add(0.28, -0.42, 0.0);
            case FEET -> player.getLocation().add(0.0, 0.15, 0.0);
        };
    }

    private void applyPose(Player player, RunningAnimation running, AnimationModelPose pose) {
        ItemSnapshot snapshot = running.snapshot;
        if (setItemModel == null || snapshot == null || pose == running.itemPose) {
            return;
        }
        ItemStack current = animatedItem(player, snapshot);
        if (current == null) {
            return;
        }
        CatItemDefinition definition = items.find(snapshot.itemId).orElse(null);
        if (definition == null) {
            return;
        }
        NamespacedKey target = pose == AnimationModelPose.REST
                ? snapshot.originalModel
                : pose.itemModelKey(definition.itemModel());
        setModel(current, target);
        player.getInventory().setItem(snapshot.slot, current);
        running.itemPose = pose;
    }

    private void restore(Player player, RunningAnimation running) {
        if (running.startedUse) {
            player.clearActiveItem();
            running.startedUse = false;
        }
        if (running.snapshot != null) {
            ItemStack current = animatedItem(player, running.snapshot);
            if (current != null) {
                armAnimations.restore(current, running.snapshot.originalConsumable);
                player.getInventory().setItem(running.snapshot.slot, current);
            }
            applyPose(player, running, AnimationModelPose.REST);
        }
        player.setPose(running.originalPose, running.originalFixedPose);
        player.setSneaking(running.originalSneaking);
        player.setBodyYaw(running.anchorBodyYaw);
    }

    private ItemSnapshot snapshot(ItemStack item, int slot) {
        if (setItemModel == null || getItemModel == null || item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        String itemId = item.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
        if (itemId == null || items.find(itemId).isEmpty()) {
            return null;
        }
        NamespacedKey model = getModel(item.getItemMeta()).orElse(items.find(itemId).orElseThrow().itemModel());
        return new ItemSnapshot(itemId, model, slot, armAnimations.snapshot(item));
    }

    private ItemStack animatedItem(Player player, ItemSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        ItemStack item = player.getInventory().getItem(snapshot.slot);
        return sameItem(item, snapshot.itemId) ? item : null;
    }

    private boolean sameItem(ItemStack item, String itemId) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return itemId.equals(item.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING));
    }

    private void setModel(ItemStack item, NamespacedKey model) {
        ItemMeta meta = item.getItemMeta();
        try {
            setItemModel.invoke(meta, model);
            item.setItemMeta(meta);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Animated item model could not be applied", exception);
        }
    }

    private Optional<NamespacedKey> getModel(ItemMeta meta) {
        try {
            Object value = getItemModel.invoke(meta);
            return value instanceof NamespacedKey key ? Optional.of(key) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return Optional.empty();
        }
    }

    static AnimationKeyframe[][] scaleTimeline(UseAnimationDefinition definition, int duration) {
        @SuppressWarnings("unchecked")
        List<AnimationKeyframe>[] grouped = new List[duration + 1];
        for (AnimationKeyframe frame : definition.keyframes()) {
            int runtimeTick = (int) Math.round((double) frame.tick() * duration / definition.durationTicks());
            runtimeTick = clamp(runtimeTick, 0, duration);
            List<AnimationKeyframe> frames = grouped[runtimeTick];
            if (frames == null) {
                frames = new ArrayList<>();
                grouped[runtimeTick] = frames;
            }
            frames.add(frame);
        }
        AnimationKeyframe[][] timeline = new AnimationKeyframe[duration + 1][];
        for (int tick = 0; tick <= duration; tick++) {
            timeline[tick] = grouped[tick] == null
                    ? EMPTY_FRAMES
                    : grouped[tick].toArray(AnimationKeyframe[]::new);
        }
        return timeline;
    }

    private BodyMotion[] motionTimeline(UseAnimationDefinition definition, int duration) {
        List<MotionKeyframe> result = new ArrayList<>();
        AnimationKeyframe.BodyPose pose = AnimationKeyframe.BodyPose.STANDING;
        float bodyYaw = 0.0F;
        result.add(new MotionKeyframe(0, pose, bodyYaw, AnimationKeyframe.Easing.LINEAR));
        for (AnimationKeyframe frame : definition.keyframes()) {
            if (!frame.hasBodyMotion()) {
                continue;
            }
            if (frame.bodyPose() != AnimationKeyframe.BodyPose.KEEP) {
                pose = frame.bodyPose();
            }
            bodyYaw = frame.bodyYaw() == null ? bodyYaw : frame.bodyYaw();
            int runtimeTick = clamp((int) Math.round((double) frame.tick() * duration / definition.durationTicks()),
                    0, duration);
            MotionKeyframe keyframe = new MotionKeyframe(runtimeTick, pose, bodyYaw, frame.easing());
            if (!result.isEmpty() && result.getLast().tick == runtimeTick) {
                result.set(result.size() - 1, keyframe);
            } else {
                result.add(keyframe);
            }
        }
        BodyMotion[] timeline = new BodyMotion[duration + 1];
        int nextIndex = 1;
        MotionKeyframe before = result.getFirst();
        MotionKeyframe after = result.getLast();
        for (int tick = 0; tick <= duration; tick++) {
            while (nextIndex < result.size() && result.get(nextIndex).tick <= tick) {
                before = result.get(nextIndex++);
            }
            after = nextIndex < result.size() ? result.get(nextIndex) : before;
            double progress = after.tick == before.tick
                    ? 1.0
                    : clamp((double) (tick - before.tick) / (after.tick - before.tick), 0.0, 1.0);
            progress = ease(progress, after.easing);
            timeline[tick] = new BodyMotion(toBukkitPose(before.pose), lerp(before.bodyYaw, after.bodyYaw, progress));
        }
        return timeline;
    }

    private Pose toBukkitPose(AnimationKeyframe.BodyPose pose) {
        return switch (pose) {
            case CROUCHING -> Pose.SNEAKING;
            case SWIMMING -> Pose.SWIMMING;
            case FALL_FLYING -> Pose.FALL_FLYING;
            case SPIN_ATTACK -> Pose.SPIN_ATTACK;
            case KEEP, STANDING -> Pose.STANDING;
        };
    }

    private static double ease(double value, AnimationKeyframe.Easing easing) {
        return switch (easing) {
            case LINEAR -> value;
            case EASE_IN -> value * value;
            case EASE_OUT -> 1.0 - (1.0 - value) * (1.0 - value);
            case EASE_IN_OUT -> value < 0.5
                    ? 2.0 * value * value
                    : 1.0 - Math.pow(-2.0 * value + 2.0, 2.0) / 2.0;
        };
    }

    private static float lerp(float start, float end, double progress) {
        return (float) (start + (end - start) * progress);
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        return normalized > 180.0F ? normalized - 360.0F : normalized < -180.0F ? normalized + 360.0F : normalized;
    }

    static boolean shouldChangeArmPose(AnimationKeyframe.ArmPose current,
                                       AnimationKeyframe.ArmPose requested) {
        return requested != AnimationKeyframe.ArmPose.KEEP && requested != current;
    }

    private static boolean moved(Location from, Location to) {
        return from.getWorld() != to.getWorld()
                || Math.abs(from.getX() - to.getX()) > 0.001
                || Math.abs(from.getY() - to.getY()) > 0.001
                || Math.abs(from.getZ() - to.getZ()) > 0.001;
    }

    private Method method(String name, Class<?>... types) {
        try {
            return ItemMeta.class.getMethod(name, types);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void ensureTicker() {
        if (ticker == null) {
            ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
        }
    }

    private void tick() {
        Iterator<Map.Entry<UUID, RunningAnimation>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RunningAnimation> entry = iterator.next();
            RunningAnimation running = entry.getValue();
            Player player = running.player;
            if (!player.isOnline()) {
                iterator.remove();
                restore(player, running);
                continue;
            }
            applyBodyMotion(player, running);
            for (AnimationKeyframe frame : running.timeline[running.elapsed]) {
                apply(player, running, frame);
            }
            maintainArmPose(player, running);
            if (running.elapsed >= running.duration) {
                iterator.remove();
                restore(player, running);
            } else {
                running.elapsed++;
            }
        }
        stopTickerWhenIdle();
    }

    private void stopTickerWhenIdle() {
        if (active.isEmpty() && ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private record ItemSnapshot(String itemId, NamespacedKey originalModel, int slot,
                                ArmAnimationBridge.ComponentSnapshot originalConsumable) { }

    private record MotionKeyframe(int tick, AnimationKeyframe.BodyPose pose, float bodyYaw,
                                  AnimationKeyframe.Easing easing) { }

    private record BodyMotion(Pose pose, float bodyYaw) { }

    private static final AnimationKeyframe[] EMPTY_FRAMES = new AnimationKeyframe[0];

    private static final class RunningAnimation {
        private final Player player;
        private final ItemSnapshot snapshot;
        private final boolean lockMovement;
        private final Location anchor;
        private final float anchorBodyYaw;
        private final Pose originalPose;
        private final boolean originalFixedPose;
        private final boolean originalSneaking;
        private final int duration;
        private final AnimationKeyframe[][] timeline;
        private final BodyMotion[] motion;
        private int elapsed;
        private boolean startedUse;
        private AnimationKeyframe.ArmPose armPose = AnimationKeyframe.ArmPose.REST;
        private AnimationModelPose itemPose = AnimationModelPose.REST;

        private RunningAnimation(Player player, ItemSnapshot snapshot, boolean lockMovement, Location anchor,
                                 float anchorBodyYaw, Pose originalPose, boolean originalFixedPose,
                                 boolean originalSneaking, int duration, AnimationKeyframe[][] timeline,
                                 BodyMotion[] motion) {
            this.player = player;
            this.snapshot = snapshot;
            this.lockMovement = lockMovement;
            this.anchor = anchor.clone();
            this.anchorBodyYaw = anchorBodyYaw;
            this.originalPose = originalPose;
            this.originalFixedPose = originalFixedPose;
            this.originalSneaking = originalSneaking;
            this.duration = duration;
            this.timeline = timeline;
            this.motion = motion;
        }
    }
}
