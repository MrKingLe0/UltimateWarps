package com.ultimatewarps.commands;

import com.ultimatewarps.UltimateWarps;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnForceCommand implements CommandExecutor {

    private final UltimateWarps plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public SpawnForceCommand(UltimateWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimatewarps.admin")) {
            sender.sendMessage(plugin.getConfigManager().getNoPermissionMessage());
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(miniMessage.deserialize("<red>ᴜsᴀɢᴇ: /sᴘᴀᴡɴꜰᴏʀᴄᴇ <ᴘʟᴀʏᴇʀ>"));
            return true;
        }

        Location spawn = plugin.getConfigManager().getSpawnLocation();
        if (spawn == null) {
            sender.sendMessage(plugin.getConfigManager().getSpawnNotSetMessage());
            return true;
        }

        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.teleport(spawn);
            plugin.getEffectManager().playTeleportEffect(targetPlayer, "Spawn");
            targetPlayer.sendMessage(plugin.getConfigManager().getTeleportConfirmedMessage("Spawn"));
            sender.sendMessage(miniMessage.deserialize("<green>ᴛᴇʟᴇᴘᴏʀᴛᴇᴅ " + playerName + "  ᴛᴏ sᴘᴀᴡɴ!</green>"));
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.getName() != null) {
                plugin.getConfigManager().getSpawnLocationManager().setPendingSpawnTeleport(offlinePlayer.getUniqueId(), spawn);
                sender.sendMessage(miniMessage.deserialize("<green>" + playerName + " ᴡɪʟʟ ʙᴇ ᴛᴇʟᴇᴘᴏʀᴛᴇᴅ ᴛᴏ sᴘᴀᴡɴ ᴡʜᴇɴ ᴛʜᴇʏ ᴊᴏɪɴ!</green>"));
            } else {
                sender.sendMessage(miniMessage.deserialize("<red>ᴘʟᴀʏᴇʀ ɴᴏᴛ ꜰᴏᴜɴᴅ: " + playerName + "</red>"));
            }
        }

        return true;
    }
}