package com.ultimatewarps.commands;

import com.ultimatewarps.TeleportTask;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
        if (spawn == null || spawn.getWorld() == null) {
            sender.sendMessage(plugin.getConfigManager().getSpawnNotSetMessage());
            return true;
        }

        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayerExact(playerName);

        if (targetPlayer != null && targetPlayer.isOnline()) {
            TeleportTask.cancelAllTasks(targetPlayer);
            PlayerWarpTeleportTask.cancelAllTasks(targetPlayer);
            targetPlayer.teleport(spawn);
            plugin.getEffectManager().playTeleportEffect(targetPlayer, "Spawn");
            targetPlayer.sendMessage(plugin.getConfigManager().getTeleportConfirmedMessage("Spawn"));
            sender.sendMessage(miniMessage.deserialize("<green>ᴛᴇʟᴇᴘᴏʀᴛᴇᴅ <player> ᴛᴏ sᴘᴀᴡɴ!</green>",
                    Placeholder.unparsed("player", targetPlayer.getName())));
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
            if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                plugin.getConfigManager().getSpawnLocationManager().setPendingSpawnTeleport(offlinePlayer.getUniqueId(), spawn);
                sender.sendMessage(miniMessage.deserialize("<green><player> ᴡɪʟʟ ʙᴇ ᴛᴇʟᴇᴘᴏʀᴛᴇᴅ ᴛᴏ sᴘᴀᴡɴ ᴡʜᴇɴ ᴛʜᴇʏ ᴊᴏɪɴ!</green>",
                        Placeholder.unparsed("player", offlinePlayer.getName() != null ? offlinePlayer.getName() : playerName)));
            } else {
                sender.sendMessage(miniMessage.deserialize("<red>ᴘʟᴀʏᴇʀ ɴᴏᴛ ꜰᴏᴜɴᴅ: <player></red>",
                        Placeholder.unparsed("player", playerName)));
            }
        }

        return true;
    }
}
