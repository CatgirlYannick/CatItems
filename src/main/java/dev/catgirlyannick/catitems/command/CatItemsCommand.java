package dev.catgirlyannick.catitems.command;

import dev.catgirlyannick.catitems.CatItemsPlugin;
import dev.catgirlyannick.catitems.api.CatItemDefinition;
import dev.catgirlyannick.catitems.feature.CatFeature;
import dev.catgirlyannick.catitems.feature.FeatureCatalog;
import dev.catgirlyannick.catitems.feature.FeatureStatus;
import dev.catgirlyannick.catitems.item.CatItemService;
import dev.catgirlyannick.catitems.pack.PackArtifact;
import dev.catgirlyannick.catitems.pack.PackFormat;
import dev.catgirlyannick.catitems.pack.ResourcePackManager;
import dev.catgirlyannick.catitems.service.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CatItemsCommand implements CommandExecutor, TabCompleter {
    private final CatItemsPlugin plugin;
    private final CatItemService items;
    private final ResourcePackManager packs;
    private final MessageService messages;

    public CatItemsCommand(CatItemsPlugin plugin, CatItemService items, ResourcePackManager packs,
                           MessageService messages) {
        this.plugin = plugin;
        this.items = items;
        this.packs = packs;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        try {
            return switch (subcommand) {
                case "help" -> help(sender);
                case "list" -> list(sender);
                case "info" -> info(sender, args);
                case "give" -> give(sender, args);
                case "reload" -> reload(sender);
                case "pack" -> pack(sender, args);
                case "status" -> status(sender);
                case "features", "parity" -> features(sender, args);
                default -> help(sender);
            };
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("CatItems command failed: " + exception.getMessage());
            exception.printStackTrace();
            messages.send(sender, "internal-error");
            return true;
        }
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage(messages.raw("<aqua><bold>CatItems</bold></aqua> <gray>- custom items without ItemsAdder</gray>"));
        sender.sendMessage(messages.raw("<white>/catitems list</white> <gray>- registered items</gray>"));
        sender.sendMessage(messages.raw("<white>/catitems info <id></white> <gray>- item details</gray>"));
        sender.sendMessage(messages.raw("<white>/catitems features [live|foundation|planned]</white> <gray>- ItemsAdder parity map</gray>"));
        if (sender.hasPermission("catitems.give")) {
            sender.sendMessage(messages.raw("<white>/catitems give <player> <id> [amount]</white>"));
        }
        if (sender.hasPermission("catitems.reload")) {
            sender.sendMessage(messages.raw("<white>/catitems reload</white>"));
        }
        if (sender.hasPermission("catitems.pack")) {
            sender.sendMessage(messages.raw("<white>/catitems pack build|send [player]</white>"));
            sender.sendMessage(messages.raw("<white>/catitems status</white>"));
        }
        return true;
    }

    private boolean list(CommandSender sender) {
        sender.sendMessage(messages.raw("<aqua>Registered CatItems (" + items.items().size() + "):</aqua>"));
        items.items().stream().sorted(Comparator.comparing(definition -> definition.id().toString()))
                .forEach(definition -> sender.sendMessage(messages.raw("<gray>•</gray> <white>"
                        + definition.id() + "</white> <dark_gray>(" + definition.material() + ", CMD "
                        + definition.customModelData() + ")</dark_gray>")));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(messages.raw("<red>Usage: /catitems info <id></red>"));
            return true;
        }
        CatItemDefinition definition = items.find(args[1]).orElse(null);
        if (definition == null) {
            messages.send(sender, "unknown-item", Map.of("id", args[1]));
            return true;
        }
        sender.sendMessage(messages.raw("<aqua>" + definition.id() + "</aqua>"));
        sender.sendMessage(messages.raw("<gray>Material:</gray> <white>" + definition.material() + "</white>"));
        sender.sendMessage(messages.raw("<gray>CustomModelData:</gray> <white>" + definition.customModelData() + "</white>"));
        sender.sendMessage(messages.raw("<gray>ItemModel:</gray> <white>" + definition.itemModel() + "</white>"));
        sender.sendMessage(messages.raw("<gray>Texture:</gray> <white>" + definition.texture() + "</white>"));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!require(sender, "catitems.give")) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.raw("<red>Usage: /catitems give <player> <id> [amount]</red>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found", Map.of("player", args[1]));
            return true;
        }
        CatItemDefinition definition = items.find(args[2]).orElse(null);
        if (definition == null) {
            messages.send(sender, "unknown-item", Map.of("id", args[2]));
            return true;
        }
        int amount;
        try {
            amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
        } catch (NumberFormatException exception) {
            messages.send(sender, "invalid-number");
            return true;
        }
        if (amount < 1 || amount > 2304) {
            messages.send(sender, "invalid-number");
            return true;
        }
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, definition.material().getMaxStackSize());
            ItemStack stack = items.create(definition.id().toString(), stackSize);
            target.getInventory().addItem(stack).values().forEach(leftover ->
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            remaining -= stackSize;
        }
        messages.send(sender, "give-success", Map.of(
                "amount", Integer.toString(amount),
                "id", definition.id().toString(),
                "player", target.getName()));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!require(sender, "catitems.reload")) {
            return true;
        }
        int count = plugin.reloadEverything();
        messages.send(sender, "reload-success", Map.of("count", Integer.toString(count)));
        return true;
    }

    private boolean pack(CommandSender sender, String[] args) {
        if (!require(sender, "catitems.pack")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.raw("<red>Usage: /catitems pack build|send [player]</red>"));
            return true;
        }
        if ("build".equalsIgnoreCase(args[1])) {
            try {
                PackArtifact artifact = plugin.rebuildPack();
                messages.send(sender, "pack-built", Map.of("file", artifact.file().toString(), "hash", artifact.sha1Hex()));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
            return true;
        }
        if ("send".equalsIgnoreCase(args[1])) {
            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
            } else if (sender instanceof Player player) {
                target = player;
            } else {
                target = null;
            }
            if (target == null) {
                messages.send(sender, args.length >= 3 ? "player-not-found" : "player-only",
                        args.length >= 3 ? Map.of("player", args[2]) : Map.of());
                return true;
            }
            if (!packs.canSend()) {
                messages.send(sender, "pack-unavailable");
                return true;
            }
            packs.send(target);
            messages.send(sender, "pack-sent", Map.of("player", target.getName()));
            return true;
        }
        return pack(sender, new String[]{"pack"});
    }

    private boolean status(CommandSender sender) {
        if (!require(sender, "catitems.pack")) {
            return true;
        }
        PackFormat format = PackFormat.forMinecraftVersion(Bukkit.getMinecraftVersion());
        messages.send(sender, "status", Map.of(
                "count", Integer.toString(items.items().size()),
                "minecraft", Bukkit.getMinecraftVersion(),
                "format", format.display(),
                "pack", packs.artifact().map(value -> value.sha1Hex().substring(0, 12)).orElse("not built")));
        sender.sendMessage(messages.raw("<gray>URL:</gray> <white>" + packs.downloadUrl() + "</white>"));
        return true;
    }

    private boolean features(CommandSender sender, String[] args) {
        FeatureStatus filter = null;
        if (args.length >= 2) {
            try {
                filter = FeatureStatus.valueOf(args[1].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(messages.raw("<red>Usage: /catitems features [live|foundation|planned]</red>"));
                return true;
            }
        }

        sender.sendMessage(messages.raw("<aqua>CatItems ItemsAdder parity:</aqua> <green>"
                + FeatureCatalog.count(FeatureStatus.LIVE) + " live</green><gray>, </gray><yellow>"
                + FeatureCatalog.count(FeatureStatus.FOUNDATION) + " foundation</yellow><gray>, </gray><red>"
                + FeatureCatalog.count(FeatureStatus.PLANNED) + " planned</red>"));
        for (CatFeature feature : FeatureCatalog.all()) {
            if (filter != null && feature.status() != filter) {
                continue;
            }
            String color = switch (feature.status()) {
                case LIVE -> "green";
                case FOUNDATION -> "yellow";
                case PLANNED -> "red";
            };
            sender.sendMessage(messages.raw("<" + color + ">" + feature.status().label() + "</" + color + "> "
                    + "<white>" + feature.id() + "</white> <gray>- " + feature.summary() + "</gray>"));
        }
        return true;
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(List.of("help", "list", "info", "features", "parity"));
            if (sender.hasPermission("catitems.give")) suggestions.add("give");
            if (sender.hasPermission("catitems.reload")) suggestions.add("reload");
            if (sender.hasPermission("catitems.pack")) suggestions.addAll(List.of("pack", "status"));
        } else if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            suggestions.addAll(itemIds());
        } else if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        } else if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            suggestions.addAll(itemIds());
        } else if (args.length == 2 && "pack".equalsIgnoreCase(args[0])) {
            suggestions.addAll(List.of("build", "send"));
        } else if (args.length == 3 && "pack".equalsIgnoreCase(args[0]) && "send".equalsIgnoreCase(args[1])) {
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        } else if (args.length == 2 && ("features".equalsIgnoreCase(args[0]) || "parity".equalsIgnoreCase(args[0]))) {
            suggestions.addAll(List.of("live", "foundation", "planned"));
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }

    private List<String> itemIds() {
        return items.items().stream().map(definition -> definition.id().toString()).sorted().toList();
    }
}
