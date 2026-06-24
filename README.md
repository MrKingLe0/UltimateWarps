<img width="1774" height="887" alt="uwarp-v3" src="https://github.com/user-attachments/assets/11b724fd-5f0d-437d-b038-8aeae9b91854" />
<p align="center">
  <b style="font-size: 18px;">Advanced warp management plugin for Paper 1.21.x</b><br>
  <sub style="color: #94a3b8; font-size: 14px;">Stunning GUIs • Immersive Teleports • Player-Created Warps • LuckPerms Integration • Full Customization</sub>
</p>

---
<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.x-blue?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java" alt="Java 21">
  <img src="https://img.shields.io/badge/LuckPerms-Supported-green?style=for-the-badge" alt="LuckPerms">
  <img src="https://img.shields.io/badge/MiniMessage-Supported-ff69b4?style=for-the-badge" alt="MiniMessage">
</p>

---

## Key Features

* **Animated Teleport Effects** — Smooth countdown BossBars, particles, sounds, and customizable title messages for every spawn and warp.
* **Modern Paginated GUIs** — Clean player and admin menus with custom icons, player heads, filler items, and easy navigation.
* **Player Warps** — Let players create, manage, and share their own warps, completely separate from admin warps. Public or private visibility, custom icons/names, per-rank warp limits, optional placement restrictions (claim/region/world/distance), and the same fully animated teleport effects as admin warps.
* **Advanced MiniMessage Support** — Use gradients, hex colors, hover text, click actions, bold, italic, and more in every message.
* **LuckPerms Integration** — Automatically detects player ranks and applies custom cooldown or delay multipliers from config.
* **Per-Warp Customization** — Set unique permissions, icons, names, cooldowns, and teleport delays for each warp.
* **Fully Configurable** — Editable YAML files, customizable messages, modular settings, and a unified `/uwarps` command with tab completion.

---
<div align="center">
  
## Preview

<img width="1915" height="809" alt="image" src="https://github.com/user-attachments/assets/1dad8b4a-c978-48b5-8262-c003cfdfdea7" />
<img height="216" alt="image" src="https://github.com/user-attachments/assets/c66c91f4-7d32-44aa-8a41-7e78ba85ac0f" />
<img width="1919" height="832" alt="image" src="https://github.com/user-attachments/assets/429ff36a-49f8-4d93-b139-a4c43f4d1009" />
<img height="216" alt="image" src="https://github.com/user-attachments/assets/89df2403-cea4-4c6e-88ec-26cb6916305f" />
<img height="216" alt="image" src="https://github.com/user-attachments/assets/e6169008-8c9c-4c34-bcf4-3ade74ca656e" />
<img height="216" alt="image" src="https://github.com/user-attachments/assets/4ff329be-a81c-42f8-b676-1d1396b47d65" />
</div>

---

## Installation

1. Download the latest `.jar` from [Releases](https://github.com/MrKingLe0/UltimateWarps/releases/)
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/UltimateWarps/config.yml` (admin warps) and `plugins/UltimateWarps/playerwarps-config.yml` (player warps) to your liking

> **Soft-dependency:** [LuckPerms](https://luckperms.net) *(optional — required for rank-based cooldown/delay multipliers)*
> **Optional integrations:** GriefPrevention and WorldGuard *(optional — only used for player-warp placement restrictions if enabled in `playerwarps-config.yml`)*

---

## Commands

### Server Warps

| Command | Description |
|---|---|
| `/spawn` | Teleport to spawn |
| `/setspawn` | Set spawn location |
| `/delspawn` | Delete spawn location |
| `/warp` , `/warps` , `/warplist` | Open warp GUI |
| `/warp <name>` | Teleport directly to a warp |
| `/setwarp <name>` | Create a new warp at your location |
| `/delwarp <name>` | Delete a warp |
| `/warpsadmin` | Open admin management GUI |
| `/ultimatewarps reload` , `/uwarps reload` | Reload config & warps |
| `/uwarps spawn` , `/uwarps help` , … | Unified sub-commands |

### Player Warps

| Command | Description |
|---|---|
| `/playerwarps` , `/pwarps` | Open the player warps browser GUI |
| `/playerwarps set <name>` | Create a player warp at your location |
| `/playerwarps del <name>` | Delete one of your own player warps |
| `/playerwarps edit <name>` | Open the edit menu for one of your own player warps |
| `/playerwarps list [player]` | List your own warps, or another player's public warps |
| `/playerwarps warp <name>` | Teleport to one of your own player warps |
| `/playerwarps warp <owner> <name>` | Teleport to another player's public warp |
| `/playerwarps admin info <player>` | *(staff)* See a player's warp count and limit |
| `/playerwarps admin wipe <player>` | *(staff)* Delete every warp a player owns |
| `/playerwarps admin tp <owner> <name>` | *(staff)* Teleport to any warp, regardless of visibility |

---

## Permissions

### Server Warps

| Permission | Default |
|---|---|
| `ultimatewarps.admin` | `op` |
| `ultimatewarps.spawn` | `true` |
| `ultimatewarps.warp.*` | `true` |
| `ultimatewarps.warp.<name>` | `true` |
| `ultimatewarps.bypass.cooldown` | `false` |
| `ultimatewarps.bypass.delay` | `false` |
| `ultimatewarps.rank.vip` | `false` |
| `ultimatewarps.rank.mvp` | `false` |

### Player Warps

| Permission | Default | Description |
|---|---|---|
| `ultimatewarps.playerwarps.create` | `false` | Allows creating player warps with `/playerwarps set` |
| `ultimatewarps.playerwarps.browse` | `true` | Allows browsing the GUI and visiting public player warps |
| `ultimatewarps.playerwarps.admin` | `op` | Staff oversight — view/wipe any player's warps, teleport regardless of visibility |
| `ultimatewarps.playerwarps.limit.1` | `false` | Allows 1 player warp |
| `ultimatewarps.playerwarps.limit.3` | `false` | Allows 3 player warps |
| `ultimatewarps.playerwarps.limit.5` | `false` | Allows 5 player warps |
| `ultimatewarps.playerwarps.limit.10` | `false` | Allows 10 player warps |
| `ultimatewarps.playerwarps.limit.unlimited` | `false` | Removes the player warp limit entirely |

> Limit permissions don't stack — the highest number a player holds wins. Players with none of these fall back to `default-limit` in `playerwarps-config.yml`. `create` defaults to `false` so upgrading server owners can choose which groups get to plant player warps.

---

## Configuration Files

<details>
<summary align="center"><b>📁 config.yml (admin warps & spawn)</b></summary>

```yaml
#==============================================================================================
#    __  ______  _            __        _      __                
#   / / / / / /_(_)_ _  ___ _/ /____   | | /| / /__ ________  ___
#  / /_/ / / __/ /  ' \/ _ `/ __/ -_)  | |/ |/ / _ `/ __/ _ \/_-<
#  \____/_/\__/_/_/_/_/\_,_/\__/\__/   |__/|__/\_,_/_/ / .__/___/
#                                                      /_/         
#  UltimateWarps - v3.0 by King_Le0_ | Knock Me on Discord for support! username: King_Le0_
#==============================================================================================
spawn:
  enabled: true
  cancel-on-move: true
  cooldown: 10
  delay: 5
  title:
    enabled: true
    message: <b><gradient:#FF0000:#9400FF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ sᴘᴀᴡɴ</gradient></b>
    subtitle: <b><gradient:#5000FF:#9400FF>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ <white>%seconds%</white> sᴇᴄᴏɴᴅs</gradient></b>
  bossbar:
    enabled: true
    color: PURPLE
    style: SOLID
    text: <b><gradient:#5000FF:#00B3FF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ...</gradient></b>
  particle:
    enabled: true
    type: PORTAL #use "DUST" for colored particles
    count: 30
    dust:
      color: "#FF0000"
      size: 1.0
  sound:
    enabled: true
    type: BLOCK_NOTE_BLOCK_PLING
    volume: 1.0
    pitch: 1.0

warp:
  enabled: true
  cancel-on-move: true
  cooldown: 5
  delay: 3
  title:
    enabled: true
    message: <gradient:#00DCFF:#0036FF><b>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ</b> <white>%warp%</white></gradient>
    subtitle: <gradient:#4547FF:#A4B7FE><b>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ</b><white> %seconds%</white><b>sᴇᴄᴏɴᴅs</b></gradient>
  bossbar:
    enabled: true
    color: BLUE
    style: SOLID
    text: <b><gradient:#4547FF:#00DCFF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ %warp%</gradient></b>
  particle:
    enabled: true
    type: DUST   #use "DUST" for colored particles
    count: 20
    dust:
      color: "#5555FF"
      size: 1.0
  sound:
    enabled: true
    type: BLOCK_NOTE_BLOCK_PLING
    volume: 1.0
    pitch: 1.0

global:
  cancel-on-move: true
  default-cooldown: 5
  default-delay: 5

rank-multipliers:
  default:
    cooldown: 1.0
    delay: 1.0
  vip:
    cooldown: 0.5
    delay: 0.5
  mvp:
    cooldown: 0.5
    delay: 0.5

gui:
  warp-gui-title: <dark_gray>ᴡᴀʀᴘs
  admin-gui-title: <dark_gray>ᴡᴀʀᴘ ᴀᴅᴍɪɴ
  size: 54
  top-filler-material: MAGENTA_STAINED_GLASS_PANE
  mid-filler-material: BLACK_STAINED_GLASS_PANE

messages:
  teleport-cancelled-move: <red>❌<gradient:#ff5555:#ffaa00> ᴛᴇʟᴇᴘᴏʀᴛ ᴄᴀɴᴄᴇʟʟᴇᴅ ʙᴇᴄᴀᴜsᴇ
    ʏᴏᴜ ᴍᴏᴠᴇᴅ!</gradient>
  teleport-cancelled-damage: <red>💥<gradient:#ff5555:#ffaa00> ᴛᴇʟᴇᴘᴏʀᴛ ᴄᴀɴᴄᴇʟʟᴇᴅ
    ʙᴇᴄᴀᴜsᴇ ʏᴏᴜ ᴛᴏᴏᴋ ᴅᴀᴍᴀɢᴇ!</gradient>
  teleportation-confirmed: <green>✅<gradient:#55ff55:#00aa00> ʏᴏᴜ ʜᴀᴠᴇ ʙᴇᴇɴ ᴛᴇʟᴇᴘᴏʀᴛᴇᴅ
    ᴛᴏ <white>%warp%</white>!</gradient>
  warp-not-found: <red>🚫<gradient:#ff5555:#ffaa00> ᴡᴀʀᴘ ɴᴏᴛ ғᴏᴜɴᴅ!</gradient>
  no-permission: <red>🚫<gradient:#ff5555:#ffaa00> ʏᴏᴜ ᴅᴏ ɴᴏᴛ ʜᴀᴠᴇ ᴘᴇʀᴍɪssɪᴏɴ!</gradient>
  cooldown-active: <blue>⏳<gradient:#ff5555:#ffaa00> ʏᴏᴜ ᴍᴜsᴛ ᴡᴀɪᴛ <white><b>%seconds%</b></white>
    sᴇᴄᴏɴᴅs ʙᴇғᴏʀᴇ ᴜsɪɴɢ ᴛʜɪs ᴡᴀʀᴘ ᴀɢᴀɪɴ.</gradient>
  spawn-set: <green>🌍<gradient:#55ff55:#00aa00> sᴜᴄᴄᴇssғᴜʟʟʏ sᴇᴛ sᴘᴀᴡɴ ʟᴏᴄᴀᴛɪᴏɴ!</gradient>
  spawn-not-set: <red>🚫<gradient:#ff5555:#ffaa00> sᴘᴀᴡɴ ʟᴏᴄᴀᴛɪᴏɴ ɪs ɴᴏᴛ sᴇᴛ!</gradient>
  spawn-deleted: <red>🗑<gradient:#55ff55:#00aa00> sᴘᴀᴡɴ ʟᴏᴄᴀᴛɪᴏɴ ᴅᴇʟᴇᴛᴇᴅ!</gradient>
  warp-created: <blue>🌍<gradient:#55ff55:#00aa00> sᴜᴄᴄᴇssғᴜʟʟʏ ᴄʀᴇᴀᴛᴇᴅ ᴡᴀʀᴘ <white><b>%name%</b></white>!</gradient>
  warp-deleted: <red>🗑<gradient:#55ff55:#00aa00> sᴜᴄᴄᴇssғᴜʟʟʏ ᴅᴇʟᴇᴛᴇᴅ ᴡᴀʀᴘ <white><b>%name%</b></white>!</gradient>
  warp-edited: <blue>🌍<gradient:#55ff55:#00aa00> ᴡᴀʀᴘ <white><b>%name%</b></white>
    ᴜᴘᴅᴀᴛᴇᴅ!</gradient>
  reload-success: <blue>🌍<gradient:#55ff55:#00aa00> ᴜʟᴛɪᴍᴀᴛᴇᴡᴀʀᴘs ᴄᴏɴғɪɢᴜʀᴀᴛɪᴏɴ ᴀɴᴅ
    ᴡᴀʀᴘs ʀᴇʟᴏᴀᴅᴇᴅ!</gradient>
```
</details>

<details>
<summary align="center"><b>📁 playerwarps-config.yml (player-created warps)</b></summary>

```yaml
# Master switch. If false, /playerwarps and the per-player limit/create
# permissions all stop working - existing player warps are kept on disk
# but cannot be visited or edited until this is turned back on.
enabled: true

# How many player warps someone can have if they hold none of the
# numbered ultimatewarps.playerwarps.limit.<n> permission nodes.
# The highest numbered node a player holds always wins (they don't stack).
# ultimatewarps.playerwarps.limit.unlimited overrides everything.
default-limit: 1

gui:
  rows: 4
  title: "<dark_aqua><b>Player Warps</b></dark_aqua>"

# Teleport behavior for player warps - separate from the admin warp/spawn
# settings in config.yml, so you can make player warps slower/costlier
# than admin warps (or vice versa) without them interfering with each other.
teleport:
  cooldown: 30
  delay: 3
  cancel-on-move: true
  title:
    enabled: true
    message: "<b><gradient:#00DCFF:#7900FF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ <white>%warp%</white></gradient></b>"
    subtitle: "<gradient:#4547FF:#A4B7FE><b>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ</b><white> %seconds% </white><b>sᴇᴄᴏɴᴅs</b></gradient>"
  bossbar:
    enabled: true
    color: BLUE        # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    style: SOLID        # SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
    text: "<b><gradient:#4547FF:#00DCFF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ %warp%</gradient></b>"
  particle:
    enabled: true
    type: DUST          # any org.bukkit.Particle name; DUST uses the color/size below
    count: 30
    dust:
      color: "#5555FF"  # "#RRGGBB" hex or "R,G,B"
      size: 1.0
  sound:
    enabled: true
    type: BLOCK_NOTE_BLOCK_PLING   # any org.bukkit.Sound name
    volume: 1.0
    pitch: 1.0

# These run at /playerwarps set time, not at teleport time - they control
# WHERE a player is allowed to create a warp, not where they can visit one.
location-requirements:
  require-own-claim: false                    # needs GriefPrevention; must own the claim you're standing in
  require-outside-worldguard-region: false     # needs WorldGuard; blocks setting inside any region
  overworld-only: false                        # only allow creation in the overworld
  min-distance-from-spawn: 0                   # blocks; 0 disables
  min-distance-from-other-playerwarps: 0       # blocks; 0 disables

# Optional per-LuckPerms-group multipliers, same idea as config.yml's
# rank-multipliers. ultimatewarps.bypass.cooldown/delay always wins (0x).
rank-multipliers:
  default:
    cooldown: 1.0
    delay: 1.0
  vip:
    cooldown: 0.5
    delay: 0.5
  mvp:
    cooldown: 0.0
    delay: 0.0

messages:
  no-permission: "<red>You don't have permission to do that.</red>"
  feature-disabled: "<red>Player warps are currently disabled.</red>"
  limit-reached: "<red>You've reached your player warp limit (%limit%). Delete one with /playerwarps del <name> first.</red>"
  warp-created: "<green>Player warp '%name%' created!</green>"
  warp-deleted: "<green>Player warp '%name%' deleted.</green>"
  warp-not-found: "<red>That player warp doesn't exist.</red>"
  not-your-warp: "<red>That's not your warp.</red>"
  not-in-own-claim: "<red>You must be inside your own GriefPrevention claim to set a player warp here.</red>"
  inside-region: "<red>You can't set a player warp inside a WorldGuard region.</red>"
  wrong-world: "<red>Player warps can only be set in the overworld.</red>"
  too-close-to-spawn: "<red>That location is too close to spawn.</red>"
  too-close-to-other-warp: "<red>That location is too close to another player's warp.</red>"
  invalid-name: "<red>Warp names may only contain letters, numbers, underscores and hyphens (max 32 characters).</red>"
  name-taken: "<red>You already have a player warp named '%name%'.</red>"
  cooldown: "<red>You must wait %seconds% more second(s) before warping again.</red>"
  teleport-confirmed: "<green>Teleported to %name%!</green>"
  teleported-cancelled-move: "<red>Teleport cancelled - you moved!</red>"
  private-warp: "<red>That warp is private.</red>"
```
</details>

## Support

<p align="center"> Join Discord Server for help or updates:</p>

[<img width="2172" height="700" alt="discordhelpbanner" src="https://github.com/user-attachments/assets/c4279560-b178-416c-9084-9390f2bc4195" />](https://discord.gg/6gCFHFHsbD)
