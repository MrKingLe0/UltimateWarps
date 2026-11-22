<img width="1774" height="887" alt="uwarp-v3" src="https://github.com/user-attachments/assets/11b724fd-5f0d-437d-b038-8aeae9b91854" />
<p align="center">
  <b style="font-size: 18px;">Advanced warp management plugin for Paper 1.21.x (v4.0)</b><br>
  <sub style="color: #94a3b8; font-size: 14px;">Stunning GUIs • Immersive Teleports • Player-Created Warps • Ratings • Economy • Whitelist/Blacklist • Favourites</sub>
</p>

## What's New in v4.0 (Player Warps)

All new Player Warps features have independent toggle switches in `playerwarps-config.yml`:

* **Safe Warp Place Detection (`safe-warp`)** — Checks the 3 blocks at the warp position (solid ground below and at least 2 blocks of air at the warp position and one block up, free of hazards). Can be toggled independently for warp creation (`check-on-create`) and teleportation (`check-on-teleport`). Unsafe teleports are cancelled with a configurable message.
* **Warps Rating (`rating`)** — After visiting another player's warp that they haven't rated yet, visitors receive a clickable chat prompt to rate the warp from 1 to 5 stars in the Rating GUI (or via Shift+Right-Click in the browser GUI / `/playerwarps rate`). The rating always appears on the first line of the warp lore (`Not rated yet` when unrated).
* **Money Requirement (`money-require`)** — Players with `ultimatewarps.playerwarps.money` can enable a teleport cost on their warp and set the amount via the Edit Warp GUI or `/playerwarps cost <warp> <amount|on|off|toggle>`. Uses a configurable PlaceholderAPI selector (`balance-placeholder`, default `%vault_eco_balance%`) when the feature is active, and runs configurable console commands (`give-command`: `/eco give %warpowner% %amount%`, `take-command`: `/eco take %visitor% %amount%`). Server staff and the warp owner never pay.
* **Whitelist & Blacklist (`whitelist` / `blacklist`)** — Warp owners can toggle whitelist mode and manage whitelisted and blacklisted players through separate paginated GUIs (via the Edit Warp GUI or `/playerwarps whitelist <warp> gui` and `/playerwarps blacklist <warp> gui`) as well as commands. Blacklist blocks players even when whitelist mode is off. Server staff and the warp owner cannot be whitelisted/blacklisted and always bypass restrictions.
* **Favourite Warps (`favourites`)** — Adds a **Favourite** button in `playerwarps-gui.yml` to filter favourite warps, and allows players to add or remove warps from their favourites list by right-clicking warps in the GUI or using `/playerwarps favourite`.

## Player Warps Commands

| Command | Description |
|---|---|
| `/playerwarps` , `/pwarps` | Open the player warps browser GUI |
| `/playerwarps set <name>` | Create a player warp at your location |
| `/playerwarps del <name>` | Delete one of your own player warps |
| `/playerwarps edit <name>` | Open the edit menu for one of your own player warps |
| `/playerwarps list [player]` | List your own warps, or another player's public warps |
| `/playerwarps warp <name>` | Teleport to a player warp |
| `/playerwarps warp <owner> <name>` | Teleport to another player's warp |
| `/playerwarps rate [owner] <warp> [1-5]` | Open the Rating GUI (or rate 1-5 stars) after visiting a warp |
| `/playerwarps cost <warp> <amount\|on\|off\|toggle>` | Set teleport cost or toggle money requirement on your warp |
| `/playerwarps whitelist <warp> [gui\|on\|off\|toggle\|add\|remove\|list] [player]` | Manage your warp's whitelist or open the Whitelist GUI |
| `/playerwarps blacklist <warp> [gui\|add\|remove\|list] [player]` | Manage your warp's blacklist or open the Blacklist GUI |
| `/playerwarps favourite [owner] [warp]` | Open favourite warps in GUI or add/remove a warp from favourites |
| `/playerwarps admin <info\|wipe\|tp> ...` | *(staff)* Admin oversight commands |
