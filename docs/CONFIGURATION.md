# Configuration

## Resource-Pack Delivery

`resource-pack.delivery` supports three modes:

- `self-host`: CatItems serves the generated ZIP.
- `external`: CatItems sends `external-url`; upload the ZIP separately.
- `disabled`: pack building remains available, automatic delivery is disabled.

For `self-host`, `public-url` must be reachable from every player's computer.
`127.0.0.1` works only when client and server are the same machine. Public
servers should normally use an HTTPS URL through a correctly configured reverse
proxy. The default port is `8164` and the path is `/catitems-pack.zip`.

## Pack Build

At startup and reload, CatItems normally creates
`plugins/CatItems/output/CatItems-pack.zip`. Do not manually edit
`generated-pack/`; place custom files under `pack/assets/`.

Minecraft 1.21.4+ uses item-model definitions. Minecraft 1.21 through 1.21.3
uses base-material model overrides with `custom_model_data`. CatItems always
builds the pack metadata for the running server version.

## Future Feature Flags

The `features` section contains deliberately disabled foundation flags for
larger future modules:

- `custom-blocks`
- `furniture`
- `custom-entities`
- `custom-recipes`
- `custom-sounds`
- `font-images`
- `huds`
- `worldgen`

These flags do not activate unfinished mechanics. `/catitems features` and the
`CatItemsApi` are the authoritative live feature catalog.

## Managed Hosting

When a hosting panel distributes the resource pack, upload the generated ZIP
there and use `resource-pack.delivery: disabled` to prevent CatItems from also
sending a local URL. Set the Minecraft client option "Server Resource Packs" to
at least Prompt or Enabled.

## Required Pack

`resource-pack.required: true` disconnects players who reject the pack. Enable
it only after testing the exact public URL and generated ZIP from outside the
server network.

---
Made By CatgirlYannick
