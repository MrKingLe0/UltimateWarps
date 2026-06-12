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

    public void addWarp(Warp warp) {
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