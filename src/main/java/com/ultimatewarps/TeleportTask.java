package com.ultimatewarps;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportTask extends BukkitRunnable {

    private static final Map<UUID, TeleportTask> activeTasks = new HashMap<>();
    
    private final Player player;
    private final Location destination;
    private final int totalSeconds;
    private int secondsLeft;
    private final Location startLocation;
    private BossBar bossBar;
    private final String warpName;
    private boolean cancelled = false;
    private final ConfigManager config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TeleportTask(Player player, Location destination, int delaySeconds, String warpName) {
        this.player = player;
        this.destination = destination;
        this.totalSeconds = delaySeconds;
        this.secondsLeft = delaySeconds;
        this.startLocation = player.getLocation().clone();
        this.warpName = warpName;
        this.config = UltimateWarps.getInstance().getConfigManager();
        
        // Cancel any existing task for this player
        TeleportTask existing = activeTasks.remove(player.getUniqueId());
        if (existing != null && !existing.cancelled) {
            existing.cancel();
        }
        
        activeTasks.put(player.getUniqueId(), this);
        createBossBar();
    }

    private void createBossBar() {
        boolean isSpawn = warpName.equals("Spawn");
        boolean bossBarEnabled = isSpawn ? config.spawnBossBarEnabled() : config.warpBossBarEnabled();
        
        if (bossBarEnabled) {
            Component bossBarText = isSpawn ? 
                config.getSpawnBossBarText(secondsLeft) : 
                config.getWarpBossBarText(warpName, secondsLeft);
            
            Color color = convertColor(isSpawn ? config.spawnBossBarColor() : config.warpBossBarColor());
            Overlay overlay = Overlay.PROGRESS;
            
            this.bossBar = BossBar.bossBar(bossBarText, 1.0f, color, overlay);
            this.bossBar.addViewer(player);
        }
    }

    private void updateBossBar() {
        if (bossBar != null) {
            boolean isSpawn = warpName.equals("Spawn");
            Component bossBarText = isSpawn ? 
                config.getSpawnBossBarText(secondsLeft) : 
                config.getWarpBossBarText(warpName, secondsLeft);
            
            bossBar = bossBar.name(bossBarText);
            bossBar = bossBar.progress((float) secondsLeft / totalSeconds);
        }
    }

    @Override
    public void run() {
        if (cancelled) return;
        
        if (secondsLeft <= 0) {
            complete();
            return;
        }
        
        boolean isSpawn = warpName.equals("Spawn");
        
        // Check movement cancellation
        if (isSpawn ? config.spawnCancelOnMove() : config.warpCancelOnMove()) {
            if (hasMoved()) {
                cancelTask(config.getTeleportCancelledMoveMessage());
                return;
            }
        }

        // Update BossBar
        updateBossBar();

        // Send Title with MiniMessage support
        if (isSpawn && config.spawnTitleEnabled()) {
            Component title = config.getSpawnTitleMessage();
            Component subtitle = config.getSpawnSubtitleMessage(secondsLeft);
            
            player.showTitle(Title.title(title, subtitle, 
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1100), Duration.ofMillis(100))));
            
        } else if (!isSpawn && config.warpTitleEnabled()) {
            Component title = config.getWarpTitleMessage(warpName);
            Component subtitle = config.getWarpSubtitleMessage(secondsLeft);
            
            player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1100), Duration.ofMillis(100))));
        }

        // Particle Effects
        playParticleEffects();

        secondsLeft--;
    }

    private boolean hasMoved() {
        Location now = player.getLocation();
        return startLocation.getBlockX() != now.getBlockX() ||
               startLocation.getBlockZ() != now.getBlockZ();
    }

    private void playParticleEffects() {
        try {
            boolean isSpawn = warpName.equals("Spawn");
            boolean particleEnabled = isSpawn ? config.spawnParticleEnabled() : config.warpParticleEnabled();
            
            if (particleEnabled) {
                Particle particleType = isSpawn ? config.spawnParticleType() : config.warpParticleType();
                int particleCount = isSpawn ? config.spawnParticleCount() : config.warpParticleCount();
                
                if (particleType == Particle.DUST) {
                    org.bukkit.Particle.DustOptions dustOptions;
                    if (isSpawn) {
                        dustOptions = new org.bukkit.Particle.DustOptions(config.spawnDustColor(), config.spawnDustSize());
                    } else {
                        dustOptions = new org.bukkit.Particle.DustOptions(config.warpDustColor(), config.warpDustSize());
                    }
                    player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0), 
                        particleCount, 0.5, 0.5, 0.5, dustOptions);
                } else {
                    player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0), 
                        particleCount, 0.5, 0.5, 0.5, 0.05);
                }
            }
        } catch (Exception e) {
            // Ignore particle errors
        }
    }

    private void complete() {
        if (cancelled) return;
        cancelled = true;
        
        cancel();
        if (bossBar != null) {
            bossBar.removeViewer(player);
        }
        activeTasks.remove(player.getUniqueId());
        
        // Teleport
        player.teleport(destination);
        
        // Final teleport effects
        try {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } catch (Exception e) {
            // Ignore
        }
        
        // Send confirmation message
        Component message = config.getTeleportConfirmedMessage(warpName);
        player.sendMessage(message);
    }

    public void cancelTask(Component reason) {
        if (cancelled) return;
        cancelled = true;
        
        cancel();
        if (bossBar != null) {
            bossBar.removeViewer(player);
        }
        activeTasks.remove(player.getUniqueId());
        
        if (reason != null) {
            player.sendMessage(reason);
        }
    }
    
    @Override
    public void cancel() {
        super.cancel();
        if (bossBar != null) {
            bossBar.removeViewer(player);
        }
        activeTasks.remove(player.getUniqueId());
    }
    
    public static void cancelAllTasks(Player player) {
        TeleportTask task = activeTasks.remove(player.getUniqueId());
        if (task != null && !task.cancelled) {
            task.cancel();
        }
    }
    
    private Color convertColor(org.bukkit.boss.BarColor color) {
        switch (color) {
            case PINK: return Color.PINK;
            case BLUE: return Color.BLUE;
            case RED: return Color.RED;
            case GREEN: return Color.GREEN;
            case YELLOW: return Color.YELLOW;
            case PURPLE: return Color.PURPLE;
            case WHITE: return Color.WHITE;
            default: return Color.BLUE;
        }
    }
}