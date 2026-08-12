package dev.catgirlyannick.catitems.feature;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class FeatureCatalog {
    private static final List<CatFeature> FEATURES = List.of(
            live("custom-items", "Custom Items", "YAML-driven namespaced items with PDC identity."),
            live("custom-model-data", "CustomModelData", "Stable automatic model numbers per item."),
            live("item-models", "Modern Item Models", "Minecraft 1.21.4+ item model definitions."),
            live("legacy-model-overrides", "Legacy Model Overrides", "CustomModelData overrides for Minecraft 1.21 through 1.21.3."),
            live("resource-pack-build", "Resource Pack Build", "Automatic resource-pack ZIP generation with SHA-1."),
            live("resource-pack-delivery", "Resource Pack Delivery", "Disabled, external URL, or self-hosted pack delivery."),
            live("resource-pack-self-host", "Self-Hosted Pack", "Built-in GET/HEAD HTTP endpoint for the generated pack."),
            live("join-pack-send", "Join Pack Sending", "Optional automatic resource-pack prompt on join."),
            live("mini-message-items", "MiniMessage Item Text", "Display names and lore support MiniMessage."),
            live("glint-items", "Glint Items", "Custom enchantment-glint override without fake enchantments."),
            live("starter-assets", "Starter Assets", "Bundled starter items and transparent textures."),
            live("bukkit-service-api", "Bukkit Service API", "Optional API for other plugins without hard dependency."),
            live("admin-commands", "Admin Commands", "List, info, give, reload, pack build/send, and status commands."),
            live("use-animations", "Use Animations", "Reusable smoke, snort, drink, eat, inhale, inject, ritual, and swallow sequences."),
            foundation("feature-catalog", "Feature Catalog", "Internal ItemsAdder-parity catalog exposed through commands and API."),
            foundation("pack-overlays", "Pack Overlays", "Tracked as a pack-system target; generation rules still need implementation."),
            foundation("resource-pack-hardening", "Resource Pack Protection", "Tracked for production delivery, hashing, and proxy-safe hosting."),
            planned("custom-blocks", "Custom Blocks", "Custom block definitions, drops, hardness, and interaction rules."),
            planned("block-sounds", "Block Sounds", "Step, break, fall, hit, and place sound sets."),
            planned("furniture", "Furniture", "Placeable, rotatable, sit-capable furniture with display entities."),
            planned("solid-furniture", "Solid Furniture", "Furniture hitboxes, collision, and safe removal rules."),
            planned("light-furniture", "Light Furniture", "Furniture that emits configurable light."),
            planned("custom-entities", "Custom Entities", "Modeled and animated entities with safe spawn definitions."),
            planned("custom-mobs", "Custom Mobs", "Mob variants, drops, AI hooks, and optional Mythic-style bridges."),
            planned("blockbench-models", "Blockbench Models", "Model and animation import conventions."),
            planned("entity-animations", "Entity Animations", "Idle, move, attack, death, and custom animation channels."),
            foundation("emotes", "Player Emotes", "Use-animation engine is live; free-form player emotes still need animation assets."),
            planned("huds", "HUDs", "Custom bars and screen overlays through fonts/resource-pack assets."),
            planned("custom-guis", "Custom GUIs", "Configurable inventories and graphical UI assets."),
            planned("font-images", "Font Images", "Private-use font images and emoji-style glyphs."),
            planned("emojis", "Emojis", "Chat/book/hologram-safe font image shortcuts."),
            planned("paintings", "Custom Paintings", "Custom painting assets and placement metadata."),
            planned("advancements", "Custom Advancements", "Configurable advancement trees and rewards."),
            planned("tags", "Custom Tags", "Server-side item/block/entity grouping."),
            planned("crops", "Crops", "Growth stages, drops, world rules, and farmland logic."),
            planned("loots", "Loots", "Block, mob, fishing, and container loot tables."),
            planned("crafting-recipes", "Crafting Recipes", "Shaped and shapeless crafting support."),
            planned("cooking-recipes", "Cooking Recipes", "Furnace, smoker, blast furnace, and campfire recipes."),
            planned("stonecutter-recipes", "Stonecutter Recipes", "Stonecutter conversion recipes."),
            planned("brewing-recipes", "Brewing Recipes", "Potion-style brewing recipes."),
            planned("anvil-recipes", "Anvil Recipes", "Repair and combine recipes."),
            planned("smithing-recipes", "Smithing Recipes", "Smithing transform and trim-style recipes."),
            planned("custom-fuels", "Custom Fuels", "Burn time definitions for custom and vanilla items."),
            planned("music-discs", "Music Discs", "Custom disc items and sound mapping."),
            planned("custom-sounds", "Custom Sounds", "Sound assets, sound events, and playback commands/hooks."),
            planned("liquids", "Custom Liquids", "Bucket items, placement rules, and liquid-like behavior."),
            planned("vehicles", "Vehicles", "Rideable vehicles with speed, fuel, hitbox, and particles."),
            planned("weapons", "Weapons", "Custom weapons and tools with durability and behaviors."),
            planned("projectiles", "Projectiles", "Gun/projectile behavior and impact rules."),
            planned("custom-durability", "Custom Durability", "Independent durability storage and display."),
            planned("stackable-items", "Stackable Custom Items", "Stacking rules for custom items with identity metadata."),
            foundation("consumables", "Consumables", "Use animations are live; native food values, cooldowns, and effects remain planned."),
            planned("armors", "Custom Armors", "Armor items, armor stats, and later geometry support."),
            planned("hats", "Hats", "Wearable head-slot cosmetic items."),
            planned("wings", "Wings", "Cosmetic wing models and animations."),
            planned("colored-items", "Colored Items", "Configurable tint and color variants."),
            planned("animated-titles", "Animated Titles", "Title/subtitle animation presets."),
            planned("text-effects", "Text Effects", "Reusable text animation and style effects."),
            planned("worldgen-surface", "Surface Populators", "Surface placement rules for custom content."),
            planned("worldgen-trees", "Tree Populators", "Custom tree placement and loot hooks."),
            planned("worldgen-caves", "Cave Decorators", "Underground decorator placement rules."),
            planned("worldgen-ores", "Ore Populators", "Ore and block population rules."),
            planned("worldgen-furniture", "Furniture Populators", "World placement for furniture/decorations."),
            planned("custom-stats", "Custom Player Stats", "Persistent stat keys exposed to plugins and placeholders."),
            planned("scoreboard-tools", "Scoreboard Display Tools", "Utilities such as hiding background/numbers where supported."),
            planned("skript-api", "Skript API", "Events and expressions for Skript integration."),
            planned("denizen-api", "Denizen API", "Denizen-facing actions and tags."),
            planned("plugin-hooks", "Plugin Hooks", "Optional hooks for external plugins such as MMOItems."),
            planned("worldedit-support", "WorldEdit Support", "Custom block paste/selection compatibility.")
    ).stream().sorted(Comparator.comparing(CatFeature::id)).toList();

    private FeatureCatalog() {
    }

    public static List<CatFeature> all() {
        return FEATURES;
    }

    public static Optional<CatFeature> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        return FEATURES.stream().filter(feature -> feature.id().equals(normalized)).findFirst();
    }

    public static long count(FeatureStatus status) {
        return FEATURES.stream().filter(feature -> feature.status() == status).count();
    }

    private static CatFeature live(String id, String title, String summary) {
        return new CatFeature(id, title, FeatureStatus.LIVE, summary);
    }

    private static CatFeature foundation(String id, String title, String summary) {
        return new CatFeature(id, title, FeatureStatus.FOUNDATION, summary);
    }

    private static CatFeature planned(String id, String title, String summary) {
        return new CatFeature(id, title, FeatureStatus.PLANNED, summary);
    }
}
