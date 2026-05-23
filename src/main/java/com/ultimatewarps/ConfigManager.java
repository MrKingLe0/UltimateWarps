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
        return miniMessage.deserialize(message);
    }

    // ---------- Spawn settings ----------
    public boolean spawnEnabled() { return config.getBoolean("spawn.enabled", true); }
    public boolean spawnCancelOnMove() { return config.getBoolean("spawn.cancel-on-move", true); }
    public int spawnCooldown() { return config.getInt("spawn.cooldown", 10); }
    public int spawnDelay() { return config.getInt("spawn.delay", 5); }

    // ---------- Rank multipliers (LuckPerms) ----------
    public double getEffectiveCooldownMultiplier(Player player) {
        return getMultiplierForPlayer(player, "cooldown");
    }

    public double getEffectiveDelayMultiplier(Player player) {
        return getMultiplierForPlayer(player, "delay");
    }
    public SpawnLocationManager getSpawnLocationManager() {
        return spawnLocationManager;
    }

    private double getMultiplierForPlayer(Player player, String type) {
        if (player.hasPermission("ultimatewarps.bypass." + type)) return 0.0;

        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                Group bestGroup = null;
                int bestWeight = Integer.MIN_VALUE;

                // ✅ Use NodeType.INHERITANCE instead of StandardNodeTypes
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

    // ---------- Title ----------
    public boolean spawnTitleEnabled() { return config.getBoolean("spawn.title.enabled", true); }
    public String spawnTitleMessage() { return config.getString("spawn.title.message", "Teleporting to Spawn"); }
    public String spawnSubtitleMessage() { return config.getString("spawn.title.subtitle", "Please wait %seconds% seconds"); }

    // ---------- BossBar ----------
    public boolean spawnBossBarEnabled() { return config.getBoolean("spawn.bossbar.enabled", true); }
    public String spawnBossBarText() { return config.getString("spawn.bossbar.text", "<gradient:light_purple:dark_purple>Teleporting...</gradient>"); }
    public BarColor spawnBossBarColor() {
        try { return BarColor.valueOf(config.getString("spawn.bossbar.color", "PURPLE")); }
        catch (IllegalArgumentException e) { return BarColor.PURPLE; }
    }
    public BarStyle spawnBossBarStyle() {
        try { return BarStyle.valueOf(config.getString("spawn.bossbar.style", "SOLID")); }
        catch (IllegalArgumentException e) { return BarStyle.SOLID; }
    }

    public Component getMessage(String path, String placeholderKey, String placeholderValue) {
        String raw = getRawMessage(path);
        raw = raw.replace("%" + placeholderKey + "%", placeholderValue);
        return parse(raw);
    }

    // ---------- Particles ----------
    public boolean spawnParticleEnabled() { return config.getBoolean("spawn.particle.enabled", true); }
    public Particle spawnParticleType() {
        try { return Particle.valueOf(config.getString("spawn.particle.type", "PORTAL")); }
        catch (IllegalArgumentException e) { return Particle.PORTAL; }
    }
    public int spawnParticleCount() { return config.getInt("spawn.particle.count", 30); }

    // ---------- Sound ----------
    public boolean spawnSoundEnabled() { return config.getBoolean("spawn.sound.enabled", true); }
    public Sound spawnSoundType() {
        String key = config.getString("spawn.sound.type", "minecraft:block.note_block.pling");
        if (!key.contains(":")) {
            key = "minecraft:" + key.toLowerCase();
        }
        Sound sound = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.SOUND_EVENT)
                .get(NamespacedKey.fromString(key));
        return sound != null ? sound : Sound.BLOCK_NOTE_BLOCK_PLING;
    }
    public float spawnSoundVolume() { return (float) config.getDouble("spawn.sound.volume", 1.0); }
    public float spawnSoundPitch() { return (float) config.getDouble("spawn.sound.pitch", 1.0); }

    // ---------- Global settings ----------
    public boolean globalCancelOnMove() { return config.getBoolean("global.cancel-on-move", true); }
    public int globalDefaultCooldown() { return config.getInt("global.default-cooldown", 0); }
    public int globalDefaultDelay() { return config.getInt("global.default-delay", 0); }
    // Spawn dust options
    public Color spawnDustColor() {
        String raw = config.getString("spawn.particle.dust.color", "#FF0000");
        return parseColor(raw);
    }
    public float spawnDustSize() {
        return (float) config.getDouble("spawn.particle.dust.size", 1.0);
    }

    // Warp dust options
    public Color warpDustColor() {
        String raw = config.getString("warp.particle.dust.color", "#00FF00");
        return parseColor(raw);
    }
    public float warpDustSize() {
        return (float) config.getDouble("warp.particle.dust.size", 1.5);
    }

    // Helper to parse a colour string
    private Color parseColor(String input) {
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
        return Color.RED; // fallback
    }

    // ---------- GUI titles ----------
    public String warpGuiTitle() { return config.getString("gui.warp-gui-title", "<dark_gray>Warps"); }
    public String adminGuiTitle() { return config.getString("gui.admin-gui-title", "<dark_gray>Warp Admin"); }
    public int guiSize() { return config.getInt("gui.size", 54); }
    public Material topFillerMaterial() {
        try { return Material.valueOf(config.getString("gui.top-filler-material", "BLACK_STAINED_GLASS_PANE")); }
        catch (IllegalArgumentException e) { return Material.BLACK_STAINED_GLASS_PANE; }
    }
    public Material middleFillerMaterial() {
        try { return Material.valueOf(config.getString("gui.mid-filler-material", "GRAY_STAINED_GLASS_PANE")); }
        catch (IllegalArgumentException e) { return Material.GRAY_STAINED_GLASS_PANE; }
    }

    public String getRawMessage(String path) {
        return config.getString("messages." + path, "<red>Missing message: " + path + "</red>");
    }

    public Component getMessage(String path) {
        return parse(getRawMessage(path));
    }

    // ---------- Warp teleport settings ----------
    public boolean warpEnabled() { return config.getBoolean("warp.enabled", true); }
    public boolean warpCancelOnMove() { return config.getBoolean("warp.cancel-on-move", true); }
    public int warpDefaultCooldown() { return config.getInt("warp.cooldown", 5); }
    public int warpDefaultDelay() { return config.getInt("warp.delay", 3); }

    public boolean warpTitleEnabled() { return config.getBoolean("warp.title.enabled", true); }
    public String warpTitleMessage() { return config.getString("warp.title.message", "<gradient:gold:yellow>Teleporting to %warp%</gradient>"); }
    public String warpSubtitleMessage() { return config.getString("warp.title.subtitle", "Please wait %seconds% seconds"); }

    public boolean warpBossBarEnabled() { return config.getBoolean("warp.bossbar.enabled", true); }
    public String warpBossBarText() { return config.getString("warp.bossbar.text", "<gradient:gold:yellow>Teleporting...</gradient>"); }
    public BarColor warpBossBarColor() {
        try { return BarColor.valueOf(config.getString("warp.bossbar.color", "YELLOW")); }
        catch (IllegalArgumentException e) { return BarColor.YELLOW; }
    }
    public BarStyle warpBossBarStyle() {
        try { return BarStyle.valueOf(config.getString("warp.bossbar.style", "SOLID")); }
        catch (IllegalArgumentException e) { return BarStyle.SOLID; }
    }

    public boolean warpParticleEnabled() { return config.getBoolean("warp.particle.enabled", true); }
    public Particle warpParticleType() {
        try { return Particle.valueOf(config.getString("warp.particle.type", "SPELL_WITCH")); }
        catch (IllegalArgumentException e) { return Particle.WITCH; }
    }
    public int warpParticleCount() { return config.getInt("warp.particle.count", 20); }

    public boolean warpSoundEnabled() { return config.getBoolean("warp.sound.enabled", true); }
    public Sound warpSoundType() {
        String key = config.getString("warp.sound.type", "minecraft:block.note_block.pling");
        if (!key.contains(":")) {
            key = "minecraft:" + key.toLowerCase();
        }
        Sound sound = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.SOUND_EVENT)
                .get(NamespacedKey.fromString(key));
        return sound != null ? sound : Sound.BLOCK_NOTE_BLOCK_PLING;
    }
    public float warpSoundVolume() { return (float) config.getDouble("warp.sound.volume", 1.0); }
    public float warpSoundPitch() { return (float) config.getDouble("warp.sound.pitch", 1.0); }

    // ---------- Spawn location storage ----------
    public Location getSpawnLocation() {
        return spawnLocationManager.getLocation();
    }

    public void setSpawnLocation(Location loc) {
        spawnLocationManager.setLocation(loc);
    }
    public void removeSpawnLocation() {
        spawnLocationManager.deleteLocation();
    }
}