package com.ultimatewarps;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EffectManager {

    private final UltimateWarps plugin;

    public EffectManager(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    public void playParticle(Player player, Particle particle, int count) {
        playParticle(player, particle, count, null);
    }

    public void playParticle(Player player, Particle particle, int count, Object data) {
        Location loc = player.getLocation().add(0, 1, 0);

        try {
            Class<?> type = particle.getDataType();
            if (type == Void.class) {
                player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.05);
                return;
            }
            Object payload = (data != null && type.isInstance(data)) ? data : defaultParticleData(type);
            if (payload != null) {
                player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, payload);
            } else {
                player.getWorld().spawnParticle(Particle.PORTAL, loc, count, 0.5, 0.5, 0.5, 0.05);
            }
        } catch (Exception e) {

            player.getWorld().spawnParticle(Particle.PORTAL, loc, count, 0.5, 0.5, 0.5, 0.05);
        }
    }

    private Object defaultParticleData(Class<?> type) {
        try {
            if (type == DustOptions.class) {
                return new DustOptions(Color.fromRGB(255, 85, 85), 1.0f);
            }
            if (type == Particle.DustTransition.class) {
                return new Particle.DustTransition(Color.fromRGB(255, 85, 85), Color.fromRGB(85, 85, 255), 1.0f);
            }
            if (type == org.bukkit.block.data.BlockData.class) {
                return Material.STONE.createBlockData();
            }
            if (type == org.bukkit.inventory.ItemStack.class) {
                return new org.bukkit.inventory.ItemStack(Material.DIAMOND);
            }
            if (type == Color.class) {
                return Color.fromRGB(255, 85, 85);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void playTeleportEffect(Player player, String type) {
        playTeleportEffect(player, type, null);
    }

    public void playTeleportEffect(Player player, String type, Warp source) {
        if (source != null && source.isCustomEffects()) {
            if (source.isEffectSoundEnabled()) {
                Sound sound = ConfigManager.parseSound(source.getEffectSoundType(), Sound.BLOCK_NOTE_BLOCK_PLING);
                playSound(player, sound, source.getEffectSoundVolume(), source.getEffectSoundPitch());
            }
            if (source.isEffectParticleEnabled()) {
                Particle particle;
                try {
                    particle = Particle.valueOf(source.getEffectParticleType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    particle = Particle.DUST;
                }
                int count = source.getEffectParticleCount();
                if (particle == Particle.DUST) {
                    Color color = ConfigManager.parseColor(source.getEffectDustColor());
                    playParticle(player, particle, count, new DustOptions(color, source.getEffectDustSize()));
                } else {
                    playParticle(player, particle, count);
                }
            }
            return;
        }

        ConfigManager config = plugin.getConfigManager();

        boolean isSpawn = type.equalsIgnoreCase("Spawn");

        boolean soundEnabled = isSpawn ? config.spawnSoundEnabled() : config.warpSoundEnabled();
        if (soundEnabled) {
            Sound soundType = isSpawn ? config.spawnSoundType() : config.warpSoundType();
            float volume = isSpawn ? config.spawnSoundVolume() : config.warpSoundVolume();
            float pitch = isSpawn ? config.spawnSoundPitch() : config.warpSoundPitch();
            playSound(player, soundType, volume, pitch);
        }

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
}
