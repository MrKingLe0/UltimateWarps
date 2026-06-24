package com.ultimatewarps.playerwarps;

import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads playerwarps-config.yml - kept entirely separate from the admin-warp config.yml so
 * server owners can tune player-warp behavior (limits, claim/region requirements,
 * cooldowns) without touching admin warp settings, and so the two systems can never
 * accidentally cross-read each other's keys.
 */
public class PlayerWarpsConfigManager {

    private final UltimateWarps plugin;
    private YamlConfiguration config;
    private final File file;

    private static final Pattern LIMIT_NODE = Pattern.compile("^ultimatewarps\\.playerwarps\\.limit\\.(\\d+)$");

    public PlayerWarpsConfigManager(UltimateWarps plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerwarps-config.yml");
        if (!file.exists()) {
            plugin.saveResource("playerwarps-config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        // Layer in any defaults newly added by a plugin update that the server owner's
        // existing file predates, so upgrading the plugin never produces a wall of
        // "key not found, using fallback" guesswork - getX() calls below still pass an
        // explicit fallback too, this is just a safety net for getKeys()-style iteration.
        java.io.InputStream defStream = plugin.getResource("playerwarps-config.yml");
        if (defStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8)));
        }
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        java.io.InputStream defStream = plugin.getResource("playerwarps-config.yml");
        if (defStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8)));
        }
    }

    // Bug fix: this used to call miniMessage.deserialize() directly, bypassing
    // TextFormat - same gap as ConfigManager had, meaning player-warp teleport
    // messages never got legacy '&'/hex support or the round-trip-safety fix.
    public Component parse(String message) {
        return TextFormat.render(message);
    }

    // ========== GENERAL ==========
    public boolean featureEnabled() { return config.getBoolean("enabled", true); }
    public int defaultLimit() { return config.getInt("default-limit", 1); }
    public int guiSize() { return Math.max(9, Math.min(54, (config.getInt("gui.rows", 4)) * 9)); }
    public String guiTitle() { return config.getString("gui.title", "<dark_aqua><b>Player Warps</b></dark_aqua>"); }

    // ========== TELEPORT BEHAVIOR (mirrors admin warp settings, own config tree) ==========
    public int cooldown() { return config.getInt("teleport.cooldown", 30); }
    public int delay() { return config.getInt("teleport.delay", 3); }
    public boolean cancelOnMove() { return config.getBoolean("teleport.cancel-on-move", true); }

    // ========== TELEPORT TITLE ==========
    public boolean titleEnabled() { return config.getBoolean("teleport.title.enabled", true); }

    // Bug fix: title/subtitle text used to be hardcoded plain Components in
    // PlayerWarpTeleportTask ("Teleporting...", "%ds remaining") with no way to configure
    // them and no MiniMessage/gradient support - unlike the admin warp title, which has
    // always read its text from config.yml. %warp% is rendered as its own separate
    // Component via TextFormat.renderTemplate() (not substituted as a raw string before
    // parsing), so a warp display name containing its own MiniMessage tags can never
    // corrupt the surrounding template - same fix as ConfigManager.getWarpTitleMessage().
    public Component getTitleMessage(String warpName) {
        String template = config.getString("teleport.title.message",
                "<b><gradient:#00DCFF:#7900FF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ</b> <white>%warp%</white></gradient>");
        return TextFormat.renderTemplate(template, "%warp%", warpName);
    }

    public Component getSubtitleMessage(int seconds) {
        String template = config.getString("teleport.title.subtitle",
                "<gradient:#4547FF:#A4B7FE><b>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ</b><white> %seconds% </white><b>sᴇᴄᴏɴᴅs</b></gradient>");
        return TextFormat.renderTemplate(template, "%seconds%", String.valueOf(seconds));
    }

    // ========== TELEPORT BOSSBAR ==========
    public boolean bossBarEnabled() { return config.getBoolean("teleport.bossbar.enabled", true); }

    public Component getBossBarText(String warpName, int seconds) {
        String template = config.getString("teleport.bossbar.text",
                "<b><gradient:#4547FF:#00DCFF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ %warp%</gradient></b>");
        return TextFormat.renderTemplate(template, "%warp%", warpName, "%seconds%", String.valueOf(seconds));
    }

    public BarColor bossBarColor() {
        try {
            return BarColor.valueOf(config.getString("teleport.bossbar.color", "BLUE").toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.BLUE;
        }
    }

    public BarStyle bossBarStyle() {
        try {
            return BarStyle.valueOf(config.getString("teleport.bossbar.style", "SOLID").toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    // ========== TELEPORT PARTICLES ==========
    public boolean particleEnabled() { return config.getBoolean("teleport.particle.enabled", true); }

    public Particle particleType() {
        try {
            return Particle.valueOf(config.getString("teleport.particle.type", "DUST").toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.DUST;
        }
    }

    public int particleCount() { return config.getInt("teleport.particle.count", 30); }

    public Color dustColor() {
        return parseColor(config.getString("teleport.particle.dust.color", "#5555FF"));
    }

    public float dustSize() { return (float) config.getDouble("teleport.particle.dust.size", 1.0); }

    public boolean isDustParticle() { return particleType() == Particle.DUST; }

    // ========== TELEPORT SOUND ==========
    public boolean soundEnabled() { return config.getBoolean("teleport.sound.enabled", true); }

    public Sound soundType() {
        String key = config.getString("teleport.sound.type", "BLOCK_NOTE_BLOCK_PLING");
        try {
            return Sound.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }

    public float soundVolume() { return (float) config.getDouble("teleport.sound.volume", 1.0); }
    public float soundPitch() { return (float) config.getDouble("teleport.sound.pitch", 1.0); }

    // Same hex/"r,g,b" parsing as ConfigManager.parseColor(), kept as a separate copy so
    // this file never needs to depend on ConfigManager internals.
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

    // ========== LOCATION REQUIREMENTS ==========
    public boolean requireOwnClaim() { return config.getBoolean("location-requirements.require-own-claim", false); }
    public boolean requireOutsideRegion() { return config.getBoolean("location-requirements.require-outside-worldguard-region", false); }
    public boolean allowOverworldOnly() { return config.getBoolean("location-requirements.overworld-only", false); }
    public double minDistanceFromSpawn() { return config.getDouble("location-requirements.min-distance-from-spawn", 0); }
    public double minDistanceFromOtherPlayerWarps() { return config.getDouble("location-requirements.min-distance-from-other-playerwarps", 0); }

    // ========== MESSAGES ==========
    public Component getRawMessage(String path) {
        return parse(config.getString("messages." + path, defaultMessage(path)));
    }

    // Bug fix: this used to substitute the placeholder as a raw string into the
    // template before parsing once - exactly the bug that corrupted gradient tags in
    // teleport-confirmed and other player-warp messages. See ConfigManager.getMessage()
    // for the full explanation.
    public Component getMessage(String path, String placeholder, String value) {
        String raw = config.getString("messages." + path, defaultMessage(path));
        if (raw == null) return Component.empty();
        if (placeholder == null) return TextFormat.render(raw);
        return TextFormat.renderTemplate(raw, "%" + placeholder + "%", value == null ? "" : value);
    }

    private String defaultMessage(String path) {
        switch (path) {
            case "no-permission": return "<red>You don't have permission to do that.</red>";
            case "feature-disabled": return "<red>Player warps are currently disabled.</red>";
            case "limit-reached": return "<red>You've reached your player warp limit (%limit%). Delete one with /playerwarps del <name> first.</red>";
            case "warp-created": return "<green>Player warp '%name%' created!</green>";
            case "warp-deleted": return "<green>Player warp '%name%' deleted.</green>";
            case "warp-not-found": return "<red>That player warp doesn't exist.</red>";
            case "not-your-warp": return "<red>That's not your warp.</red>";
            case "not-in-own-claim": return "<red>You must be inside your own GriefPrevention claim to set a player warp here.</red>";
            case "inside-region": return "<red>You can't set a player warp inside a WorldGuard region.</red>";
            case "wrong-world": return "<red>Player warps can only be set in the overworld.</red>";
            case "too-close-to-spawn": return "<red>That location is too close to spawn.</red>";
            case "too-close-to-other-warp": return "<red>That location is too close to another player's warp.</red>";
            case "invalid-name": return "<red>Warp names may only contain letters, numbers, underscores and hyphens (max 32 characters).</red>";
            case "name-taken": return "<red>You already have a player warp named '%name%'.</red>";
            case "cooldown": return "<red>You must wait %seconds% more second(s) before warping again.</red>";
            case "teleport-confirmed": return "<green>Teleported to %name%!</green>";
            case "teleported-cancelled-move": return "<red>Teleport cancelled - you moved!</red>";
            case "private-warp": return "<red>That warp is private.</red>";
            default: return "";
        }
    }

    /**
     * Per-player warp limit, resolved from numbered permission nodes
     * (ultimatewarps.playerwarps.limit.<n>) - the highest number the player holds wins.
     * Falls back to default-limit in playerwarps-config.yml if the player holds none of
     * the numbered nodes. Unlimited is represented as -1 (via
     * ultimatewarps.playerwarps.limit.unlimited, checked separately since it isn't a number).
     */
    public int getWarpLimit(Player player) {
        if (player.hasPermission("ultimatewarps.playerwarps.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }

        int best = -1;
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue; // explicitly negated node - doesn't count
            Matcher m = LIMIT_NODE.matcher(info.getPermission());
            if (m.matches()) {
                try {
                    int value = Integer.parseInt(m.group(1));
                    if (value > best) best = value;
                } catch (NumberFormatException ignored) {
                    // node had a non-numeric suffix somehow - skip it
                }
            }
        }

        return best >= 0 ? best : defaultLimit();
    }

    /** Improvement: mirrors ConfigManager's LuckPerms-based rank multiplier lookup, scoped to player warps. */
    public double getRankMultiplier(Player player, String type) {
        if (player.hasPermission("ultimatewarps.bypass." + type)) return 0.0;
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                net.luckperms.api.model.group.Group bestGroup = null;
                int bestWeight = Integer.MIN_VALUE;
                for (net.luckperms.api.node.types.InheritanceNode node : user.getNodes(net.luckperms.api.node.NodeType.INHERITANCE)) {
                    net.luckperms.api.model.group.Group group = luckPerms.getGroupManager().getGroup(node.getGroupName());
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
                    if (config.contains("rank-multipliers." + groupName)) {
                        return config.getDouble("rank-multipliers." + groupName + "." + type, 1.0);
                    }
                }
            }
        } catch (IllegalStateException e) {
            // LuckPerms not present
        }
        return config.getDouble("rank-multipliers.default." + type, 1.0);
    }
}
