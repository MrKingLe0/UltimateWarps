package com.ultimatewarps.gui;

import com.ultimatewarps.ChatInput;
import com.ultimatewarps.ConfigManager;
import com.ultimatewarps.HeadUtils;
import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.Warp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
    private static final Map<Inventory, Warp> editInventories = new HashMap<>();
    // Bug fix: getInventory() used to unconditionally return null, breaking the
    // InventoryHolder contract (Bukkit internals and other plugins may call
    // holder.getInventory() and NPE on the result). Track whichever inventory this
    // holder most recently opened and return that instead.
    private Inventory currentInventory;

    public AdminGUI(Player player) {
        this.player = player;
        this.config = UltimateWarps.getInstance().getConfigManager();
        this.size = config.guiSize();
        this.warpSlots = size - 18;
        this.warps = new ArrayList<>(UltimateWarps.getInstance().getWarpManager().getAllWarps());
    }

    public void open(int page) {
        this.page = page;
        Component title = TextFormat.render(config.adminGuiTitle());
        Inventory inv = Bukkit.createInventory(this, size, title);
        this.currentInventory = inv;
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
            // Bug fix: this used to concatenate a hardcoded "&6&l" prefix with the warp's
            // display name into one raw string before a single render pass. That breaks
            // the moment the display name uses a different format than the hardcoded
            // prefix (e.g. a MiniMessage-tagged display name with a legacy '&' prefix) -
            // per Adventure's own guidance, mixing MiniMessage and legacy formatting in
            // one string has no supported, correct behavior. TextFormat.renderTemplate()
            // renders the prefix and the display name as separate Components and composes
            // them, so each is parsed correctly regardless of what format the other uses.
            String raw = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
            meta.displayName(TextFormat.renderTemplate("&6&l{name}", "{name}", raw));

            List<Component> lore = new ArrayList<>();
            lore.add(TextFormat.render("§f§lᴇɴᴀʙʟᴇᴅ: " + (warp.isEnabled() ? "§a§lʏᴇꜱ" : "§c§lɴᴏ")));
            lore.add(TextFormat.render("§7ᴄᴏᴏʟᴅᴏᴡɴ: §f" + warp.getCooldown() + "s"));
            lore.add(TextFormat.render("§7ᴅᴇʟᴀʏ: §f" + warp.getDelay() + "s"));
            lore.add(TextFormat.render("§7ᴘᴇʀᴍɪꜱꜱɪᴏɴ: §f" + (warp.getPermission() != null ? warp.getPermission() : "ɴᴏɴᴇ")));
            lore.add(Component.empty());
            lore.add(TextFormat.render("§e§lᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ ᴛʜɪꜱ ᴡᴀʀᴘ"));
            meta.lore(lore);
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
                if (!UltimateWarps.getInstance().getWarpManager().isValidWarpName(name)) {
                    player.sendMessage(Component.text(
                        "Invalid warp name. Use only letters, numbers, underscores and hyphens (max 32 characters).",
                        NamedTextColor.RED));
                    open(page);
                    return;
                }
                Warp newWarp = new Warp(name, player.getLocation());
                newWarp.setCooldown(config.globalDefaultCooldown());
                newWarp.setDelay(config.globalDefaultDelay());
                UltimateWarps.getInstance().getWarpManager().addWarp(newWarp);
                // Bug fix: this used to call .replaceText() on an already-parsed
                // Component, which just finds the literal text "%name%" inside the
                // rendered tree and swaps in plain, unformatted text - it has nothing to
                // do with MiniMessage parsing, so this never showed any of the warp's
                // own formatting and could land the replacement inside whatever style
                // happened to be active in the tree at that point. getMessage(path,
                // placeholder, value) renders the template and the value correctly as
                // one document via TextFormat.renderTemplate().
                player.sendMessage(config.getMessage("warp-created", "name", name));
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
        Component title = TextFormat.renderTemplate("<dark_gray>Edit: {name}", "{name}", warp.getName());
        Inventory inv = Bukkit.createInventory(this, 27, title);
        this.currentInventory = inv;
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
        meta.displayName(TextFormat.render("§d§lᴄᴜꜱᴛᴏᴍ ɪᴄᴏɴ"));
        List<Component> lore = new ArrayList<>();
        lore.add(TextFormat.render("§7ᴅʀᴀɢ ᴀɴ ɪᴛᴇᴍ ꜰʀᴏᴍ ʏᴏᴜʀ ɪɴᴠᴇɴᴛᴏʀʏ"));
        lore.add(TextFormat.render("§7ᴏɴᴛᴏ ᴛʜɪꜱ ꜱʟᴏᴛ ᴛᴏ ꜱᴇᴛ ɪᴛ ᴀꜱ ᴛʜᴇ ᴡᴀʀᴘ ɪᴄᴏɴ"));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createDisplayNameItem(Warp warp) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render("§e§lꜱᴇᴛ ᴅɪꜱᴘʟᴀʏ ɴᴀᴍᴇ"));
        List<Component> lore = new ArrayList<>();
        // Bug fix: this used to concatenate a hardcoded "§f§l" prefix with the display
        // name into one raw string before a single render pass, breaking the moment the
        // display name used a different format than the hardcoded prefix (e.g. a
        // MiniMessage-tagged display name with a legacy '§' prefix).
        // TextFormat.renderTemplate() renders the prefix and the display name as separate
        // Components and composes them, so each is parsed correctly regardless of format.
        String raw = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        lore.add(TextFormat.renderTemplate("§f§lᴄᴜʀʀᴇɴᴛ: {name}", "{name}", raw));
        lore.add(TextFormat.render("§7ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ ᴅɪꜱᴘʟᴀʏ ɴᴀᴍᴇ ᴠɪᴀ ᴄʜᴀᴛ"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleEditClick(int slot, Warp warp) {
        switch (slot) {
            case 10:
                warp.setEnabled(!warp.isEnabled());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                openWarpEditMenu(warp);
                break;
            case 12:
                if (warp.getPermission() == null) {
                    warp.setPermission("ultimatewarps.warp." + warp.getName());
                } else {
                    warp.setPermission(null);
                }
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
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
                    player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
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
                        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
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
                        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Invalid number. Enter seconds.", NamedTextColor.RED));
                    }
                    openWarpEditMenu(warp);
                });
                break;
            case 24:
                warp.setLocation(player.getLocation());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                openWarpEditMenu(warp);
                break;
            case 26:
                UltimateWarps.getInstance().getWarpManager().removeWarp(warp.getName());
                player.sendMessage(config.getMessage("warp-deleted", "name", warp.getName()));
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
            player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
            openWarpEditMenu(warp);
        }
    }

    private ItemStack createEditItem(String texture, String name, String... lore) {
        ItemStack item = HeadUtils.getHead(texture);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render(name));
        List<Component> loreList = new ArrayList<>();
        for (String s : lore) loreList.add(TextFormat.render(s));
        meta.lore(loreList);
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

    @Override public Inventory getInventory() { return currentInventory; }
    public static Warp getEditingWarp(Inventory inv) { return editInventories.get(inv); }
    public static void removeEditingInventory(Inventory inv) { editInventories.remove(inv); }
}