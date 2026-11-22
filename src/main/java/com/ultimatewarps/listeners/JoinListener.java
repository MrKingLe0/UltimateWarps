package com.ultimatewarps.listeners;

import com.ultimatewarps.ChatInput;
import com.ultimatewarps.SpawnLocationManager;
import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class JoinListener implements Listener {

    private final UltimateWarps plugin;

    public JoinListener(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        TeleportTask.cancelAllTasks(player);
        PlayerWarpTeleportTask.cancelAllTasks(player);
        ChatInput.cancel(player);
        if (plugin.getWarpCommand() != null) {
            plugin.getWarpCommand().clearPlayer(uuid);
        }
        if (plugin.getSpawnCommand() != null) {
            plugin.getSpawnCommand().clearPlayer(uuid);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Sync stored ownerName on PlayerWarps if the player renamed their Minecraft account
        if (plugin.getPlayerWarpManager() != null) {
            for (PlayerWarp pw : plugin.getPlayerWarpManager().getWarpsByOwner(uuid)) {
                if (pw.getOwnerName() == null || !pw.getOwnerName().equals(player.getName())) {
                    pw.setOwnerName(player.getName());
                    pw.save();
                }
            }
        }

        SpawnLocationManager spawnManager = plugin.getConfigManager().getSpawnLocationManager();

        if (spawnManager.hasPendingSpawnTeleport(uuid)) {
            Location pendingSpawn = spawnManager.getPendingSpawnTeleport(uuid);
            if (pendingSpawn != null && pendingSpawn.getWorld() != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline() || pendingSpawn.getWorld() == null) return;
                    if (player.teleport(pendingSpawn)) {
                        plugin.getEffectManager().playTeleportEffect(player, "Spawn");
                        String template = plugin.getConfigManager().getRawMessage("teleportation-confirmed");
                        player.sendMessage(TextFormat.renderTemplate(template, "%warp%", "Spawn"));
                    }
                }, 1L);
            }
            spawnManager.setJoinedBefore(uuid);
            return;
        }

        Location spawn = plugin.getConfigManager().getSpawnLocation();
        if (spawn == null || spawn.getWorld() == null) {
            if (!spawnManager.hasJoinedBefore(uuid)) {
                spawnManager.setJoinedBefore(uuid);
            }
            return;
        }

        boolean isFirstJoin = !spawnManager.hasJoinedBefore(uuid);

        if (isFirstJoin && plugin.getConfigManager().spawnTeleportOnFirstJoin()) {
            String message = plugin.getConfigManager().getSpawnTeleportOnFirstJoinMessage();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || spawn.getWorld() == null) return;
                if (player.teleport(spawn)) {
                    player.sendMessage(TextFormat.render(message));
                }
            }, 1L);
            spawnManager.setJoinedBefore(uuid);
            return;
        }

        if (plugin.getConfigManager().spawnTeleportOnEveryJoin()) {
            String message = plugin.getConfigManager().getSpawnTeleportOnEveryJoinMessage();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || spawn.getWorld() == null) return;
                if (player.teleport(spawn)) {
                    player.sendMessage(TextFormat.render(message));
                }
            }, 1L);
        }

        if (isFirstJoin) {
            spawnManager.setJoinedBefore(uuid);
        }
    }
}
