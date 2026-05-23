package com.ultimatewarps;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportTask extends BukkitRunnable {

    private final Player player;
    private final Location destination;
    private final int totalSeconds;
    private int secondsLeft;
    private final Location startLocation;
    private final boolean cancelOnMove;
    private BossBar bossBar;
    private final String titleMiniMessage;
    private final String subtitleFormat;
    private final EffectManager effectManager;
    private final String warpName;
    private boolean cancelled = false;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TeleportTask(Player player, Location destination, int delaySeconds,
                        boolean cancelOnMove, String titleMiniMessage, String subtitleFormat,
                        String warpName) {
        this.player = player;
        this.destination = destination;
        this.totalSeconds = delaySeconds;
        this.secondsLeft = delaySeconds;
        this.startLocation = player.getLocation().clone();
        this.cancelOnMove = cancelOnMove;
        this.titleMiniMessage = titleMiniMessage;
        this.subtitleFormat = subtitleFormat;
        this.warpName = warpName;
        this.effectManager = UltimateWarps.getInstance().getEffectManager();

        ConfigManager config = UltimateWarps.getInstance().getConfigManager();
        boolean bossBarEnabled;
        String bossBarText;
        BarColor barColor;
        BarStyle barStyle;

        if (warpName.equals("Spawn")) {
            bossBarEnabled = config.spawnBossBarEnabled();
            bossBarText = config.spawnBossBarText();
            barColor = config.spawnBossBarColor();
            barStyle = config.spawnBossBarStyle();
        } else {
            bossBarEnabled = config.warpBossBarEnabled();
            bossBarText = config.warpBossBarText().replace("%warp%", warpName);
            barColor = config.warpBossBarColor();
            barStyle = config.warpBossBarStyle();
        }

        if (bossBarEnabled) {
            this.bossBar = BossBar.bossBar(
                    miniMessage.deserialize(bossBarText),
                    1.0f,
                    mapBarColor(barColor),
                    mapBarStyle(barStyle)
            );
            this.bossBar.addViewer(player);
        }
    }

    @Override
    public void run() {
        if (cancelled) return;
        if (secondsLeft <= 0) {
            complete();
            return;
        }
        if (cancelOnMove && player.getLocation().distanceSquared(startLocation) > 0.04) {
            cancelTask(UltimateWarps.getInstance().getConfigManager().getMessage("teleport-cancelled-move"));
            return;
        }

        // Update BossBar
        if (bossBar != null) {
            bossBar.progress((float) secondsLeft / totalSeconds);
            ConfigManager cfg = UltimateWarps.getInstance().getConfigManager();
            String barText = warpName.equals("Spawn") ?
                    cfg.spawnBossBarText() :
                    cfg.warpBossBarText().replace("%warp%", warpName);
            bossBar.name(miniMessage.deserialize(barText.replace("%seconds%", String.valueOf(secondsLeft))));
        }

        // Title & Subtitle
        Component title = miniMessage.deserialize(titleMiniMessage.replace("%warp%", warpName));
        Component subtitle = miniMessage.deserialize(subtitleFormat.replace("%seconds%", String.valueOf(secondsLeft)));
        player.showTitle(Title.title(title, subtitle, Title.Times.times(
                java.time.Duration.ofMillis(0),
                java.time.Duration.ofMillis(1100),
                java.time.Duration.ofMillis(100)
        )));

        // Effects
        ConfigManager cfg = UltimateWarps.getInstance().getConfigManager();
        boolean particleEnabled;
        Particle particleType;
        int particleCount;
        Object particleData = null;

        if (warpName.equals("Spawn")) {
            particleEnabled = cfg.spawnParticleEnabled();
            particleType = cfg.spawnParticleType();
            particleCount = cfg.spawnParticleCount();
            if (particleType == Particle.DUST) {
                particleData = new Particle.DustOptions(cfg.spawnDustColor(), cfg.spawnDustSize());
            }
        } else {
            particleEnabled = cfg.warpParticleEnabled();
            particleType = cfg.warpParticleType();
            particleCount = cfg.warpParticleCount();
            if (particleType == Particle.DUST) {
                particleData = new Particle.DustOptions(cfg.warpDustColor(), cfg.warpDustSize());
            }
        }

        if (particleEnabled) {
            effectManager.playParticle(player, particleType, particleCount, particleData);
        }

        boolean soundEnabled = warpName.equals("Spawn") ? cfg.spawnSoundEnabled() : cfg.warpSoundEnabled();
        Sound soundType = warpName.equals("Spawn") ? cfg.spawnSoundType() : cfg.warpSoundType();
        float volume = warpName.equals("Spawn") ? cfg.spawnSoundVolume() : cfg.warpSoundVolume();
        float pitch = warpName.equals("Spawn") ? cfg.spawnSoundPitch() : cfg.warpSoundPitch();

        if (soundEnabled) {
            effectManager.playSound(player, soundType, volume, pitch);
        }

        secondsLeft--;
    }

    private void complete() {
        if (cancelled) return;
        cancel();
        removeBossBar();
        player.teleport(destination);
        effectManager.playParticle(player, Particle.PORTAL, 100);
        effectManager.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        ConfigManager config = UltimateWarps.getInstance().getConfigManager();
        Component msg = config.getMessage("teleportation-confirmed", "warp", warpName);
        player.sendMessage(msg);
    }

    public void cancelTask(Component reason) {
        cancelled = true;
        cancel();
        removeBossBar();
        player.sendMessage(reason);
    }

    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeViewer(player);
            bossBar = null;
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        removeBossBar();
    }

    private BossBar.Color mapBarColor(org.bukkit.boss.BarColor color) {
        return switch (color) {
            case PINK -> BossBar.Color.PINK;
            case BLUE -> BossBar.Color.BLUE;
            case RED -> BossBar.Color.RED;
            case GREEN -> BossBar.Color.GREEN;
            case YELLOW -> BossBar.Color.YELLOW;
            case PURPLE -> BossBar.Color.PURPLE;
            case WHITE -> BossBar.Color.WHITE;
        };
    }

    private BossBar.Overlay mapBarStyle(org.bukkit.boss.BarStyle style) {
        return switch (style) {
            case SOLID -> BossBar.Overlay.PROGRESS;
            case SEGMENTED_6 -> BossBar.Overlay.NOTCHED_6;
            case SEGMENTED_10 -> BossBar.Overlay.NOTCHED_10;
            case SEGMENTED_12 -> BossBar.Overlay.NOTCHED_12;
            case SEGMENTED_20 -> BossBar.Overlay.NOTCHED_20;
        };
    }
}