package com.ultimatewarps;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {

    private final UltimateWarps plugin;
    private FileConfiguration config;
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
        return TextFormat.render(message);
    }

    public boolean spawnEnabled() { return config.getBoolean("spawn.enabled", true); }
    public boolean spawnCancelOnMove() { return config.getBoolean("spawn.cancel-on-move", true); }
    public int spawnCooldown() { return config.getInt("spawn.cooldown", 10); }
    public int spawnDelay() { return config.getInt("spawn.delay", 5); }

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

    public boolean spawnSoundEnabled() {
        return config.getBoolean("spawn.sound.enabled", true);
    }

    public Sound spawnSoundType() {
        return parseSound(config.getString("spawn.sound.type", "BLOCK_NOTE_BLOCK_PLING"), Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    public float spawnSoundVolume() {
        return (float) config.getDouble("spawn.sound.volume", 1.0);
    }

    public float spawnSoundPitch() {
        return (float) config.getDouble("spawn.sound.pitch", 1.0);
    }

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

    public boolean warpTitleEnabled() {
        return config.getBoolean("warp.title.enabled", true);
    }

    public Component getWarpTitleMessage(String warpName) {
        String template = config.getString("warp.title.message", "<gradient:#00DCFF:#0036FF><b>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ</b> <white>%warp%</white></gradient>");

        return TextFormat.renderTemplate(template, "%warp%", warpName);
    }

    public Component getWarpSubtitleMessage(int seconds) {
        String template = config.getString("warp.title.subtitle", "<gradient:#4547FF:#A4B7FE><b>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ</b><white> %seconds% </white><b>sᴇᴄᴏɴᴅs</b></gradient>");
        return TextFormat.renderTemplate(template, "%seconds%", String.valueOf(seconds));
    }

    public boolean warpBossBarEnabled() {
        return config.getBoolean("warp.bossbar.enabled", true);
    }

    public Component getWarpBossBarText(String warpName, int seconds) {
        String template = config.getString("warp.bossbar.text", "<b><gradient:#4547FF:#00DCFF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ %warp%</gradient></b>");
        return TextFormat.renderTemplate(template, "%warp%", warpName, "%seconds%", String.valueOf(seconds));
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

    public boolean warpParticleEnabled() {
        return config.getBoolean("warp.particle.enabled", true);
    }

    public Particle warpParticleType() {
        try {
            return Particle.valueOf(config.getString("warp.particle.type", "DUST").toUpperCase());
        } catch (IllegalArgumentException e) {

            return Particle.DUST;
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

    public boolean warpSoundEnabled() {
        return config.getBoolean("warp.sound.enabled", true);
    }

    public Sound warpSoundType() {
        return parseSound(config.getString("warp.sound.type", "BLOCK_NOTE_BLOCK_PLING"), Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    public float warpSoundVolume() {
        return (float) config.getDouble("warp.sound.volume", 1.0);
    }

    public float warpSoundPitch() {
        return (float) config.getDouble("warp.sound.pitch", 1.0);
    }

    public String warpParticleTypeRaw() { return config.getString("warp.particle.type", "DUST"); }
    public String warpDustColorRaw() { return config.getString("warp.particle.dust.color", "#5555FF"); }
    public String warpSoundTypeRaw() { return config.getString("warp.sound.type", "BLOCK_NOTE_BLOCK_PLING"); }

    public int globalDefaultCooldown() {
        return config.getInt("global.default-cooldown", 5);
    }

    public int globalDefaultDelay() {
        return config.getInt("global.default-delay", 5);
    }

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

        }

        return config.getDouble("rank-multipliers.default." + type, 1.0);
    }

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

    public boolean shouldCancelOnDamage() {
        return config.getBoolean("settings.teleport-cancel-on-damage", true);
    }

    public String getRawMessage(String path) {
        return config.getString("messages." + path, "<red>Missing message: " + path + "</red>");
    }

    public Component getMessage(String path) {
        return parse(getRawMessage(path));
    }

    public Component getMessage(String path, String placeholder, String value) {
        String raw = getRawMessage(path);
        if (raw == null) return Component.empty();
        return TextFormat.renderTemplate(raw, "%" + placeholder + "%", value);
    }

    public boolean spawnTeleportOnEveryJoin() {
        return config.getBoolean("spawn.teleport-on-every-join.enabled", false);
    }

    public String getSpawnTeleportOnEveryJoinMessage() {
        return config.getString("spawn.teleport-on-every-join.message", "<gradient:#55ff55:#00aa00>Welcome back! You have been teleported to spawn.</gradient>");
    }

    public boolean spawnTeleportOnFirstJoin() {
        return config.getBoolean("spawn.teleport-on-first-join.enabled", true);
    }

    public String getSpawnTeleportOnFirstJoinMessage() {
        return config.getString("spawn.teleport-on-first-join.message", "<gradient:#55ff55:#00aa00>Welcome to the server! You have been teleported to spawn.</gradient>");
    }

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

    private static volatile List<String> SOUND_KEYS;
    private static Map<String, Sound> SOUND_BY_KEY;
    private static Map<String, Sound> SOUND_BY_ENUM;

    private static synchronized void initSounds() {
        if (SOUND_KEYS != null) return;
        List<String> keys = new ArrayList<>();
        Map<String, Sound> byKey = new HashMap<>();
        Map<String, Sound> byEnum = new HashMap<>();
        try {
            for (Sound s : RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT)) {
                NamespacedKey nk = s.getKey();
                if (nk == null) continue;
                String path = nk.getKey();
                keys.add(path);
                byKey.put(path.toLowerCase(Locale.ROOT), s);
                byEnum.put(path.replace('.', '_').replace('/', '_').toUpperCase(Locale.ROOT), s);
            }
        } catch (Exception ignored) {
        }
        Collections.sort(keys);
        SOUND_BY_KEY = byKey;
        SOUND_BY_ENUM = byEnum;
        SOUND_KEYS = keys;
    }

    public static List<String> allSoundKeys() {
        initSounds();
        return SOUND_KEYS;
    }

    public static Sound parseSound(String key, Sound fallback) {
        if (key == null) return fallback;
        String k = key.trim();
        if (k.isEmpty()) return fallback;
        initSounds();

        if (k.contains(":")) {
            try {
                NamespacedKey nk = NamespacedKey.fromString(k.toLowerCase(Locale.ROOT));
                if (nk != null) {
                    Sound s = RegistryAccess.registryAccess()
                            .getRegistry(RegistryKey.SOUND_EVENT)
                            .get(nk);
                    if (s != null) return s;
                }
            } catch (Exception ignored) {
            }
        }

        Sound byKey = SOUND_BY_KEY.get(k.toLowerCase(Locale.ROOT));
        if (byKey != null) return byKey;

        Sound byEnum = SOUND_BY_ENUM.get(k.toUpperCase(Locale.ROOT));
        if (byEnum != null) return byEnum;

        return fallback;
    }

    public static Color parseColor(String input) {
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
