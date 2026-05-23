package com.ultimatewarps.commands;

import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (!sender.hasPermission("ultimatewarps.admin")) {
            sender.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("no-permission"));
            return true;
        }
        UltimateWarps.getInstance().getConfigManager().setSpawnLocation(player.getLocation());
        player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("spawn-set"));
        return true;
    }
}