package com.ultimatewarps;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EffectManager {


    public void playParticle(Player player, Particle particle, int count) {
        playParticle(player, particle, count, null);
    }

    public void playParticle(Player player, Particle particle, int count, Object data) {
        if (data != null) {
            player.getWorld().spawnParticle(particle,
                    player.getLocation().add(0, 1, 0),
                    count, 0.5, 0.5, 0.5, data);
        } else {
            player.getWorld().spawnParticle(particle,
                    player.getLocation().add(0, 1, 0),
                    count, 0.5, 0.5, 0.5, 0.05);
        }
    }

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}