# Changelog

## [3.1.0] - 2026-09-10

### Added

- **Per-warp teleport effects.** Every admin warp can now override the global particle and sound from `config.yml`. A new **Teleport Effects** menu in the warp editor lets you:
  - switch between **Global** and **Custom** mode (switching to Custom copies your current global settings as a starting point)
  - toggle the particle and sound on or off
  - set the particle type, count and dust colour
  - set the sound type, volume and pitch

  The countdown tick sound also uses the warp's custom sound.
- **Particle picker.** Lists every particle available on your server version, sorted A-Z, with pagination.
- **Sound picker.** Lists every sound in the server's sound registry (about 1,400 on 1.21.x), sorted A-Z, with pagination. Clicking a sound selects it and plays a preview.
- **Dust colour picker.** Offers the 16 dye colours, or type any `#RRGGBB` / `r,g,b` value in chat.
- **Type-in-chat fallback** on every picker, for anything not listed.
- **Pin to slot.** Pin a warp to a fixed slot in the `/warps` GUI. The slot picker shows which slots are free, already taken by another warp, or pinned to this one. **Reset to auto** unpins it. Unpinned warps fill the remaining slots around pinned ones.
- **Custom lore.** Add lore lines to any warp through chat. Use `|` to separate lines and `clear` to remove them. Custom lore appears above the lore template from `warps-gui.yml`.
- **Icon glow toggle.** Adds the enchantment shimmer to a warp's icon without needing a real enchantment.

### Changed

- **Warp editor layout reorganised.** The settings now sit in two clean rows of five, with **Back / Set Location / Delete** across the bottom row. The editor grew from 3 rows to 4 to fit them.
- The slot picker's **Reset** button now sits in the middle of the bottom row.
- The **Effect Mode** button now shows a different head for Global and Custom mode, so you can tell the current mode at a glance.
- Warp icons no longer show vanilla tooltip clutter (attack damage, armour, enchantment lists, etc.) in either the player or admin GUI. Custom player-head textures are kept.
- Feedback messages in the admin warp editor now use small caps in the blue-purple theme.
- Data-carrying particles such as `BLOCK`, `ITEM` and `DUST_COLOR_TRANSITION` now render with sensible defaults. Previously they always fell back to `PORTAL`.
- Per-warp sounds are now saved as registry keys (`block.note_block.pling`). The old names (`BLOCK_NOTE_BLOCK_PLING`) and namespaced keys (`minecraft:block.note_block.pling`) are still accepted everywhere, so existing configs keep working.

### Fixed

- **Cancelled teleports no longer start a cooldown.** Cooldowns were applied on a timer even when a teleport was cancelled (by moving or taking damage), so you could be locked out of a warp you never reached. They are now applied only when the teleport completes. This fix covers `/spawn`, `/warp` and player warps.
- **Configured sounds now work.** Any `sound.type` in `config.yml` other than the default silently fell back to the note-block pling. All sound names now resolve correctly.
- The sound picker no longer shows up empty.
- The `/warps` GUI now calculates its page count correctly when some warps are pinned to slots.

### Removed

- These config keys were never read by the plugin and have been removed. You can safely delete them from existing files:
  - `config.yml`: `settings.teleport-cancel-on-move`, `global.cancel-on-move`, and the legacy `ranks:` section (replaced by `rank-multipliers:`)
  - `playerwarps-config.yml`: `gui.rows` and `gui.title` (player-warp GUI settings live in `playerwarps-gui.yml`)

### Upgrading from 3.0.x

Drop in the new jar and restart. You don't need to migrate anything: existing warps, configs and sound names load unchanged.
