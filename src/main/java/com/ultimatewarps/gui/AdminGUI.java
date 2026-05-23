package com.ultimatewarps.gui;

import com.ultimatewarps.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AdminGUI implements InventoryHolder {

    private final Player player;
    private List<Warp> warps;
    private int page = 0;
    private final int size;
    private final int warpSlots;
    private final ConfigManager config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Map<Inventory, Warp> editInventories = new HashMap<>();

    public AdminGUI(Player player) {
        this.player = player;
        this.config = UltimateWarps.getInstance().getConfigManager();
        this.size = config.guiSize();
        this.warpSlots = size - 18;
        this.warps = new ArrayList<>(UltimateWarps.getInstance().getWarpManager().getAllWarps());
    }

    public void open(int page) {
        this.page = page;
        Component title = miniMessage.deserialize(config.adminGuiTitle());
        Inventory inv = Bukkit.createInventory(this, size, title);
        fillBorders(inv);

        int start = page * warpSlots;
        int end = Math.min(start + warpSlots, warps.size());
        for (int i = start; i < end; i++) {
            int slot = 9 + (i - start);
            Warp warp = warps.get(i);

            ItemStack head = warp.getIcon();
            if (head == null) {
                head = HeadUtils.getHead(HeadUtils.WARP_ICON);
            } else {
                head = head.clone();
            }

            ItemMeta meta = head.getItemMeta();
            String raw = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
            String display = ChatColor.translateAlternateColorCodes('&', raw);
            meta.setDisplayName("§6§l" + display);

            List<String> lore = new ArrayList<>();
            lore.add("§f§lᴇɴᴀʙʟᴇᴅ: " + (warp.isEnabled() ? "§a§lʏᴇꜱ" : "§c§lɴᴏ"));
            lore.add("§7ᴄᴏᴏʟᴅᴏᴡɴ: §f" + warp.getCooldown() + "s");
            lore.add("§7ᴅᴇʟᴀʏ: §f" + warp.getDelay() + "s");
            lore.add("§7ᴘᴇʀᴍɪꜱꜱɪᴏɴ: §f" + (warp.getPermission() != null ? warp.getPermission() : "ɴᴏɴᴇ"));
            lore.add("");
            lore.add("§e§lᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ ᴛʜɪꜱ ᴡᴀʀᴘ");
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(slot, head);
        }

        if (page > 0) inv.setItem(size - 9, createNavButton(HeadUtils.PREV_ICON, "§e§lᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ"));
        if (end < warps.size()) inv.setItem(size - 2, createNavButton(HeadUtils.NEXT_ICON, "§e§lɴᴇxᴛ ᴘᴀɢᴇ"));
        inv.setItem(size - 5, createCreateButton());
        inv.setItem(size - 1, createCloseButton());

        player.openInventory(inv);
    }

    public void handleClick(int slot) {
        if (slot == size - 9 && page > 0) {
            open(page - 1);
            return;
        }
        if (slot == size - 2 && (page + 1) * warpSlots < warps.size()) {
            open(page + 1);
            return;
        }
        if (slot == size - 5) {
            player.closeInventory();
            ChatInput.waitForInput(player, 60, name -> {
                if (name.equalsIgnoreCase("cancel")) {
                    open(page);
                    return;
                }
                if (UltimateWarps.getInstance().getWarpManager().getWarp(name) != null) {
                    player.sendMessage(Component.text("A warp with that name already exists.", NamedTextColor.RED));
                    open(page);
                    return;
                }
                Warp newWarp = new Warp(name, player.getLocation());
                newWarp.setCooldown(config.globalDefaultCooldown());
                newWarp.setDelay(config.globalDefaultDelay());
                UltimateWarps.getInstance().getWarpManager().addWarp(newWarp);
                player.sendMessage(config.getMessage("warp-created")
                        .replaceText(b -> b.matchLiteral("%name%").replacement(name)));
                // Refresh the warp list and stay on the same page (or adjust if full)
                warps = new ArrayList<>(UltimateWarps.getInstance().getWarpManager().getAllWarps());
                // If the page now has zero warps, go to previous page (but not below 0)
                if (page > 0 && page * warpSlots >= warps.size()) {
                    page--;
                }
                open(page);
            });
            return;
        }
        if (slot == size - 1) {
            player.closeInventory();
            return;
        }
        if (slot >= 9 && slot < 9 + warpSlots) {
            int index = page * warpSlots + (slot - 9);
            if (index < warps.size()) {
                Warp warp = warps.get(index);
                openWarpEditMenu(warp);
            }
        }
    }

    private void openWarpEditMenu(Warp warp) {
        Component title = miniMessage.deserialize("<dark_gray>Edit: " + warp.getName());
        Inventory inv = Bukkit.createInventory(this, 27, title);
        editInventories.put(inv, warp);

        inv.setItem(10, createEditItem(HeadUtils.ENABLED_ICON, "§e§lᴛᴏɢɢʟᴇ ᴇɴᴀʙʟᴇᴅ",
                "§7ᴄᴜʀʀᴇɴᴛ: " + (warp.isEnabled() ? "§a§lᴇɴᴀʙʟᴇᴅ" : "§c§lᴅɪꜱᴀʙʟᴇᴅ")));
        inv.setItem(12, createEditItem(HeadUtils.PERMISSION_ICON, "§e§lᴘᴇʀᴍɪꜱꜱɪᴏɴ",
                "§7ᴄᴜʀʀᴇɴᴛ: " + (warp.getPermission() != null
                        ? "§a§lᴇɴᴀʙʟᴇᴅ §7(" + warp.getPermission() + ")"
                        : "§c§lᴅɪꜱᴀʙʟᴇᴅ"),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ ᴘᴇʀᴍɪꜱꜱɪᴏɴ ʀᴇQᴜɪʀᴇᴍᴇɴᴛ"));
        inv.setItem(14, createDisplayNameItem(warp));
        inv.setItem(16, createIconItem(warp));

        inv.setItem(20, createEditItem(HeadUtils.COOLDOWN_ICON, "§e§lꜱᴇᴛ ᴄᴏᴏʟᴅᴏᴡɴ",
                "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ §8(§f§l" + warp.getCooldown() + "s§8)"));
        inv.setItem(22, createEditItem(HeadUtils.DELAY_ICON, "§e§lꜱᴇᴛ ᴅᴇʟᴀʏ",
                "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ §8(§f§l" + warp.getDelay() + "s§8)"));
        inv.setItem(24, createEditItem(HeadUtils.CREATE_ICON, "§e§lꜱᴇᴛ ʟᴏᴄᴀᴛɪᴏɴ",
                "§aᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ ᴛᴏ ʏᴏᴜʀ ʟᴏᴄᴀᴛɪᴏɴ"));

        inv.setItem(26, createEditItem(HeadUtils.DELETE_ICON, "§c§lᴅᴇʟᴇᴛᴇ ᴡᴀʀᴘ",
                "§4ᴄʟɪᴄᴋ ᴛᴏ ᴅᴇʟᴇᴛᴇ ᴛʜɪꜱ ᴡᴀʀᴘ"));
        inv.setItem(18, createEditItem(HeadUtils.BACK_ICON, "§e§lʙᴀᴄᴋ ᴛᴏ ᴀᴅᴍɪɴ ᴍᴇɴᴜ"));

        fillBorders(inv);
        player.openInventory(inv);
    }

    private ItemStack createIconItem(Warp warp) {
        ItemStack icon = warp.getIcon();
        if (icon == null) {
            icon = HeadUtils.getHead(HeadUtils.WARP_ICON);
        } else {
            icon = icon.clone();
        }
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName("§d§lᴄᴜꜱᴛᴏᴍ ɪᴄᴏɴ");
        List<String> lore = new ArrayList<>();
        lore.add("§7ᴅʀᴀɢ ᴀɴ ɪᴛᴇᴍ ꜰʀᴏᴍ ʏᴏᴜʀ ɪɴᴠᴇɴᴛᴏʀʏ");
        lore.add("§7ᴏɴᴛᴏ ᴛʜɪꜱ ꜱʟᴏᴛ ᴛᴏ ꜱᴇᴛ ɪᴛ ᴀꜱ ᴛʜᴇ ᴡᴀʀᴘ ɪᴄᴏɴ");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createDisplayNameItem(Warp warp) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lꜱᴇᴛ ᴅɪꜱᴘʟᴀʏ ɴᴀᴍᴇ");
        List<String> lore = new ArrayList<>();
        String raw = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        String display = ChatColor.translateAlternateColorCodes('&', raw);
        lore.add("§f§lᴄᴜʀʀᴇɴᴛ: §f§l" + display);
        lore.add("§7ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ ᴅɪꜱᴘʟᴀʏ ɴᴀᴍᴇ ᴠɪᴀ ᴄʜᴀᴛ");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleEditClick(int slot, Warp warp) {
        switch (slot) {
            case 10:
                warp.setEnabled(!warp.isEnabled());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited")
                        .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
                openWarpEditMenu(warp);
                break;
            case 12:
                if (warp.getPermission() == null) {
                    warp.setPermission("ultimatewarps.warp." + warp.getName());
                } else {
                    warp.setPermission(null);
                }
                warp.save();
                player.sendMessage(config.getMessage("warp-edited")
                        .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
                openWarpEditMenu(warp);
                break;
            case 14:
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        openWarpEditMenu(warp);
                        return;
                    }
                    warp.setDisplayName(input);
                    warp.save();
                    player.sendMessage(config.getMessage("warp-edited")
                            .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
                    openWarpEditMenu(warp);
                });
                break;
            case 16:
                // handled by GUIListener
                break;
            case 20:
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        openWarpEditMenu(warp);     // ← cancel goes back to edit menu
                        return;
                    }
                    try {
                        int cd = Integer.parseInt(input);
                        warp.setCooldown(cd);
                        warp.save();
                        player.sendMessage(config.getMessage("warp-edited")
                                .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Invalid number. Enter seconds.", NamedTextColor.RED));
                    }
                    openWarpEditMenu(warp);
                });
                break;
            case 22:
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        openWarpEditMenu(warp);     // ← cancel goes back to edit menu
                        return;
                    }
                    try {
                        int del = Integer.parseInt(input);
                        warp.setDelay(del);
                        warp.save();
                        player.sendMessage(config.getMessage("warp-edited")
                                .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Invalid number. Enter seconds.", NamedTextColor.RED));
                    }
                    openWarpEditMenu(warp);
                });
                break;
            case 24:
                warp.setLocation(player.getLocation());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited")
                        .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
                openWarpEditMenu(warp);
                break;
            case 26:
                UltimateWarps.getInstance().getWarpManager().removeWarp(warp.getName());
                player.sendMessage(config.getMessage("warp-deleted")
                        .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
                player.closeInventory();
                // Refresh the warp list and adjust page if needed
                warps = new ArrayList<>(UltimateWarps.getInstance().getWarpManager().getAllWarps());
                if (page > 0 && page * warpSlots >= warps.size()) {
                    page--;
                }
                open(page);
                break;
            case 18:
                open(page);
                break;
        }
    }

    public void setIcon(Warp warp, ItemStack cursorItem) {
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            warp.setIcon(cursorItem.clone());
            warp.save();
            player.setItemOnCursor(null);
            player.sendMessage(config.getMessage("warp-edited")
                    .replaceText(b -> b.matchLiteral("%name%").replacement(warp.getName())));
            openWarpEditMenu(warp);
        }
    }

    private ItemStack createEditItem(String texture, String name, String... lore) {
        ItemStack item = HeadUtils.getHead(texture);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> loreList = new ArrayList<>();
        for (String s : lore) loreList.add(s);
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavButton(String texture, String name) { return createEditItem(texture, name); }
    private ItemStack createCreateButton() { return createEditItem(HeadUtils.CREATE_ICON, "§a§lᴄʀᴇᴀᴛᴇ ɴᴇᴡ ᴡᴀʀᴘ"); }
    private ItemStack createCloseButton() { return createEditItem(HeadUtils.CLOSE_ICON, "§c§lᴄʟᴏꜱᴇ"); }

    private void fillBorders(Inventory inv) {
        int invSize = inv.getSize();
        ItemStack topFiller = new ItemStack(config.topFillerMaterial());
        ItemMeta topMeta = topFiller.getItemMeta();
        topMeta.setDisplayName(" ");
        topFiller.setItemMeta(topMeta);
        ItemStack middleFiller = new ItemStack(config.middleFillerMaterial());
        ItemMeta midMeta = middleFiller.getItemMeta();
        midMeta.setDisplayName(" ");
        middleFiller.setItemMeta(midMeta);
        for (int i = 0; i < invSize; i++) {
            if (inv.getItem(i) != null) continue;
            if (i < 9 || i >= invSize - 9) {
                inv.setItem(i, topFiller);
            } else {
                inv.setItem(i, middleFiller);
            }
        }
    }

    @Override public Inventory getInventory() { return null; }
    public static Warp getEditingWarp(Inventory inv) { return editInventories.get(inv); }
    public static void removeEditingInventory(Inventory inv) { editInventories.remove(inv); }
}