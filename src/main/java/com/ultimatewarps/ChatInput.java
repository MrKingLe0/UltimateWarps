package com.ultimatewarps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInput {

    private static final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitRunnable> timeouts = new ConcurrentHashMap<>();

    public static void waitForInput(Player player, int timeoutSeconds, Consumer<String> callback) {
        UUID uuid = player.getUniqueId();
        cancel(player);
        pending.put(uuid, callback);
        player.sendMessage(Component.text("Please type your input in chat (or 'cancel' to abort).", NamedTextColor.YELLOW));
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (pending.remove(uuid) != null) {
                    player.sendMessage(Component.text("Input timed out.", NamedTextColor.RED));
                }
            }
        };
        task.runTaskLater(UltimateWarps.getInstance(), timeoutSeconds * 20L);
        timeouts.put(uuid, task);
    }

    public static void cancel(Player player) {
        UUID uuid = player.getUniqueId();
        pending.remove(uuid);
        BukkitRunnable task = timeouts.remove(uuid);
        if (task != null) task.cancel();
    }

    public static boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public static void handleInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        Consumer<String> callback = pending.remove(uuid);
        BukkitRunnable task = timeouts.remove(uuid);
        if (task != null) task.cancel();
        if (callback != null) {
            // ✅ Always execute callback on the main server thread
            UltimateWarps plugin = UltimateWarps.getInstance();
            if (Bukkit.isPrimaryThread()) {
                callback.accept(message);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(message));
            }
        }
    }
}