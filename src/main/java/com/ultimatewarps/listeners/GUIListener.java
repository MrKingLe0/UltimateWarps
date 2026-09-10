package com.ultimatewarps.listeners;

import com.ultimatewarps.gui.AdminGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof AdminGUI adminGui) {
            Inventory inv = event.getInventory();
            int rawSlot = event.getRawSlot();

            if (rawSlot >= inv.getSize()) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            AdminGUI.MenuState state = AdminGUI.getMenu(inv);
            if (state == null) {
                adminGui.handleClick(rawSlot);
                return;
            }
            switch (state.type()) {
                case EDIT -> {

                    if (rawSlot == AdminGUI.EDIT_ICON_SLOT
                            && event.getCursor() != null
                            && event.getCursor().getType() != org.bukkit.Material.AIR) {
                        adminGui.setIcon(state.warp(), event.getCursor());
                    } else {
                        adminGui.handleEditClick(rawSlot, state.warp());
                    }
                }
                case EFFECTS -> adminGui.handleEffectsClick(rawSlot, state.warp());
                case SLOT_PICKER -> adminGui.handleSlotPickerClick(rawSlot, state.warp());
                case EFFECT_PARTICLE -> adminGui.handleParticlePickerClick(rawSlot, state.warp(), state.page());
                case EFFECT_SOUND -> adminGui.handleSoundPickerClick(rawSlot, state.warp(), state.page());
                case EFFECT_COLOR -> adminGui.handleColorPickerClick(rawSlot, state.warp());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        AdminGUI.removeMenu(inv);
    }
}
