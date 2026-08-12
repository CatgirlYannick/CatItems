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
}
```

Compile against the CatItems JAR with `provided` scope. Add CatItems to
`softdepend` in `plugin.yml` when the integration is optional.

- `find(id)` - find a definition
- `items()` - read the immutable definition collection
- `create(id, amount)` - create an identified ItemStack
- `identify(stack)` - read a CatItems ID from PDC
- `features()` - read the complete feature catalog
- `feature(id)` - find one catalog entry

The API does not execute CatDrugs-specific logic.

---
Made By CatgirlYannick
