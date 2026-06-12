package com.ultimatewarps.listeners;

import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.SpawnLocationManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final UltimateWarps plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public JoinListener(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SpawnLocationManager spawnManager = plugin.getConfigManager().getSpawnLocationManager();
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        
        if (spawn == null) return;
        
        // Check for pending force spawn teleport (from /spawnforce command) - HIGHEST PRIORITY
        if (spawnManager.hasPendingSpawnTeleport(player.getUniqueId())) {
            Location pendingSpawn = spawnManager.getPendingSpawnTeleport(player.getUniqueId());
            if (pendingSpawn != null) {
                player.teleport(pendingSpawn);
                plugin.getEffectManager().playTeleportEffect(player, "Spawn");
                String message = plugin.getConfigManager().getRawMessage("teleportation-confirmed");
                message = message.replace("%warp%", "Spawn");
                player.sendMessage(miniMessage.deserialize(message));
            }
            spawnManager.setJoinedBefore(player.getUniqueId());
            return;
        }
        
        // Check for first join teleport
        boolean isFirstJoin = !spawnManager.hasJoinedBefore(player.getUniqueId());
        
        if (isFirstJoin && plugin.getConfigManager().spawnTeleportOnFirstJoin()) {
            player.teleport(spawn);
            String message = plugin.getConfigManager().getSpawnTeleportOnFirstJoinMessage();
            player.sendMessage(miniMessage.deserialize(message));
            spawnManager.setJoinedBefore(player.getUniqueId());
            return;
        }
        
        // Check for teleport on every join (for non-first joins)
        if (!isFirstJoin && plugin.getConfigManager().spawnTeleportOnEveryJoin()) {
            player.teleport(spawn);
            String message = plugin.getConfigManager().getSpawnTeleportOnEveryJoinMessage();
            player.sendMessage(miniMessage.deserialize(message));
        }
        
        // Mark that this player has joined before (if not already marked)
        if (!spawnManager.hasJoinedBefore(player.getUniqueId())) {
            spawnManager.setJoinedBefore(player.getUniqueId());
        }
    }
}