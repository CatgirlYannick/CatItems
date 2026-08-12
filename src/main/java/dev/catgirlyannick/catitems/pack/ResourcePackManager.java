package dev.catgirlyannick.catitems.pack;

import dev.catgirlyannick.catitems.api.CatItemDefinition;
import dev.catgirlyannick.catitems.service.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ResourcePackManager {
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("catitems-resource-pack".getBytes(StandardCharsets.UTF_8));

    private final JavaPlugin plugin;
    private final ResourcePackBuilder builder;
    private final ResourcePackServer server = new ResourcePackServer();
    private final MessageService messages;
    private volatile PackArtifact artifact;
    private volatile String downloadUrl;

    public ResourcePackManager(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.builder = new ResourcePackBuilder(plugin);
    }

    public synchronized PackArtifact rebuild(Collection<CatItemDefinition> definitions, boolean modernItemModels)
            throws IOException {
        FileConfiguration config = plugin.getConfig();
        PackArtifact built = builder.build(definitions, Bukkit.getMinecraftVersion(), modernItemModels);
        String delivery = config.getString("resource-pack.delivery", "self-host").toLowerCase(Locale.ROOT);
        server.stop();
        switch (delivery) {
            case "disabled" -> downloadUrl = null;
            case "external" -> downloadUrl = requireHttpUrl(
                    config.getString("resource-pack.external-url", ""), "resource-pack.external-url");
            case "self-host" -> {
                downloadUrl = requireHttpUrl(config.getString("resource-pack.self-host.public-url", ""),
                        "resource-pack.self-host.public-url");
                server.start(built, config);
            }
            default -> throw new IOException("Unknown resource-pack.delivery value: " + delivery);
        }
        artifact = built;
        return built;
    }

    public void send(Player player) {
        PackArtifact current = artifact;
        String currentUrl = downloadUrl;
        if (current == null || currentUrl == null) {
            throw new IllegalStateException("No distributable resource pack is available");
        }
        Component prompt = messages.raw(plugin.getConfig().getString("resource-pack.prompt",
                "<gold>CatItems requires its resource pack.</gold>"));
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        player.setResourcePack(PACK_ID, currentUrl, current.sha1(), prompt, required);
    }

    public Optional<PackArtifact> artifact() {
        return Optional.ofNullable(artifact);
    }

    public boolean canSend() {
        return artifact != null && downloadUrl != null;
    }

    public String downloadUrl() {
        return downloadUrl == null ? "disabled" : downloadUrl;
    }

    public boolean selfHosted() {
        return server.running();
    }

    public void stop() {
        server.stop();
    }

    private String requireHttpUrl(String value, String path) throws IOException {
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IOException(path + " must contain a complete HTTP(S) URL", exception);
        }
    }
}
