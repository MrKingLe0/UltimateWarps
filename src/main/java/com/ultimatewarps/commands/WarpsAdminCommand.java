package com.ultimatewarps.commands;

import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.gui.AdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpsAdminCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!sender.hasPermission("ultimatewarps.admin")) {
            sender.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("no-permission"));
            return true;
        }
        new AdminGUI(player).open(0);
        return true;
    }
}