# UltimateWarps v4.1 — Security & Stability Patch

## Critical Fixes
- **GUI Item Pickup & Lag Protection (`GUIListener`, `AdminGUI`, `WarpGUI`, `PlayerWarp*GUI`)**:
  - Blocked uncancelled bottom-inventory double-click (`COLLECT_TO_CURSOR`), shift-click (`MOVE_TO_OTHER_INVENTORY`), number-key, and offhand-swap exploits across all GUIs.
  - Added `InventoryDragEvent` protection to all 6 GUIs.
  - Switched `WarpGUI` and `PlayerWarpGUI` to `InventoryHolder` checks and deferred all `openInventory()`/`closeInventory()` calls by 1 tick to eliminate cursor/window desyncs under lag.
  - Closed all open GUIs automatically on `/ultimatewarps reload` and plugin disable so listeners are never stripped while a menu is open.
  - Stopped `AdminGUI` and `PlayerWarpEditGUI` from deleting the player's real cursor item when setting a warp icon (and sanitized icons via `HeadUtils.cleanIcon()`).
- **PlayerWarps Economy & Countdown TOCTOU (`EconomyBridge`, `PlayerWarpsCommand`, `PlayerWarp`)**:
  - Added atomic Vault `withdrawPlayer`/`depositPlayer` payment flow with `transactionSuccess()` checks and automatic refund if `player.teleport()` is cancelled.
  - Fixed `parseBalanceString()` regex so currency words (like `credit`) don't trigger `k/m/b/t` multipliers and European decimals parse cleanly.
  - Rejected `NaN`/`Infinity` warp costs and rounded costs to 2 decimal places.
  - Re-verified live warp existence, visibility, blacklist/whitelist, and unchanged price inside `executeArrival()`.
- **WorldGuard & GriefPrevention (`ClaimGuard`, `plugin.yml`)**:
  - Fixed WorldGuard 7 `BlockVector3` reflection lookup (`NoSuchMethodException`), switched claim/region checks to fail-closed, fixed GriefPrevention subdivision ownership (`getOwnerID()`), and added `WorldGuard`, `GriefPrevention`, and `Vault` to `softdepend`.
- **Lifecycle & Soft-Dependency Crashes (`ConfigManager`, `PlayerWarpsConfigManager`, `JoinListener`, `ChatInput`, `TeleportTask`, `PlayerWarpTeleportTask`)**:
  - Guarded LuckPerms calls with `isPluginEnabled("LuckPerms")` and added permission fallback (`ultimatewarps.rank.*`) so servers without LuckPerms no longer crash with `NoClassDefFoundError`.
  - Cancelled active teleport tasks and `ChatInput` prompts on player quit, added `isOnline() && !isDead()` guards, and fixed `ChatInput` timeout map leaks.

## High & Medium Fixes
- Fixed `/spawn` `ultimatewarps.bypass.delay` (now sets delay to 0), added missing `ultimatewarps.spawn` check, and enforced `warp.enabled`.
- Replaced blocking main-thread `Bukkit.getOfflinePlayer(String)` calls with `Bukkit.getOfflinePlayerIfCached()` in `SpawnForceCommand`, `PlayerWarpsCommand`, and `PlayerWarpListGUI`.
- Sanitized untrusted `displayName`/`description` inputs to prevent MiniMessage `<click:run_command:...>` injection.
- Hardened `SafeLocationChecker` against signs, open trapdoors, unsupported gravity blocks, and overhead lava/anvils.
- Fixed Y-axis/world movement detection (`event.getTo()`), zero-damage teleport griefing, cooldown ceiling truncation, BossBar style mapping, unloaded-world `saveAll()` NPEs, tab-complete permission leaks, and `PlayerWarpGUI` pagination/config bugs.

## Minecraft 1.21.4 – 26.3 Compatibility
- Updated `build.gradle` to compile against `paper-api:1.21.4-R0.1-SNAPSHOT` targeting Java 21 bytecode (runs natively on Java 21 for 1.21.4–1.21.11 and Java 25 for 26.1–26.3).
- Replaced deprecated `Sound.getKey()` with `Registry.getKey(Sound)` via `ConfigManager.soundKey()` (26.x safe).
- Fixed `WarpGUI` click sound lookup (now uses `ConfigManager.parseSound()` instead of raw `Registry.SOUNDS.get`) and replaced `Enchantment.LURE` with `setEnchantmentGlintOverride(true)`.
- Migrated `ChatListener` from deprecated `AsyncPlayerChatEvent` to Paper's `AsyncChatEvent`.
- Updated `HeadUtils` to use `URI.create().toURL()` with deterministic `PlayerProfile` caching (`"uwhead"` <=16 ASCII chars).
- Fixed `TextFormat` unclosed `<`, `§x` hex codes, null templates, and `EffectManager` `"Spawn"` warp name collision.
