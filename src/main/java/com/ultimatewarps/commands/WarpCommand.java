package com.ultimatewarps.commands;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.Warp;
import com.ultimatewarps.gui.WarpGUI;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final UltimateWarps plugin;
    private final Map<UUID, Long> cooldownMessages = new HashMap<>();
    private final Map<UUID, Long> commandCooldown = new HashMap<>();

    public void clearPlayer(UUID uuid) {
        cooldownMessages.remove(uuid);
        commandCooldown.remove(uuid);
    }

    public WarpCommand(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (!plugin.getConfigManager().warpEnabled() && !player.hasPermission("ultimatewarps.admin")) {
            player.sendMessage(plugin.getConfigManager().getWarpNotFoundMessage());
            return true;
        }

        long now = System.currentTimeMillis();

        if (args.length == 0) {
            long lastOpen = commandCooldown.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastOpen < 1000) {
                return true;
            }
            commandCooldown.put(player.getUniqueId(), now);
            WarpGUI.getOrCreate(plugin, player).open();
            return true;
        }

        Warp warp = plugin.getWarpManager().getWarp(args[0]);
        if (warp == null || !warp.isEnabled()) {
            player.sendMessage(plugin.getConfigManager().getWarpNotFoundMessage());
            return true;
        }

        String requiredPermission = warp.getPermission();
        boolean hasAccess = requiredPermission == null || requiredPermission.isEmpty()
                || player.hasPermission(requiredPermission)
                || player.hasPermission("ultimatewarps.warp.*");
        if (!hasAccess) {
            player.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
            return true;
        }

        String cooldownKey = "warp_" + warp.getName();
        if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
            if (plugin.getCooldownManager().hasCooldown(player, cooldownKey)) {
                int remaining = plugin.getCooldownManager().getRemaining(player, cooldownKey);
                long lastMessage = cooldownMessages.getOrDefault(player.getUniqueId(), 0L);

                if (now - lastMessage > 2000) {
                    player.sendMessage(plugin.getConfigManager().getCooldownMessage(remaining));
                    cooldownMessages.put(player.getUniqueId(), now);
                }
                return true;
            }
        }

        TeleportTask.cancelAllTasks(player);
        PlayerWarpTeleportTask.cancelAllTasks(player);

        startTeleport(player, warp);
        return true;
    }

    private void startTeleport(Player player, Warp warp) {
        Location dest = warp.getLocation();
        if (dest == null || dest.getWorld() == null || Bukkit.getWorld(dest.getWorld().getUID()) == null) {
            player.sendMessage(plugin.getConfigManager().getWarpNotFoundMessage());
            return;
        }

        int delay = warp.getDelay();

        if (!player.hasPermission("ultimatewarps.bypass.delay")) {
            delay = (int) (delay * plugin.getConfigManager().getRankMultiplier(player, "delay"));
        } else {
            delay = 0;
        }

        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();

        if (delay <= 0) {
            if (!player.teleport(dest)) {
                return;
            }

            plugin.getEffectManager().playTeleportEffect(player, warp.getName(), warp);
            Component msg = plugin.getConfigManager().getTeleportConfirmedMessage(displayLabel);
            player.sendMessage(msg);

            if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
                int cooldown = (int) (warp.getCooldown() * plugin.getConfigManager().getRankMultiplier(player, "cooldown"));
                if (cooldown > 0) {
                    plugin.getCooldownManager().setCooldown(player, "warp_" + warp.getName(), cooldown);
                }
            }
        } else {
            Runnable onSuccess = null;
            if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
                int finalCooldown = (int) (warp.getCooldown() * plugin.getConfigManager().getRankMultiplier(player, "cooldown"));
                if (finalCooldown > 0) {
                    onSuccess = () -> plugin.getCooldownManager().setCooldown(player, "warp_" + warp.getName(), finalCooldown);
                }
            }

            new TeleportTask(player, dest, delay, warp.getName(), displayLabel, onSuccess, warp).runTaskTimer(plugin, 0L, 20L);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            if (!plugin.getConfigManager().warpEnabled() && !player.hasPermission("ultimatewarps.admin")) {
                return new ArrayList<>();
            }
            String partial = args[0].toLowerCase(Locale.ROOT);
            return plugin.getWarpManager().getAccessibleWarps(player).stream()
                .map(Warp::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                .sorted()
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
