package com.ultimatewarps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;

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

        // Store icon using YAML-native ItemStack serialization (ConfigurationSerializable).
        // This stores the material as a namespaced string key (e.g. "minecraft:diamond")
        // rather than as a Java-serialized object, so icons survive cross-version migrations
        // where Minecraft renames materials between versions. The old format (BukkitObjectOutputStream
        // base64) baked in the internal Java class structure and broke whenever a material
        // name changed, which is what caused "Material cannot be null" errors on load.
        if (icon != null) {
            yml.set("icon", icon);
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

        if (yml.contains("icon")) {
            // Try YAML-native format first (new format: ConfigurationSerializable map).
            // Fall back to the legacy base64/BukkitObjectOutputStream format for warps
            // saved by older versions of this plugin, so existing data isn't lost on upgrade.
            // The next save() will automatically rewrite it in the new format.
            Object raw = yml.get("icon");
            if (raw instanceof ItemStack loaded) {
                // YAML-native: validate material is actually known on this server version
                // before accepting it. If the warp was saved on a newer server with a
                // material that doesn't exist here (e.g. items added in a later MC version),
                // accept the null/AIR material silently and fall back to the default icon
                // rather than crashing or logging a confusing error.
                if (loaded.getType() != null && loaded.getType() != org.bukkit.Material.AIR) {
                    warp.icon = loaded;
                } else {
                    UltimateWarps.getInstance().getLogger().info(
                            "Icon for warp " + name + " uses a material not available in this " +
                            "server version - reverting to default icon.");
                }
            } else if (raw instanceof String base64) {
                // Legacy base64 format - migrate transparently on load.
                // Bukkit's ConfigurationSerialization logger prints its own ERROR-level stack
                // trace before the exception reaches our catch block, which we can't suppress
                // from inside the catch. Instead, silence that specific logger for the duration
                // of the deserialization attempt so only our clean INFO message appears.
                java.util.logging.Logger csLogger = java.util.logging.Logger.getLogger(
                        "org.bukkit.configuration.serialization.ConfigurationSerialization");
                java.util.logging.Level prevLevel = csLogger.getLevel();
                try {
                    csLogger.setLevel(java.util.logging.Level.OFF);
                    byte[] bytes = java.util.Base64.getDecoder().decode(base64);
                    java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
                    org.bukkit.util.io.BukkitObjectInputStream bois = new org.bukkit.util.io.BukkitObjectInputStream(bais);
                    ItemStack loaded = (ItemStack) bois.readObject();
                    bois.close();
                    if (loaded != null && loaded.getType() != null && loaded.getType() != org.bukkit.Material.AIR) {
                        warp.icon = loaded;
                    } else {
                        UltimateWarps.getInstance().getLogger().info(
                                "Icon for warp " + name + " uses a material not available in this " +
                                "server version - reverting to default icon.");
                    }
                } catch (Exception e) {
                    UltimateWarps.getInstance().getLogger().info(
                            "Icon for warp " + name + " could not be loaded (material may not " +
                            "exist in this server version) - reverting to default icon.");
                } finally {
                    csLogger.setLevel(prevLevel);
                }
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