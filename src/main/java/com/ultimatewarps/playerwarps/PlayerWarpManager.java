package com.ultimatewarps.playerwarps;

import com.ultimatewarps.UltimateWarps;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Stores and looks up player-created warps. Each owner gets their own subfolder under
 * playerwarps/, one file per warp, so two different players can each have a warp named
 * "home" without any collision, and wiping/listing one player's warps never requires
 * scanning every other player's files.
 */
public class PlayerWarpManager {

    private static final Pattern VALID_WARP_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    // Key: ownerId + ":" + lowercase warp name
    private final Map<String, PlayerWarp> warpsByKey = new ConcurrentHashMap<>();
    private File rootFolder;

    public PlayerWarpManager(UltimateWarps plugin) {
        // plugin reference kept implicit via UltimateWarps.getInstance() elsewhere,
        // matching the rest of this codebase's static-singleton access pattern.
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

    private static String key(UUID ownerId, String name) {
        return ownerId.toString() + ":" + name.toLowerCase();
    }

    // "warp" is reserved - it's the literal subcommand word for /playerwarps warp <name>,
    // so a player warp named "warp" would be unreachable by name (handleTeleportShorthand
    // would never see it as the <name> argument, only as the subcommand itself). This is
    // the same class of bug that used to affect "edit"/"del"/"set"/"list"/"gui"/"admin"
    // before teleporting got its own explicit subcommand - "warp" is the one word that
    // still needs to stay off-limits, so it's blocked here at creation time instead of
    // silently producing an unreachable warp.
    private static final java.util.Set<String> RESERVED_WARP_NAMES = java.util.Set.of("warp");

    public boolean isValidWarpName(String name) {
        return name != null
                && VALID_WARP_NAME.matcher(name).matches()
                && !RESERVED_WARP_NAMES.contains(name.toLowerCase());
    }

    public void loadAll() {
        warpsByKey.clear();
        File root = getRootFolder();
        File[] ownerFolders = root.listFiles(File::isDirectory);
        if (ownerFolders == null) return;

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

    public void saveAll() {
        for (PlayerWarp warp : warpsByKey.values()) {
            warp.save();
        }
    }

    public PlayerWarp getWarp(UUID ownerId, String name) {
        return warpsByKey.get(key(ownerId, name));
    }

    /** Look up a player warp by owner name + warp name, for commands like /playerwarps <owner> <warp>. */
    public PlayerWarp getWarp(String ownerName, String name) {
        String lowerName = name.toLowerCase();
        String lowerOwner = ownerName.toLowerCase();
        return warpsByKey.values().stream()
                .filter(w -> w.getName().toLowerCase().equals(lowerName)
                        && w.getOwnerName() != null && w.getOwnerName().toLowerCase().equals(lowerOwner))
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

    /** Public warps belonging to anyone, or any warp belonging to the viewer themselves, or anything if they're an admin/moderator. */
    public List<PlayerWarp> getVisibleWarps(Player viewer) {
        boolean canSeeAll = viewer.hasPermission("ultimatewarps.playerwarps.admin");
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
        PlayerWarp warp = warpsByKey.remove(key(ownerId, name));
        if (warp != null) {
            warp.delete();
        }
    }

    /** Removes every warp owned by a player - used for /playerwarps admin wipe <player>. */
    public int removeAllWarpsByOwner(UUID ownerId) {
        List<String> toRemove = warpsByKey.values().stream()
                .filter(w -> w.getOwnerId().equals(ownerId))
                .map(w -> key(w.getOwnerId(), w.getName()))
                .collect(Collectors.toList());
        for (String k : toRemove) {
            PlayerWarp warp = warpsByKey.remove(k);
            if (warp != null) warp.delete();
        }
        return toRemove.size();
    }
}
