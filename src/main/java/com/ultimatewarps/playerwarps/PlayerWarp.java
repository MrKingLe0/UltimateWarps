package com.ultimatewarps.playerwarps;

import com.ultimatewarps.playerwarps.integration.EconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class PlayerWarp {

    private final String name;
    private final UUID ownerId;
    private String ownerName;
    private Location location;
    private String worldName;
    private String displayName;
    private ItemStack icon;
    private boolean isPublic = true;
    private String description;
    private long createdAt;

    // Money require feature
    private boolean moneyRequired = false;
    private double teleportCost = 0.0;

    // Whitelist & Blacklist feature
    private boolean whitelistEnabled = false;
    private final Set<UUID> whitelist = new LinkedHashSet<>();
    private final Set<UUID> blacklist = new LinkedHashSet<>();
    private final Map<UUID, String> knownPlayerNames = new HashMap<>();

    // Rating feature
    private final Map<UUID, Integer> ratings = new HashMap<>();
    private final Set<UUID> visitors = new HashSet<>();

    private File file;

    public PlayerWarp(String name, UUID ownerId, String ownerName, Location location) {
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location.clone();
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : null;
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

    public void setLocation(Location location) {
        this.location = location.clone();
        if (location.getWorld() != null) {
            this.worldName = location.getWorld().getName();
        }
    }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setIcon(ItemStack icon) { this.icon = icon != null ? icon.clone() : null; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setDescription(String description) { this.description = description; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setFile(File file) { this.file = file; }

    // ===== Money Require =====
    public boolean isMoneyRequired() { return moneyRequired; }
    public void setMoneyRequired(boolean moneyRequired) { this.moneyRequired = moneyRequired; }

    public double getTeleportCost() { return teleportCost; }
    public void setTeleportCost(double teleportCost) {
        if (!Double.isFinite(teleportCost) || teleportCost < 0.0) {
            this.teleportCost = 0.0;
        } else {
            this.teleportCost = EconomyBridge.roundCost(teleportCost);
        }
    }

    public String getFormattedCost(PlayerWarpsConfigManager config) {
        if (!config.moneyEnabled() || !moneyRequired || teleportCost <= 0) {
            return "Free";
        }
        return "$" + EconomyBridge.formatAmount(teleportCost);
    }

    // ===== Whitelist & Blacklist =====
    public boolean isWhitelistEnabled() { return whitelistEnabled; }
    public void setWhitelistEnabled(boolean whitelistEnabled) { this.whitelistEnabled = whitelistEnabled; }

    public Set<UUID> getWhitelist() { return Collections.unmodifiableSet(whitelist); }
    public Set<UUID> getBlacklist() { return Collections.unmodifiableSet(blacklist); }

    public boolean isWhitelisted(UUID uuid) { return whitelist.contains(uuid); }
    public boolean isBlacklisted(UUID uuid) { return blacklist.contains(uuid); }

    public boolean addWhitelistedPlayer(UUID uuid, String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            knownPlayerNames.put(uuid, playerName);
        }
        return whitelist.add(uuid);
    }

    public boolean removeWhitelistedPlayer(UUID uuid) {
        boolean removed = whitelist.remove(uuid);
        if (removed && !blacklist.contains(uuid)) {
            knownPlayerNames.remove(uuid);
        }
        return removed;
    }

    public boolean addBlacklistedPlayer(UUID uuid, String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            knownPlayerNames.put(uuid, playerName);
        }
        return blacklist.add(uuid);
    }

    public boolean removeBlacklistedPlayer(UUID uuid) {
        boolean removed = blacklist.remove(uuid);
        if (removed && !whitelist.contains(uuid)) {
            knownPlayerNames.remove(uuid);
        }
        return removed;
    }

    public String getKnownPlayerName(UUID uuid) {
        String cached = knownPlayerNames.get(uuid);
        if (cached != null && !cached.isBlank()) return cached;
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.getName() != null) {
            knownPlayerNames.put(uuid, op.getName());
            return op.getName();
        }
        return uuid.toString().substring(0, 8);
    }

    // ===== Rating & Visits =====
    public boolean recordVisit(UUID visitorUuid) {
        if (!ownerId.equals(visitorUuid)) {
            return visitors.add(visitorUuid);
        }
        return false;
    }

    public boolean hasVisited(UUID visitorUuid) {
        return visitors.contains(visitorUuid);
    }

    public boolean hasRated(UUID visitorUuid) {
        return ratings.containsKey(visitorUuid);
    }

    public int getPlayerRating(UUID visitorUuid) {
        return ratings.getOrDefault(visitorUuid, 0);
    }

    public void setRating(UUID visitorUuid, int stars) {
        int clamped = Math.max(1, Math.min(5, stars));
        ratings.put(visitorUuid, clamped);
    }

    public int getRatingCount() {
        return ratings.size();
    }

    public double getAverageRating() {
        if (ratings.isEmpty()) return 0.0;
        long sum = 0;
        for (int r : ratings.values()) {
            sum += r;
        }
        return (double) sum / ratings.size();
    }

    public String getStarDisplay() {
        int filled = (int) Math.round(getAverageRating());
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= filled ? "★" : "☆");
        }
        return sb.toString();
    }

    public String getFormattedRatingLore(PlayerWarpsConfigManager config) {
        if (ratings.isEmpty()) {
            return config.getUnratedLore();
        }
        String avgStr = String.format(Locale.US, "%.1f", getAverageRating());
        return config.getRatedLoreFormat()
                .replace("{stars}", getStarDisplay())
                .replace("{average}", avgStr)
                .replace("{count}", String.valueOf(ratings.size()));
    }

    public void save() {
        if (file == null) {
            file = new File(PlayerWarpManager.getOwnerFolder(ownerId), name + ".yml");
        }
        String wName = (location != null && location.getWorld() != null) ? location.getWorld().getName() : worldName;
        if (wName == null) {
            com.ultimatewarps.UltimateWarps.getInstance().getLogger()
                    .warning("Skipping save for player warp " + name + " (world is null).");
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("owner-id", ownerId.toString());
        yml.set("owner-name", ownerName);
        yml.set("location.world", wName);
        yml.set("location.x", location.getX());
        yml.set("location.y", location.getY());
        yml.set("location.z", location.getZ());
        yml.set("location.yaw", location.getYaw());
        yml.set("location.pitch", location.getPitch());
        yml.set("public", isPublic);
        yml.set("created-at", createdAt);
        if (displayName != null) yml.set("display-name", displayName);
        if (description != null) yml.set("description", description);

        if (icon != null) {
            yml.set("icon", icon);
        }

        // Money require
        yml.set("money-required", moneyRequired);
        yml.set("teleport-cost", teleportCost);

        // Whitelist & Blacklist
        yml.set("whitelist-enabled", whitelistEnabled);
        List<String> wlStrings = new ArrayList<>();
        for (UUID id : whitelist) wlStrings.add(id.toString());
        yml.set("whitelist", wlStrings);

        List<String> blStrings = new ArrayList<>();
        for (UUID id : blacklist) blStrings.add(id.toString());
        yml.set("blacklist", blStrings);

        for (Map.Entry<UUID, String> entry : knownPlayerNames.entrySet()) {
            yml.set("known-player-names." + entry.getKey().toString(), entry.getValue());
        }

        // Ratings & Visitors
        for (Map.Entry<UUID, Integer> entry : ratings.entrySet()) {
            yml.set("ratings." + entry.getKey().toString(), entry.getValue());
        }
        List<String> visitorStrings = new ArrayList<>();
        for (UUID id : visitors) visitorStrings.add(id.toString());
        yml.set("visitors", visitorStrings);

        try {
            yml.save(file);
        } catch (Exception e) {
            com.ultimatewarps.UltimateWarps.getInstance().getLogger()
                    .severe("Could not save player warp " + name + ": " + e.getMessage());
        }
    }

    public static PlayerWarp load(File file, UUID ownerIdFromFolder) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        String name = file.getName().substring(0, file.getName().length() - 4);

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

        // Money require
        warp.moneyRequired = yml.getBoolean("money-required", false);
        warp.setTeleportCost(yml.getDouble("teleport-cost", 0.0));

        // Whitelist & Blacklist
        warp.whitelistEnabled = yml.getBoolean("whitelist-enabled", false);
        for (String s : yml.getStringList("whitelist")) {
            try { warp.whitelist.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        for (String s : yml.getStringList("blacklist")) {
            try { warp.blacklist.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        ConfigurationSection namesSec = yml.getConfigurationSection("known-player-names");
        if (namesSec != null) {
            for (String k : namesSec.getKeys(false)) {
                try {
                    warp.knownPlayerNames.put(UUID.fromString(k), namesSec.getString(k, "Unknown"));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Ratings & Visitors
        ConfigurationSection ratingsSec = yml.getConfigurationSection("ratings");
        if (ratingsSec != null) {
            for (String k : ratingsSec.getKeys(false)) {
                try {
                    int val = ratingsSec.getInt(k, 0);
                    if (val >= 1 && val <= 5) {
                        warp.ratings.put(UUID.fromString(k), val);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        for (String s : yml.getStringList("visitors")) {
            try { warp.visitors.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }

        if (yml.contains("icon")) {
            Object raw = yml.get("icon");
            if (raw instanceof ItemStack loaded) {
                if (loaded.getType() != null && loaded.getType() != org.bukkit.Material.AIR) {
                    warp.icon = loaded;
                }
            } else if (raw instanceof String base64) {
                try {
                    byte[] bytes = java.util.Base64.getDecoder().decode(base64);
                    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
                         org.bukkit.util.io.BukkitObjectInputStream bois = new org.bukkit.util.io.BukkitObjectInputStream(bais)) {
                        ItemStack loaded = (ItemStack) bois.readObject();
                        if (loaded != null && loaded.getType() != null && loaded.getType() != org.bukkit.Material.AIR) {
                            warp.icon = loaded;
                        }
                    }
                } catch (Exception e) {
                    com.ultimatewarps.UltimateWarps.getInstance().getLogger().info(
                            "Icon for player warp " + name + " could not be loaded - reverting to default icon.");
                }
            }
        }

        return warp;
    }

    public void delete() {
        if (file != null && file.exists()) file.delete();
    }
}
