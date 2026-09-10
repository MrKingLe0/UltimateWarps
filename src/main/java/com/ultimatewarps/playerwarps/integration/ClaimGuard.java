package com.ultimatewarps.playerwarps.integration;

import com.ultimatewarps.UltimateWarps;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class ClaimGuard {

    private static Boolean griefPreventionAvailable = null;
    private static Boolean worldGuardAvailable = null;

    public static boolean isGriefPreventionAvailable() {
        if (griefPreventionAvailable == null) {
            griefPreventionAvailable = UltimateWarps.getInstance().getServer().getPluginManager().getPlugin("GriefPrevention") != null
                    && classExists("me.ryanhamshire.GriefPrevention.GriefPrevention");
        }
        return griefPreventionAvailable;
    }

    public static boolean isWorldGuardAvailable() {
        if (worldGuardAvailable == null) {
            worldGuardAvailable = UltimateWarps.getInstance().getServer().getPluginManager().getPlugin("WorldGuard") != null
                    && classExists("com.sk89q.worldguard.WorldGuard");
        }
        return worldGuardAvailable;
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
        if (!isGriefPreventionAvailable()) return true;
        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object gpInstance = gpClass.getField("instance").get(null);
            Object dataStore = gpClass.getField("dataStore").get(gpInstance);
            Class<?> dataStoreClass = dataStore.getClass();
            Method getClaimAt = dataStoreClass.getMethod("getClaimAt", Location.class, boolean.class,
                    Class.forName("me.ryanhamshire.GriefPrevention.Claim"));
            Object claim = getClaimAt.invoke(dataStore, location, false, null);
            if (claim == null) return false;

            Class<?> claimClass = claim.getClass();
            Object ownerId = claimClass.getField("ownerID").get(claim);
            return ownerId != null && ownerId.equals(player.getUniqueId());
        } catch (Throwable t) {
            UltimateWarps.getInstance().getLogger().warning(
                    "GriefPrevention claim check failed (treating as passed): " + t);
            return true;
        }
    }

    public static boolean isOutsideAnyRegion(Location location) {
        if (!isWorldGuardAvailable()) return true;
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
            Method getApplicableRegions = regionManager.getClass().getMethod("getApplicableRegions", blockVector.getClass());
            Object regionSet = getApplicableRegions.invoke(regionManager, blockVector);
            int size = (int) regionSet.getClass().getMethod("size").invoke(regionSet);
            return size == 0;
        } catch (Throwable t) {
            UltimateWarps.getInstance().getLogger().warning(
                    "WorldGuard region check failed (treating as passed): " + t);
            return true;
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
