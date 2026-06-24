package com.ultimatewarps.playerwarps.integration;

import com.ultimatewarps.UltimateWarps;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Soft-dependency bridge to GriefPrevention and WorldGuard, implemented with pure
 * reflection rather than compile-time dependencies. This plugin does not ship either
 * plugin's jar or declare them as build dependencies at all - every lookup happens by
 * class/method name at runtime, inside a server that may or may not have them installed.
 *
 * This trades a bit of verbosity for a hard guarantee: a missing plugin, or even a
 * future API change in GriefPrevention/WorldGuard, can only ever disable this one
 * feature gracefully (logged once, then treated as "check passed") - it can never break
 * compilation or prevent UltimateWarps itself from loading.
 */
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

    /**
     * @return true if the location is inside a GriefPrevention claim owned by the given
     *         player. If GriefPrevention isn't installed, or the reflective lookup fails
     *         for any reason, returns true (the check is treated as "passed" rather than
     *         blocking warp creation on a check that can't actually run) - whether this
     *         check applies at all is gated by playerwarps-config.yml on the caller's side.
     */
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
            if (claim == null) return false; // not inside any claim at all

            Class<?> claimClass = claim.getClass();
            Object ownerId = claimClass.getField("ownerID").get(claim);
            return ownerId != null && ownerId.equals(player.getUniqueId());
        } catch (Throwable t) {
            UltimateWarps.getInstance().getLogger().warning(
                    "GriefPrevention claim check failed (treating as passed): " + t);
            return true;
        }
    }

    /**
     * @return true if the location is NOT inside any WorldGuard region. If WorldGuard
     *         isn't installed, or the reflective lookup fails, returns true.
     */
    public static boolean isOutsideAnyRegion(Location location) {
        if (!isWorldGuardAvailable()) return true;
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(wgInstance);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            // RegionContainer#get(World) takes WorldGuard's wrapped World type on newer
            // versions, but the Bukkit-facing convenience method that accepts a raw
            // org.bukkit.World has been kept across 7.x for integrations exactly like
            // this one - using it avoids needing WorldEdit's BukkitAdapter as a dependency.
            Object regionManager;
            try {
                Method getByBukkitWorld = regionContainer.getClass().getMethod("get", org.bukkit.World.class);
                regionManager = getByBukkitWorld.invoke(regionContainer, location.getWorld());
            } catch (NoSuchMethodException nsme) {
                // Fall back to the documented com.sk89q.worldedit.world.World overload.
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

    /**
     * Combined check driven by playerwarps-config.yml. Returns null if the location
     * passes every enabled check, or a reason key (used to look up a player-facing
     * message) if it fails one.
     */
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

