package com.ultimatewarps;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

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

    public void reload() {
        cachedLocation = null;
        pendingSpawnTeleports.clear();
        loadSpawnData();
    }

    private void loadSpawnData() {
        if (!spawnDataFile.exists()) {
            spawnData = new YamlConfiguration();
            return;
        }
        spawnData = YamlConfiguration.loadConfiguration(spawnDataFile);

        if (spawnData.contains("spawn-location")) {
            Object raw = spawnData.get("spawn-location");
            if (raw instanceof Location loc) {
                cachedLocation = loc;
            }
        }

        ConfigurationSection pendingSection = spawnData.getConfigurationSection("pending-spawn-teleports");
        if (pendingSection != null) {
            for (String uuidStr : pendingSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Object raw = spawnData.get("pending-spawn-teleports." + uuidStr);
                    if (raw instanceof Location loc) {
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
        return cachedLocation != null ? cachedLocation.clone() : null;
    }

    public void setLocation(Location loc) {
        this.cachedLocation = loc != null ? loc.clone() : null;
        spawnData.set("spawn-location", this.cachedLocation);
        saveSpawnData();
    }

    public void deleteLocation() {
        this.cachedLocation = null;
        spawnData.set("spawn-location", null);
        saveSpawnData();
    }

    public void setPendingSpawnTeleport(UUID playerId, Location spawn) {
        Location copy = spawn != null ? spawn.clone() : null;
        pendingSpawnTeleports.put(playerId, copy);
        spawnData.set("pending-spawn-teleports." + playerId.toString(), copy);
        saveSpawnData();
    }

    public Location getPendingSpawnTeleport(UUID playerId) {
        Location loc = pendingSpawnTeleports.remove(playerId);
        if (loc != null || spawnData.contains("pending-spawn-teleports." + playerId.toString())) {
            spawnData.set("pending-spawn-teleports." + playerId.toString(), null);
            saveSpawnData();
        }
        return loc != null ? loc.clone() : null;
    }

    public boolean hasPendingSpawnTeleport(UUID playerId) {
        return pendingSpawnTeleports.containsKey(playerId);
    }

    public void removePendingSpawnTeleport(UUID playerId) {
        boolean removed = pendingSpawnTeleports.remove(playerId) != null;
        if (removed || spawnData.contains("pending-spawn-teleports." + playerId.toString())) {
            spawnData.set("pending-spawn-teleports." + playerId.toString(), null);
            saveSpawnData();
        }
    }

    public boolean hasJoinedBefore(UUID playerId) {
        return spawnData.contains("joined-before." + playerId.toString());
    }

    public void setJoinedBefore(UUID playerId) {
        if (hasJoinedBefore(playerId)) return;
        spawnData.set("joined-before." + playerId.toString(), true);
        saveSpawnData();
    }
}
