package com.ultimatewarps.listeners;

import com.ultimatewarps.ChatInput;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    // Reverted to the legacy AsyncPlayerChatEvent/getMessage() capture approach (same as
    // the original pre-MiniMessage version of this plugin) instead of Paper's
    // AsyncChatEvent/Component-based capture. This is purely about HOW the typed input is
    // read off the event - it has no effect on how a display name is rendered or stored;
    // TextFormat.render() and Warp.setDisplayName() downstream are completely unchanged,
    // so MiniMessage tags, gradients, and legacy '&'/hex codes are still stored and
    // rendered exactly as before. The only thing that changes is which Bukkit event type
    // and field this listener captures the raw chat text from.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (ChatInput.hasPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            ChatInput.handleInput(event.getPlayer(), message);
        }
    }
}
