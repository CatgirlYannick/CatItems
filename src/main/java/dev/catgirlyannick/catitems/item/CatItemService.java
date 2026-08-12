package dev.catgirlyannick.catitems.item;

import dev.catgirlyannick.catitems.api.CatItemDefinition;
import dev.catgirlyannick.catitems.api.CatItemsApi;
import dev.catgirlyannick.catitems.config.ItemRegistry;
import dev.catgirlyannick.catitems.feature.CatFeature;
import dev.catgirlyannick.catitems.feature.FeatureCatalog;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

public final class CatItemService implements CatItemsApi {
    private final ItemRegistry registry;
    private final NamespacedKey identityKey;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Method setItemModel;

    public CatItemService(JavaPlugin plugin, ItemRegistry registry) {
        this.registry = registry;
        this.identityKey = new NamespacedKey(plugin, "item_id");
        this.setItemModel = findSetItemModel();
    }

    @Override
    public Optional<CatItemDefinition> find(String id) {
        return registry.find(id);
    }

    @Override
    public Collection<CatItemDefinition> items() {
        return registry.definitions();
    }

    @Override
    public ItemStack create(String id, int amount) {
        CatItemDefinition definition = find(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown CatItem: " + id));
        if (amount < 1 || amount > definition.material().getMaxStackSize()) {
            throw new IllegalArgumentException("The amount must be between 1 and "
                    + definition.material().getMaxStackSize());
        }
        ItemStack itemStack = new ItemStack(definition.material(), amount);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(miniMessage.deserialize(definition.displayName()));
        if (!definition.lore().isEmpty()) {
            meta.lore(definition.lore().stream().map(miniMessage::deserialize).toList());
        }
        meta.setCustomModelData(definition.customModelData());
        meta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, definition.id().toString());
        meta.setEnchantmentGlintOverride(definition.glint());
        applyModernItemModel(meta, definition.itemModel());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public Optional<String> identify(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }
        String id = itemStack.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
        return id == null || registry.find(id).isEmpty() ? Optional.empty() : Optional.of(id);
    }

    @Override
    public Collection<CatFeature> features() {
        return FeatureCatalog.all();
    }

    @Override
    public Optional<CatFeature> feature(String id) {
        return FeatureCatalog.find(id);
    }

    public boolean supportsModernItemModels() {
        return setItemModel != null;
    }

    private void applyModernItemModel(ItemMeta meta, NamespacedKey itemModel) {
        if (setItemModel == null) {
            return;
        }
        try {
            setItemModel.invoke(meta, itemModel);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("minecraft:item_model could not be set", exception);
        }
    }

    private Method findSetItemModel() {
        try {
            return ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
