package com.ultimatewarps.listeners;

import com.ultimatewarps.ChatInput;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (ChatInput.hasPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            ChatInput.handleInput(event.getPlayer(), message);
        }
    }
}
