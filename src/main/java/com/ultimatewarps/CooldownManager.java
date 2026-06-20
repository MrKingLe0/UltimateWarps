package com.ultimatewarps;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public void setCooldown(Player player, String key, int seconds) {
        long expiry = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(key, expiry);
    }

    public boolean hasCooldown(Player player, String key) {
        if (player.hasPermission("ultimatewarps.bypass.cooldown")) return false;
        
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;
        
        Long expiry = playerCooldowns.get(key);
        if (expiry == null) return false;
        
        return System.currentTimeMillis() < expiry;
    }

    public int getRemaining(Player player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        
        Long expiry = playerCooldowns.get(key);
        if (expiry == null) return 0;
        
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }
    
    // Improvement: removed several unused methods that were dead weight or active
    // footguns:
    //  - applyCooldown(player, key) silently applied a hardcoded 30-second cooldown
    //    regardless of the warp/spawn's actual configured cooldown - anything that
    //    called it without realizing this would set the wrong cooldown with no warning.
    //  - getEffectiveDelay/getEffectiveCooldown/getRemainingCooldown duplicated logic
    //    that SpawnCommand/WarpCommand already do correctly inline (rank multiplier
    //    applied at the call site), and were never actually called from anywhere.
    public void applyCooldown(Player player, String key, int seconds) {
        setCooldown(player, key, seconds);
    }

    public void removeCooldown(Player player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns != null) {
            playerCooldowns.remove(key);
        }
    }

    public void clearCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /**
     * Improvement: cooldown entries are just expiry timestamps, so once they pass they
     * become inert but still sit in memory forever - on a long-running server with many
     * unique players this slowly leaks. Deliberately NOT wiping a player's cooldowns on
     * quit, since that would let someone dodge an active cooldown just by disconnecting
     * and rejoining. Instead this periodically sweeps out only entries that have already
     * expired, which is safe to do at any time. Call this on a repeating timer (e.g. once
     * every few minutes) from onEnable().
     */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> {
            Map<String, Long> playerCooldowns = entry.getValue();
            playerCooldowns.values().removeIf(expiry -> expiry < now);
            return playerCooldowns.isEmpty();
        });
    }
}