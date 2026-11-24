<p align="center">
  <img width="2560" height="1440" alt="UltimateWarps Banner" src="https://github.com/user-attachments/assets/9c4e20d1-bc3a-4dc8-8486-5e817d48cc0b" />
</p>

<p align="center">
  <strong>Modern server spawn, warp, and player warp management for Paper & Purpur</strong><br>
  <sub>Interactive GUIs • Dynamic Teleport Effects • Player Warps • Economy & Permissions • Fully Customizable</sub>
</p>

<p align="center">
  <a href="https://modrinth.com/plugin/ultimate-warps"><img src="https://img.shields.io/badge/Modrinth-Ultimate%20Warps-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth" /></a>
  <img src="https://img.shields.io/static/v1?label=Version&message=4.1&color=6366f1&style=for-the-badge" alt="Version 4.1" />
  <img src="https://img.shields.io/static/v1?label=Minecraft&message=1.21.4%20-%2026.3&color=3b82f6&style=for-the-badge" alt="Minecraft 1.21.4 - 26.3" />
  <img src="https://img.shields.io/static/v1?label=Platform&message=Paper%20%2F%20Purpur&color=8b5cf6&style=for-the-badge" alt="Paper and Purpur" />
</p>

## Features

### Server Spawn & Warps

* **Spawn Management** — Set or delete server spawn, teleport players automatically on first join or every join, and force-teleport online or offline players with `/spawnforce`.

* **Interactive Warp GUI (`/warp`)** — Customizable multi-page menu with pinned or auto-flowing warp slots, custom icons, enchantment glint, custom lore, filler items, and click sounds.

* **In-Game Admin Editor (`/warpsadmin`)** — Create, edit, and delete server warps directly in-game: toggle status, set permissions, cooldowns, delays, display names, lore, icons (drag-and-drop), GUI slots, and per-warp teleport effects.

### Player Warps (`/pwarps`)

* **Player-Created Warps** — Completely separate from server warps with dedicated config and GUI layout files, per-rank warp limits, and public/private visibility toggles.

* **Paid Warps & Economy** — Warp owners with permission can charge visitors to teleport. Works directly with **Vault** (atomic withdraw, deposit, and automatic refund if a teleport is blocked) or falls back to **PlaceholderAPI** + configurable economy commands.

* **Safe Warp Detection** — Checks ground stability, passable space, and overhead hazards (blocking traps like signs, open trapdoors, unsupported sand/gravel, and lava/anvils) on creation and teleportation.

* **Ratings & Favourites** — Visitors receive a clickable chat prompt to rate warps (1–5 stars) in a GUI, and can right-click warps in `/pwarps` to bookmark and filter their favourites.

* **Whitelist & Blacklist** — Manage warp access through dedicated paginated GUIs or commands. Blacklists apply even when whitelist mode is off; owners and staff always bypass restrictions.

* **Claim & Region Protection** — Optional creation rules for own **GriefPrevention** claims (including subdivisions), outside **WorldGuard** regions, overworld-only, and minimum distance from spawn or other player warps.

### Dynamic Animations, Permissions & Customization

* **Teleport Countdowns & Effects** — Configurable BossBars (color, style, text), titles/subtitles, particles (including colored dust), and sounds for spawn, server warps, and player warps, with cancel-on-move and cancel-on-damage options.

* **Economy & Permission Support** — Optional **LuckPerms** rank multipliers for cooldowns and delays (with standard permission fallback when LuckPerms isn't installed), plus automatic admin access for server OPs.

* **Full MiniMessage & Legacy Formatting** — Supports MiniMessage gradients, hex colors (`&#RRGGBB` / `&x...`), hover/click actions, and legacy `&`/`§` color codes across every config and GUI.

## Requirements & Compatibility

* **Server Software:** Paper or Purpur

* **Minecraft Versions:** `1.21.4` – `26.3` (single JAR)

* **Java:** Java 21+ (runs on Java 21 for 1.21.x and Java 25 for 26.x)

* **Required Dependencies:** None!

* **Optional Integrations:**

  * **Vault** (+ any Vault economy plugin like EssentialsX or CMI) — recommended for paid player warps

  * **PlaceholderAPI** — optional balance fallback if Vault is not installed

  * **LuckPerms** — rank-based cooldown and delay multipliers

  * **WorldGuard** & **GriefPrevention** — optional region and claim placement checks

## Installation

1. Download the latest `UltimateWarps-4.1.jar` from [Modrinth](https://modrinth.com/plugin/ultimate-warps) or build it from source.

2. Drop the `.jar` file into your server's `plugins/` folder.

3. Restart your server and customize the generated files in `plugins/UltimateWarps/`.

## Commands

### Server Spawn & Warps

| Command | Aliases | Description | 
| ----- | ----- | ----- | 
| `/spawn` | — | Teleport to server spawn | 
| `/setspawn` | — | Set the server spawn location *(Admin)* | 
| `/delspawn` | — | Delete the server spawn location *(Admin)* | 
| `/spawnforce <player>` | `/forcespawn` | Force-teleport an online or offline player to spawn *(Admin)* | 
| `/warp [name]` | `/warps`, `/warpslist`, `/warplist` | Open the warp GUI or teleport directly to a warp | 
| `/setwarp <name>` | — | Create a server warp at your location *(Admin)* | 
| `/delwarp <name>` | — | Delete a server warp *(Admin)* | 
| `/warpsadmin` | — | Open the in-game admin warp editor GUI *(Admin)* | 
| `/ultimatewarps <subcommand>` | `/uwarps` | Unified command (`reload`, `spawn`, `setspawn`, `delspawn`, `warp`, `setwarp`, `delwarp`, `warpsadmin`, `spawnforce`, `help`) | 

### Player Warps

| Command | Description | 
| ----- | ----- | 
| `/playerwarps` or `/pwarps` | Open the player warps browser GUI | 
| `/pwarps set <name>` | Create a player warp at your current location | 
| `/pwarps del <name>` | Delete one of your own player warps | 
| `/pwarps edit <name>` | Open the edit menu for one of your player warps | 
| `/pwarps list [player]` | List your own warps or another player's public warps | 
| `/pwarps warp <name>` | Teleport to a player warp | 
| `/pwarps warp <owner> <name>` | Teleport to a specific player's warp | 
| `/pwarps rate [owner] <warp> [1-5]` | Open the Rating GUI or rate a visited warp (1–5 stars) | 
| `/pwarps cost <warp> <amount|on|off|toggle>` | Set teleport cost or toggle paid mode on your warp | 
| `/pwarps whitelist <warp> [gui|on|off|toggle|add|remove|list] [player]` | Manage your warp's whitelist via GUI or commands | 
| `/pwarps blacklist <warp> [gui|add|remove|list] [player]` | Manage your warp's blacklist via GUI or commands | 
| `/pwarps favourite [owner] [warp]` | View favourite warps in the GUI or toggle a favourite | 
| `/pwarps admin <info|wipe|tp> ...` | *(Staff)* Inspect warp limits, wipe a player's warps, or teleport to any warp | 

## Permissions

> **Note:** Server operators (`OP`) automatically have full access to `/warpsadmin` and all admin commands.

### Server Warps & General

| Permission | Default | Description | 
| ----- | ----- | ----- | 
| `ultimatewarps.admin` | `op` | Full access to all admin commands and `/warpsadmin` | 
| `ultimatewarps.spawn` | `true` | Allows using `/spawn` | 
| `ultimatewarps.warp.*` | `op` | Access to all permission-restricted server warps | 
| `ultimatewarps.warp.<name>` | `true` | Access to a specific server warp (when enabled in `/warpsadmin`) | 
| `ultimatewarps.bypass.cooldown` | `false` | Bypass spawn, server warp, and player warp cooldowns | 
| `ultimatewarps.bypass.delay` | `false` | Bypass spawn, server warp, and player warp teleport delays | 
| `ultimatewarps.rank.<group>` | `false` | Fallback rank multiplier permission (e.g., `vip`, `mvp`) when LuckPerms is not installed | 

### Player Warps

| Permission | Default | Description | 
| ----- | ----- | ----- | 
| `ultimatewarps.playerwarps.browse` | `true` | Browse `/pwarps` and visit public player warps | 
| `ultimatewarps.playerwarps.create` | `false` | Create and manage own player warps with `/pwarps set` | 
| `ultimatewarps.playerwarps.money` | `false` | Enable paid teleports and set warp prices | 
| `ultimatewarps.playerwarps.admin` | `op` | Staff oversight — view/wipe/teleport to any warp and bypass restrictions | 
| `ultimatewarps.playerwarps.limit.<number>` | `false` | Sets max player warps (e.g., `.1`, `.3`, `.5`, `.10`; highest wins) | 
| `ultimatewarps.playerwarps.limit.unlimited` | `false` | Removes the player warp limit completely | 

## Configuration Files

All configuration files are generated automatically inside `plugins/UltimateWarps/` and can be reloaded in-game with `/uwarps reload`:

* **`config.yml`** — Spawn settings, global warp defaults, countdown BossBar/title/particle/sound effects, rank multipliers, and messages.

* **`warps-gui.yml`** — Layout, rows, warp slots, navigation buttons, custom decorative items, and click sounds for `/warp`.

* **`playerwarps-config.yml`** — Feature toggles (`safe-warp`, `rating`, `money-require`, `whitelist`, `blacklist`, `favourites`), placement rules, teleport effects, and messages.

* **`playerwarps-gui.yml`** — Layout, warp item templates, buttons, and custom items for `/pwarps`.

## Building from Source

Requires **JDK 21** (or newer) and **Gradle**:

```
gradle build

```

The compiled plugin JAR will be created at `build/libs/UltimateWarps-4.1.jar`.

## Links & Author

* **Author:** `King_Le0_`

* **Discord:** [Click here to Join discord server for Support](https://discord.com/invite/r4gRhda8Mw)
