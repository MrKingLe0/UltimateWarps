package com.ultimatewarps.playerwarps.integration;

import com.ultimatewarps.UltimateWarps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class ClaimGuard {

    public static boolean isGriefPreventionAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")
                && classExists("me.ryanhamshire.GriefPrevention.GriefPrevention");
    }

    public static boolean isWorldGuardAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard")
                && classExists("com.sk89q.worldguard.WorldGuard");
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isInsideOwnClaim(Player player, Location location) {
        if (!isGriefPreventionAvailable()) {
            UltimateWarps.getInstance().getLogger().warning(
                    "require-own-claim is enabled but GriefPrevention is not available (failing closed).");
            return false;
        }
        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object gpInstance = gpClass.getField("instance").get(null);
            Object dataStore = gpClass.getField("dataStore").get(gpInstance);
            Class<?> dataStoreClass = dataStore.getClass();
            Method getClaimAt = dataStoreClass.getMethod("getClaimAt", Location.class, boolean.class,
                    Class.forName("me.ryanhamshire.GriefPrevention.Claim"));
            Object claim = getClaimAt.invoke(dataStore, location, false, null);
            if (claim == null) return false;

            Object ownerId;
            try {
                ownerId = claim.getClass().getMethod("getOwnerID").invoke(claim);
            } catch (NoSuchMethodException nsme) {
                ownerId = claim.getClass().getField("ownerID").get(claim);
            }
            return ownerId != null && ownerId.equals(player.getUniqueId());
        } catch (Throwable t) {
            UltimateWarps.getInstance().getLogger().warning(
                    "GriefPrevention claim check failed (failing closed): " + t);
            return false;
        }
    }

    public static boolean isOutsideAnyRegion(Location location) {
        if (!isWorldGuardAvailable()) {
            UltimateWarps.getInstance().getLogger().warning(
                    "require-outside-worldguard-region is enabled but WorldGuard is not available (failing closed).");
            return false;
        }
        if (location == null || location.getWorld() == null) return false;
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(wgInstance);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Object regionManager;
            try {
                Method getByBukkitWorld = regionContainer.getClass().getMethod("get", org.bukkit.World.class);
                regionManager = getByBukkitWorld.invoke(regionContainer, location.getWorld());
            } catch (NoSuchMethodException nsme) {
                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class)
                        .invoke(null, location.getWorld());
                Method getByWeWorld = regionContainer.getClass().getMethod("get",
                        Class.forName("com.sk89q.worldedit.world.World"));
                regionManager = getByWeWorld.invoke(regionContainer, adaptedWorld);
            }
            if (regionManager == null) return true;

            Object blockVector = blockVectorFrom(location);
            Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Method getApplicableRegions = regionManager.getClass().getMethod("getApplicableRegions", blockVector3Class);
            Object regionSet = getApplicableRegions.invoke(regionManager, blockVector);
            int size = (int) regionSet.getClass().getMethod("size").invoke(regionSet);
            return size == 0;
        } catch (Throwable t) {
            UltimateWarps.getInstance().getLogger().warning(
                    "WorldGuard region check failed (failing closed): " + t);
            return false;
        }
    }

    private static Object blockVectorFrom(Location location) throws Exception {
        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        try {
            Method asBlockVector = bukkitAdapterClass.getMethod("asBlockVector", Location.class);
            return asBlockVector.invoke(null, location);
        } catch (NoSuchMethodException nsme) {
            Method adapt = bukkitAdapterClass.getMethod("adapt", Location.class);
            Object weVector = adapt.invoke(null, location);
            return weVector.getClass().getMethod("toBlockPoint").invoke(weVector);
        }
    }

    public static String checkLocation(Player player, Location location, boolean requireOwnClaim, boolean requireOutsideRegion) {
        if (requireOwnClaim && !isInsideOwnClaim(player, location)) {
            return "not-in-own-claim";
        }
        if (requireOutsideRegion && !isOutsideAnyRegion(location)) {
            return "inside-region";
        }
        return null;
    }
}
