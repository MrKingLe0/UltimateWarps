package com.ultimatewarps.commands;

import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelSpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (!player.hasPermission("ultimatewarps.admin")) {
            player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("no-permission"));
            return true;
        }

        UltimateWarps.getInstance().getConfigManager().removeSpawnLocation();
        player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("spawn-deleted"));
        return true;
    }
}