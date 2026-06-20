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

    /**
     * Improvement: these anti-spam maps were never cleaned up when a player disconnected,
     * so on a long-running server with many unique players they'd grow forever. Called
     * from the quit listener.
     */
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
        
    // Anti-spam
    long lastExec = commandCooldown.getOrDefault(player.getUniqueId(), 0L);
    long now = System.currentTimeMillis();
    if (now - lastExec < 1000) {
        return true;
    }
    commandCooldown.put(player.getUniqueId(), now);

    if (args.length == 0) {
        WarpGUI.getOrCreate(plugin, player).open();
        return true;
    }

    Warp warp = plugin.getWarpManager().getWarp(args[0]);
    if (warp == null || !warp.isEnabled()) {
        player.sendMessage(plugin.getConfigManager().getWarpNotFoundMessage());
        return true;
    }

    // Check per-warp permission (bug fix: this was never enforced for direct /warp <name> usage,
    // only the GUI filtered warps by permission, so anyone could bypass it via the command)
    String requiredPermission = warp.getPermission();
    boolean hasAccess = requiredPermission == null || requiredPermission.isEmpty()
            || player.hasPermission(requiredPermission)
            || player.hasPermission("ultimatewarps.warp.*");
    if (!hasAccess) {
        player.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
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
        
        // Bug fix: every player-facing message (boss bar, title, chat confirmation) used
        // to be built from warp.getName() - the internal/file name - completely ignoring
        // warp.getDisplayName(), the custom name set via /warpsadmin. That value was saved
        // and reloaded correctly, it just never reached anything the player actually sees.
        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        
        if (delay <= 0) {
            // Instant teleport
            player.teleport(warp.getLocation());
            // warp.getName() here is the identity key EffectManager uses to pick spawn.*
            // vs warp.* config - it's never shown to the player, so it stays the internal
            // name. The confirmation message is what the player sees, so it uses displayLabel.
            plugin.getEffectManager().playTeleportEffect(player, warp.getName());
            Component msg = plugin.getConfigManager().getTeleportConfirmedMessage(displayLabel);
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
            new TeleportTask(player, warp.getLocation(), delay, warp.getName(), displayLabel).runTaskTimer(plugin, 0L, 20L);
            
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
        // Improvement: this used to suggest every warp regardless of whether it was
        // disabled or permission-gated, which leaks warp names/existence to players who
        // can't actually use them. Reuse the same accessibility filter as the GUI.
        if (args.length == 1 && sender instanceof Player player) {
            String partial = args[0].toLowerCase();
            return plugin.getWarpManager().getAccessibleWarps(player).stream()
                .map(Warp::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .sorted()
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}