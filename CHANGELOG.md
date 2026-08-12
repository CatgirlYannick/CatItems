# Changelog

## 0.7.0-ALPHA - 2026-08-12

- Added separate approach, contact, release, forearm, and aftercare model poses
  so held items travel through short transitions instead of snapping endpoints.
- Reworked all ten bundled intake timelines to keep native arm poses active
  through intermediate sound, particle, and body-motion keyframes.
- Added a smooth v5 animation migration that backs up the previous file,
  refreshes bundled routes, and preserves separately named custom animations.
- Kept every animation free of forced head or camera rotation.

## 0.6.2-ALPHA - 2026-08-12

- Replaced one scheduler task per active emote with one shared, lazily running
  animation ticker.
- Precomputed direct per-tick keyframe and body-motion timelines instead of
  scanning motion keyframes for every player on every server tick.
- Cached immutable native arm components, animation/model registry snapshots,
  short item-ID lookups, static messages, and generated model-pose lists.
- Added lazily cloned custom-item prototypes so repeated item creation no longer
  reparses MiniMessage names and lore or rebuilds all metadata.
- Added regression coverage for lossless direct-tick timelines and cached model
  pose snapshots.

## 0.6.1-ALPHA - 2026-08-12

- Kept identical arm poses active across keyframes instead of restarting them.
- Added automatic per-tick recovery when the client briefly drops an active pose.
- Added semantic `face` and `face_both` poses backed by the native horn and
  spyglass arm positions.
- Extended smoking and vaping holds so the item remains directly in front of
  the mouth; snorting now holds both arms in front of the face.
- Reduced unnecessary pose switching in injection, ritual, eating, and drinking
  timelines for visibly smoother playback.
- Added automatic v3-to-v4 animation migration with a versioned backup.

## 0.6.0-ALPHA - 2026-08-12

- Replaced forced head/view movement with real held-item arm-use animations.
- Added YAML `arm-pose` keyframes for eat, drink, block, bow, trident,
  crossbow, spyglass, toot-horn, brush, bundle, and spear poses.
- Added distinct arm choreography for all ten CatDrugs consumption routes.
- Preserved and restored the item's original consumable component safely.
- Added automatic v2-to-v3 animation migration with a versioned backup.
- Kept the plugin loadable on early Paper 1.21 builds with a legacy fallback.

## 0.5.0-ALPHA - 2026-08-12

- Added smooth per-tick player body and head animation with easing.
- Added fixed standing, crouching, swimming, fall-flying, and spin poses.
- Added optional movement locking with safe restoration after every emote.
- Added ten intake-specific emotes, including separate joint, pipe, stimulant,
  snorting, bottle, edible, vaping, injection, ritual, and pill movements.
- Added a backed-up automatic migration from version-1 animation timelines.

## 0.4.0-ALPHA - 2026-08-12

- Replaced hard-coded intake effects with an editable YAML keyframe engine.
- Added ten self-made first- and third-person 3D model poses.
- Added per-keyframe hand motion, native use poses, particles, sounds, and timing.
- Added animation discovery and preview commands to the public API and CLI.
- Generated animation item models automatically for every registered CatItem.
- Kept the held item visible until CatDrugs completes the final keyframe.
- Resolved sounds through namespaced keys for Paper/Leaf 1.21.11 compatibility.

## 0.3.0-ALPHA - 2026-08-12

- Added a reusable public use-animation API.
- Added smoke, snort, drink, eat, inhale, inject, ritual, and swallow presets.
- Added first- and third-person hand motion, particles, and synchronized sounds.
- Added duration limits and clean cancellation for active animations.

## 0.2.2-ALPHA - 2026-08-12

- Added central Small Caps rendering for messages, item names, and lore.
- Preserved real `ä`, `ö`, `ü` and `ß` characters without ASCII replacements.
- Disabled Vanilla italic styling on generated item names and lore.

## 0.2.1-ALPHA - 2026-08-12

- Added a queryable feature catalog and ItemsAdder parity map.
- Added `/catitems features` and `/catitems parity` status filters.
- Extended the public API with feature listing and lookup.
- Added disabled future-module flags and matching documentation.

## 0.2.0-ALPHA - 2026-08-12

- Added compact 3D cube models instead of large flat sprites.
- Defined first-person, third-person, GUI, ground, and frame transforms.
- Kept resource-pack output compatible with Paper 1.21 through 1.21.11.

## 0.1.1-ALPHA - 2026-08-11

- Converted all visible plugin text and starter content to English.
- Hardened generated ZIP paths for managed hosting.

## 0.1.0-ALPHA - 2026-08-11

- Created the YAML custom-item registry, stable model data, and PDC identity.
- Published the `CatItemsApi` Bukkit service.
- Added version-aware resource-pack building and delivery.
- Added starter items, textures, commands, permissions, and contract tests.

---
Made By CatgirlYannick
