package dev.catgirlyannick.catitems.api;

import dev.catgirlyannick.catitems.feature.CatFeature;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

public interface CatItemsApi {
    Optional<CatItemDefinition> find(String id);

    Collection<CatItemDefinition> items();

    ItemStack create(String id, int amount);

    Optional<String> identify(ItemStack itemStack);

    Collection<CatFeature> features();

    Optional<CatFeature> feature(String id);
}
