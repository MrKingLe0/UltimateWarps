package com.ultimatewarps.playerwarps.gui;

import com.ultimatewarps.ChatInput;
import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerWarpEditGUI implements InventoryHolder, Listener {

    private final Player player;
    private final PlayerWarp warp;
    private Inventory inventory;

    private boolean registered = false;
    private boolean reopening = false;

    public PlayerWarpEditGUI(Player player, PlayerWarp warp) {
        this.player = player;
        this.warp = warp;
    }

    public void open() {
        if (!registered) {
            UltimateWarps.getInstance().getServer().getPluginManager().registerEvents(this, UltimateWarps.getInstance());
            registered = true;
        }

        Component title = Component.text("Edit: " + warp.getName(), NamedTextColor.DARK_AQUA);
        inventory = Bukkit.createInventory(this, 27, title);

        fillBorder();
        inventory.setItem(10, visibilityItem());
        inventory.setItem(12, renameItem());
        inventory.setItem(14, descriptionItem());
        inventory.setItem(16, iconItem());
        inventory.setItem(22, deleteItem());

        player.openInventory(inventory);
    }

    private void fillBorder() {
        ItemStack filler = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack named(Material material, String name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render(name));
        meta.lore(java.util.Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String text) {
        return TextFormat.render(text);
    }

    private ItemStack visibilityItem() {
        boolean isPublic = warp.isPublic();
        return named(isPublic ? Material.LIME_DYE : Material.GRAY_DYE,
                "&e&lVisibility: " + (isPublic ? "&aPublic" : "&cPrivate"),
                line("&7Public warps show up for everyone"),
                line("&7in the player warps browser."),
                line("&7Private warps only show up for you"),
                line("&7and server staff."),
                Component.empty(),
                line("&eClick to toggle"));
    }

    private ItemStack renameItem() {

        String display = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        return named(Material.NAME_TAG, "&e&lSet Display Name",
                TextFormat.renderTemplate("&fCurrent: {value}", "{value}", display),
                line("&7Click to change via chat"));
    }

    private ItemStack descriptionItem() {
        String description = warp.getDescription() != null ? warp.getDescription() : "&7(none)";
        return named(Material.WRITABLE_BOOK, "&e&lSet Description",
                TextFormat.renderTemplate("&fCurrent: {value}", "{value}", description),
                line("&7Click to change via chat"));
    }

    private ItemStack iconItem() {
        ItemStack icon = warp.getIcon();
        if (icon == null) {
            icon = new ItemStack(Material.PLAYER_HEAD);
        } else {
            icon = icon.clone();
        }
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(TextFormat.render("&d&lCustom Icon"));
        List<Component> lore = new ArrayList<>();
        lore.add(TextFormat.render("&7Drag an item from your inventory"));
        lore.add(TextFormat.render("&7onto this slot to set it as the icon"));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack deleteItem() {
        return named(Material.BARRIER, "&c&lDelete Warp",
                line("&4Click to permanently delete this warp"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot >= inventory.getSize()) return;
        event.setCancelled(true);

        switch (rawSlot) {
            case 10:
                warp.setPublic(!warp.isPublic());
                warp.save();
                player.sendMessage(Component.text("Visibility updated.", NamedTextColor.GREEN));
                open();
                break;
            case 12:
                reopening = true;
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        warp.setDisplayName(input);
                        warp.save();
                        player.sendMessage(Component.text("Display name updated.", NamedTextColor.GREEN));
                    }
                    open();
                });
                break;
            case 14:
                reopening = true;
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        warp.setDescription(input);
                        warp.save();
                        player.sendMessage(Component.text("Description updated.", NamedTextColor.GREEN));
                    }
                    open();
                });
                break;
            case 16:
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    warp.setIcon(cursor.clone());
                    warp.save();
                    player.setItemOnCursor(null);
                    player.sendMessage(Component.text("Icon updated.", NamedTextColor.GREEN));
                    open();
                }
                break;
            case 22:
                player.closeInventory();
                UltimateWarps.getInstance().getPlayerWarpManager().removeWarp(warp.getOwnerId(), warp.getName());
                player.sendMessage(Component.text("Player warp '" + warp.getName() + "' deleted.", NamedTextColor.GREEN));
                unregister();
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (reopening) {

            reopening = false;
            return;
        }
        unregister();
    }

    private void unregister() {
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
