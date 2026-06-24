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

    // Improvement: WarpCommand/SpawnCommand kept small per-player anti-spam maps
    // (cooldown-message throttling, command-spam throttling) that were never cleaned up
    // when a player disconnected, so they grew forever on a long-running server. These
    // are pure UX throttles with no gameplay stakes, so it's safe to just drop them on
    // quit (unlike actual cooldowns, which are handled separately via a periodic sweep
    // in CooldownManager so quitting can't be used to dodge an active cooldown).
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
        
        // Check for pending force spawn teleport (from /spawnforce command) - HIGHEST PRIORITY
        if (spawnManager.hasPendingSpawnTeleport(player.getUniqueId())) {
            Location pendingSpawn = spawnManager.getPendingSpawnTeleport(player.getUniqueId());
            if (pendingSpawn != null) {
                player.teleport(pendingSpawn);
                plugin.getEffectManager().playTeleportEffect(player, "Spawn");
                // Bug fix: this used to substitute %warp% as a raw string into the
                // template and call miniMessage.deserialize() directly, bypassing
                // TextFormat entirely (no legacy '&'/hex support, and the same nested-tag
                // corruption risk as every other raw-replace-then-parse call site fixed
                // this round). TextFormat.renderTemplate() composes the template and the
                // value as separate Components instead.
                String template = plugin.getConfigManager().getRawMessage("teleportation-confirmed");
                player.sendMessage(TextFormat.renderTemplate(template, "%warp%", "Spawn"));
            }
            spawnManager.setJoinedBefore(player.getUniqueId());
            return;
        }
        
        // Check for first join teleport
        boolean isFirstJoin = !spawnManager.hasJoinedBefore(player.getUniqueId());
        
        if (isFirstJoin && plugin.getConfigManager().spawnTeleportOnFirstJoin()) {
            player.teleport(spawn);
            String message = plugin.getConfigManager().getSpawnTeleportOnFirstJoinMessage();
            player.sendMessage(TextFormat.render(message));
            spawnManager.setJoinedBefore(player.getUniqueId());
            return;
        }
        
        // Check for teleport on every join (for non-first joins)
        if (!isFirstJoin && plugin.getConfigManager().spawnTeleportOnEveryJoin()) {
            player.teleport(spawn);
            String message = plugin.getConfigManager().getSpawnTeleportOnEveryJoinMessage();
            player.sendMessage(TextFormat.render(message));
        }
        
        // Mark that this player has joined before (if not already marked)
        if (!spawnManager.hasJoinedBefore(player.getUniqueId())) {
            spawnManager.setJoinedBefore(player.getUniqueId());
        }
    }
}