package com.ultimatewarps.commands;

import com.ultimatewarps.*;
import com.ultimatewarps.gui.WarpGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WarpCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            new WarpGUI(player).open(0);
            return true;
        }
        String warpName = args[0];
        Warp warp = UltimateWarps.getInstance().getWarpManager().getWarp(warpName);
        if (warp == null || !warp.isEnabled()) {
            player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("warp-not-found"));
            return true;
        }
        if (warp.getPermission() != null && !player.hasPermission(warp.getPermission()) && !player.hasPermission("ultimatewarps.admin")) {
            player.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("no-permission"));
            return true;
        }
        ConfigManager config = UltimateWarps.getInstance().getConfigManager();
        CooldownManager cm = UltimateWarps.getInstance().getCooldownManager();
        int cd = cm.getRemainingCooldown(player, "warp_" + warp.getName(), cm.getEffectiveCooldown(player, warp.getCooldown()));
        if (cd > 0) {
            player.sendMessage(config.getMessage("cooldown-active")
                    .replaceText(b -> b.matchLiteral("%seconds%").replacement(String.valueOf(cd))));
            return true;
        }
        int delay = cm.getEffectiveDelay(player, warp.getDelay());
        TeleportTask task = new TeleportTask(player, warp.getLocation(), delay,
                config.warpCancelOnMove(),
                config.warpTitleMessage().replace("%warp%", warp.getName()),
                config.warpSubtitleMessage(),
                warp.getName());
        task.runTaskTimer(UltimateWarps.getInstance(), 0L, 20L);
        UltimateWarps.getInstance().getActiveTeleports().put(player.getUniqueId(), task);
        cm.applyCooldown(player, "warp_" + warp.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Warp warp : UltimateWarps.getInstance().getWarpManager().getAllWarps()) {
                if (!warp.isEnabled()) continue;
                // Permission check
                if (warp.getPermission() != null && !sender.hasPermission(warp.getPermission()) && !sender.hasPermission("ultimatewarps.admin")) {
                    continue;
                }
                if (warp.getName().toLowerCase().startsWith(input)) {
                    completions.add(warp.getName());
                }
            }
        }
        return completions;
    }
}