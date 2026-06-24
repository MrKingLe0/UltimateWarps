package com.ultimatewarps.listeners;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    // Bug fix: this previously looked up the active task from a map that was never
    // populated (UltimateWarps#activeTeleports was always empty) and then did nothing
    // with it anyway. Now that the map is actually kept up to date by TeleportTask,
    // use it to cancel teleports the instant a player moves, instead of waiting up to
    // a full second for TeleportTask's own per-tick movement check to catch it.
    //
    // Also checks the separate player-warp teleport map - admin and player warp
    // countdowns are tracked independently, so both have to be checked here.
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
}