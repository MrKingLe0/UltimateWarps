package com.ultimatewarps;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ConfigManager {

    private final UltimateWarps plugin;
    private FileConfiguration config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final SpawnLocationManager spawnLocationManager;

    public ConfigManager(UltimateWarps plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.spawnLocationManager = new SpawnLocationManager(plugin);
    }
    
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public Component parse(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        return miniMessage.deserialize(message);
    }

    // ========== SPAWN SETTINGS ==========
    public boolean spawnEnabled() { return config.getBoolean("spawn.enabled", true); }
    public boolean spawnCancelOnMove() { return config.getBoolean("spawn.cancel-on-move", true); }
    public int spawnCooldown() { return config.getInt("spawn.cooldown", 10); }
    public int spawnDelay() { return config.getInt("spawn.delay", 5); }

    // ========== SPAWN TITLE ==========
    public boolean spawnTitleEnabled() { 
        return config.getBoolean("spawn.title.enabled", true); 
    }
    
    public Component getSpawnTitleMessage() {
        String msg = config.getString("spawn.title.message", "<b><gradient:#FF0000:#9400FF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ sᴘᴀᴡɴ</gradient></b>");
        return parse(msg);
    }
    
    public Component getSpawnSubtitleMessage(int seconds) {
        String msg = config.getString("spawn.title.subtitle", "<b><gradient:#5000FF:#9400FF>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ <white>%seconds%</white> sᴇᴄᴏɴᴅs</gradient></b>");
        msg = msg.replace("%seconds%", String.valueOf(seconds));
        return parse(msg);
    }

    // ========== SPAWN BOSSBAR ==========
    public boolean spawnBossBarEnabled() { 
        return config.getBoolean("spawn.bossbar.enabled", true); 
    }
    
    public Component getSpawnBossBarText(int seconds) {
        String msg = config.getString("spawn.bossbar.text", "<b><gradient:#5000FF:#00B3FF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ...</gradient></b>");
        msg = msg.replace("%seconds%", String.valueOf(seconds));
        return parse(msg);
    }
    
    public BarColor spawnBossBarColor() {
        try { 
            return BarColor.valueOf(config.getString("spawn.bossbar.color", "PURPLE").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return BarColor.PURPLE; 
        }
    }
    
    public BarStyle spawnBossBarStyle() {
        try { 
            return BarStyle.valueOf(config.getString("spawn.bossbar.style", "SOLID").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return BarStyle.SOLID; 
        }
    }

    // ========== SPAWN PARTICLES ==========
    public boolean spawnParticleEnabled() { 
        return config.getBoolean("spawn.particle.enabled", true); 
    }
    
    public Particle spawnParticleType() {
        try { 
            return Particle.valueOf(config.getString("spawn.particle.type", "PORTAL").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return Particle.PORTAL; 
        }
    }
    
    public int spawnParticleCount() { 
        return config.getInt("spawn.particle.count", 30); 
    }
    
    public Color spawnDustColor() {
        String raw = config.getString("spawn.particle.dust.color", "#FF0000");
        return parseColor(raw);
    }
    
    public float spawnDustSize() {
        return (float) config.getDouble("spawn.particle.dust.size", 1.0);
    }
    
    public boolean isSpawnDustParticle() {
        return spawnParticleType() == Particle.DUST;
    }

    // ========== SPAWN SOUND ==========
    public boolean spawnSoundEnabled() { 
        return config.getBoolean("spawn.sound.enabled", true); 
    }
    
    public Sound spawnSoundType() {
        String key = config.getString("spawn.sound.type", "BLOCK_NOTE_BLOCK_PLING");
        try {
            return Sound.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }
    
    public float spawnSoundVolume() { 
        return (float) config.getDouble("spawn.sound.volume", 1.0); 
    }
    
    public float spawnSoundPitch() { 
        return (float) config.getDouble("spawn.sound.pitch", 1.0); 
    }

    // ========== WARP SETTINGS ==========
    public boolean warpEnabled() { 
        return config.getBoolean("warp.enabled", true); 
    }
    
    public boolean warpCancelOnMove() { 
        return config.getBoolean("warp.cancel-on-move", true); 
    }
    
    public int warpDefaultCooldown() { 
        return config.getInt("warp.cooldown", 5); 
    }
    
    public int warpDefaultDelay() { 
        return config.getInt("warp.delay", 3); 
    }

    // ========== WARP TITLE ==========
    public boolean warpTitleEnabled() { 
        return config.getBoolean("warp.title.enabled", true); 
    }
    
    public Component getWarpTitleMessage(String warpName) {
        String msg = config.getString("warp.title.message", "<gradient:#00DCFF:#0036FF><b>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ</b> <white>%warp%</white></gradient>");
        msg = msg.replace("%warp%", warpName);
        return parse(msg);
    }
    
    public Component getWarpSubtitleMessage(int seconds) {
        String msg = config.getString("warp.title.subtitle", "<gradient:#4547FF:#A4B7FE><b>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ</b><white> %seconds% </white><b>sᴇᴄᴏɴᴅs</b></gradient>");
        msg = msg.replace("%seconds%", String.valueOf(seconds));
        return parse(msg);
    }

    // ========== WARP BOSSBAR ==========
    public boolean warpBossBarEnabled() { 
        return config.getBoolean("warp.bossbar.enabled", true); 
    }
    
    public Component getWarpBossBarText(String warpName, int seconds) {
        String msg = config.getString("warp.bossbar.text", "<b><gradient:#4547FF:#00DCFF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ %warp%</gradient></b>");
        msg = msg.replace("%warp%", warpName).replace("%seconds%", String.valueOf(seconds));
        return parse(msg);
    }
    
    public BarColor warpBossBarColor() {
        try { 
            return BarColor.valueOf(config.getString("warp.bossbar.color", "BLUE").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return BarColor.BLUE; 
        }
    }
    
    public BarStyle warpBossBarStyle() {
        try { 
            return BarStyle.valueOf(config.getString("warp.bossbar.style", "SOLID").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return BarStyle.SOLID; 
        }
    }

    // ========== WARP PARTICLES ==========
    public boolean warpParticleEnabled() { 
        return config.getBoolean("warp.particle.enabled", true); 
    }
    
    public Particle warpParticleType() {
        try { 
            return Particle.valueOf(config.getString("warp.particle.type", "DUST").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return Particle.PORTAL; 
        }
    }
    
    public int warpParticleCount() { 
        return config.getInt("warp.particle.count", 30); 
    }
    
    public Color warpDustColor() {
        String raw = config.getString("warp.particle.dust.color", "#5555FF");
        return parseColor(raw);
    }
    
    public float warpDustSize() {
        return (float) config.getDouble("warp.particle.dust.size", 1.0);
    }
    
    public boolean isWarpDustParticle() {
        return warpParticleType() == Particle.DUST;
    }

    // ========== WARP SOUND ==========
    public boolean warpSoundEnabled() { 
        return config.getBoolean("warp.sound.enabled", true); 
    }
    
    public Sound warpSoundType() {
        String key = config.getString("warp.sound.type", "BLOCK_NOTE_BLOCK_PLING");
        try {
            return Sound.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }
    
    public float warpSoundVolume() { 
        return (float) config.getDouble("warp.sound.volume", 1.0); 
    }
    
    public float warpSoundPitch() { 
        return (float) config.getDouble("warp.sound.pitch", 1.0); 
    }

    // ========== GLOBAL SETTINGS ==========
    public boolean globalCancelOnMove() { 
        return config.getBoolean("global.cancel-on-move", true); 
    }
    
    public int globalDefaultCooldown() { 
        return config.getInt("global.default-cooldown", 5); 
    }
    
    public int globalDefaultDelay() { 
        return config.getInt("global.default-delay", 5); 
    }

    // ========== RANK MULTIPLIERS ==========
    public double getEffectiveCooldownMultiplier(Player player) {
        return getMultiplierForPlayer(player, "cooldown");
    }

    public double getEffectiveDelayMultiplier(Player player) {
        return getMultiplierForPlayer(player, "delay");
    }
    
    public double getRankMultiplier(Player player, String type) {
        if (type.equalsIgnoreCase("cooldown")) {
            return getEffectiveCooldownMultiplier(player);
        } else if (type.equalsIgnoreCase("delay")) {
            return getEffectiveDelayMultiplier(player);
        }
        return 1.0;
    }

    private double getMultiplierForPlayer(Player player, String type) {
        if (player.hasPermission("ultimatewarps.bypass." + type)) return 0.0;

        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                Group bestGroup = null;
                int bestWeight = Integer.MIN_VALUE;

                for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
                    String groupName = node.getGroupName();
                    Group group = luckPerms.getGroupManager().getGroup(groupName);
                    if (group != null) {
                        int weight = group.getWeight().orElse(0);
                        if (weight > bestWeight) {
                            bestWeight = weight;
                            bestGroup = group;
                        }
                    }
                }

                if (bestGroup != null) {
                    String groupName = bestGroup.getName().toLowerCase();
                    ConfigurationSection section = config.getConfigurationSection("rank-multipliers");
                    if (section != null && section.contains(groupName)) {
                        return config.getDouble("rank-multipliers." + groupName + "." + type, 1.0);
                    }
                }
            }
        } catch (IllegalStateException e) {
            // LuckPerms not present
        }

        return config.getDouble("rank-multipliers.default." + type, 1.0);
    }

    // ========== GUI SETTINGS ==========
    public String warpGuiTitle() { 
        return config.getString("gui.warp-gui-title", "<dark_gray>ᴡᴀʀᴘs"); 
    }
    
    public String adminGuiTitle() { 
        return config.getString("gui.admin-gui-title", "<dark_gray>ᴡᴀʀᴘ ᴀᴅᴍɪɴ"); 
    }
    
    public int guiSize() { 
        return config.getInt("gui.size", 54); 
    }
    
    public Material topFillerMaterial() {
        try { 
            return Material.valueOf(config.getString("gui.top-filler-material", "MAGENTA_STAINED_GLASS_PANE").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return Material.MAGENTA_STAINED_GLASS_PANE; 
        }
    }
    
    public Material middleFillerMaterial() {
        try { 
            return Material.valueOf(config.getString("gui.mid-filler-material", "BLACK_STAINED_GLASS_PANE").toUpperCase()); 
        } catch (IllegalArgumentException e) { 
            return Material.BLACK_STAINED_GLASS_PANE; 
        }
    }

    // ========== SPAWN LOCATION ==========
    public SpawnLocationManager getSpawnLocationManager() {
        return spawnLocationManager;
    }
    
    public Location getSpawnLocation() {
        return spawnLocationManager.getLocation();
    }

    public void setSpawnLocation(Location loc) {
        spawnLocationManager.setLocation(loc);
    }
    
    public void removeSpawnLocation() {
        spawnLocationManager.deleteLocation();
    }

    // ========== TELEPORT SETTINGS ==========
    public boolean shouldCancelOnMove() {
        return config.getBoolean("settings.teleport-cancel-on-move", true);
    }

    public boolean shouldCancelOnDamage() {
        return config.getBoolean("settings.teleport-cancel-on-damage", true);
    }

    // ========== MESSAGE METHODS ==========
    public String getRawMessage(String path) {
        return config.getString("messages." + path, "<red>Missing message: " + path + "</red>");
    }

    public Component getMessage(String path) {
        return parse(getRawMessage(path));
    }

    public Component getMessage(String path, String placeholder, String value) {
        String raw = getRawMessage(path);
        if (raw != null) {
            raw = raw.replace("%" + placeholder + "%", value);
        }
        return parse(raw);
    }
    
    // ========== CONVENIENCE MESSAGE METHODS ==========
    public Component getTeleportCancelledMoveMessage() {
        return getMessage("teleport-cancelled-move");
    }

    public Component getTeleportCancelledDamageMessage() {
        return getMessage("teleport-cancelled-damage");
    }

    public Component getWarpNotFoundMessage() {
        return getMessage("warp-not-found");
    }

    public Component getWarpCreatedMessage() {
        return getMessage("warp-created");
    }

    public Component getWarpDeletedMessage() {
        return getMessage("warp-deleted");
    }

    public Component getWarpEditedMessage() {
        return getMessage("warp-edited");
    }

    public Component getCooldownMessage(int seconds) {
        return getMessage("cooldown-active", "seconds", String.valueOf(seconds));
    }

    public Component getTeleportConfirmedMessage(String warpName) {
        return getMessage("teleportation-confirmed", "warp", warpName);
    }

    public Component getSpawnSetMessage() {
        return getMessage("spawn-set");
    }

    public Component getSpawnNotSetMessage() {
        return getMessage("spawn-not-set");
    }

    public Component getSpawnDeletedMessage() {
        return getMessage("spawn-deleted");
    }

    public Component getReloadSuccessMessage() {
        return getMessage("reload-success");
    }

    public Component getNoPermissionMessage() {
        return getMessage("no-permission");
    }

    // Helper to parse color
    private Color parseColor(String input) {
        if (input == null) return Color.RED;
        if (input.startsWith("#")) {
            try {
                int rgb = Integer.parseInt(input.substring(1), 16);
                return Color.fromRGB(rgb);
            } catch (Exception ignored) {}
        } else if (input.contains(",")) {
            String[] parts = input.split(",");
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Color.fromRGB(r, g, b);
            } catch (Exception ignored) {}
        }
        return Color.RED;
    }
}