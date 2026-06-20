package com.ultimatewarps.commands;

import com.ultimatewarps.*;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand implements CommandExecutor {
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
            player.sendMessage(Component.text("Usage: /setwarp <name>", net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }
        String name = args[0];
        WarpManager wm = UltimateWarps.getInstance().getWarpManager();
        if (!wm.isValidWarpName(name)) {
            player.sendMessage(Component.text(
                "Invalid warp name. Use only letters, numbers, underscores and hyphens (max 32 characters).",
                net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }
        if (wm.getWarp(name) != null) {
            player.sendMessage(Component.text("A warp with that name already exists.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }
        ConfigManager config = UltimateWarps.getInstance().getConfigManager();
        Warp warp = new Warp(name, player.getLocation());
        warp.setCooldown(config.globalDefaultCooldown());
        warp.setDelay(config.globalDefaultDelay());
        wm.addWarp(warp);
        player.sendMessage(config.getMessage("warp-created").replaceText(b -> b.matchLiteral("%name%").replacement(name)));
        return true;
    }
}