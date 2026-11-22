package com.ultimatewarps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Warp {

    private final String name;
    private Location location;
    private String worldName;
    private boolean enabled = true;
    private int cooldown = 0;
    private int delay = 0;
    private String permission = null;
    private ItemStack icon;
    private String displayName;

    private boolean glow = false;

    private List<String> customLore = new ArrayList<>();

    private int slot = -1;

    private boolean customEffects = false;
    private boolean effectParticleEnabled = true;
    private String effectParticleType = "DUST";
    private int effectParticleCount = 30;
    private String effectDustColor = "#5555FF";
    private float effectDustSize = 1.0f;
    private boolean effectSoundEnabled = true;
    private String effectSoundType = "BLOCK_NOTE_BLOCK_PLING";
    private float effectSoundVolume = 1.0f;
    private float effectSoundPitch = 1.0f;

    private File file;

    public Warp(String name, Location location) {
        this.name = name;
        this.location = location.clone();
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : null;
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
    public void setLocation(Location location) { this.location = location.clone(); this.worldName = location.getWorld() != null ? location.getWorld().getName() : this.worldName; }
    public void setIcon(ItemStack icon) { this.icon = icon != null ? icon.clone() : null; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setFile(File file) { this.file = file; }

    public boolean isGlow() { return glow; }
    public void setGlow(boolean glow) { this.glow = glow; }

    public List<String> getCustomLore() { return new ArrayList<>(customLore); }
    public void setCustomLore(List<String> lore) { this.customLore = lore != null ? new ArrayList<>(lore) : new ArrayList<>(); }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public boolean isCustomEffects() { return customEffects; }
    public void setCustomEffects(boolean customEffects) { this.customEffects = customEffects; }
    public boolean isEffectParticleEnabled() { return effectParticleEnabled; }
    public void setEffectParticleEnabled(boolean v) { this.effectParticleEnabled = v; }
    public String getEffectParticleType() { return effectParticleType; }
    public void setEffectParticleType(String v) { this.effectParticleType = v; }
    public int getEffectParticleCount() { return effectParticleCount; }
    public void setEffectParticleCount(int v) { this.effectParticleCount = v; }
    public String getEffectDustColor() { return effectDustColor; }
    public void setEffectDustColor(String v) { this.effectDustColor = v; }
    public float getEffectDustSize() { return effectDustSize; }
    public void setEffectDustSize(float v) { this.effectDustSize = v; }
    public boolean isEffectSoundEnabled() { return effectSoundEnabled; }
    public void setEffectSoundEnabled(boolean v) { this.effectSoundEnabled = v; }
    public String getEffectSoundType() { return effectSoundType; }
    public void setEffectSoundType(String v) { this.effectSoundType = v; }
    public float getEffectSoundVolume() { return effectSoundVolume; }
    public void setEffectSoundVolume(float v) { this.effectSoundVolume = v; }
    public float getEffectSoundPitch() { return effectSoundPitch; }
    public void setEffectSoundPitch(float v) { this.effectSoundPitch = v; }

    public void seedEffectsFromGlobal(ConfigManager cfg) {
        this.effectParticleEnabled = cfg.warpParticleEnabled();
        this.effectParticleType = cfg.warpParticleTypeRaw();
        this.effectParticleCount = cfg.warpParticleCount();
        this.effectDustColor = cfg.warpDustColorRaw();
        this.effectDustSize = cfg.warpDustSize();
        this.effectSoundEnabled = cfg.warpSoundEnabled();
        this.effectSoundType = cfg.warpSoundTypeRaw();
        this.effectSoundVolume = cfg.warpSoundVolume();
        this.effectSoundPitch = cfg.warpSoundPitch();
    }

    public void save() {
        if (file == null) {
            file = new File(UltimateWarps.getInstance().getWarpManager().getWarpsFolder(), name + ".yml");
        }
        YamlConfiguration yml = new YamlConfiguration();
        String wName = (location != null && location.getWorld() != null) ? location.getWorld().getName() : worldName;
        if (wName == null) {
            UltimateWarps.getInstance().getLogger().warning("Skipping save for warp " + name + " (world is null).");
            return;
        }
        yml.set("location.world", wName);
        yml.set("location.x", location.getX());
        yml.set("location.y", location.getY());
        yml.set("location.z", location.getZ());
        yml.set("location.yaw", location.getYaw());
        yml.set("location.pitch", location.getPitch());
        yml.set("enabled", enabled);
        yml.set("cooldown", cooldown);
        yml.set("delay", delay);
        yml.set("permission", permission);

        if (icon != null) {
            yml.set("icon", icon);
        }
        if (displayName != null) {
            yml.set("display-name", displayName);
        }

        if (glow) {
            yml.set("glow", true);
        }

        if (!customLore.isEmpty()) {
            yml.set("lore", customLore);
        }

        yml.set("slot", slot);

        if (customEffects) {
            yml.set("effects.enabled", true);
            yml.set("effects.particle.enabled", effectParticleEnabled);
            yml.set("effects.particle.type", effectParticleType);
            yml.set("effects.particle.count", effectParticleCount);
            yml.set("effects.particle.dust-color", effectDustColor);
            yml.set("effects.particle.dust-size", effectDustSize);
            yml.set("effects.sound.enabled", effectSoundEnabled);
            yml.set("effects.sound.type", effectSoundType);
            yml.set("effects.sound.volume", effectSoundVolume);
            yml.set("effects.sound.pitch", effectSoundPitch);
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

            Object raw = yml.get("icon");
            if (raw instanceof ItemStack loaded) {

                if (loaded.getType() != null && loaded.getType() != org.bukkit.Material.AIR) {
                    warp.icon = loaded;
                } else {
                    UltimateWarps.getInstance().getLogger().info(
                            "Icon for warp " + name + " uses a material not available in this " +
                            "server version - reverting to default icon.");
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

        warp.displayName = yml.getString("display-name", null);
        warp.glow = yml.getBoolean("glow", false);

        warp.customLore = yml.getStringList("lore");
        warp.slot = yml.getInt("slot", -1);

        warp.customEffects = yml.getBoolean("effects.enabled", false);
        if (warp.customEffects) {
            warp.effectParticleEnabled = yml.getBoolean("effects.particle.enabled", true);
            warp.effectParticleType = yml.getString("effects.particle.type", "DUST");
            warp.effectParticleCount = yml.getInt("effects.particle.count", 30);
            warp.effectDustColor = yml.getString("effects.particle.dust-color", "#5555FF");
            warp.effectDustSize = (float) yml.getDouble("effects.particle.dust-size", 1.0);
            warp.effectSoundEnabled = yml.getBoolean("effects.sound.enabled", true);
            warp.effectSoundType = yml.getString("effects.sound.type", "BLOCK_NOTE_BLOCK_PLING");
            warp.effectSoundVolume = (float) yml.getDouble("effects.sound.volume", 1.0);
            warp.effectSoundPitch = (float) yml.getDouble("effects.sound.pitch", 1.0);
        }

        return warp;
    }

    public void delete() {
        if (file != null && file.exists()) file.delete();
    }
}
