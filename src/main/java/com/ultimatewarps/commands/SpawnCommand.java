package com.ultimatewarps.commands;

import com.ultimatewarps.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (!sender.hasPermission("ultimatewarps.spawn")) {
            sender.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("no-permission"));
            return true;
        }
        ConfigManager config = UltimateWarps.getInstance().getConfigManager();
        if (!config.spawnEnabled()) {
            player.sendMessage(Component.text("Spawn is disabled.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }
        Location spawnLoc = config.getSpawnLocation();
        if (spawnLoc == null) {
            player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("spawn-not-set"));
            return true;
        }
        CooldownManager cm = UltimateWarps.getInstance().getCooldownManager();
        int cd = cm.getRemainingCooldown(player, "spawn", cm.getEffectiveCooldown(player, config.spawnCooldown()));
        if (cd > 0) {
            player.sendMessage(config.getMessage("cooldown-active").replaceText(b -> b.matchLiteral("%seconds%").replacement(String.valueOf(cd))));
            return true;
        }
        int delay = cm.getEffectiveDelay(player, config.spawnDelay());
        TeleportTask task = new TeleportTask(player, spawnLoc, delay,
                config.spawnCancelOnMove(),
                config.spawnTitleMessage(),
                config.spawnSubtitleMessage(),
                "Spawn");
        task.runTaskTimer(UltimateWarps.getInstance(), 0L, 20L);
        UltimateWarps.getInstance().getActiveTeleports().put(player.getUniqueId(), task);
        cm.applyCooldown(player, "spawn");
        return true;
    }
}