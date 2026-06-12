package com.ultimatewarps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class Warp {

    private final String name;
    private Location location;
    private boolean enabled = true;
    private int cooldown = 0;
    private int delay = 0;
    private String permission = null;
    private ItemStack icon;          // null = default
    private String displayName;      // null = use name

    private File file;

    public Warp(String name, Location location) {
        this.name = name;
        this.location = location.clone();
    }

    public String getName() { return name; }
    public Location getLocation() { return location.clone(); }
    public boolean isEnabled() { return enabled; }
    public int getCooldown() { return cooldown; }
    public int getDelay() { return delay; }
    public String getPermission() { return permission; }
    public ItemStack getIcon() { return icon != null ? icon.clone() : null; }
    public String getDisplayName() { return displayName; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public void setDelay(int delay) { this.delay = delay; }
    public void setPermission(String permission) { this.permission = permission; }
    public void setLocation(Location location) { this.location = location.clone(); }
    public void setIcon(ItemStack icon) { this.icon = icon != null ? icon.clone() : null; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setFile(File file) { this.file = file; }

    public void save() {
        if (file == null) {
            file = new File(UltimateWarps.getInstance().getWarpManager().getWarpsFolder(), name + ".yml");
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("location.world", location.getWorld().getName());
        yml.set("location.x", location.getX());
        yml.set("location.y", location.getY());
        yml.set("location.z", location.getZ());
        yml.set("location.yaw", location.getYaw());
        yml.set("location.pitch", location.getPitch());
        yml.set("enabled", enabled);
        yml.set("cooldown", cooldown);
        yml.set("delay", delay);
        yml.set("permission", permission);

        // Fix: Use proper ItemStack serialization
        if (icon != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
                boos.writeObject(icon);
                boos.close();
                yml.set("icon", Base64.getEncoder().encodeToString(baos.toByteArray()));
            } catch (Exception e) {
                UltimateWarps.getInstance().getLogger().warning("Could not save icon for warp " + name);
            }
        }
        if (displayName != null) {
            yml.set("display-name", displayName);
        }

        try {
            yml.save(file);
        } catch (Exception e) {
            UltimateWarps.getInstance().getLogger().severe("Could not save warp " + name + ": " + e.getMessage());
        }
    }

    public static Warp load(File file) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        String name = file.getName().replace(".yml", "");
        String worldName = yml.getString("location.world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            UltimateWarps.getInstance().getLogger().warning("Warp " + name + " has invalid world, skipping.");
            return null;
        }
        double x = yml.getDouble("location.x");
        double y = yml.getDouble("location.y");
        double z = yml.getDouble("location.z");
        float yaw = (float) yml.getDouble("location.yaw");
        float pitch = (float) yml.getDouble("location.pitch");
        Location loc = new Location(world, x, y, z, yaw, pitch);
        Warp warp = new Warp(name, loc);
        warp.enabled = yml.getBoolean("enabled", true);
        warp.cooldown = yml.getInt("cooldown", 0);
        warp.delay = yml.getInt("delay", 0);
        warp.permission = yml.getString("permission", null);
        warp.file = file;

        // Fix: Use proper ItemStack deserialization
        if (yml.contains("icon")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(yml.getString("icon"));
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
                warp.icon = (ItemStack) bois.readObject();
                bois.close();
            } catch (Exception e) {
                UltimateWarps.getInstance().getLogger().warning("Could not load icon for warp " + name);
            }
        }
        // Load display name
        warp.displayName = yml.getString("display-name", null);

        return warp;
    }

    public void delete() {
        if (file != null && file.exists()) file.delete();
    }
}