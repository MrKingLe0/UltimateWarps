package com.ultimatewarps.commands;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.gui.WarpGUI;

import net.kyori.adventure.text.Component;

import com.ultimatewarps.Warp;
import org.bukkit.ChatColor;
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
    
    public WarpCommand(UltimateWarps plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        
    // Anti-spam
    long lastExec = commandCooldown.getOrDefault(player.getUniqueId(), 0L);
    long now = System.currentTimeMillis();
    if (now - lastExec < 1000) {
        return true;
    }
    commandCooldown.put(player.getUniqueId(), now);

    if (args.length == 0) {
        new WarpGUI(plugin, player).open();
        return true;
    }

    Warp warp = plugin.getWarpManager().getWarp(args[0]);
    if (warp == null || !warp.isEnabled()) {
        player.sendMessage(plugin.getConfigManager().getWarpNotFoundMessage());
        return true;
    }

    // Check cooldown
    String cooldownKey = "warp_" + warp.getName();
    if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
        if (plugin.getCooldownManager().hasCooldown(player, cooldownKey)) {
            int remaining = plugin.getCooldownManager().getRemaining(player, cooldownKey);
            long lastMessage = cooldownMessages.getOrDefault(player.getUniqueId(), 0L);
            // Remove "long now =" here - reuse the existing 'now' variable
            if (now - lastMessage > 2000) {
                player.sendMessage(plugin.getConfigManager().getCooldownMessage(remaining));
                cooldownMessages.put(player.getUniqueId(), now);
            }
            return true;
        }
    }
        
        // Cancel any existing teleport task
        TeleportTask.cancelAllTasks(player);
        
        startTeleport(player, warp);
        return true;
    }
    
    private void startTeleport(Player player, Warp warp) {
        int delay = warp.getDelay();
        
        // Apply rank multiplier if player doesn't have bypass
        if (!player.hasPermission("ultimatewarps.bypass.delay")) {
            delay = (int) (delay * plugin.getConfigManager().getRankMultiplier(player, "delay"));
        } else {
            delay = 0;
        }
        
        if (delay <= 0) {
            // Instant teleport
            player.teleport(warp.getLocation());
            plugin.getEffectManager().playTeleportEffect(player, warp.getName());
            Component msg = plugin.getConfigManager().getTeleportConfirmedMessage(warp.getName());
            player.sendMessage(msg);
            
            // Set cooldown
            if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
                int cooldown = (int) (warp.getCooldown() * plugin.getConfigManager().getRankMultiplier(player, "cooldown"));
                if (cooldown > 0) {
                    plugin.getCooldownManager().setCooldown(player, "warp_" + warp.getName(), cooldown);
                }
            }
        } else {
            // Start countdown with TeleportTask
            new TeleportTask(player, warp.getLocation(), delay, warp.getName()).runTaskTimer(plugin, 0L, 20L);
            
            // Set cooldown after teleport
            if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
                int finalCooldown = (int) (warp.getCooldown() * plugin.getConfigManager().getRankMultiplier(player, "cooldown"));
                if (finalCooldown > 0) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        plugin.getCooldownManager().setCooldown(player, "warp_" + warp.getName(), finalCooldown);
                    }, delay * 20L);
                }
            }
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            String partial = args[0].toLowerCase();
            return plugin.getWarpManager().getAllWarps().stream()
                .map(Warp::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}