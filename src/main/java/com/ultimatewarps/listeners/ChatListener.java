package com.ultimatewarps.listeners;

import com.ultimatewarps.ChatInput;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (ChatInput.hasPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.message());
            ChatInput.handleInput(event.getPlayer(), message);
        }
    }
}