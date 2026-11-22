package com.ultimatewarps.listeners;

import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.gui.AdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();

        if (holder instanceof AdminGUI adminGui) {
            int rawSlot = event.getRawSlot();
            AdminGUI.MenuState state = AdminGUI.getMenu(top);
            boolean isEditMenu = state != null && state.type() == AdminGUI.MenuType.EDIT;

            if (rawSlot >= top.getSize()) {
                if (!isEditMenu
                        || event.isShiftClick()
                        || event.getClick() == ClickType.DOUBLE_CLICK
                        || event.getClick() == ClickType.NUMBER_KEY
                        || event.getClick() == ClickType.SWAP_OFFHAND
                        || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                        || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }

            event.setCancelled(true);
            if (rawSlot < 0 || event.getCurrentItem() == null) return;

            final ItemStack cursorSnapshot = (event.getCursor() != null && event.getCursor().getType() != Material.AIR)
                    ? event.getCursor().clone()
                    : null;

            Bukkit.getScheduler().runTask(UltimateWarps.getInstance(), () -> {
                if (!player.isOnline()) return;
                if (state == null) {
                    adminGui.handleClick(rawSlot);
                    return;
                }
                switch (state.type()) {
                    case EDIT -> {
                        if (rawSlot == AdminGUI.EDIT_ICON_SLOT && cursorSnapshot != null) {
                            adminGui.setIcon(state.warp(), cursorSnapshot);
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
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AdminGUI)) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < top.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        AdminGUI.removeMenu(inv);
    }
}
