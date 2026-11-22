package com.ultimatewarps.commands;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import org.bukkit.Bukkit;
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

    public void clearPlayer(UUID uuid) {
        commandCooldown.remove(uuid);
    }

    public SpawnCommand(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (!player.hasPermission("ultimatewarps.spawn")) {
            player.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
            return true;
        }

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
        if (spawn == null || spawn.getWorld() == null || Bukkit.getWorld(spawn.getWorld().getUID()) == null) {
            player.sendMessage(plugin.getConfigManager().getSpawnNotSetMessage());
            return true;
        }

        if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
            int remaining = plugin.getCooldownManager().getRemaining(player, "spawn");
            if (remaining > 0) {
                player.sendMessage(plugin.getConfigManager().getCooldownMessage(remaining));
                return true;
            }
        }

        int delay = plugin.getConfigManager().spawnDelay();

        if (!player.hasPermission("ultimatewarps.bypass.delay")) {
            delay = (int) (delay * plugin.getConfigManager().getRankMultiplier(player, "delay"));
        } else {
            delay = 0;
        }

        TeleportTask.cancelAllTasks(player);
        PlayerWarpTeleportTask.cancelAllTasks(player);

        if (delay <= 0) {
            if (!player.teleport(spawn)) {
                return true;
            }
            plugin.getEffectManager().playTeleportEffect(player, "Spawn");
            player.sendMessage(plugin.getConfigManager().getTeleportConfirmedMessage("Spawn"));
            if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
                int cooldown = (int) (plugin.getConfigManager().spawnCooldown() * plugin.getConfigManager().getRankMultiplier(player, "cooldown"));
                if (cooldown > 0) {
                    plugin.getCooldownManager().setCooldown(player, "spawn", cooldown);
                }
            }
        } else {
            Runnable onSuccess = null;
            if (!player.hasPermission("ultimatewarps.bypass.cooldown")) {
                int finalCooldown = (int) (plugin.getConfigManager().spawnCooldown() * plugin.getConfigManager().getRankMultiplier(player, "cooldown"));
                if (finalCooldown > 0) {
                    onSuccess = () -> plugin.getCooldownManager().setCooldown(player, "spawn", finalCooldown);
                }
            }
            new TeleportTask(player, spawn, delay, "Spawn", "Spawn", onSuccess).runTaskTimer(plugin, 0L, 20L);
        }

        return true;
    }
}
