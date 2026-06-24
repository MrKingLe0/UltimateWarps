package com.ultimatewarps.playerwarps;

import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Countdown teleport task for player warps. Deliberately separate from the admin
 * TeleportTask class - that class is wired specifically to ConfigManager's spawn and warp
 * config branches, and player warps have their own independent settings in
 * playerwarps-config.yml, so there's only ever one settings source here (no
 * isSpawn-style branching needed).
 *
 * Shares the same active-task bookkeeping idea as the admin TeleportTask (registered in
 * a map on the plugin instance so it can be cancelled on disable/move), but keeps its own
 * separate map so a player warp countdown and an admin warp/spawn countdown can never be
 * confused with each other, and so cancelling one never accidentally cancels the other.
 */
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

    public PlayerWarpTeleportTask(Player player, Location destination, int delaySeconds, String displayLabel) {
        this.player = player;
        this.destination = destination;
        this.totalSeconds = Math.max(1, delaySeconds);
        this.secondsLeft = this.totalSeconds;
        this.startLocation = player.getLocation().clone();
        this.displayLabel = displayLabel;
        this.config = UltimateWarps.getInstance().getPlayerWarpsConfigManager();

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

        if (config.cancelOnMove() && hasMoved()) {
            cancelTask(Component.text("Teleport cancelled - you moved!", NamedTextColor.RED));
            return;
        }

        updateBossBar();

        // Bug fix: title/subtitle used to be hardcoded plain Components ("Teleporting...",
        // "%ds remaining") with no MiniMessage support and no way to configure them -
        // unlike the admin warp title. Now reads from teleport.title.message/subtitle in
        // playerwarps-config.yml, same as the admin TeleportTask does for warp.title.*.
        if (config.titleEnabled()) {
            Component title = config.getTitleMessage(displayLabel);
            Component subtitle = config.getSubtitleMessage(secondsLeft);
            player.showTitle(Title.title(title, subtitle,
                    Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1100), Duration.ofMillis(100))));
        }

        // Bug fix: particle type/count/color used to be hardcoded to Particle.PORTAL with
        // a fixed count of 10, ignoring whatever was configured. Now mirrors the admin
        // TeleportTask.playParticleEffects() pattern: DUST uses the configured color/size,
        // everything else just uses the configured type/count.
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
                // particle errors are never worth interrupting a teleport over
            }
        }

        // Bug fix: sound used to be hardcoded to Sound.UI_BUTTON_CLICK at a fixed
        // volume/pitch, ignoring whatever was configured.
        if (config.soundEnabled()) {
            player.playSound(player.getLocation(), config.soundType(), config.soundVolume(), config.soundPitch());
        }

        secondsLeft--;
    }

    private boolean hasMoved() {
        Location now = player.getLocation();
        return startLocation.getBlockX() != now.getBlockX() ||
               startLocation.getBlockZ() != now.getBlockZ();
    }

    /** Called by the move listener for instant cancellation, mirroring the admin TeleportTask's cancelIfMoved(). */
    public void cancelIfMoved() {
        if (cancelled) return;
        if (config.cancelOnMove() && hasMoved()) {
            cancelTask(Component.text("Teleport cancelled - you moved!", NamedTextColor.RED));
        }
    }

    private void complete() {
        if (cancelled) return;
        cancelled = true;
        cancel();
        if (bossBar != null) bossBar.removeViewer(player);
        activeTasks().remove(player.getUniqueId());

        player.teleport(destination);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage(config.getMessage("teleport-confirmed", "name", displayLabel));
    }

    public void cancelTask(Component reason) {
        if (cancelled) return;
        cancelled = true;
        cancel();
        if (bossBar != null) bossBar.removeViewer(player);
        activeTasks().remove(player.getUniqueId());
        if (reason != null) player.sendMessage(reason);
    }

    @Override
    public void cancel() {
        super.cancel();
        if (bossBar != null) bossBar.removeViewer(player);
        activeTasks().remove(player.getUniqueId());
    }

    public static void cancelAllTasks(Player player) {
        PlayerWarpTeleportTask task = activeTasks().remove(player.getUniqueId());
        if (task != null && !task.cancelled) {
            task.cancel();
        }
    }

    // Same Bukkit BarColor -> Adventure BossBar.Color mapping as the admin TeleportTask,
    // duplicated here rather than shared so this class never needs to depend on it.
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

    // Bukkit BarStyle -> Adventure BossBar.Overlay. The admin TeleportTask never made
    // this conversion (it always passes Overlay.PROGRESS regardless of the configured
    // warp.bossbar.style), so a configured SEGMENTED_x style is silently ignored there -
    // that's a separate, pre-existing gap left alone since it wasn't part of this request.
    // Player warps get the full mapping here so teleport.bossbar.style actually works.
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
