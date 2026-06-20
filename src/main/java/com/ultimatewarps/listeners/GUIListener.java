package com.ultimatewarps.listeners;

import com.ultimatewarps.Warp;
import com.ultimatewarps.gui.AdminGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    // Bug fix: WarpGUI creates its inventory with a null InventoryHolder
    // (Bukkit.createInventory(null, ...)), so `holder instanceof WarpGUI` could never
    // match here - that branch was permanently dead code. WarpGUI already registers its
    // own Listener and handles its own clicks via inventory-reference equality, so this
    // class only needs to handle AdminGUI, which does pass itself as the holder.
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof AdminGUI adminGui) {
            Inventory inv = event.getInventory();
            int rawSlot = event.getRawSlot();
            // If click is in the player's own inventory (bottom part), allow it
            if (rawSlot >= inv.getSize()) {
                return; // allow normal movement
            }
            // All top inventory clicks are cancelled
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Warp editing = AdminGUI.getEditingWarp(inv);
            if (editing != null) {
                // If icon slot (16) and cursor has an item, set the icon
                if (rawSlot == 16 && event.getCursor() != null && event.getCursor().getType() != org.bukkit.Material.AIR) {
                    adminGui.setIcon(editing, event.getCursor());
                } else {
                    adminGui.handleEditClick(rawSlot, editing);
                }
            } else {
                adminGui.handleClick(rawSlot);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        AdminGUI.removeEditingInventory(inv);
    }
}