# CatItems API

Other Paper plugins can access CatItems optionally through Bukkit's service
manager. This keeps the consuming plugin startable when CatItems is absent.

```java
RegisteredServiceProvider<CatItemsApi> registration =
        Bukkit.getServicesManager().getRegistration(CatItemsApi.class);

if (registration != null) {
    CatItemsApi catItems = registration.getProvider();
    ItemStack ruby = catItems.create("catitems:ruby", 1);
    catItems.identify(ruby).ifPresent(id -> getLogger().info(id));
    catItems.playUseAnimation(player, "smoke", 44);
}
```

Compile against the CatItems JAR with `provided` scope. Add CatItems to
`softdepend` in `plugin.yml` when the integration is optional.

- `find(id)` - find a definition
- `items()` - read the immutable definition collection
- `create(id, amount)` - create an identified ItemStack
- `identify(stack)` - read a CatItems ID from PDC
- `playUseAnimation(player, preset, durationTicks)` - play a reusable animation
- `stopUseAnimation(player)` - cancel that player's active use animation
- `features()` - read the complete feature catalog
- `feature(id)` - find one catalog entry

Available presets are `smoke`, `snort`, `drink`, `eat`, `inhale`, `inject`,
`ritual`, and `swallow`. The API only renders the sequence; it never removes an
item or applies gameplay effects. It does not execute CatDrugs-specific logic.

---
Made By CatgirlYannick
