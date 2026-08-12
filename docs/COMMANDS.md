# Commands and Permissions

| Command | Permission | Purpose |
| --- | --- | --- |
| `/catitems help` | `catitems.use` | Show help |
| `/catitems list` | `catitems.use` | List all registered IDs |
| `/catitems info <id>` | `catitems.use` | Show technical item details |
| `/catitems give <player> <id> [amount]` | `catitems.give` | Give a custom item |
| `/catitems reload` | `catitems.reload` | Reload config, registry, and pack |
| `/catitems pack build` | `catitems.pack` | Build the pack immediately |
| `/catitems pack send [player]` | `catitems.pack` | Send the pack |
| `/catitems features [live\|foundation\|planned]` | `catitems.use` | Browse the feature catalog |
| `/catitems status` | `catitems.pack` | Show version, pack format, hash, and URL |

`catitems.admin` grants every administrative child permission and defaults to
server operators. Unauthorized subcommands are hidden from help and tab completion.

---
Made By CatgirlYannick
