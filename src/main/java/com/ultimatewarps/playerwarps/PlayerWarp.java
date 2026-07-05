package com.ultimatewarps.playerwarps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.UUID;

/**
 * A warp created and owned by a player, as opposed to an admin-created Warp.
 * Stored separately from admin warps, one file per warp, under a per-owner folder
 * (playerwarps/<owner-uuid>/<warp-name>.yml) so warps from different players can never
 * collide on name and an owner's warps can be listed/wiped without scanning everything.
 */
public class PlayerWarp {

    private final String name;
    private final UUID ownerId;
    private String ownerName; // cached display name, refreshed on use - never used for lookups
    private Location location;
    private String displayName;       // null = use name
    private ItemStack icon;           // null = default
    private boolean isPublic = true;  // private warps are only visible/usable by the owner (and admins)
    private String description;       // shown in lore - purely cosmetic
    private long createdAt;           // epoch millis

    private File file;

    public PlayerWarp(String name, UUID ownerId, String ownerName, Location location) {
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location.clone();
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public UUID getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public Location getLocation() { return location.clone(); }
    public String getDisplayName() { return displayName; }
    public ItemStack getIcon() { return icon != null ? icon.clone() : null; }
    public boolean isPublic() { return isPublic; }
    public String getDescription() { return description; }
    public long getCreatedAt() { return createdAt; }

    public void setLocation(Location location) { this.location = location.clone(); }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setIcon(ItemStack icon) { this.icon = icon != null ? icon.clone() : null; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setDescription(String description) { this.description = description; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setFile(File file) { this.file = file; }

    public void save() {
        if (file == null) {
            file = new File(PlayerWarpManager.getOwnerFolder(ownerId), name + ".yml");
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("owner-id", ownerId.toString());
        yml.set("owner-name", ownerName);
        yml.set("location.world", location.getWorld().getName());
        yml.set("location.x", location.getX());
        yml.set("location.y", location.getY());
        yml.set("location.z", location.getZ());
        yml.set("location.yaw", location.getYaw());
        yml.set("location.pitch", location.getPitch());
        yml.set("public", isPublic);
        yml.set("created-at", createdAt);
        if (displayName != null) yml.set("display-name", displayName);
        if (description != null) yml.set("description", description);

        // Same YAML-native serialization fix as Warp.java - see that file for full explanation.
        if (icon != null) {
            yml.set("icon", icon);
        }

        try {
            yml.save(file);
        } catch (Exception e) {
            com.ultimatewarps.UltimateWarps.getInstance().getLogger()
                    .severe("Could not save player warp " + name + ": " + e.getMessage());
        }
    }

    public static PlayerWarp load(File file, UUID ownerIdFromFolder) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        String name = file.getName().substring(0, file.getName().length() - 4); // strip ".yml"

        String ownerIdStr = yml.getString("owner-id");
        UUID ownerId;
        try {
            ownerId = ownerIdStr != null ? UUID.fromString(ownerIdStr) : ownerIdFromFolder;
        } catch (IllegalArgumentException e) {
            ownerId = ownerIdFromFolder;
        }
        String ownerName = yml.getString("owner-name", "Unknown");

        String worldName = yml.getString("location.world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            com.ultimatewarps.UltimateWarps.getInstance().getLogger()
                    .warning("Player warp " + name + " (owner " + ownerId + ") has an invalid/unloaded world, skipping.");
            return null;
        }

        double x = yml.getDouble("location.x");
        double y = yml.getDouble("location.y");
        double z = yml.getDouble("location.z");
        float yaw = (float) yml.getDouble("location.yaw");
        float pitch = (float) yml.getDouble("location.pitch");
        Location loc = new Location(world, x, y, z, yaw, pitch);

        PlayerWarp warp = new PlayerWarp(name, ownerId, ownerName, loc);
        warp.isPublic = yml.getBoolean("public", true);
        warp.createdAt = yml.getLong("created-at", System.currentTimeMillis());
        warp.displayName = yml.getString("display-name", null);
        warp.description = yml.getString("description", null);
        warp.file = file;

        if (yml.contains("icon")) {
            Object raw = yml.get("icon");
            if (raw instanceof ItemStack loaded) {
                if (loaded.getType() != null && loaded.getType() != org.bukkit.Material.AIR) {
                    warp.icon = loaded;
                } else {
                    com.ultimatewarps.UltimateWarps.getInstance().getLogger().info(
                            "Icon for player warp " + name + " uses a material not available " +
                            "in this server version - reverting to default icon.");
                }
            } else if (raw instanceof String base64) {
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
                        com.ultimatewarps.UltimateWarps.getInstance().getLogger().info(
                                "Icon for player warp " + name + " uses a material not available " +
                                "in this server version - reverting to default icon.");
                    }
                } catch (Exception e) {
                    com.ultimatewarps.UltimateWarps.getInstance().getLogger().info(
                            "Icon for player warp " + name + " could not be loaded (material may " +
                            "not exist in this server version) - reverting to default icon.");
                } finally {
                    csLogger.setLevel(prevLevel);
                }
            }
        }

        return warp;
    }

    public void delete() {
        if (file != null && file.exists()) file.delete();
    }
}
