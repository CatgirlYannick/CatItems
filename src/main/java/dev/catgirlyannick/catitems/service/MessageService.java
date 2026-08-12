package dev.catgirlyannick.catitems.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public final class MessageService {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;

    public MessageService(YamlConfiguration messages) {
        this.messages = messages;
    }

    public void reload(YamlConfiguration messages) {
        this.messages = messages;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(component(path, placeholders));
    }

    public Component component(String path, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "<dark_gray>[<aqua>CatItems</aqua>]</dark_gray> ");
        String value = messages.getString(path, "<red>Fehlender Text: " + path + "</red>");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", escape(entry.getValue()));
        }
        return miniMessage.deserialize(prefix + value);
    }

    public Component raw(String text) {
        return miniMessage.deserialize(text);
    }

    private String escape(String value) {
        return value.replace("<", "\\<").replace(">", "\\>");
    }
}
