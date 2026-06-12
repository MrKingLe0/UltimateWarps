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
    
    public int getRemainingCooldown(Player player, String key, int cooldownSeconds) {
        return getRemaining(player, key);
    }
    
    public void applyCooldown(Player player, String key) {
        // This needs the actual cooldown value - you should pass it
        setCooldown(player, key, 30); // Default fallback
    }
    
    public void applyCooldown(Player player, String key, int seconds) {
        setCooldown(player, key, seconds);
    }
    
    public int getEffectiveDelay(Player player, int baseDelay) {
        if (player.hasPermission("ultimatewarps.bypass.delay")) return 0;
        double multiplier = UltimateWarps.getInstance().getConfigManager().getEffectiveDelayMultiplier(player);
        return (int) Math.round(baseDelay * multiplier);
    }
    
    public int getEffectiveCooldown(Player player, int baseCooldown) {
        if (player.hasPermission("ultimatewarps.bypass.cooldown")) return 0;
        double multiplier = UltimateWarps.getInstance().getConfigManager().getEffectiveCooldownMultiplier(player);
        return (int) Math.round(baseCooldown * multiplier);
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
}