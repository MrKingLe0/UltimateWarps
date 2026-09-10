package com.ultimatewarps.commands;

import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.Warp;
import com.ultimatewarps.gui.AdminGUI;
import com.ultimatewarps.gui.WarpGUI;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class UltimateWarpsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "spawn", "setspawn","delspawn", "warp", "setwarp", "delwarp", "warpsadmin", "spawnforce", "help"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            String version = UltimateWarps.getInstance().getDescription().getVersion();
            sender.sendMessage(TextFormat.render("§eUltimateWarps v" + version + " - Use /" + label + " help"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                if (!sender.hasPermission("ultimatewarps.admin")) {
                    sender.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("no-permission"));
                    return true;
                }
                UltimateWarps.getInstance().reloadPlugin();
                sender.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("reload-success"));
                break;

            case "spawn":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextFormat.render("Only players can use this command."));
                    return true;
                }
                new SpawnCommand(UltimateWarps.getInstance()).onCommand(sender, null, null, new String[0]);
                break;

            case "setspawn":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextFormat.render("Only players can use this command."));
                    return true;
                }
                new SetSpawnCommand().onCommand(sender, null, null, new String[0]);
                break;
            case "delspawn":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextFormat.render("Only players can use this command."));
                    return true;
                }
                new DelSpawnCommand().onCommand(sender, null, null, new String[0]);
                break;
            case "warp":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextFormat.render("Only players can use this command."));
                    return true;
                }
                if (args.length == 1) {
                    new WarpGUI(player).open(0);
                } else {
                    new WarpCommand(UltimateWarps.getInstance()).onCommand(sender, null, null, new String[]{args[1]});
                }
                break;

            case "setwarp":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextFormat.render("Only players can use this command."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(TextFormat.render("§cUsage: /" + label + " setwarp <name>"));
                    return true;
                }
                new SetWarpCommand().onCommand(sender, null, null, new String[]{args[1]});
                break;

            case "delwarp":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextFormat.render("Only players can use this command."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(TextFormat.render("§cUsage: /" + label + " delwarp <name>"));
                    return true;
                }
                new DelWarpCommand().onCommand(sender, null, null, new String[]{args[1]});
                break;

            case "warpsadmin":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(TextFormat.render("Only players can use this command."));
                    return true;
                }
                new WarpsAdminCommand().onCommand(sender, null, null, new String[0]);
                break;

            case "spawnforce":
                if (!sender.hasPermission("ultimatewarps.admin")) {
                    sender.sendMessage(UltimateWarps.getInstance().getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(TextFormat.render("§cUsage: /" + label + " spawnforce <player>"));
                    return true;
                }
                new SpawnForceCommand(UltimateWarps.getInstance()).onCommand(sender, null, null, new String[]{args[1]});
                break;

            case "help":
                sendHelp(sender, label);
                break;

            default:
                sender.sendMessage(TextFormat.render("§cUnknown subcommand. Use /" + label + " help"));
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(TextFormat.render("§9=== §dᴜʟᴛɪᴍᴀᴛᴇᴡᴀʀᴘꜱ ʜᴇʟᴘ §9==="));
        sender.sendMessage(TextFormat.render("§b/" + label + " ʀᴇʟᴏᴀᴅ §7- ʀᴇʟᴏᴀᴅ ᴛʜᴇ ᴘʟᴜɢɪɴ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ꜱᴘᴀᴡɴ §7- ᴛᴇʟᴇᴘᴏʀᴛ ᴛᴏ ꜱᴘᴀᴡɴ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ꜱᴇᴛꜱᴘᴀᴡɴ §7- ꜱᴇᴛ ꜱᴘᴀᴡɴ ʟᴏᴄᴀᴛɪᴏɴ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ᴅᴇʟꜱᴘᴀᴡɴ §7- ᴅᴇʟᴇᴛᴇ ꜱᴘᴀᴡɴ ʟᴏᴄᴀᴛɪᴏɴ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ᴡᴀʀᴘ [ɴᴀᴍᴇ] §7- ᴏᴘᴇɴ ᴡᴀʀᴘ ɢᴜɪ ᴏʀ ᴛᴇʟᴇᴘᴏʀᴛ ᴛᴏ ᴡᴀʀᴘ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ꜱᴇᴛᴡᴀʀᴘ <ɴᴀᴍᴇ> §7- ᴄʀᴇᴀᴛᴇ ᴀ ɴᴇᴡ ᴡᴀʀᴘ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ᴅᴇʟᴡᴀʀᴘ <ɴᴀᴍᴇ> §7- ᴅᴇʟᴇᴛᴇ ᴀ ᴡᴀʀᴘ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ᴡᴀʀᴘꜱᴀᴅᴍɪɴ §7- ᴏᴘᴇɴ ᴀᴅᴍɪɴ ɢᴜɪ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ꜱᴘᴀᴡɴꜰᴏʀᴄᴇ <ᴘʟᴀʏᴇʀ> §7- ꜰᴏʀᴄᴇ ᴛᴇʟᴇᴘᴏʀᴛ ᴀ ᴘʟᴀʏᴇʀ ᴛᴏ ꜱᴘᴀᴡɴ"));
        sender.sendMessage(TextFormat.render("§b/" + label + " ʜᴇʟᴘ §7- ꜱʜᴏᴡ ᴛʜɪꜱ ʜᴇʟᴘ"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {

            for (String sub : SUBCOMMANDS) {
                if (hasSubPermission(sender, sub) && sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("warp") || sub.equals("delwarp")) {

                for (Warp warp : UltimateWarps.getInstance().getWarpManager().getAllWarps()) {
                    if (warp.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(warp.getName());
                    }
                }
                return completions;
            }

            if (sub.equals("spawnforce")) {
                for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(online.getName());
                    }
                }
                return completions;
            }
            if (sub.equals("setwarp")) {
                return List.of("<name>");
            }
        }

        return completions;
    }

    private boolean hasSubPermission(CommandSender sender, String sub) {
        return switch (sub) {
            case "reload", "setspawn","delspawn", "setwarp", "delwarp", "warpsadmin", "spawnforce" -> sender.hasPermission("ultimatewarps.admin");
            case "spawn" -> sender.hasPermission("ultimatewarps.spawn");
            case "warp" -> sender.hasPermission("ultimatewarps.warp.*") || sender.hasPermission("ultimatewarps.admin");
            default -> true;
        };
    }
}
