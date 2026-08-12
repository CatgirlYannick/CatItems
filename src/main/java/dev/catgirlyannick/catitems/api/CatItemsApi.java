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

    /** Plays a named YAML keyframe animation without consuming the held item. */
    boolean playUseAnimation(Player player, String animationId, int durationTicks);

    void stopUseAnimation(Player player);

    Collection<String> animations();

    Optional<Integer> animationDuration(String id);

    Collection<CatFeature> features();

    Optional<CatFeature> feature(String id);
}
