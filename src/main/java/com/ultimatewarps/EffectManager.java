package com.ultimatewarps;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectManager {

    private final UltimateWarps plugin;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public EffectManager(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    public void playParticle(Player player, Particle particle, int count) {
        playParticle(player, particle, count, null);
    }

    public void playParticle(Player player, Particle particle, int count, Object data) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        try {
            if (particle == Particle.DUST && data == null) {
                // Default dust options if none provided
                data = new DustOptions(Color.fromRGB(255, 85, 85), 1.0f);
            }
            
            if (data != null && (particle == Particle.DUST || particle == Particle.DUST_COLOR_TRANSITION)) {
                player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, data);
            } else {
                player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.05);
            }
        } catch (Exception e) {
            // Fallback to a safe particle
            player.getWorld().spawnParticle(Particle.PORTAL, loc, count, 0.5, 0.5, 0.5, 0.05);
        }
    }

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
    
    public void playTeleportEffect(Player player, String type) {
        Location loc = player.getLocation();
        ConfigManager config = plugin.getConfigManager();
        
        boolean isSpawn = type.equalsIgnoreCase("Spawn");
        
        // Play sound
        boolean soundEnabled = isSpawn ? config.spawnSoundEnabled() : config.warpSoundEnabled();
        if (soundEnabled) {
            Sound soundType = isSpawn ? config.spawnSoundType() : config.warpSoundType();
            float volume = isSpawn ? config.spawnSoundVolume() : config.warpSoundVolume();
            float pitch = isSpawn ? config.spawnSoundPitch() : config.warpSoundPitch();
            playSound(player, soundType, volume, pitch);
        }
        
        // Play particles
        boolean particleEnabled = isSpawn ? config.spawnParticleEnabled() : config.warpParticleEnabled();
        if (particleEnabled) {
            Particle particle = isSpawn ? config.spawnParticleType() : config.warpParticleType();
            int count = isSpawn ? config.spawnParticleCount() : config.warpParticleCount();
            
            if (particle == Particle.DUST) {
                Color color = isSpawn ? config.spawnDustColor() : config.warpDustColor();
                float size = isSpawn ? config.spawnDustSize() : config.warpDustSize();
                playParticle(player, particle, count, new DustOptions(color, size));
            } else {
                playParticle(player, particle, count);
            }
        }
    }
    
    public void startCountdown(Player player, String target, int seconds, boolean isSpawn, Runnable onFinish) {
        UUID uuid = player.getUniqueId();
        
        // Cancel any existing countdown for this player
        cancelCountdown(player);
        
        ConfigManager config = plugin.getConfigManager();
        
        // Create boss bar
        BossBar bossBar = org.bukkit.Bukkit.createBossBar(
            isSpawn ? "§5Teleporting to Spawn..." : "§bTeleporting to " + target + "...",
            isSpawn ? BarColor.PURPLE : BarColor.BLUE,
            BarStyle.SEGMENTED_10
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        activeBossBars.put(uuid, bossBar);
        
        Location startLoc = player.getLocation().clone();
        
        BukkitRunnable task = new BukkitRunnable() {
            int time = seconds;
            boolean cancelled = false;
            
            @Override
            public void run() {
                if (!player.isOnline() || time < 0) {
                    cancelCountdown(player);
                    return;
                }
                
                if (time == 0) {
                    cancelCountdown(player);
                    onFinish.run();
                    cancel();
                    return;
                }
                
                // Cancel if moved
                if (config.globalCancelOnMove() && hasMoved()) {
                    if (!cancelled) {
                        player.sendMessage(config.getMessage("teleport-cancelled-move"));
                        cancelled = true;
                    }
                    cancelCountdown(player);
                    cancel();
                    return;
                }
                
                // Update boss bar
                bossBar.setTitle(isSpawn ? 
                    "§5Teleporting to Spawn in §f" + time + "§5s..." : 
                    "§bTeleporting to " + target + " in §f" + time + "§bs...");
                bossBar.setProgress((double) time / seconds);
                
                // Play particle effect
                // Bug fix: the warp branch was only guarded by config.warpParticleEnabled(),
                // with no check that isSpawn was actually false. That meant a spawn teleport
                // with spawn particles disabled would fall through and incorrectly play warp
                // particles instead of none.
                if (isSpawn && config.spawnParticleEnabled()) {
                    Particle particle = config.spawnParticleType();
                    if (particle == Particle.DUST) {
                        playParticle(player, particle, 5, new DustOptions(config.spawnDustColor(), config.spawnDustSize()));
                    } else {
                        playParticle(player, particle, 5);
                    }
                } else if (!isSpawn && config.warpParticleEnabled()) {
                    Particle particle = config.warpParticleType();
                    if (particle == Particle.DUST) {
                        playParticle(player, particle, 5, new DustOptions(config.warpDustColor(), config.warpDustSize()));
                    } else {
                        playParticle(player, particle, 5);
                    }
                }
                
                // Play tick sound
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                
                time--;
            }
            
            private boolean hasMoved() {
                Location now = player.getLocation();
                return startLoc.getBlockX() != now.getBlockX() || 
                       startLoc.getBlockZ() != now.getBlockZ();
            }
        };
        
        task.runTaskTimer(plugin, 0L, 20L);
        activeTasks.put(uuid, task);
    }
    
    public void cancelCountdown(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (activeBossBars.containsKey(uuid)) {
            activeBossBars.get(uuid).removeAll();
            activeBossBars.remove(uuid);
        }
        
        if (activeTasks.containsKey(uuid)) {
            activeTasks.get(uuid).cancel();
            activeTasks.remove(uuid);
        }
    }
}