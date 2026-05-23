package com.ultimatewarps.commands;

import com.ultimatewarps.*;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelWarpCommand implements CommandExecutor {
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
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /delwarp <name>", net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }
        String name = args[0];
        WarpManager wm = UltimateWarps.getInstance().getWarpManager();
        if (wm.getWarp(name) == null) {
            player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("warp-not-found"));
            return true;
        }
        wm.removeWarp(name);
        player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("warp-deleted").replaceText(b -> b.matchLiteral("%name%").replacement(name)));
        return true;
    }
}