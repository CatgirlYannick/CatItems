package dev.catgirlyannick.catitems.listener;

import dev.catgirlyannick.catitems.pack.ResourcePackManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourcePackListener implements Listener {
    private final JavaPlugin plugin;
    private final ResourcePackManager packManager;

    public ResourcePackListener(JavaPlugin plugin, ResourcePackManager packManager) {
        this.plugin = plugin;
        this.packManager = packManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.send-on-join", true) || !packManager.canSend()) {
            return;
        }
        long delay = Math.max(1L, plugin.getConfig().getLong("resource-pack.join-delay-ticks", 30L));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline() && packManager.canSend()) {
                packManager.send(event.getPlayer());
            }
        }, delay);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        if (plugin.getConfig().getBoolean("logging.pack-status", true)) {
            plugin.getLogger().info("Resource-pack status for " + event.getPlayer().getName() + ": " + event.getStatus());
        }
    }
}
