package com.ultimatewarps.playerwarps;

import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayerWarpTeleportTask extends BukkitRunnable {

    private static Map<UUID, PlayerWarpTeleportTask> activeTasks() {
        return UltimateWarps.getInstance().getActivePlayerWarpTeleports();
    }

    private final Player player;
    private final Location destination;
    private final int totalSeconds;
    private int secondsLeft;
    private final Location startLocation;
    private BossBar bossBar;
    private final String displayLabel;
    private boolean cancelled = false;
    private final PlayerWarpsConfigManager config;

    private final Runnable onSuccess;
    private final Runnable customArrivalHandler;

    public PlayerWarpTeleportTask(Player player, Location destination, int delaySeconds, String displayLabel) {
        this(player, destination, delaySeconds, displayLabel, null, null);
    }

    public PlayerWarpTeleportTask(Player player, Location destination, int delaySeconds, String displayLabel, Runnable onSuccess) {
        this(player, destination, delaySeconds, displayLabel, onSuccess, null);
    }

    public PlayerWarpTeleportTask(Player player, Location destination, int delaySeconds, String displayLabel, Runnable onSuccess, Runnable customArrivalHandler) {
        this.player = player;
        this.destination = destination != null ? destination.clone() : null;
        this.totalSeconds = Math.max(1, delaySeconds);
        this.secondsLeft = this.totalSeconds;
        this.startLocation = player.getLocation().clone();
        this.displayLabel = displayLabel;
        this.config = UltimateWarps.getInstance().getPlayerWarpsConfigManager();
        this.onSuccess = onSuccess;
        this.customArrivalHandler = customArrivalHandler;

        PlayerWarpTeleportTask existing = activeTasks().remove(player.getUniqueId());
        if (existing != null && !existing.cancelled) {
            existing.cancel();
        }
        activeTasks().put(player.getUniqueId(), this);
        createBossBar();
    }

    private void createBossBar() {
        if (config.bossBarEnabled()) {
            Component text = config.getBossBarText(displayLabel, secondsLeft);
            BossBar.Color color = convertColor(config.bossBarColor());
            BossBar.Overlay overlay = convertStyle(config.bossBarStyle());
            this.bossBar = BossBar.bossBar(text, 1.0f, color, overlay);
            this.bossBar.addViewer(player);
        }
    }

    private void updateBossBar() {
        if (bossBar != null) {
            bossBar = bossBar.name(config.getBossBarText(displayLabel, secondsLeft));
            float progress = Math.max(0.0f, Math.min(1.0f, (float) secondsLeft / totalSeconds));
            bossBar = bossBar.progress(progress);
        }
    }

    @Override
    public void run() {
        if (cancelled) return;
        if (!player.isOnline() || player.isDead()) {
            cancelTask(null);
            return;
        }

        if (secondsLeft <= 0) {
            complete();
            return;
        }

        if (config.cancelOnMove() && hasMoved(player.getLocation())) {
            cancelTask(config.getRawMessage("teleported-cancelled-move"));
            return;
        }

        updateBossBar();

        if (config.titleEnabled()) {
            Component title = config.getTitleMessage(displayLabel);
            Component subtitle = config.getSubtitleMessage(secondsLeft);
            player.showTitle(Title.title(title, subtitle,
                    Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1100), Duration.ofMillis(100))));
        }

        if (config.particleEnabled()) {
            try {
                Particle particleType = config.particleType();
                int count = config.particleCount();
                if (particleType == Particle.DUST) {
                    player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0),
                            count, 0.5, 0.5, 0.5, new Particle.DustOptions(config.dustColor(), config.dustSize()));
                } else {
                    player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0),
                            count, 0.5, 0.5, 0.5, 0.05);
                }
            } catch (Exception ignored) {
            }
        }

        if (config.soundEnabled()) {
            player.playSound(player.getLocation(), config.soundType(), config.soundVolume(), config.soundPitch());
        }

        secondsLeft--;
    }

    private boolean hasMoved(Location target) {
        Location now = target != null ? target : player.getLocation();
        return !Objects.equals(startLocation.getWorld(), now.getWorld()) ||
               startLocation.getBlockX() != now.getBlockX() ||
               startLocation.getBlockY() != now.getBlockY() ||
               startLocation.getBlockZ() != now.getBlockZ();
    }

    public void cancelIfMoved() {
        cancelIfMoved(player.getLocation());
    }

    public void cancelIfMoved(Location to) {
        if (cancelled) return;
        if (config.cancelOnMove() && hasMoved(to)) {
            cancelTask(config.getRawMessage("teleported-cancelled-move"));
        }
    }

    private void complete() {
        if (cancelled) return;
        cancel();

        if (!player.isOnline() || player.isDead()) return;

        if (customArrivalHandler != null) {
            customArrivalHandler.run();
            return;
        }

        if (destination == null || destination.getWorld() == null
                || Bukkit.getWorld(destination.getWorld().getUID()) == null) {
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return;
        }

        if (config.safeWarpCheckOnTeleport() && !SafeLocationChecker.isSafeLocation(destination)) {
            player.sendMessage(config.getMessage("unsafe-warp-teleport", "name", displayLabel));
            return;
        }

        if (!player.teleport(destination)) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage(config.getMessage("teleport-confirmed", "name", displayLabel));

        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    public void cancelTask(Component reason) {
        if (cancelled) return;
        cancel();
        if (reason != null && player.isOnline()) player.sendMessage(reason);
    }

    @Override
    public void cancel() {
        this.cancelled = true;
        try {
            super.cancel();
        } catch (IllegalStateException ignored) {}
        if (bossBar != null) bossBar.removeViewer(player);
        activeTasks().remove(player.getUniqueId(), this);
    }

    public static void cancelAllTasks(Player player) {
        if (player == null) return;
        PlayerWarpTeleportTask task = activeTasks().remove(player.getUniqueId());
        if (task != null && !task.cancelled) {
            task.cancel();
        }
    }

    private BossBar.Color convertColor(BarColor color) {
        switch (color) {
            case PINK: return BossBar.Color.PINK;
            case BLUE: return BossBar.Color.BLUE;
            case RED: return BossBar.Color.RED;
            case GREEN: return BossBar.Color.GREEN;
            case YELLOW: return BossBar.Color.YELLOW;
            case PURPLE: return BossBar.Color.PURPLE;
            case WHITE: return BossBar.Color.WHITE;
            default: return BossBar.Color.BLUE;
        }
    }

    private BossBar.Overlay convertStyle(BarStyle style) {
        switch (style) {
            case SEGMENTED_6: return BossBar.Overlay.NOTCHED_6;
            case SEGMENTED_10: return BossBar.Overlay.NOTCHED_10;
            case SEGMENTED_12: return BossBar.Overlay.NOTCHED_12;
            case SEGMENTED_20: return BossBar.Overlay.NOTCHED_20;
            case SOLID:
            default: return BossBar.Overlay.PROGRESS;
        }
    }
}
