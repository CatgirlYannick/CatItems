package dev.catgirlyannick.catitems.animation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UseAnimationService {
    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> active = new HashMap<>();

    public UseAnimationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean play(Player player, String presetId, int requestedDurationTicks) {
        if (!plugin.getConfig().getBoolean("animations.enabled", true) || player == null || !player.isOnline()) {
            return false;
        }
        int minimum = clamp(plugin.getConfig().getInt("animations.min-duration-ticks", 12), 4, 200);
        int maximum = clamp(plugin.getConfig().getInt("animations.max-duration-ticks", 100), minimum, 400);
        int duration = clamp(requestedDurationTicks, minimum, maximum);
        UseAnimationPreset preset = UseAnimationPreset.parse(presetId);
        stop(player);

        BukkitRunnable sequence = new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                if (!player.isOnline() || elapsed > duration) {
                    finish(player.getUniqueId());
                    cancel();
                    return;
                }
                frame(player, preset, elapsed, duration);
                elapsed += 2;
            }
        };
        active.put(player.getUniqueId(), sequence.runTaskTimer(plugin, 0L, 2L));
        return true;
    }

    public void stop(Player player) {
        if (player != null) {
            stop(player.getUniqueId());
        }
    }

    public void shutdown() {
        active.values().forEach(BukkitTask::cancel);
        active.clear();
    }

    private void frame(Player player, UseAnimationPreset preset, int elapsed, int duration) {
        if (elapsed == 0) {
            playStartSound(player, preset);
        }
        int swingInterval = switch (preset) {
            case DRINK, EAT, INJECT -> 6;
            case SNORT, INHALE, SWALLOW -> 8;
            case SMOKE, RITUAL -> 10;
        };
        if (elapsed % swingInterval == 0) {
            player.swingMainHand();
        }
        if (elapsed % 4 == 0) {
            spawnParticles(player, preset);
        }
        if (elapsed >= duration) {
            playFinishSound(player, preset);
        }
    }

    private void spawnParticles(Player player, UseAnimationPreset preset) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location focus = eye.clone().add(direction.multiply(0.55)).add(0.0, -0.18, 0.0);
        Particle particle = switch (preset) {
            case SMOKE -> Particle.CAMPFIRE_COSY_SMOKE;
            case SNORT, INHALE -> Particle.CLOUD;
            case DRINK -> Particle.SPLASH;
            case EAT, SWALLOW -> Particle.ENCHANTED_HIT;
            case INJECT -> Particle.ELECTRIC_SPARK;
            case RITUAL -> Particle.WITCH;
        };
        int count = preset == UseAnimationPreset.RITUAL ? 3 : 2;
        player.getWorld().spawnParticle(particle, focus, count, 0.06, 0.05, 0.06, 0.005);
    }

    private void playStartSound(Player player, UseAnimationPreset preset) {
        Sound sound = switch (preset) {
            case SMOKE -> Sound.BLOCK_CAMPFIRE_CRACKLE;
            case SNORT, INHALE -> Sound.ENTITY_PLAYER_BREATH;
            case DRINK -> Sound.ITEM_BOTTLE_FILL;
            case EAT -> Sound.ENTITY_GENERIC_EAT;
            case INJECT -> Sound.ITEM_TRIDENT_RETURN;
            case RITUAL -> Sound.BLOCK_BREWING_STAND_BREW;
            case SWALLOW -> Sound.ENTITY_GENERIC_DRINK;
        };
        player.playSound(player.getLocation(), sound, 0.65f, preset == UseAnimationPreset.RITUAL ? 0.75f : 1.05f);
    }

    private void playFinishSound(Player player, UseAnimationPreset preset) {
        Sound sound = switch (preset) {
            case SMOKE, SNORT, INHALE -> Sound.ENTITY_PLAYER_BREATH;
            case DRINK, SWALLOW -> Sound.ENTITY_GENERIC_DRINK;
            case EAT -> Sound.ENTITY_PLAYER_BURP;
            case INJECT -> Sound.ITEM_BOTTLE_EMPTY;
            case RITUAL -> Sound.ENTITY_ENDERMAN_AMBIENT;
        };
        player.playSound(player.getLocation(), sound, 0.55f, preset == UseAnimationPreset.RITUAL ? 1.35f : 0.9f);
    }

    private void stop(UUID playerId) {
        BukkitTask previous = active.remove(playerId);
        if (previous != null) {
            previous.cancel();
        }
    }

    private void finish(UUID playerId) {
        active.remove(playerId);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
