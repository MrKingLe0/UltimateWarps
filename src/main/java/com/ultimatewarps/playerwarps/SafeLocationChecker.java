package com.ultimatewarps.playerwarps;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;

import java.util.EnumSet;
import java.util.Set;

public final class SafeLocationChecker {

    private static final Set<Material> HAZARDOUS_MATERIALS = EnumSet.noneOf(Material.class);

    static {
        for (String name : new String[]{
                "LAVA", "FIRE", "SOUL_FIRE", "MAGMA_BLOCK", "CACTUS",
                "CAMPFIRE", "SOUL_CAMPFIRE", "SWEET_BERRY_BUSH", "WITHER_ROSE",
                "POWDER_SNOW", "POINTED_DRIPSTONE", "END_PORTAL", "NETHER_PORTAL",
                "COBWEB"
        }) {
            try {
                HAZARDOUS_MATERIALS.add(Material.valueOf(name));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private SafeLocationChecker() {}

    /**
     * Checks that the warp location has non-passable, non-hazardous ground below,
     * safe passable space for feet and head, and no immediate lava/falling hazard overhead.
     */
    public static boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        if (by <= world.getMinHeight() || by + 1 >= world.getMaxHeight()) {
            return false;
        }

        Block below = world.getBlockAt(bx, by - 1, bz);
        Block atFeet = world.getBlockAt(bx, by, bz);
        Block atHead = world.getBlockAt(bx, by + 1, bz);

        if (!isSafeSolidGround(below)) {
            return false;
        }
        if (!isSafeBodySpace(atFeet) || !isSafeBodySpace(atHead)) {
            return false;
        }

        if (by + 2 < world.getMaxHeight()) {
            Block aboveHead = world.getBlockAt(bx, by + 2, bz);
            Material aboveMat = aboveHead.getType();
            if (aboveMat == Material.LAVA || aboveMat == Material.ANVIL
                    || aboveMat == Material.CHIPPED_ANVIL || aboveMat == Material.DAMAGED_ANVIL) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSafeSolidGround(Block block) {
        Material mat = block.getType();
        if (mat == null || mat.isAir() || block.isLiquid() || block.isPassable()) {
            return false;
        }
        if (HAZARDOUS_MATERIALS.contains(mat)) {
            return false;
        }
        if (block.getBlockData() instanceof Openable openable && openable.isOpen()) {
            return false;
        }
        if (mat.hasGravity()) {
            Block under = block.getRelative(0, -1, 0);
            if (under.isPassable() || under.isLiquid() || HAZARDOUS_MATERIALS.contains(under.getType())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSafeBodySpace(Block block) {
        Material mat = block.getType();
        if (mat == null) return false;
        if (HAZARDOUS_MATERIALS.contains(mat)) return false;
        if (block.isLiquid()) return false;
        return block.isPassable();
    }
}
