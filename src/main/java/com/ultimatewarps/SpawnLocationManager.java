package com.ultimatewarps;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnLocationManager {

    private final UltimateWarps plugin;
    private Location cachedLocation;
    private final Map<UUID, Location> pendingSpawnTeleports = new HashMap<>();
    private final File spawnDataFile;
    private YamlConfiguration spawnData;

    public SpawnLocationManager(UltimateWarps plugin) {
        this.plugin = plugin;
        this.spawnDataFile = new File(plugin.getDataFolder(), "spawn.yml");
        loadSpawnData();
    }

    private void loadSpawnData() {
        if (!spawnDataFile.exists()) {
            spawnData = new YamlConfiguration();
            return;
        }
        spawnData = YamlConfiguration.loadConfiguration(spawnDataFile);
        
        // Load cached spawn location
        if (spawnData.contains("spawn-location")) {
            cachedLocation = (Location) spawnData.get("spawn-location");
        }
        
        // Load pending teleports
        if (spawnData.contains("pending-spawn-teleports")) {
            for (String uuidStr : spawnData.getConfigurationSection("pending-spawn-teleports").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Location loc = (Location) spawnData.get("pending-spawn-teleports." + uuidStr);
                    if (loc != null) {
                        pendingSpawnTeleports.put(uuid, loc);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in spawn.yml: " + uuidStr);
                }
            }
        }
    }

    private void saveSpawnData() {
        try {
            spawnData.save(spawnDataFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save spawn data: " + e.getMessage());
        }
    }

    public Location getLocation() {
        return cachedLocation;
    }

    public void setLocation(Location loc) {
        this.cachedLocation = loc;
        spawnData.set("spawn-location", loc);
        saveSpawnData();
    }

    public void deleteLocation() {
        this.cachedLocation = null;
        spawnData.set("spawn-location", null);
        saveSpawnData();
    }
    
    // ========== PENDING SPAWN TELEPORT METHODS ==========
    
    public void setPendingSpawnTeleport(UUID playerId, Location spawn) {
        pendingSpawnTeleports.put(playerId, spawn);
        spawnData.set("pending-spawn-teleports." + playerId.toString(), spawn);
        saveSpawnData();
    }
    
    public Location getPendingSpawnTeleport(UUID playerId) {
        Location loc = pendingSpawnTeleports.remove(playerId);
        spawnData.set("pending-spawn-teleports." + playerId.toString(), null);
        saveSpawnData();
        return loc;
    }
    
    public boolean hasPendingSpawnTeleport(UUID playerId) {
        return pendingSpawnTeleports.containsKey(playerId);
    }
    
    public void removePendingSpawnTeleport(UUID playerId) {
        pendingSpawnTeleports.remove(playerId);
        spawnData.set("pending-spawn-teleports." + playerId.toString(), null);
        saveSpawnData();
    }
    
    // ========== FIRST JOIN TRACKING METHODS ==========
    
    public boolean hasJoinedBefore(UUID playerId) {
        return spawnData.contains("joined-before." + playerId.toString());
    }
    
    public void setJoinedBefore(UUID playerId) {
        spawnData.set("joined-before." + playerId.toString(), true);
        saveSpawnData();
    }
}