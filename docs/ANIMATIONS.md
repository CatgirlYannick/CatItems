# Custom Arm Animations

CatItems reads animation timelines from `plugins/CatItems/animations.yml`.
Twenty ticks equal one second. Reload changes with `/catitems reload`.

```yaml
animations:
  custom_sip:
    duration-ticks: 30
    lock-movement: true
    keyframes:
      - {at: 0, item-pose: low, arm-pose: rest,
         body-pose: standing, body-yaw: 0}
      - {at: 8, item-pose: mouth, arm-pose: drink,
         body-pose: standing, body-yaw: -4,
         sound: ENTITY_GENERIC_DRINK, particle: SPLASH,
         particle-anchor: mouth, particle-count: 2}
      - {at: 24, item-pose: low, arm-pose: bundle,
         body-pose: crouching, body-yaw: 3}
      - {at: 30, item-pose: rest, arm-pose: rest,
         body-pose: standing, body-yaw: 0}
```

## Keyframe Fields

- `lock-movement`: prevents walking during the emote while the player remains
  free to move their camera
- `at`: tick between zero and `duration-ticks`
- `item-pose`: `rest`, `low`, `approach`, `raise`, `contact`, `mouth`,
  `release`, `tilt_left`, `tilt_right`, `inhale`, `extend`, `forearm`, `chest`,
  `aftercare`, `shake_up`, or `shake_down`
- `arm-pose`: `keep`, `rest`, `face`, `face_both`, `eat`, `drink`, `block`,
  `bow`, `trident`, `crossbow`, `spyglass`, `toot_horn`, `brush`, `bundle`, or
  `spear`; `face` holds one arm at the mouth and `face_both` holds both arms
  directly in front of the face
- `hand`: optional legacy one-shot swing: `none`, `main`, `off`, or `both`
- `use-action`: optional legacy active-item control: `none`, `start`, or `stop`
- `body-pose`: `keep`, `standing`, `crouching`, `swimming`, `fall_flying`, or
  `spin_attack`
- `body-yaw`: optional relative torso rotation from -180 through 180 degrees
- `easing`: `linear`, `ease_in`, `ease_out`, or `ease_in_out`
- `sound`: a Paper sound name or namespaced registry key
- `particle`: a data-free Paper 1.21 particle enum name
- `particle-anchor`: `eye`, `mouth`, `hand`, or `feet`
- `particle-count`, `particle-spread`, and `particle-speed`: optional values

`arm-pose` temporarily assigns Paper's consumable use-animation component to
the held CatItems object and starts its real client-side use pose. CatItems uses
a very long internal consume duration, stops it before completion, and restores
the exact original component, item model, body pose, torso yaw, and sneaking
state. It does not rotate or reset the player's head or camera.

Repeated identical `arm-pose` values do not restart the native animation.
CatItems keeps that pose active continuously and reasserts it only when the
client unexpectedly drops it. This avoids the lowering/raising jerk between
sound, particle, and item-model keyframes.

Bundled v5 timelines use short approach/contact/release stages around the
native pose. On upgrade, CatItems creates a versioned backup, refreshes the ten
bundled routes, and copies separately named custom animations into the new file.

The bundled routes use distinct arm choreography: joint and vape raise one arm
to the mouth, drinks use the drinking motion, edibles use eating, snorting moves
the hand toward the face, injections cross both arms, and ritual routes combine
two-handed bow/crossbow and raised-arm poses.

Modern arm poses require Minecraft/Paper 1.21.4 or newer. CatItems remains
loadable on 1.21 through 1.21.3, but those builds use only the legacy hand/use
timeline because their Paper API lacks the consumable animation component.

Use `/catitems animations list` to inspect IDs and `/catitems animations play
<player> <id> [duration]` to preview a sequence without consuming an item.

---
Made By CatgirlYannick
