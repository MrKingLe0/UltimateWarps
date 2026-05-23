package com.ultimatewarps.listeners;

import com.ultimatewarps.ChatInput;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (ChatInput.hasPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            ChatInput.handleInput(event.getPlayer(), event.getMessage());
        }
    }
}