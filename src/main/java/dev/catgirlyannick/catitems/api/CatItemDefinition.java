package dev.catgirlyannick.catitems.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

public record CatItemDefinition(
        NamespacedKey id,
        Material material,
        String displayName,
        List<String> lore,
        int customModelData,
        NamespacedKey itemModel,
        NamespacedKey texture,
        boolean glint,
        String permission
) {
    public CatItemDefinition {
        lore = List.copyOf(lore);
        permission = permission == null ? "" : permission;
    }
}
