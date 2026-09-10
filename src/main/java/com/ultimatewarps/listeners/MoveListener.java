package com.ultimatewarps.listeners;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        Player player = event.getPlayer();

        TeleportTask task = UltimateWarps.getInstance().getActiveTeleports().get(player.getUniqueId());
        if (task != null) {
            task.cancelIfMoved();
        }

        PlayerWarpTeleportTask playerWarpTask = UltimateWarps.getInstance().getActivePlayerWarpTeleports().get(player.getUniqueId());
        if (playerWarpTask != null) {
            playerWarpTask.cancelIfMoved();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!UltimateWarps.getInstance().getConfigManager().shouldCancelOnDamage()) return;

        TeleportTask task = UltimateWarps.getInstance().getActiveTeleports().get(player.getUniqueId());
        if (task != null) {
            task.cancelTask(UltimateWarps.getInstance().getConfigManager().getTeleportCancelledDamageMessage());
        }

        PlayerWarpTeleportTask playerWarpTask = UltimateWarps.getInstance().getActivePlayerWarpTeleports().get(player.getUniqueId());
        if (playerWarpTask != null) {
            playerWarpTask.cancelTask(UltimateWarps.getInstance().getConfigManager().getTeleportCancelledDamageMessage());
        }
    }
}
