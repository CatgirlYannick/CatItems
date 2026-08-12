# Item Format

CatItems reads every `.yml` file in `plugins/CatItems/items/`. One file can
contain any number of items.

```yaml
config-version: 1
namespace: myserver

items:
  moonstone:
    enabled: true
    material: PAPER
    display-name: '<aqua><bold>Moonstone</bold></aqua>'
    lore:
      - '<gray>A rare discovery.</gray>'
    custom-model-data: auto
    item-model: 'myserver:moonstone'
    texture: 'myserver:item/moonstone'
    glint: true
    permission: ''
```

The corresponding PNG is stored at:

`plugins/CatItems/pack/assets/myserver/textures/item/moonstone.png`

## Fields

| Field | Meaning |
| --- | --- |
| `enabled` | Load or skip the item |
| `material` | Visible Vanilla base material |
| `display-name` | MiniMessage/RGB name |
| `lore` | MiniMessage/RGB description lines |
| `custom-model-data` | Positive integer or recommended `auto` |
| `item-model` | Model ID; defaults to the item ID |
| `texture` | Texture ID without `textures/` or `.png` |
| `glint` | Enchantment glint without a fake enchantment |
| `permission` | Reserved access rule for integrations |

Automatic values are stored in `plugins/CatItems/data/model-data.yml`. Do not
delete or let this file diverge between servers after items have been distributed.

Custom JSON models can be placed under `pack/assets/<namespace>/models/`. The
builder preserves an existing matching model.

---
Made By CatgirlYannick
