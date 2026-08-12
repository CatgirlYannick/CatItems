package dev.catgirlyannick.catitems.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class MessageService {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Component> staticComponents = new HashMap<>();
    private YamlConfiguration messages;

    public MessageService(YamlConfiguration messages) {
        this.messages = messages;
    }

    public void reload(YamlConfiguration messages) {
        this.messages = messages;
        staticComponents.clear();
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(component(path, placeholders));
    }

    public Component component(String path, Map<String, String> placeholders) {
        if (placeholders.isEmpty()) {
            return staticComponents.computeIfAbsent(path, this::staticComponent);
        }
        return render(path, placeholders);
    }

    private Component staticComponent(String path) {
        return render(path, Map.of());
    }

    private Component render(String path, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "<dark_gray>[<aqua>CatItems</aqua>]</dark_gray> ");
        String value = messages.getString(path, "<red>Fehlender Text: " + path + "</red>");
        String formatted = SmallCapsFormatter.formatTemplate(prefix + value);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}",
                    escape(SmallCapsFormatter.formatValue(entry.getValue())));
        }
        return miniMessage.deserialize(formatted);
    }

    public Component raw(String text) {
        return miniMessage.deserialize(SmallCapsFormatter.formatTemplate(text));
    }

    private String escape(String value) {
        return value.replace("<", "\\<").replace(">", "\\>");
    }
}
