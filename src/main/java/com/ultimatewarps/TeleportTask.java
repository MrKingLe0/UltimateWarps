package com.ultimatewarps;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class TeleportTask extends BukkitRunnable {

    // Bug fix: this used to keep its own private static map of active tasks, completely
    // separate from UltimateWarps#getActiveTeleports(). That meant the plugin's shared
    // map was always empty, so onDisable() never actually cancelled in-progress teleports
    // (it iterated an empty map) and MoveListener could never look anything up either.
    // Now there is a single shared map that both this class and the plugin use.
    private static Map<UUID, TeleportTask> activeTasks() {
        return UltimateWarps.getInstance().getActiveTeleports();
    }

    private final Player player;
    private final Location destination;
    private final int totalSeconds;
    private int secondsLeft;
    private final Location startLocation;
    private BossBar bossBar;
    private final String warpName;
    // Bug fix: warpName is also used as a stable identity key ("Spawn" vs. everything
    // else) to decide which config branch (spawn.* vs warp.*) applies, so it has to stay
    // the internal name. But every player-visible string (boss bar, title, chat
    // confirmation) was built from that same internal name too, completely ignoring
    // warp.getDisplayName() - the custom name set via /warpsadmin. That value was saved
    // and reloaded correctly, it just never reached anything the player actually sees.
    // displayLabel is what gets shown; warpName is only ever used for identity checks
    // and as the EffectManager.playTeleportEffect "type" argument.
    private final String displayLabel;
    private boolean cancelled = false;
    private final ConfigManager config;

    public TeleportTask(Player player, Location destination, int delaySeconds, String warpName) {
        this(player, destination, delaySeconds, warpName, warpName);
    }

    public TeleportTask(Player player, Location destination, int delaySeconds, String warpName, String displayLabel) {
        this.player = player;
        this.destination = destination;
        this.totalSeconds = delaySeconds;
        this.secondsLeft = delaySeconds;
        this.startLocation = player.getLocation().clone();
        this.warpName = warpName;
        this.displayLabel = displayLabel != null ? displayLabel : warpName;
        this.config = UltimateWarps.getInstance().getConfigManager();
        
        // Cancel any existing task for this player
        TeleportTask existing = activeTasks().remove(player.getUniqueId());
        if (existing != null && !existing.cancelled) {
            existing.cancel();
        }
        
        activeTasks().put(player.getUniqueId(), this);
        createBossBar();
    }

    private void createBossBar() {
        boolean isSpawn = warpName.equals("Spawn");
        boolean bossBarEnabled = isSpawn ? config.spawnBossBarEnabled() : config.warpBossBarEnabled();
        
        if (bossBarEnabled) {
            Component bossBarText = isSpawn ? 
                config.getSpawnBossBarText(secondsLeft) : 
                config.getWarpBossBarText(displayLabel, secondsLeft);
            
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
                config.getWarpBossBarText(displayLabel, secondsLeft);
            
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
            Component title = config.getWarpTitleMessage(displayLabel);
            Component subtitle = config.getWarpSubtitleMessage(secondsLeft);
            
            player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1100), Duration.ofMillis(100))));
        }

        // Particle Effects
        playParticleEffects();

        // Play tick sound during countdown (configurable)
        if (isSpawn && config.spawnSoundEnabled()) {
            player.playSound(player.getLocation(), config.spawnSoundType(), 0.5f, 1.0f);
        } else if (!isSpawn && config.warpSoundEnabled()) {
            player.playSound(player.getLocation(), config.warpSoundType(), 0.5f, 1.0f);
        }

        secondsLeft--;
    }

    private boolean hasMoved() {
        Location now = player.getLocation();
        return startLocation.getBlockX() != now.getBlockX() ||
               startLocation.getBlockZ() != now.getBlockZ();
    }

    /**
     * Called by MoveListener on every PlayerMoveEvent so the teleport is cancelled the
     * instant the player moves, instead of waiting for the next per-second tick in run().
     * Respects the same cancel-on-move config setting as the tick-based check.
     */
    public void cancelIfMoved() {
        if (cancelled) return;
        boolean isSpawn = warpName.equals("Spawn");
        boolean cancelOnMove = isSpawn ? config.spawnCancelOnMove() : config.warpCancelOnMove();
        if (cancelOnMove && hasMoved()) {
            cancelTask(config.getTeleportCancelledMoveMessage());
        }
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
        activeTasks().remove(player.getUniqueId());
        
        // Teleport
        player.teleport(destination);
        
        // Final teleport effects - use EffectManager for configurable sound
        // (warpName here is the identity key, not display text - EffectManager only
        // uses it to decide spawn.* vs warp.* config, never shows it to the player)
        UltimateWarps.getInstance().getEffectManager().playTeleportEffect(player, warpName);
        
        // Send confirmation message
        Component message = config.getTeleportConfirmedMessage(displayLabel);
        player.sendMessage(message);
    }

    public void cancelTask(Component reason) {
        if (cancelled) return;
        cancelled = true;
        
        cancel();
        if (bossBar != null) {
            bossBar.removeViewer(player);
        }
        activeTasks().remove(player.getUniqueId());
        
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
        activeTasks().remove(player.getUniqueId());
    }
    
    public static void cancelAllTasks(Player player) {
        TeleportTask task = activeTasks().remove(player.getUniqueId());
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