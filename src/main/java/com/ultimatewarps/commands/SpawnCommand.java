package com.ultimatewarps.commands;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnCommand implements CommandExecutor {

    private final UltimateWarps plugin;
    private final Map<UUID, Long> commandCooldown = new HashMap<>();

    public SpawnCommand(UltimateWarps plugin) {
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

        if (!plugin.getConfigManager().spawnEnabled()) {
            player.sendMessage(plugin.getConfigManager().getSpawnNotSetMessage());
            return true;
        }

        Location spawn = plugin.getConfigManager().getSpawnLocation();
        if (spawn == null) {
            player.sendMessage(plugin.getConfigManager().getSpawnNotSetMessage());
            return true;
        }

        // Check cooldown
        int remaining = plugin.getCooldownManager().getRemaining(player, "spawn");
        if (remaining > 0) {
            player.sendMessage(plugin.getConfigManager().getCooldownMessage(remaining));
            return true;
        }

        int delay = plugin.getConfigManager().spawnDelay();
        
        // Apply rank multiplier
        if (!player.hasPermission("ultimatewarps.bypass.delay")) {
            delay = (int) (delay * plugin.getConfigManager().getRankMultiplier(player, "delay"));
        }

        // Cancel any existing teleport task
        TeleportTask.cancelAllTasks(player);

        if (delay <= 0) {
            // Instant teleport
            player.teleport(spawn);
            plugin.getEffectManager().playTeleportEffect(player, "Spawn");
            player.sendMessage(plugin.getConfigManager().getTeleportConfirmedMessage("Spawn"));
            plugin.getCooldownManager().setCooldown(player, "spawn", plugin.getConfigManager().spawnCooldown());
        } else {
            // Start countdown with TeleportTask
            new TeleportTask(player, spawn, delay, "Spawn").runTaskTimer(plugin, 0L, 20L);
        }

        return true;
    }
}