package com.ultimatewarps.playerwarps;

import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerWarpsConfigManager {

    private final UltimateWarps plugin;
    private YamlConfiguration config;
    private final File file;

    private static final Pattern LIMIT_NODE = Pattern.compile("^ultimatewarps\\.playerwarps\\.limit\\.(\\d+)$");

    public PlayerWarpsConfigManager(UltimateWarps plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerwarps-config.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("playerwarps-config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        try (InputStream defStream = plugin.getResource("playerwarps-config.yml")) {
            if (defStream != null) {
                try (InputStreamReader reader = new InputStreamReader(defStream, StandardCharsets.UTF_8)) {
                    config.setDefaults(YamlConfiguration.loadConfiguration(reader));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public Component parse(String message) {
        return TextFormat.render(message);
    }

    public boolean featureEnabled() { return config.getBoolean("enabled", true); }
    public int defaultLimit() { return config.getInt("default-limit", 1); }

    // ===== Feature Toggles =====
    public boolean safeWarpEnabled() { return config.getBoolean("safe-warp.enabled", true); }
    public boolean safeWarpCheckOnCreate() { return safeWarpEnabled() && config.getBoolean("safe-warp.check-on-create", true); }
    public boolean safeWarpCheckOnTeleport() { return safeWarpEnabled() && config.getBoolean("safe-warp.check-on-teleport", true); }

    public boolean ratingEnabled() { return config.getBoolean("rating.enabled", true); }
    public String getRatedLoreFormat() {
        return config.getString("rating.rated-lore-format", "&7ʀᴀᴛɪɴɢ: &e{stars} &f({average}/5) &7({count})");
    }
    public String getUnratedLore() {
        return config.getString("rating.unrated-lore", "Not rated yet");
    }

    public boolean moneyEnabled() { return config.getBoolean("money-require.enabled", true); }
    public String getMoneyPermission() {
        return config.getString("money-require.permission", "ultimatewarps.playerwarps.money");
    }
    public String getBalancePlaceholder() {
        return config.getString("money-require.balance-placeholder", "%vault_eco_balance%");
    }
    public String getGiveCommand() {
        return config.getString("money-require.give-command", "/eco give %warpowner% %amount%");
    }
    public String getTakeCommand() {
        return config.getString("money-require.take-command", "/eco take %visitor% %amount%");
    }

    public boolean whitelistEnabled() { return config.getBoolean("whitelist.enabled", true); }
    public boolean blacklistEnabled() { return config.getBoolean("blacklist.enabled", true); }
    public boolean favouritesEnabled() { return config.getBoolean("favourites.enabled", true); }

    // ===== Teleport Settings =====
    public int cooldown() { return config.getInt("teleport.cooldown", 30); }
    public int delay() { return config.getInt("teleport.delay", 3); }
    public boolean cancelOnMove() { return config.getBoolean("teleport.cancel-on-move", true); }

    public boolean titleEnabled() { return config.getBoolean("teleport.title.enabled", true); }

    public Component getTitleMessage(String warpName) {
        String template = config.getString("teleport.title.message",
                "<b><gradient:#00DCFF:#7900FF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ <white>%warp%</white></gradient></b>");
        return TextFormat.renderTemplate(template, "%warp%", sanitizeUntrusted(warpName));
    }

    public Component getSubtitleMessage(int seconds) {
        String template = config.getString("teleport.title.subtitle",
                "<gradient:#4547FF:#A4B7FE><b>ᴘʟᴇᴀsᴇ ᴡᴀɪᴛ</b><white> %seconds% </white><b>sᴇᴄᴏɴᴅs</b></gradient>");
        return TextFormat.renderTemplate(template, "%seconds%", String.valueOf(seconds));
    }

    public boolean bossBarEnabled() { return config.getBoolean("teleport.bossbar.enabled", true); }

    public Component getBossBarText(String warpName, int seconds) {
        String template = config.getString("teleport.bossbar.text",
                "<b><gradient:#4547FF:#00DCFF>ᴛᴇʟᴇᴘᴏʀᴛɪɴɢ ᴛᴏ %warp%</gradient></b>");
        return TextFormat.renderTemplate(template, "%warp%", sanitizeUntrusted(warpName), "%seconds%", String.valueOf(seconds));
    }

    public BarColor bossBarColor() {
        try {
            return BarColor.valueOf(config.getString("teleport.bossbar.color", "BLUE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BarColor.BLUE;
        }
    }

    public BarStyle bossBarStyle() {
        try {
            return BarStyle.valueOf(config.getString("teleport.bossbar.style", "SOLID").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    public boolean particleEnabled() { return config.getBoolean("teleport.particle.enabled", true); }

    public Particle particleType() {
        try {
            return Particle.valueOf(config.getString("teleport.particle.type", "DUST").toUpperCase(Locale.ROOT));
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

    public boolean soundEnabled() { return config.getBoolean("teleport.sound.enabled", true); }

    public Sound soundType() {
        return com.ultimatewarps.ConfigManager.parseSound(
                config.getString("teleport.sound.type", "BLOCK_NOTE_BLOCK_PLING"),
                Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    public float soundVolume() { return (float) config.getDouble("teleport.sound.volume", 1.0); }
    public float soundPitch() { return (float) config.getDouble("teleport.sound.pitch", 1.0); }

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

    public boolean requireOwnClaim() { return config.getBoolean("location-requirements.require-own-claim", false); }
    public boolean requireOutsideRegion() { return config.getBoolean("location-requirements.require-outside-worldguard-region", false); }
    public boolean allowOverworldOnly() { return config.getBoolean("location-requirements.overworld-only", false); }
    public double minDistanceFromSpawn() { return config.getDouble("location-requirements.min-distance-from-spawn", 0); }
    public double minDistanceFromOtherPlayerWarps() { return config.getDouble("location-requirements.min-distance-from-other-playerwarps", 0); }

    public Component getRawMessage(String path) {
        return parse(config.getString("messages." + path, defaultMessage(path)));
    }

    public static String sanitizeUntrusted(String value) {
        if (value == null) return "";
        // Escape MiniMessage tags and strip single/double quotes that could break attribute values
        String escaped = MiniMessage.miniMessage().escapeTags(value);
        return escaped.replace("'", "").replace("\"", "");
    }

    public Component getMessage(String path, String placeholder, String value) {
        String raw = config.getString("messages." + path, defaultMessage(path));
        if (raw == null) return Component.empty();
        if (placeholder == null) return TextFormat.render(raw);
        return TextFormat.renderTemplate(raw, "%" + placeholder + "%", sanitizeUntrusted(value));
    }

    public Component getMessage(String path, String... placeholders) {
        String raw = config.getString("messages." + path, defaultMessage(path));
        if (raw == null) return Component.empty();
        if (placeholders == null || placeholders.length == 0) return TextFormat.render(raw);
        String updated = raw;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            updated = updated.replace("%" + placeholders[i] + "%", sanitizeUntrusted(placeholders[i + 1]));
        }
        return TextFormat.render(updated);
    }

    private String defaultMessage(String path) {
        switch (path) {
            case "no-permission": return "<red>You don't have permission to do that.</red>";
            case "feature-disabled": return "<red>Player warps are currently disabled.</red>";
            case "subfeature-disabled": return "<red>This feature is currently disabled.</red>";
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
            case "unsafe-warp-create": return "<red>You cannot create a warp here! The location is unsafe (needs solid ground below and 2 blocks of air).</red>";
            case "unsafe-warp-teleport": return "<red>Teleport cancelled! The warp '%name%' is currently unsafe.</red>";
            case "rate-warp-prompt": return "<yellow>⭐ Enjoying <white>%name%</white>? <click:run_command:'/playerwarps rate %owner% %warp%'><hover:show_text:'<green>Click to rate %name%</green>'><aqua><b>[Click here to rate this warp]</b></aqua></hover></click></yellow>";
            case "warp-rated": return "<green>You rated '%name%' <yellow>%stars% ★</yellow>!</green>";
            case "already-rated": return "<red>You have already rated this warp.</red>";
            case "must-visit-to-rate": return "<red>You must visit this warp before you can rate it.</red>";
            case "cannot-rate-own-warp": return "<red>You cannot rate your own warp.</red>";
            case "not-enough-money": return "<red>You need at least <yellow>$%amount%</yellow> to teleport to this warp!</red>";
            case "money-paid": return "<yellow>You paid <green>$%amount%</green> to visit <white>%name%</white>.</yellow>";
            case "money-received": return "<green>%visitor% paid $%amount% to visit your warp '%name%'!</green>";
            case "money-toggled": return "<green>Money requirement for '%name%' is now %status%.</green>";
            case "money-cost-set": return "<green>Teleport cost for '%name%' set to <yellow>$%amount%</yellow>.</green>";
            case "invalid-amount": return "<red>Please enter a valid non-negative number.</red>";
            case "papi-missing": return "<red>PlaceholderAPI is required to check player balance for paid warps.</red>";
            case "not-whitelisted": return "<red>You are not whitelisted on this warp.</red>";
            case "blacklisted-from-warp": return "<red>You are blacklisted from this warp.</red>";
            case "whitelist-toggled": return "<green>Whitelist for '%name%' is now %status%.</green>";
            case "whitelist-added": return "<green>Added <white>%player%</white> to the whitelist of '%name%'.</green>";
            case "whitelist-removed": return "<yellow>Removed <white>%player%</white> from the whitelist of '%name%'.</yellow>";
            case "blacklist-added": return "<green>Added <white>%player%</white> to the blacklist of '%name%'.</green>";
            case "blacklist-removed": return "<yellow>Removed <white>%player%</white> from the blacklist of '%name%'.</yellow>";
            case "cannot-list-self": return "<red>You cannot add yourself to the whitelist or blacklist.</red>";
            case "cannot-list-staff": return "<red>You cannot whitelist or blacklist server staff.</red>";
            case "favourite-added": return "<green>Added <white>%name%</white> to your favourite warps!</green>";
            case "favourite-removed": return "<yellow>Removed <white>%name%</white> from your favourite warps.</yellow>";
            default: return "";
        }
    }

    public int getWarpLimit(Player player) {
        if (player.hasPermission("ultimatewarps.playerwarps.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }

        int best = -1;
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            Matcher m = LIMIT_NODE.matcher(info.getPermission());
            if (m.matches()) {
                try {
                    int value = Integer.parseInt(m.group(1));
                    if (value > best) best = value;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return best >= 0 ? best : defaultLimit();
    }

    public double getRankMultiplier(Player player, String type) {
        if (player.hasPermission("ultimatewarps.bypass." + type)) return 0.0;

        ConfigurationSection section = config.getConfigurationSection("rank-multipliers");
        if (section != null && Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String bestGroupName = null;
                    int bestWeight = Integer.MIN_VALUE;
                    for (net.luckperms.api.node.types.InheritanceNode node : user.getNodes(net.luckperms.api.node.NodeType.INHERITANCE)) {
                        if (!node.getValue() || node.hasExpired()) continue;
                        String groupName = node.getGroupName().toLowerCase(Locale.ROOT);
                        if (!section.contains(groupName)) continue;
                        net.luckperms.api.model.group.Group group = luckPerms.getGroupManager().getGroup(node.getGroupName());
                        int weight = group != null ? group.getWeight().orElse(0) : 0;
                        if (weight > bestWeight) {
                            bestWeight = weight;
                            bestGroupName = groupName;
                        }
                    }
                    if (bestGroupName != null) {
                        return config.getDouble("rank-multipliers." + bestGroupName + "." + type, 1.0);
                    }
                }
            } catch (IllegalStateException | LinkageError ignored) {
            }
        }

        if (section != null) {
            double bestPermMult = Double.MAX_VALUE;
            boolean matched = false;
            for (String key : section.getKeys(false)) {
                if (key.equalsIgnoreCase("default")) continue;
                if (player.hasPermission("ultimatewarps.rank." + key.toLowerCase(Locale.ROOT))) {
                    double m = config.getDouble("rank-multipliers." + key + "." + type, 1.0);
                    if (m < bestPermMult) {
                        bestPermMult = m;
                        matched = true;
                    }
                }
            }
            if (matched) return bestPermMult;
        }

        return config.getDouble("rank-multipliers.default." + type, 1.0);
    }
}
