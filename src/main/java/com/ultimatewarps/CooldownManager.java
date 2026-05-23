package com.ultimatewarps;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> lastUsage = new ConcurrentHashMap<>();

    public long getLastUsage(UUID playerId, String key) {
        Map<String, Long> map = lastUsage.get(playerId);
        if (map != null) {
            Long time = map.get(key);
            return time != null ? time : 0;
        }
        return 0;
    }

    public void setLastUsage(UUID playerId, String key, long time) {
        lastUsage.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, time);
    }

    public int getRemainingCooldown(Player player, String key, int cooldownSeconds) {
        if (player.hasPermission("ultimatewarps.bypass.cooldown")) return 0;
        long last = getLastUsage(player.getUniqueId(), key);
        long now = System.currentTimeMillis() / 1000;
        long diff = now - last;
        return (int) Math.max(0, cooldownSeconds - diff);
    }

    public void applyCooldown(Player player, String key) {
        setLastUsage(player.getUniqueId(), key, System.currentTimeMillis() / 1000);
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
}