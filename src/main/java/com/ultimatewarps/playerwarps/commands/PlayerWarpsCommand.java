package com.ultimatewarps.playerwarps.commands;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.*;
import com.ultimatewarps.playerwarps.gui.PlayerWarpEditGUI;
import com.ultimatewarps.playerwarps.gui.PlayerWarpGUI;
import com.ultimatewarps.playerwarps.gui.PlayerWarpListGUI;
import com.ultimatewarps.playerwarps.gui.PlayerWarpRateGUI;
import com.ultimatewarps.playerwarps.integration.ClaimGuard;
import com.ultimatewarps.playerwarps.integration.EconomyBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

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
            return openGui(player, false);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui":
                return openGui(player, false);
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
            case "rate":
                return handleRate(player, args);
            case "cost":
            case "money":
            case "price":
                return handleCost(player, args);
            case "whitelist":
            case "wl":
                return handleWhitelist(player, args);
            case "blacklist":
            case "bl":
                return handleBlacklist(player, args);
            case "favourite":
            case "favorite":
            case "favourites":
            case "favorites":
            case "fav":
                return handleFavourite(player, args);
            case "admin":
                return handleAdmin(player, args);
            default:
                player.sendMessage(Component.text(
                        "Unknown subcommand. Use /playerwarps <gui|set|del|edit|list|warp|rate|cost|whitelist|blacklist|favourite|admin>.",
                        NamedTextColor.RED));
                return true;
        }
    }

    private boolean openGui(Player player, boolean favouritesFilter) {
        if (!player.hasPermission("ultimatewarps.playerwarps.create")
                && !player.hasPermission("ultimatewarps.playerwarps.browse")) {
            player.sendMessage(plugin.getPlayerWarpsConfigManager().getRawMessage("no-permission"));
            return true;
        }
        PlayerWarpGUI gui = PlayerWarpGUI.getOrCreate(plugin, player);
        if (favouritesFilter) {
            gui.setFavouritesOnly(true);
        }
        gui.open(0);
        return true;
    }

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

        if (config.safeWarpCheckOnCreate() && !SafeLocationChecker.isSafeLocation(loc)) {
            player.sendMessage(config.getRawMessage("unsafe-warp-create"));
            return true;
        }

        if (config.allowOverworldOnly() && loc.getWorld().getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage(config.getRawMessage("wrong-world"));
            return true;
        }

        if (config.minDistanceFromSpawn() > 0) {
            Location spawn = plugin.getConfigManager().getSpawnLocation();
            if (spawn == null && loc.getWorld() != null) {
                spawn = loc.getWorld().getSpawnLocation();
            }
            if (spawn != null && Objects.equals(spawn.getWorld(), loc.getWorld())
                    && spawn.distance(loc) < config.minDistanceFromSpawn()) {
                player.sendMessage(config.getRawMessage("too-close-to-spawn"));
                return true;
            }
        }

        if (config.minDistanceFromOtherPlayerWarps() > 0) {
            double minDist = config.minDistanceFromOtherPlayerWarps();
            boolean tooClose = manager.getAllWarps().stream()
                    .filter(w -> Objects.equals(w.getLocation().getWorld(), loc.getWorld()))
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

    private OfflinePlayer resolveCachedPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private boolean canBrowse(Player player) {
        return player.hasPermission("ultimatewarps.playerwarps.browse")
                || player.hasPermission("ultimatewarps.playerwarps.create")
                || isStaff(player);
    }

    private boolean handleList(Player player, String[] args) {
        if (!canBrowse(player)) {
            player.sendMessage(plugin.getPlayerWarpsConfigManager().getRawMessage("no-permission"));
            return true;
        }
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        List<PlayerWarp> warps;
        String headerName;

        if (args.length >= 2) {
            OfflinePlayer target = resolveCachedPlayer(args[1]);
            if (target == null) {
                player.sendMessage(Component.text("No player warps found.", NamedTextColor.GRAY));
                return true;
            }
            warps = manager.getWarpsByOwner(target.getUniqueId()).stream()
                    .filter(w -> w.isPublic() || isStaff(player)
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

    private boolean handleTeleportShorthand(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        if (!canBrowse(player)) {
            player.sendMessage(config.getRawMessage("no-permission"));
            return true;
        }
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        PlayerWarp warp;

        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /playerwarps warp <name> or /playerwarps warp <owner> <name>",
                    NamedTextColor.RED));
            return true;
        }

        if (args.length == 2) {
            warp = manager.getWarp(player.getUniqueId(), args[1]);
            if (warp == null) {
                warp = manager.findVisibleWarpByName(player, args[1]);
            }
            if (warp == null) {
                player.sendMessage(config.getRawMessage("warp-not-found"));
                return true;
            }
        } else {
            warp = manager.getWarp(args[1], args[2]);
            if (warp == null) {
                player.sendMessage(config.getRawMessage("warp-not-found"));
                return true;
            }
        }

        boolean owner = warp.getOwnerId().equals(player.getUniqueId());
        boolean admin = isStaff(player);
        if (!warp.isPublic() && !owner && !admin) {
            player.sendMessage(config.getRawMessage("private-warp"));
            return true;
        }

        startTeleport(player, warp);
        return true;
    }

    private boolean isStaff(Player player) {
        return player.isOp()
                || player.hasPermission("ultimatewarps.playerwarps.admin")
                || player.hasPermission("ultimatewarps.admin");
    }

    private void startTeleport(Player player, PlayerWarp warp) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        boolean exempt = warp.getOwnerId().equals(player.getUniqueId()) || isStaff(player);

        // 1. Blacklist check (works even when whitelist is off)
        if (!exempt && config.blacklistEnabled() && warp.isBlacklisted(player.getUniqueId())) {
            player.sendMessage(config.getRawMessage("blacklisted-from-warp"));
            return;
        }

        // 2. Whitelist check
        if (!exempt && config.whitelistEnabled() && warp.isWhitelistEnabled()
                && !warp.isWhitelisted(player.getUniqueId())) {
            player.sendMessage(config.getRawMessage("not-whitelisted"));
            return;
        }

        // 3. Cooldown check
        String cooldownKey = "pwarp_" + warp.getOwnerId() + "_" + warp.getName().toLowerCase();
        if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
            int remaining = plugin.getCooldownManager().getRemaining(player, cooldownKey);
            if (remaining > 0) {
                player.sendMessage(config.getMessage("cooldown", "seconds", String.valueOf(remaining)));
                return;
            }
        }

        // 4. Safe warp location check (pre-countdown)
        if (config.safeWarpCheckOnTeleport() && !SafeLocationChecker.isSafeLocation(warp.getLocation())) {
            player.sendMessage(config.getMessage("unsafe-warp-teleport", "name", displayLabel));
            return;
        }

        // 5. Money requirement check (pre-countdown)
        boolean mustPay = !exempt && config.moneyEnabled() && warp.isMoneyRequired() && warp.getTeleportCost() > 0;
        if (mustPay) {
            double balance = EconomyBridge.getPlayerBalance(player, config.getBalancePlaceholder());
            if (Double.isNaN(balance)) {
                player.sendMessage(config.getRawMessage("papi-missing"));
                return;
            }
            if (balance < warp.getTeleportCost()) {
                player.sendMessage(config.getMessage("not-enough-money",
                        "amount", EconomyBridge.formatAmount(warp.getTeleportCost())));
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
        double expectedCost = mustPay ? EconomyBridge.roundCost(warp.getTeleportCost()) : 0.0;

        TeleportTask.cancelAllTasks(player);
        PlayerWarpTeleportTask.cancelAllTasks(player);

        if (delay <= 0) {
            executeArrival(player, warp.getOwnerId(), warp.getName(), expectedCost, cooldownKey, cooldownSeconds);
        } else {
            UUID targetOwner = warp.getOwnerId();
            String targetName = warp.getName();
            Runnable arrival = () -> executeArrival(player, targetOwner, targetName, expectedCost, cooldownKey, cooldownSeconds);
            new PlayerWarpTeleportTask(player, warp.getLocation(), delay, displayLabel, null, arrival)
                    .runTaskTimer(plugin, 0L, 20L);
        }
    }

    private void executeArrival(Player player, UUID ownerId, String warpName, double expectedCost, String cooldownKey, int cooldownSeconds) {
        if (!player.isOnline() || player.isDead()) return;
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();

        // Re-fetch live warp to prevent deleted-warp resurrection or stale references
        PlayerWarp warp = plugin.getPlayerWarpManager().getWarp(ownerId, warpName);
        if (warp == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return;
        }

        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        boolean exempt = warp.getOwnerId().equals(player.getUniqueId()) || isStaff(player);

        // Re-verify visibility, blacklist, and whitelist at arrival time
        if (!exempt) {
            if (!warp.isPublic()) {
                player.sendMessage(config.getRawMessage("private-warp"));
                return;
            }
            if (config.blacklistEnabled() && warp.isBlacklisted(player.getUniqueId())) {
                player.sendMessage(config.getRawMessage("blacklisted-from-warp"));
                return;
            }
            if (config.whitelistEnabled() && warp.isWhitelistEnabled() && !warp.isWhitelisted(player.getUniqueId())) {
                player.sendMessage(config.getRawMessage("not-whitelisted"));
                return;
            }
        }

        Location dest = warp.getLocation();
        if (dest == null || dest.getWorld() == null || Bukkit.getWorld(dest.getWorld().getUID()) == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return;
        }

        // Re-check safe location at arrival time
        if (config.safeWarpCheckOnTeleport() && !SafeLocationChecker.isSafeLocation(dest)) {
            player.sendMessage(config.getMessage("unsafe-warp-teleport", "name", displayLabel));
            return;
        }

        // Re-check and atomically charge money at arrival time (guard against mid-countdown price spikes)
        double currentCost = EconomyBridge.roundCost(warp.getTeleportCost());
        boolean mustPay = !exempt && config.moneyEnabled() && warp.isMoneyRequired() && currentCost > 0;
        if (mustPay) {
            if (currentCost > expectedCost + 0.001) {
                player.sendMessage(Component.text("Teleport cancelled: warp price changed during countdown.", NamedTextColor.RED));
                return;
            }
            double balance = EconomyBridge.getPlayerBalance(player, config.getBalancePlaceholder());
            if (Double.isNaN(balance)) {
                player.sendMessage(config.getRawMessage("papi-missing"));
                return;
            }
            if (balance < currentCost) {
                player.sendMessage(config.getMessage("not-enough-money",
                        "amount", EconomyBridge.formatAmount(currentCost)));
                return;
            }
            if (!EconomyBridge.chargeAndPay(player, warp, config)) {
                player.sendMessage(config.getMessage("not-enough-money",
                        "amount", EconomyBridge.formatAmount(currentCost)));
                return;
            }
        }

        if (!player.teleport(dest)) {
            if (mustPay) {
                EconomyBridge.refund(player, warp);
            }
            return;
        }

        if (mustPay) {
            String formattedCost = EconomyBridge.formatAmount(currentCost);
            player.sendMessage(config.getMessage("money-paid",
                    "amount", formattedCost,
                    "name", displayLabel));
            Player ownerOnline = Bukkit.getPlayer(warp.getOwnerId());
            if (ownerOnline != null && ownerOnline.isOnline()) {
                ownerOnline.sendMessage(config.getMessage("money-received",
                        "visitor", player.getName(),
                        "amount", formattedCost,
                        "name", displayLabel));
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage(config.getMessage("teleport-confirmed", "name", displayLabel));

        if (!player.hasPermission("ultimatewarps.bypass.cooldown") && cooldownSeconds > 0) {
            plugin.getCooldownManager().setCooldown(player, cooldownKey, cooldownSeconds);
        }

        // Record visit (only save on first visit) and send clickable rating prompt
        if (!warp.getOwnerId().equals(player.getUniqueId())) {
            if (warp.recordVisit(player.getUniqueId())) {
                warp.save();
            }
            if (config.ratingEnabled() && !warp.hasRated(player.getUniqueId())) {
                player.sendMessage(config.getMessage("rate-warp-prompt",
                        "name", displayLabel,
                        "owner", warp.getOwnerName() != null ? warp.getOwnerName() : "",
                        "warp", warp.getName()));
            }
        }
    }

    // ===== Rate Subcommand =====
    private boolean handleRate(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        PlayerWarpManager manager = plugin.getPlayerWarpManager();

        if (!config.ratingEnabled()) {
            player.sendMessage(config.getRawMessage("subfeature-disabled"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /playerwarps rate [owner] <warp> [1-5]", NamedTextColor.RED));
            return true;
        }

        PlayerWarp warp = null;
        Integer directStars = null;

        if (args.length == 2) {
            warp = manager.findVisibleWarpByName(player, args[1]);
        } else if (args.length == 3) {
            warp = manager.getWarp(args[1], args[2]);
            if (warp == null) {
                warp = manager.findVisibleWarpByName(player, args[1]);
                if (warp != null) {
                    try { directStars = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
            }
        } else {
            warp = manager.getWarp(args[1], args[2]);
            try { directStars = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
        }

        if (warp == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return true;
        }
        if (warp.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage(config.getRawMessage("cannot-rate-own-warp"));
            return true;
        }
        if (!warp.hasVisited(player.getUniqueId())) {
            player.sendMessage(config.getRawMessage("must-visit-to-rate"));
            return true;
        }

        if (directStars != null && directStars >= 1 && directStars <= 5) {
            warp.setRating(player.getUniqueId(), directStars);
            warp.save();
            String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
            player.sendMessage(config.getMessage("warp-rated",
                    "name", displayLabel,
                    "stars", String.valueOf(directStars)));
            return true;
        }

        new PlayerWarpRateGUI(player, warp, false).open();
        return true;
    }

    // ===== Cost / Money Subcommand =====
    private boolean handleCost(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        if (!config.moneyEnabled()) {
            player.sendMessage(config.getRawMessage("subfeature-disabled"));
            return true;
        }
        if (!player.hasPermission(config.getMoneyPermission()) && !isStaff(player)) {
            player.sendMessage(config.getRawMessage("no-permission"));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /playerwarps cost <warp> <amount|on|off|toggle>", NamedTextColor.RED));
            return true;
        }

        PlayerWarp warp = plugin.getPlayerWarpManager().getWarp(player.getUniqueId(), args[1]);
        if (warp == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return true;
        }

        String valArg = args[2].toLowerCase();
        switch (valArg) {
            case "on", "enable", "true" -> {
                warp.setMoneyRequired(true);
                warp.save();
                player.sendMessage(config.getMessage("money-toggled",
                        "name", warp.getName(), "status", "enabled"));
            }
            case "off", "disable", "false" -> {
                warp.setMoneyRequired(false);
                warp.save();
                player.sendMessage(config.getMessage("money-toggled",
                        "name", warp.getName(), "status", "disabled"));
            }
            case "toggle" -> {
                warp.setMoneyRequired(!warp.isMoneyRequired());
                warp.save();
                player.sendMessage(config.getMessage("money-toggled",
                        "name", warp.getName(),
                        "status", warp.isMoneyRequired() ? "enabled" : "disabled"));
            }
            default -> {
                try {
                    double amount = Double.parseDouble(valArg);
                    if (!Double.isFinite(amount) || amount < 0) {
                        player.sendMessage(config.getRawMessage("invalid-amount"));
                        return true;
                    }
                    double rounded = EconomyBridge.roundCost(amount);
                    warp.setTeleportCost(rounded);
                    warp.setMoneyRequired(rounded > 0);
                    warp.save();
                    player.sendMessage(config.getMessage("money-cost-set",
                            "name", warp.getName(),
                            "amount", EconomyBridge.formatAmount(rounded)));
                } catch (NumberFormatException e) {
                    player.sendMessage(config.getRawMessage("invalid-amount"));
                }
            }
        }
        return true;
    }

    // ===== Whitelist Subcommand =====
    private boolean handleWhitelist(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        if (!config.whitelistEnabled()) {
            player.sendMessage(config.getRawMessage("subfeature-disabled"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /playerwarps whitelist <warp> [gui|on|off|toggle|add|remove|list] [player]",
                    NamedTextColor.RED));
            return true;
        }

        PlayerWarp warp = manager.getWarp(player.getUniqueId(), args[1]);
        if (warp == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return true;
        }

        if (args.length == 2 || args[2].equalsIgnoreCase("gui")) {
            new PlayerWarpListGUI(player, warp, PlayerWarpListGUI.Mode.WHITELIST).open(0);
            return true;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "on", "enable", "true" -> {
                warp.setWhitelistEnabled(true);
                warp.save();
                player.sendMessage(config.getMessage("whitelist-toggled",
                        "name", warp.getName(), "status", "enabled"));
            }
            case "off", "disable", "false" -> {
                warp.setWhitelistEnabled(false);
                warp.save();
                player.sendMessage(config.getMessage("whitelist-toggled",
                        "name", warp.getName(), "status", "disabled"));
            }
            case "toggle" -> {
                warp.setWhitelistEnabled(!warp.isWhitelistEnabled());
                warp.save();
                player.sendMessage(config.getMessage("whitelist-toggled",
                        "name", warp.getName(),
                        "status", warp.isWhitelistEnabled() ? "enabled" : "disabled"));
            }
            case "add" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /playerwarps whitelist <warp> add <player>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = resolveCachedPlayer(args[3]);
                if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                    player.sendMessage(Component.text("Player not found: " + args[3], NamedTextColor.RED));
                    return true;
                }
                String targetName = target.getName() != null ? target.getName() : args[3];
                if (target.getUniqueId().equals(warp.getOwnerId())) {
                    player.sendMessage(config.getRawMessage("cannot-list-self"));
                    return true;
                }
                if (manager.isStaff(target)) {
                    player.sendMessage(config.getRawMessage("cannot-list-staff"));
                    return true;
                }
                warp.addWhitelistedPlayer(target.getUniqueId(), targetName);
                warp.save();
                player.sendMessage(config.getMessage("whitelist-added",
                        "player", targetName, "name", warp.getName()));
            }
            case "remove", "del", "delete" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /playerwarps whitelist <warp> remove <player>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = resolveCachedPlayer(args[3]);
                if (target == null) {
                    player.sendMessage(Component.text("Player not found: " + args[3], NamedTextColor.RED));
                    return true;
                }
                String targetName = target.getName() != null ? target.getName() : args[3];
                warp.removeWhitelistedPlayer(target.getUniqueId());
                warp.save();
                player.sendMessage(config.getMessage("whitelist-removed",
                        "player", targetName, "name", warp.getName()));
            }
            case "list" -> {
                Set<UUID> wl = warp.getWhitelist();
                if (wl.isEmpty()) {
                    player.sendMessage(Component.text("Whitelist for '" + warp.getName() + "' is empty.", NamedTextColor.GRAY));
                } else {
                    String names = wl.stream().map(warp::getKnownPlayerName).collect(Collectors.joining(", "));
                    player.sendMessage(Component.text("Whitelisted on '" + warp.getName() + "': " + names, NamedTextColor.GREEN));
                }
            }
            default -> player.sendMessage(Component.text(
                    "Usage: /playerwarps whitelist <warp> <gui|on|off|toggle|add|remove|list> [player]",
                    NamedTextColor.RED));
        }
        return true;
    }

    // ===== Blacklist Subcommand =====
    private boolean handleBlacklist(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        if (!config.blacklistEnabled()) {
            player.sendMessage(config.getRawMessage("subfeature-disabled"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text(
                    "Usage: /playerwarps blacklist <warp> [gui|add|remove|list] [player]",
                    NamedTextColor.RED));
            return true;
        }

        PlayerWarp warp = manager.getWarp(player.getUniqueId(), args[1]);
        if (warp == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return true;
        }

        if (args.length == 2 || args[2].equalsIgnoreCase("gui")) {
            new PlayerWarpListGUI(player, warp, PlayerWarpListGUI.Mode.BLACKLIST).open(0);
            return true;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /playerwarps blacklist <warp> add <player>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = resolveCachedPlayer(args[3]);
                if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                    player.sendMessage(Component.text("Player not found: " + args[3], NamedTextColor.RED));
                    return true;
                }
                String targetName = target.getName() != null ? target.getName() : args[3];
                if (target.getUniqueId().equals(warp.getOwnerId())) {
                    player.sendMessage(config.getRawMessage("cannot-list-self"));
                    return true;
                }
                if (manager.isStaff(target)) {
                    player.sendMessage(config.getRawMessage("cannot-list-staff"));
                    return true;
                }
                warp.addBlacklistedPlayer(target.getUniqueId(), targetName);
                warp.save();
                player.sendMessage(config.getMessage("blacklist-added",
                        "player", targetName, "name", warp.getName()));
            }
            case "remove", "del", "delete" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /playerwarps blacklist <warp> remove <player>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = resolveCachedPlayer(args[3]);
                if (target == null) {
                    player.sendMessage(Component.text("Player not found: " + args[3], NamedTextColor.RED));
                    return true;
                }
                String targetName = target.getName() != null ? target.getName() : args[3];
                warp.removeBlacklistedPlayer(target.getUniqueId());
                warp.save();
                player.sendMessage(config.getMessage("blacklist-removed",
                        "player", targetName, "name", warp.getName()));
            }
            case "list" -> {
                Set<UUID> bl = warp.getBlacklist();
                if (bl.isEmpty()) {
                    player.sendMessage(Component.text("Blacklist for '" + warp.getName() + "' is empty.", NamedTextColor.GRAY));
                } else {
                    String names = bl.stream().map(warp::getKnownPlayerName).collect(Collectors.joining(", "));
                    player.sendMessage(Component.text("Blacklisted on '" + warp.getName() + "': " + names, NamedTextColor.RED));
                }
            }
            default -> player.sendMessage(Component.text(
                    "Usage: /playerwarps blacklist <warp> <gui|add|remove|list> [player]",
                    NamedTextColor.RED));
        }
        return true;
    }

    // ===== Favourite Subcommand =====
    private boolean handleFavourite(Player player, String[] args) {
        PlayerWarpsConfigManager config = plugin.getPlayerWarpsConfigManager();
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        if (!config.favouritesEnabled()) {
            player.sendMessage(config.getRawMessage("subfeature-disabled"));
            return true;
        }
        if (!canBrowse(player)) {
            player.sendMessage(config.getRawMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            return openGui(player, true);
        }

        PlayerWarp warp;
        if (args.length == 2) {
            warp = manager.findVisibleWarpByName(player, args[1]);
        } else {
            warp = manager.getWarp(args[1], args[2]);
        }

        if (warp == null || (!warp.isPublic() && !warp.getOwnerId().equals(player.getUniqueId()) && !isStaff(player))) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return true;
        }

        boolean added = manager.toggleFavourite(player.getUniqueId(), warp);
        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        player.sendMessage(config.getMessage(added ? "favourite-added" : "favourite-removed",
                "name", displayLabel));
        return true;
    }

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
                OfflinePlayer target = resolveCachedPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                    return true;
                }
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
                OfflinePlayer target = resolveCachedPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                    return true;
                }
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
                if (warp == null || warp.getLocation() == null || warp.getLocation().getWorld() == null) {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of(
                    "gui", "set", "del", "edit", "list", "warp",
                    "rate", "cost", "whitelist", "blacklist", "favourite"
            ));
            if (player.hasPermission("ultimatewarps.playerwarps.admin")) options.add("admin");
            String partial = args[0].toLowerCase();
            return options.stream().filter(o -> o.toLowerCase().startsWith(partial)).sorted().collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            if (sub.equals("del") || sub.equals("delete") || sub.equals("edit")
                    || sub.equals("cost") || sub.equals("money") || sub.equals("price")
                    || sub.equals("whitelist") || sub.equals("wl")
                    || sub.equals("blacklist") || sub.equals("bl")) {
                return plugin.getPlayerWarpManager().getWarpsByOwner(player.getUniqueId()).stream()
                        .map(PlayerWarp::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted().collect(Collectors.toList());
            }
            if (sub.equals("warp") || sub.equals("rate") || sub.startsWith("fav")) {
                Set<String> options = new HashSet<>();
                plugin.getPlayerWarpManager().getVisibleWarps(player)
                        .forEach(w -> options.add(w.getName()));
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                return options.stream().filter(o -> o.toLowerCase().startsWith(partial))
                        .sorted().collect(Collectors.toList());
            }
            if (sub.equals("admin") && isStaff(player)) {
                return List.of("info", "wipe", "tp").stream()
                        .filter(o -> o.toLowerCase().startsWith(partial)).collect(Collectors.toList());
            }
            if (sub.equals("list")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String partial = args[2].toLowerCase();
            if (sub.equals("warp") || sub.equals("rate") || sub.startsWith("fav")) {
                OfflinePlayer owner = resolveCachedPlayer(args[1]);
                if (owner == null) return Collections.emptyList();
                return plugin.getPlayerWarpManager().getWarpsByOwner(owner.getUniqueId()).stream()
                        .filter(w -> w.isPublic() || w.getOwnerId().equals(player.getUniqueId()) || isStaff(player))
                        .map(PlayerWarp::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted().collect(Collectors.toList());
            }
            if (sub.equals("cost") || sub.equals("money") || sub.equals("price")) {
                return List.of("on", "off", "toggle", "100", "500", "1000").stream()
                        .filter(o -> o.startsWith(partial)).collect(Collectors.toList());
            }
            if (sub.equals("whitelist") || sub.equals("wl")) {
                return List.of("gui", "on", "off", "toggle", "add", "remove", "list").stream()
                        .filter(o -> o.startsWith(partial)).collect(Collectors.toList());
            }
            if (sub.equals("blacklist") || sub.equals("bl")) {
                return List.of("gui", "add", "remove", "list").stream()
                        .filter(o -> o.startsWith(partial)).collect(Collectors.toList());
            }
            if (sub.equals("admin") && isStaff(player)) {
                String action = args[1].toLowerCase();
                if (action.equals("info") || action.equals("wipe") || action.equals("tp")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
                }
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String partial = args[3].toLowerCase();
            if ((sub.equals("whitelist") || sub.equals("wl") || sub.equals("blacklist") || sub.equals("bl"))
                    && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
            }
            if (sub.equals("admin") && isStaff(player) && args[1].equalsIgnoreCase("tp")) {
                OfflinePlayer owner = resolveCachedPlayer(args[2]);
                if (owner == null) return Collections.emptyList();
                return plugin.getPlayerWarpManager().getWarpsByOwner(owner.getUniqueId()).stream()
                        .map(PlayerWarp::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted().collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
