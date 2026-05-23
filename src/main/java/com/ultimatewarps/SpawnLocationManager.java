package com.ultimatewarps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SpawnLocationManager {

    private final UltimateWarps plugin;
    private final File file;
    private YamlConfiguration config;

    public SpawnLocationManager(UltimateWarps plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        load();
    }

    public void load() {
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            config = new YamlConfiguration();
            save();
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawn.yml: " + e.getMessage());
        }
    }

    public Location getLocation() {
        if (!config.contains("world")) return null;
        World world = Bukkit.getWorld(config.getString("world"));
        if (world == null) return null;
        return new Location(world,
                config.getDouble("x"),
                config.getDouble("y"),
                config.getDouble("z"),
                (float) config.getDouble("yaw"),
                (float) config.getDouble("pitch"));
    }

    public void setLocation(Location loc) {
        config.set("world", loc.getWorld().getName());
        config.set("x", loc.getX());
        config.set("y", loc.getY());
        config.set("z", loc.getZ());
        config.set("yaw", loc.getYaw());
        config.set("pitch", loc.getPitch());
        save();
    }

    public void deleteLocation() {
        config.set("world", null);
        config.set("x", null);
        config.set("y", null);
        config.set("z", null);
        config.set("yaw", null);
        config.set("pitch", null);
        save();
    }
}