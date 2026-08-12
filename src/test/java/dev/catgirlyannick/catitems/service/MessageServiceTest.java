package dev.catgirlyannick.catitems.service;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class MessageServiceTest {
    @Test
    void cachesStaticMessagesAndInvalidatesThemOnReload() {
        YamlConfiguration firstConfig = new YamlConfiguration();
        firstConfig.set("prefix", "<gray>[CatItems]</gray> ");
        firstConfig.set("status", "<green>Ready</green>");
        MessageService service = new MessageService(firstConfig);

        Component first = service.component("status", Map.of());
        assertSame(first, service.component("status", Map.of()));

        YamlConfiguration secondConfig = new YamlConfiguration();
        secondConfig.set("prefix", "<gray>[CatItems]</gray> ");
        secondConfig.set("status", "<yellow>Reloaded</yellow>");
        service.reload(secondConfig);
        assertNotSame(first, service.component("status", Map.of()));
    }
}
