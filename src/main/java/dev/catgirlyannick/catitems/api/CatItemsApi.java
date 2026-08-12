package dev.catgirlyannick.catitems.api;

import dev.catgirlyannick.catitems.feature.CatFeature;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public interface CatItemsApi {
    Optional<CatItemDefinition> find(String id);

    Collection<CatItemDefinition> items();

    ItemStack create(String id, int amount);

    Optional<String> identify(ItemStack itemStack);

    /** Plays a reusable first- and third-person use animation without consuming the held item. */
    boolean playUseAnimation(Player player, String preset, int durationTicks);

    void stopUseAnimation(Player player);

    Collection<CatFeature> features();

    Optional<CatFeature> feature(String id);
}
