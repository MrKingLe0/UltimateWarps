package com.ultimatewarps.playerwarps;

import com.ultimatewarps.UltimateWarps;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlayerWarpManager {

    private static final Pattern VALID_WARP_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    private final Map<String, PlayerWarp> warpsByKey = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> favouritesByPlayer = new ConcurrentHashMap<>();
    private File rootFolder;
    private File favouritesFile;

    public PlayerWarpManager(UltimateWarps plugin) {

    }

    public File getRootFolder() {
        if (rootFolder == null) {
            rootFolder = new File(UltimateWarps.getInstance().getDataFolder(), "playerwarps");
            if (!rootFolder.exists()) rootFolder.mkdirs();
        }
        return rootFolder;
    }

    public static File getOwnerFolder(UUID ownerId) {
        File folder = new File(UltimateWarps.getInstance().getPlayerWarpManager().getRootFolder(), ownerId.toString());
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    public static String key(UUID ownerId, String name) {
        return ownerId.toString() + ":" + (name != null ? name.toLowerCase(Locale.ROOT) : "");
    }

    public static String key(PlayerWarp warp) {
        return key(warp.getOwnerId(), warp.getName());
    }

    private static final Set<String> RESERVED_WARP_NAMES = Set.of("warp");

    public boolean isValidWarpName(String name) {
        return name != null
                && VALID_WARP_NAME.matcher(name).matches()
                && !RESERVED_WARP_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    public void loadAll() {
        warpsByKey.clear();
        File root = getRootFolder();
        File[] ownerFolders = root.listFiles(File::isDirectory);
        if (ownerFolders != null) {
            for (File ownerFolder : ownerFolders) {
                UUID ownerId;
                try {
                    ownerId = UUID.fromString(ownerFolder.getName());
                } catch (IllegalArgumentException e) {
                    UltimateWarps.getInstance().getLogger()
                            .warning("Skipping non-UUID folder in playerwarps/: " + ownerFolder.getName());
                    continue;
                }
                File[] files = ownerFolder.listFiles((dir, name) -> name.endsWith(".yml"));
                if (files == null) continue;
                for (File file : files) {
                    PlayerWarp warp = PlayerWarp.load(file, ownerId);
                    if (warp != null) {
                        warpsByKey.put(key(warp.getOwnerId(), warp.getName()), warp);
                    }
                }
            }
        }
        loadFavourites();
    }

    public void saveAll() {
        for (PlayerWarp warp : warpsByKey.values()) {
            try {
                warp.save();
            } catch (Exception e) {
                UltimateWarps.getInstance().getLogger()
                        .severe("Failed to save player warp " + warp.getName() + ": " + e.getMessage());
            }
        }
        saveFavourites();
    }

    // ===== Favourites Persistence =====
    private File getFavouritesFile() {
        if (favouritesFile == null) {
            favouritesFile = new File(getRootFolder(), "favourites.yml");
        }
        return favouritesFile;
    }

    public void loadFavourites() {
        favouritesByPlayer.clear();
        File favFile = getFavouritesFile();
        if (!favFile.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(favFile);
        ConfigurationSection sec = yml.getConfigurationSection("favourites");
        if (sec == null) return;
        for (String uuidStr : sec.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(uuidStr);
                List<String> keys = sec.getStringList(uuidStr);
                Set<String> set = ConcurrentHashMap.newKeySet();
                set.addAll(keys);
                favouritesByPlayer.put(playerId, set);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveFavourites() {
        File favFile = getFavouritesFile();
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> entry : favouritesByPlayer.entrySet()) {
            yml.set("favourites." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        try {
            yml.save(favFile);
        } catch (Exception e) {
            UltimateWarps.getInstance().getLogger()
                    .severe("Could not save player warp favourites: " + e.getMessage());
        }
    }

    public boolean isFavourite(UUID playerUuid, PlayerWarp warp) {
        if (warp == null) return false;
        Set<String> set = favouritesByPlayer.get(playerUuid);
        return set != null && set.contains(key(warp));
    }

    public boolean toggleFavourite(UUID playerUuid, PlayerWarp warp) {
        Set<String> set = favouritesByPlayer.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet());
        String k = key(warp);
        boolean added;
        if (set.contains(k)) {
            set.remove(k);
            added = false;
        } else {
            set.add(k);
            added = true;
        }
        saveFavourites();
        return added;
    }

    public List<PlayerWarp> getFavouriteWarps(Player viewer) {
        Set<String> favKeys = favouritesByPlayer.getOrDefault(viewer.getUniqueId(), Collections.emptySet());
        return getVisibleWarps(viewer).stream()
                .filter(w -> favKeys.contains(key(w)))
                .collect(Collectors.toList());
    }

    // ===== Staff Check Helper =====
    public boolean isStaff(OfflinePlayer target) {
        if (target == null) return false;
        if (target.isOp()) return true;
        Player online = target.getPlayer();
        if (online != null) {
            return online.hasPermission("ultimatewarps.playerwarps.admin")
                    || online.hasPermission("ultimatewarps.admin");
        }
        return false;
    }

    public PlayerWarp getWarp(UUID ownerId, String name) {
        if (ownerId == null || name == null) return null;
        return warpsByKey.get(key(ownerId, name));
    }

    public PlayerWarp getWarp(String ownerName, String name) {
        if (ownerName == null || name == null) return null;
        String lowerName = name.toLowerCase(Locale.ROOT);
        String lowerOwner = ownerName.toLowerCase(Locale.ROOT);
        return warpsByKey.values().stream()
                .filter(w -> w.getName().toLowerCase(Locale.ROOT).equals(lowerName)
                        && w.getOwnerName() != null && w.getOwnerName().toLowerCase(Locale.ROOT).equals(lowerOwner))
                .findFirst().orElse(null);
    }

    public PlayerWarp findVisibleWarpByName(Player viewer, String name) {
        if (name == null) return null;
        PlayerWarp own = getWarp(viewer.getUniqueId(), name);
        if (own != null) return own;
        String lowerName = name.toLowerCase(Locale.ROOT);
        return getVisibleWarps(viewer).stream()
                .filter(w -> w.getName().toLowerCase(Locale.ROOT).equals(lowerName))
                .findFirst().orElse(null);
    }

    public List<PlayerWarp> getWarpsByOwner(UUID ownerId) {
        return warpsByKey.values().stream()
                .filter(w -> w.getOwnerId().equals(ownerId))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    public int countWarpsByOwner(UUID ownerId) {
        int count = 0;
        for (PlayerWarp w : warpsByKey.values()) {
            if (w.getOwnerId().equals(ownerId)) count++;
        }
        return count;
    }

    public Collection<PlayerWarp> getAllWarps() {
        return Collections.unmodifiableCollection(warpsByKey.values());
    }

    public List<PlayerWarp> getVisibleWarps(Player viewer) {
        boolean canSeeAll = isStaff(viewer);
        return warpsByKey.values().stream()
                .filter(w -> canSeeAll || w.isPublic() || w.getOwnerId().equals(viewer.getUniqueId()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    public void addWarp(PlayerWarp warp) {
        if (!isValidWarpName(warp.getName())) {
            throw new IllegalArgumentException(
                "Invalid player warp name: '" + warp.getName() + "'. " +
                "Warp names may only contain letters, numbers, underscores and hyphens (max 32 characters)."
            );
        }
        warp.setFile(new File(getOwnerFolder(warp.getOwnerId()), warp.getName() + ".yml"));
        warp.save();
        warpsByKey.put(key(warp.getOwnerId(), warp.getName()), warp);
    }

    public void removeWarp(UUID ownerId, String name) {
        String k = key(ownerId, name);
        PlayerWarp warp = warpsByKey.remove(k);
        if (warp != null) {
            warp.delete();
        }
        for (Set<String> favs : favouritesByPlayer.values()) {
            favs.remove(k);
        }
        saveFavourites();
    }

    public int removeAllWarpsByOwner(UUID ownerId) {
        List<String> toRemove = warpsByKey.values().stream()
                .filter(w -> w.getOwnerId().equals(ownerId))
                .map(w -> key(w.getOwnerId(), w.getName()))
                .collect(Collectors.toList());
        for (String k : toRemove) {
            PlayerWarp warp = warpsByKey.remove(k);
            if (warp != null) warp.delete();
            for (Set<String> favs : favouritesByPlayer.values()) {
                favs.remove(k);
            }
        }
        if (!toRemove.isEmpty()) {
            saveFavourites();
        }
        return toRemove.size();
    }
}
