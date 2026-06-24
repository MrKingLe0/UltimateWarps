package com.ultimatewarps.playerwarps.commands;

import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import com.ultimatewarps.playerwarps.PlayerWarpManager;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import com.ultimatewarps.playerwarps.PlayerWarpsConfigManager;
import com.ultimatewarps.playerwarps.gui.PlayerWarpEditGUI;
import com.ultimatewarps.playerwarps.gui.PlayerWarpGUI;
import com.ultimatewarps.playerwarps.integration.ClaimGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /playerwarps (alias /pwarps) - everything related to player-created warps.
 *
 * Subcommands:
 *   /playerwarps                       - open the browser GUI
 *   /playerwarps gui                   - same as above, explicit
 *   /playerwarps set <name>             - create a warp at your location
 *   /playerwarps del <name>             - delete one of your own warps
 *   /playerwarps edit <name>            - open the edit menu for one of your own warps
 *   /playerwarps list [player]          - list your own warps, or another player's public ones
 *   /playerwarps warp <name>            - teleport to your own warp
 *   /playerwarps warp <owner> <name>    - teleport to another player's public warp
 *   /playerwarps admin info <player>    - staff: see a player's warp count/limit
 *   /playerwarps admin wipe <player>    - staff: delete every warp a player owns
 *   /playerwarps admin tp <owner> <name> - staff: teleport to any warp regardless of visibility
 *
 * Bug fix: teleporting used to be the unnamed default branch of the subcommand switch
 * ("/playerwarps <name>" / "/playerwarps <owner> <name>"), which meant a player-created
 * warp literally named "edit", "del", "set", "list", "gui", or "admin" could never be
 * teleported to directly - the switch always matched the reserved word first and ran
 * that subcommand instead. Teleporting now lives behind its own explicit "warp"
 * subcommand, so the only name that's actually reserved is the word "warp" itself.
 */
public class PlayerWarpsCommand implements CommandExecutor, TabCompleter {

    private final UltimateWarps plugin;

    public PlayerWarpsCommand(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();

        if (!config.featureEnabled() && !sender.hasPermission("ultimatewarps.playerwarps.admin")) {
            sender.sendMessage(config.getRawMessage("feature-disabled"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            return openGui(player);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui":
                return openGui(player);
            case "set":
                return handleSet(player, args);
            case "del":
            case "delete":
                return handleDelete(player, args);
            case "edit":
                return handleEdit(player, args);
            case "list":
                return handleList(player, args);
            case "warp":
                return handleTeleportShorthand(player, args);
            case "admin":
                return handleAdmin(player, args);
            default:
                player.sendMessage(Component.text(
                        "Unknown subcommand. Use /playerwarps warp <name> to teleport to a warp.",
                        NamedTextColor.RED));
                return true;
        }
    }

    private boolean openGui(Player player) {
        if (!player.hasPermission("ultimatewarps.playerwarps.create") && !player.hasPermission("ultimatewarps.playerwarps.browse")) {
            player.sendMessage(plugin.getPlayerWarpsConfigManager().getRawMessage("no-permission"));
            return true;
        }
        PlayerWarpGUI.getOrCreate(plugin, player).open(0);
        return true;
    }

    // ===================================================================================
    //  /playerwarps set <name>
    // ===================================================================================
    private boolean handleSet(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        PlayerWarpManager manager = plugin.getPlayerWarpManager();

        if (!player.hasPermission("ultimatewarps.playerwarps.create")) {
            player.sendMessage(config.getRawMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /playerwarps set <name>", NamedTextColor.RED));
            return true;
        }
        String name = args[1];

        if (!manager.isValidWarpName(name)) {
            player.sendMessage(config.getRawMessage("invalid-name"));
            return true;
        }
        if (manager.getWarp(player.getUniqueId(), name) != null) {
            player.sendMessage(config.getMessage("name-taken", "name", name));
            return true;
        }

        int limit = config.getWarpLimit(player);
        int current = manager.countWarpsByOwner(player.getUniqueId());
        if (current >= limit) {
            player.sendMessage(config.getMessage("limit-reached", "limit", String.valueOf(limit)));
            return true;
        }

        Location loc = player.getLocation();

        if (config.allowOverworldOnly() && loc.getWorld().getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage(config.getRawMessage("wrong-world"));
            return true;
        }

        if (config.minDistanceFromSpawn() > 0) {
            Location spawn = plugin.getConfigManager().getSpawnLocation();
            if (spawn != null && spawn.getWorld().equals(loc.getWorld())
                    && spawn.distance(loc) < config.minDistanceFromSpawn()) {
                player.sendMessage(config.getRawMessage("too-close-to-spawn"));
                return true;
            }
        }

        if (config.minDistanceFromOtherPlayerWarps() > 0) {
            double minDist = config.minDistanceFromOtherPlayerWarps();
            boolean tooClose = manager.getAllWarps().stream()
                    .filter(w -> w.getLocation().getWorld().equals(loc.getWorld()))
                    .anyMatch(w -> w.getLocation().distance(loc) < minDist);
            if (tooClose) {
                player.sendMessage(config.getRawMessage("too-close-to-other-warp"));
                return true;
            }
        }

        String denyReason = ClaimGuard.checkLocation(player, loc, config.requireOwnClaim(), config.requireOutsideRegion());
        if (denyReason != null) {
            player.sendMessage(config.getRawMessage(denyReason));
            return true;
        }

        PlayerWarp warp = new PlayerWarp(name, player.getUniqueId(), player.getName(), loc);
        try {
            manager.addWarp(warp);
        } catch (IllegalArgumentException e) {
            player.sendMessage(config.getRawMessage("invalid-name"));
            return true;
        }

        player.sendMessage(config.getMessage("warp-created", "name", name));
        return true;
    }

    // ===================================================================================
    //  /playerwarps del <name>
    // ===================================================================================
    private boolean handleDelete(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /playerwarps del <name>", NamedTextColor.RED));
            return true;
        }
        PlayerWarp warp = plugin.getPlayerWarpManager().getWarp(player.getUniqueId(), args[1]);
        if (warp == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return true;
        }
        plugin.getPlayerWarpManager().removeWarp(player.getUniqueId(), args[1]);
        player.sendMessage(config.getMessage("warp-deleted", "name", args[1]));
        return true;
    }

    // ===================================================================================
    //  /playerwarps edit <name>
    // ===================================================================================
    private boolean handleEdit(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /playerwarps edit <name>", NamedTextColor.RED));
            return true;
        }
        PlayerWarp warp = plugin.getPlayerWarpManager().getWarp(player.getUniqueId(), args[1]);
        if (warp == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return true;
        }
        new PlayerWarpEditGUI(player, warp).open();
        return true;
    }

    // ===================================================================================
    //  /playerwarps list [player]
    // ===================================================================================
    private boolean handleList(Player player, String[] args) {
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        List<PlayerWarp> warps;
        String headerName;

        if (args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            warps = manager.getWarpsByOwner(target.getUniqueId()).stream()
                    .filter(w -> w.isPublic() || player.hasPermission("ultimatewarps.playerwarps.admin")
                            || w.getOwnerId().equals(player.getUniqueId()))
                    .collect(Collectors.toList());
            headerName = target.getName() != null ? target.getName() : args[1];
        } else {
            warps = manager.getWarpsByOwner(player.getUniqueId());
            headerName = "your";
        }

        if (warps.isEmpty()) {
            player.sendMessage(Component.text("No player warps found.", NamedTextColor.GRAY));
            return true;
        }

        player.sendMessage(Component.text("Player warps for " + headerName + ":", NamedTextColor.AQUA));
        for (PlayerWarp w : warps) {
            String label = w.getDisplayName() != null ? w.getDisplayName() : w.getName();
            String visibility = w.isPublic() ? "public" : "private";
            player.sendMessage(Component.text(" - " + label + " (" + visibility + ")", NamedTextColor.GRAY));
        }
        return true;
    }

    // ===================================================================================
    //  /playerwarps warp <name>   and   /playerwarps warp <owner> <name>
    // ===================================================================================
    private boolean handleTeleportShorthand(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        PlayerWarp warp;

        // args[0] is the literal word "warp" here, so the actual warp name/owner
        // arguments are shifted one slot later than they used to be when this method
        // was reached directly off the top-level switch.
        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /playerwarps warp <name> or /playerwarps warp <owner> <name>",
                    NamedTextColor.RED));
            return true;
        }

        if (args.length == 2) {
            // /playerwarps warp <name> - the player's own warp.
            warp = manager.getWarp(player.getUniqueId(), args[1]);
            if (warp == null) {
                player.sendMessage(config.getRawMessage("warp-not-found"));
                return true;
            }
        } else {
            // /playerwarps warp <owner> <name> - someone else's warp.
            warp = manager.getWarp(args[1], args[2]);
            if (warp == null) {
                player.sendMessage(config.getRawMessage("warp-not-found"));
                return true;
            }
            boolean owner = warp.getOwnerId().equals(player.getUniqueId());
            boolean admin = player.hasPermission("ultimatewarps.playerwarps.admin");
            if (!warp.isPublic() && !owner && !admin) {
                player.sendMessage(config.getRawMessage("private-warp"));
                return true;
            }
        }

        startTeleport(player, warp);
        return true;
    }

    private void startTeleport(Player player, PlayerWarp warp) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        String cooldownKey = "pwarp_" + warp.getOwnerId() + "_" + warp.getName().toLowerCase();

        if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
            int remaining = plugin.getCooldownManager().getRemaining(player, cooldownKey);
            if (remaining > 0) {
                player.sendMessage(config.getMessage("cooldown", "seconds", String.valueOf(remaining)));
                return;
            }
        }

        int delay = config.delay();
        if (!player.hasPermission("ultimatewarps.bypass.delay")) {
            delay = (int) (delay * config.getRankMultiplier(player, "delay"));
        } else {
            delay = 0;
        }

        int cooldownSeconds = (int) (config.cooldown() * config.getRankMultiplier(player, "cooldown"));

        if (delay <= 0) {
            player.teleport(warp.getLocation());
            player.sendMessage(config.getMessage("teleport-confirmed", "name", displayLabel));
            if (!player.hasPermission("ultimatewarps.bypass.cooldown") && cooldownSeconds > 0) {
                plugin.getCooldownManager().setCooldown(player, cooldownKey, cooldownSeconds);
            }
        } else {
            new PlayerWarpTeleportTask(player, warp.getLocation(), delay, displayLabel).runTaskTimer(plugin, 0L, 20L);
            if (!player.hasPermission("ultimatewarps.bypass.cooldown") && cooldownSeconds > 0) {
                int finalCooldown = cooldownSeconds;
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        plugin.getCooldownManager().setCooldown(player, cooldownKey, finalCooldown), delay * 20L);
            }
        }
    }

    // ===================================================================================
    //  /playerwarps admin ...
    // ===================================================================================
    private boolean handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("ultimatewarps.playerwarps.admin")) {
            player.sendMessage(plugin.getPlayerWarpsConfigManager().getRawMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /playerwarps admin <info|wipe|tp> ...", NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "info": {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /playerwarps admin info <player>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                int count = plugin.getPlayerWarpManager().countWarpsByOwner(target.getUniqueId());
                int limit = target.getPlayer() != null
                        ? plugin.getPlayerWarpsConfigManager().getWarpLimit(target.getPlayer())
                        : -1;
                String limitText = limit < 0 ? "(unknown, player offline)" : String.valueOf(limit);
                player.sendMessage(Component.text(
                        (target.getName() != null ? target.getName() : args[2]) + " has " + count + " player warp(s), limit " + limitText + ".",
                        NamedTextColor.AQUA));
                return true;
            }
            case "wipe": {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /playerwarps admin wipe <player>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                int removed = plugin.getPlayerWarpManager().removeAllWarpsByOwner(target.getUniqueId());
                player.sendMessage(Component.text(
                        "Removed " + removed + " player warp(s) belonging to " + (target.getName() != null ? target.getName() : args[2]) + ".",
                        NamedTextColor.GREEN));
                return true;
            }
            case "tp": {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /playerwarps admin tp <owner> <name>", NamedTextColor.RED));
                    return true;
                }
                PlayerWarp warp = plugin.getPlayerWarpManager().getWarp(args[2], args[3]);
                if (warp == null) {
                    player.sendMessage(plugin.getPlayerWarpsConfigManager().getRawMessage("warp-not-found"));
                    return true;
                }
                player.teleport(warp.getLocation());
                player.sendMessage(Component.text("Teleported to " + args[2] + "'s warp '" + warp.getName() + "'.", NamedTextColor.GREEN));
                return true;
            }
            default:
                player.sendMessage(Component.text("Usage: /playerwarps admin <info|wipe|tp> ...", NamedTextColor.RED));
                return true;
        }
    }

    // ===================================================================================
    //  Tab completion
    // ===================================================================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            // Bug fix: warp names used to be suggested here too, since teleporting was the
            // default/unnamed branch of the subcommand switch. Now that teleporting lives
            // behind its own "warp" subcommand, only the actual subcommand words belong at
            // the top level - this also means a warp can finally be named "edit", "del",
            // etc. without colliding with the subcommand of the same name.
            List<String> options = new ArrayList<>(List.of("gui", "set", "del", "edit", "list", "warp"));
            if (player.hasPermission("ultimatewarps.playerwarps.admin")) options.add("admin");
            String partial = args[0].toLowerCase();
            return options.stream().filter(o -> o.toLowerCase().startsWith(partial)).sorted().collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            if (sub.equals("del") || sub.equals("delete") || sub.equals("edit")) {
                return plugin.getPlayerWarpManager().getWarpsByOwner(player.getUniqueId()).stream()
                        .map(PlayerWarp::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted().collect(Collectors.toList());
            }
            if (sub.equals("warp")) {
                // /playerwarps warp <name> - suggest the player's own warps first, then
                // online player names (for the "/playerwarps warp <owner> <name>" form).
                List<String> options = new ArrayList<>(plugin.getPlayerWarpManager().getWarpsByOwner(player.getUniqueId())
                        .stream().map(PlayerWarp::getName).toList());
                options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                return options.stream().filter(o -> o.toLowerCase().startsWith(partial))
                        .sorted().collect(Collectors.toList());
            }
            if (sub.equals("admin")) {
                return java.util.Arrays.asList("info", "wipe", "tp").stream()
                        .filter(o -> o.toLowerCase().startsWith(partial)).collect(Collectors.toList());
            }
            if (sub.equals("list")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("warp")) {
            // /playerwarps warp <owner> <name> - suggest that owner's warp names.
            OfflinePlayer owner = Bukkit.getOfflinePlayer(args[1]);
            String partial = args[2].toLowerCase();
            return plugin.getPlayerWarpManager().getWarpsByOwner(owner.getUniqueId()).stream()
                    .map(PlayerWarp::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .sorted().collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            String action = args[1].toLowerCase();
            String partial = args[2].toLowerCase();
            if (action.equals("info") || action.equals("wipe") || action.equals("tp")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("tp")) {
            // /playerwarps admin tp <owner> <name> - suggest that owner's warp names.
            OfflinePlayer owner = Bukkit.getOfflinePlayer(args[2]);
            String partial = args[3].toLowerCase();
            return plugin.getPlayerWarpManager().getWarpsByOwner(owner.getUniqueId()).stream()
                    .map(PlayerWarp::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .sorted().collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
