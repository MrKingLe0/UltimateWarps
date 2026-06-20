package com.ultimatewarps;

import com.ultimatewarps.Warp;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WarpManager {

    private final UltimateWarps plugin;
    private final Map<String, Warp> warps = new ConcurrentHashMap<>();
    private File warpsFolder;

    public WarpManager(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    public File getWarpsFolder() {
        if (warpsFolder == null) {
            warpsFolder = new File(plugin.getDataFolder(), "warps");
            if (!warpsFolder.exists()) warpsFolder.mkdirs();
        }
        return warpsFolder;
    }

    public void loadWarps() {
        warps.clear();
        File folder = getWarpsFolder();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            Warp warp = Warp.load(file);
            if (warp != null) {
                warps.put(warp.getName().toLowerCase(), warp);
            }
        }
    }

    public void saveAllWarps() {
        for (Warp warp : warps.values()) {
            warp.save();
        }
    }

    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public Collection<Warp> getAllWarps() {
        return Collections.unmodifiableCollection(warps.values());
    }

    /**
     * Improvement: warp names used to flow straight from /setwarp and the admin GUI into
     * a filename with zero validation. A name containing '/', '\', or '..' could escape
     * the warps folder entirely (path traversal), and other special characters could
     * produce an invalid or confusing filename. This is the single choke point both
     * warp-creation paths go through, so validating here protects all of them at once.
     */
    private static final java.util.regex.Pattern VALID_WARP_NAME = java.util.regex.Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    public boolean isValidWarpName(String name) {
        return name != null && VALID_WARP_NAME.matcher(name).matches();
    }

    public void addWarp(Warp warp) {
        if (!isValidWarpName(warp.getName())) {
            throw new IllegalArgumentException(
                "Invalid warp name: '" + warp.getName() + "'. " +
                "Warp names may only contain letters, numbers, underscores and hyphens (max 32 characters)."
            );
        }
        warp.setFile(new File(getWarpsFolder(), warp.getName() + ".yml"));
        warp.save();
        warps.put(warp.getName().toLowerCase(), warp);
    }

    public void removeWarp(String name) {
        Warp warp = warps.remove(name.toLowerCase());
        if (warp != null) {
            warp.delete();
        }
    }

    public List<Warp> getAccessibleWarps(Player player) {
        return warps.values().stream()
            .filter(w -> w.isEnabled() && w.getLocation() != null)
            .filter(w -> w.getPermission() == null || w.getPermission().isEmpty() || player.hasPermission(w.getPermission()) || player.hasPermission("ultimatewarps.warp.*"))
            .collect(Collectors.toList());
    }
}