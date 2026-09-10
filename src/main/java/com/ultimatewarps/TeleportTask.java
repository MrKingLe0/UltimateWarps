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

    private final String displayLabel;
    private boolean cancelled = false;
    private final ConfigManager config;

    private final Runnable onSuccess;

    private final Warp sourceWarp;

    public TeleportTask(Player player, Location destination, int delaySeconds, String warpName) {
        this(player, destination, delaySeconds, warpName, warpName);
    }

    public TeleportTask(Player player, Location destination, int delaySeconds, String warpName, String displayLabel) {
        this(player, destination, delaySeconds, warpName, displayLabel, null);
    }

    public TeleportTask(Player player, Location destination, int delaySeconds, String warpName, String displayLabel, Runnable onSuccess) {
        this(player, destination, delaySeconds, warpName, displayLabel, onSuccess, null);
    }

    public TeleportTask(Player player, Location destination, int delaySeconds, String warpName, String displayLabel, Runnable onSuccess, Warp sourceWarp) {
        this.player = player;
        this.destination = destination;
        this.totalSeconds = delaySeconds;
        this.secondsLeft = delaySeconds;
        this.startLocation = player.getLocation().clone();
        this.warpName = warpName;
        this.displayLabel = displayLabel != null ? displayLabel : warpName;
        this.config = UltimateWarps.getInstance().getConfigManager();
        this.onSuccess = onSuccess;
        this.sourceWarp = sourceWarp;

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

        if (isSpawn ? config.spawnCancelOnMove() : config.warpCancelOnMove()) {
            if (hasMoved()) {
                cancelTask(config.getTeleportCancelledMoveMessage());
                return;
            }
        }

        updateBossBar();

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

        playParticleEffects();

        if (isSpawn) {
            if (config.spawnSoundEnabled()) {
                player.playSound(player.getLocation(), config.spawnSoundType(), 0.5f, 1.0f);
            }
        } else if (sourceWarp != null && sourceWarp.isCustomEffects()) {
            if (sourceWarp.isEffectSoundEnabled()) {
                Sound tick = ConfigManager.parseSound(sourceWarp.getEffectSoundType(), config.warpSoundType());
                player.playSound(player.getLocation(), tick, 0.5f, 1.0f);
            }
        } else if (config.warpSoundEnabled()) {
            player.playSound(player.getLocation(), config.warpSoundType(), 0.5f, 1.0f);
        }

        secondsLeft--;
    }

    private boolean hasMoved() {
        Location now = player.getLocation();
        return startLocation.getBlockX() != now.getBlockX() ||
               startLocation.getBlockZ() != now.getBlockZ();
    }

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

            if (!isSpawn && sourceWarp != null && sourceWarp.isCustomEffects()) {
                if (sourceWarp.isEffectParticleEnabled()) {
                    Particle particleType;
                    try {
                        particleType = Particle.valueOf(sourceWarp.getEffectParticleType().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        particleType = Particle.DUST;
                    }
                    int particleCount = sourceWarp.getEffectParticleCount();
                    if (particleType == Particle.DUST) {
                        org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(
                                ConfigManager.parseColor(sourceWarp.getEffectDustColor()), sourceWarp.getEffectDustSize());
                        player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0),
                            particleCount, 0.5, 0.5, 0.5, dustOptions);
                    } else {
                        player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0),
                            particleCount, 0.5, 0.5, 0.5, 0.05);
                    }
                }
                return;
            }

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

        player.teleport(destination);

        UltimateWarps.getInstance().getEffectManager().playTeleportEffect(player, warpName, sourceWarp);

        Component message = config.getTeleportConfirmedMessage(displayLabel);
        player.sendMessage(message);

        if (onSuccess != null) {
            onSuccess.run();
        }
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
