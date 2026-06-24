package com.ultimatewarps.commands;

import com.ultimatewarps.*;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DelWarpCommand implements CommandExecutor, TabCompleter {
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
        player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("warp-deleted", "name", name));
        return true;
    }

    // Improvement: no tab-completion meant admins had to type the full warp name from
    // memory, with no feedback until after hitting enter - a typo would just bounce off
    // "warp not found" with no hint about what was actually available, or worse, silently
    // delete a different similarly-named warp than intended.
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("ultimatewarps.admin")) {
            String partial = args[0].toLowerCase();
            return UltimateWarps.getInstance().getWarpManager().getAllWarps().stream()
                    .map(Warp::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}