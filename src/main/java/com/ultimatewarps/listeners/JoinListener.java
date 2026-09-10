package com.ultimatewarps.listeners;

import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.SpawnLocationManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final UltimateWarps plugin;

    public JoinListener(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
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
        SpawnLocationManager spawnManager = plugin.getConfigManager().getSpawnLocationManager();
        Location spawn = plugin.getConfigManager().getSpawnLocation();

        if (spawn == null) return;

        if (spawnManager.hasPendingSpawnTeleport(player.getUniqueId())) {
            Location pendingSpawn = spawnManager.getPendingSpawnTeleport(player.getUniqueId());
            if (pendingSpawn != null) {
                player.teleport(pendingSpawn);
                plugin.getEffectManager().playTeleportEffect(player, "Spawn");

                String template = plugin.getConfigManager().getRawMessage("teleportation-confirmed");
                player.sendMessage(TextFormat.renderTemplate(template, "%warp%", "Spawn"));
            }
            spawnManager.setJoinedBefore(player.getUniqueId());
            return;
        }

        boolean isFirstJoin = !spawnManager.hasJoinedBefore(player.getUniqueId());

        if (isFirstJoin && plugin.getConfigManager().spawnTeleportOnFirstJoin()) {
            player.teleport(spawn);
            String message = plugin.getConfigManager().getSpawnTeleportOnFirstJoinMessage();
            player.sendMessage(TextFormat.render(message));
            spawnManager.setJoinedBefore(player.getUniqueId());
            return;
        }

        if (!isFirstJoin && plugin.getConfigManager().spawnTeleportOnEveryJoin()) {
            player.teleport(spawn);
            String message = plugin.getConfigManager().getSpawnTeleportOnEveryJoinMessage();
            player.sendMessage(TextFormat.render(message));
        }

        if (!spawnManager.hasJoinedBefore(player.getUniqueId())) {
            spawnManager.setJoinedBefore(player.getUniqueId());
        }
    }
}
