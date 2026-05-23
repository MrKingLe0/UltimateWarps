package com.ultimatewarps.listeners;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        Player player = event.getPlayer();
        TeleportTask task = UltimateWarps.getInstance().getActiveTeleports().get(player.getUniqueId());
        // TeleportTask already checks movement each second, no need to duplicate.
    }
}