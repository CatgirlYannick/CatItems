package dev.catgirlyannick.catitems.pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ResourcePackServer {
    private HttpServer server;
    private ExecutorService executor;
    private volatile PackArtifact artifact;

    public synchronized void start(PackArtifact artifact, FileConfiguration config) throws IOException {
        stop();
        this.artifact = artifact;
        String bindAddress = config.getString("resource-pack.self-host.bind-address", "0.0.0.0");
        int port = config.getInt("resource-pack.self-host.port", 8164);
        String path = normalizePath(config.getString("resource-pack.self-host.path", "/catitems-pack.zip"));
        server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.createContext(path, this::handle);
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CatItems-PackServer");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
    }

    public synchronized void update(PackArtifact artifact) {
        this.artifact = artifact;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public boolean running() {
        return server != null;
    }

    private void handle(HttpExchange exchange) throws IOException {
        PackArtifact current = artifact;
        String method = exchange.getRequestMethod();
        if (current == null || !Files.isRegularFile(current.file())) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
            return;
        }
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Allow", "GET, HEAD");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("ETag", "\"" + current.sha1Hex() + "\"");
        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Content-Length", Long.toString(current.size()));
            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(200, current.size());
            try (var body = exchange.getResponseBody()) {
                Files.copy(current.file(), body);
            }
        }
        exchange.close();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/catitems-pack.zip";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
