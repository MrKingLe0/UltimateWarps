package com.ultimatewarps.gui;

import com.ultimatewarps.ChatInput;
import com.ultimatewarps.ConfigManager;
import com.ultimatewarps.HeadUtils;
import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.Warp;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    private static final int ENABLED_SLOT     = 11;
    private static final int PERMISSION_SLOT  = 12;
    private static final int DISPLAYNAME_SLOT = 13;

    public  static final int EDIT_ICON_SLOT   = 14;
    private static final int COOLDOWN_SLOT    = 15;
    private static final int GLOW_SLOT        = 21;
    private static final int DELAY_SLOT       = 20;
    private static final int LOCATION_SLOT    = 31;
    private static final int LORE_SLOT        = 22;
    private static final int EFFECTS_SLOT     = 23;
    private static final int SLOTPOS_SLOT     = 24;
    private static final int BACK_SLOT        = 27;
    private static final int DELETE_SLOT      = 35;

    private static final int EFF_MASTER    = 10;
    private static final int EFF_P_ENABLED = 12;
    private static final int EFF_P_TYPE    = 13;
    private static final int EFF_P_COUNT   = 14;
    private static final int EFF_P_COLOR   = 15;
    private static final int EFF_S_ENABLED = 21;
    private static final int EFF_S_TYPE    = 22;
    private static final int EFF_S_VOLUME  = 23;
    private static final int EFF_S_PITCH   = 24;
    private static final int EFF_BACK      = 31;

    private static final int PICKER_SIZE     = 54;
    private static final int PICKER_PER_PAGE = 45;
    private static final int PICK_PREV = 45;
    private static final int PICK_CHAT = 47;
    private static final int PICK_BACK = 49;
    private static final int PICK_INFO = 51;
    private static final int PICK_NEXT = 53;

    public enum MenuType { EDIT, EFFECTS, SLOT_PICKER, EFFECT_PARTICLE, EFFECT_SOUND, EFFECT_COLOR }

    public record MenuState(MenuType type, Warp warp, int page) {}
    private static final Map<Inventory, MenuState> menuStates = new HashMap<>();

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

            ItemStack head = HeadUtils.cleanIcon(warp.getIcon());
            if (head == null) {
                head = HeadUtils.getHead(HeadUtils.WARP_ICON);
            }

            ItemMeta meta = head.getItemMeta();

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
                    player.sendMessage(TextFormat.render("<#A98BFF>ᴀ ᴡᴀʀᴘ ᴡɪᴛʜ ᴛʜᴀᴛ ɴᴀᴍᴇ ᴀʟʀᴇᴀᴅʏ ᴇxɪꜱᴛꜱ."));
                    open(page);
                    return;
                }
                if (!UltimateWarps.getInstance().getWarpManager().isValidWarpName(name)) {
                    player.sendMessage(TextFormat.render(
                        "<#A98BFF>ɪɴᴠᴀʟɪᴅ ᴡᴀʀᴘ ɴᴀᴍᴇ. ᴜꜱᴇ ᴏɴʟʏ ʟᴇᴛᴛᴇʀꜱ, ɴᴜᴍʙᴇʀꜱ, ᴜɴᴅᴇʀꜱᴄᴏʀᴇꜱ ᴀɴᴅ ʜʏᴘʜᴇɴꜱ (ᴍᴀx 32 ᴄʜᴀʀᴀᴄᴛᴇʀꜱ)."));
                    open(page);
                    return;
                }
                Warp newWarp = new Warp(name, player.getLocation());
                newWarp.setCooldown(config.globalDefaultCooldown());
                newWarp.setDelay(config.globalDefaultDelay());
                UltimateWarps.getInstance().getWarpManager().addWarp(newWarp);

                player.sendMessage(config.getMessage("warp-created", "name", name));

                warps = new ArrayList<>(UltimateWarps.getInstance().getWarpManager().getAllWarps());

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
        Inventory inv = Bukkit.createInventory(this, 36, title);
        this.currentInventory = inv;
        putMenu(inv, MenuType.EDIT, warp);

        inv.setItem(ENABLED_SLOT, createEditItem(HeadUtils.ENABLED_ICON, "§e§lᴛᴏɢɢʟᴇ ᴇɴᴀʙʟᴇᴅ",
                "§7ᴄᴜʀʀᴇɴᴛ: " + (warp.isEnabled() ? "§a§lᴇɴᴀʙʟᴇᴅ" : "§c§lᴅɪꜱᴀʙʟᴇᴅ")));
        inv.setItem(PERMISSION_SLOT, createEditItem(HeadUtils.PERMISSION_ICON, "§e§lᴘᴇʀᴍɪꜱꜱɪᴏɴ",
                "§7ᴄᴜʀʀᴇɴᴛ: " + (warp.getPermission() != null
                        ? "§a§lᴇɴᴀʙʟᴇᴅ §7(" + warp.getPermission() + ")"
                        : "§c§lᴅɪꜱᴀʙʟᴇᴅ"),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ ᴘᴇʀᴍɪꜱꜱɪᴏɴ ʀᴇQᴜɪʀᴇᴍᴇɴᴛ"));
        inv.setItem(DISPLAYNAME_SLOT, createDisplayNameItem(warp));
        inv.setItem(EDIT_ICON_SLOT, createIconItem(warp));
        inv.setItem(COOLDOWN_SLOT, createEditItem(HeadUtils.COOLDOWN_ICON, "§e§lꜱᴇᴛ ᴄᴏᴏʟᴅᴏᴡɴ",
                "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ §8(§f§l" + warp.getCooldown() + "s§8)"));
        inv.setItem(GLOW_SLOT, createMaterialItem(Material.GLOWSTONE_DUST, "§e§lɪᴄᴏɴ ɢʟᴏᴡ",
                "§7ᴄᴜʀʀᴇɴᴛ: " + (warp.isGlow() ? "§a§lᴏɴ" : "§c§lᴏꜰꜰ"),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ ᴛʜᴇ ᴇɴᴄʜᴀɴᴛ ɢʟɪɴᴛ"));

        inv.setItem(DELAY_SLOT, createEditItem(HeadUtils.DELAY_ICON, "§e§lꜱᴇᴛ ᴅᴇʟᴀʏ",
                "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ §8(§f§l" + warp.getDelay() + "s§8)"));
        inv.setItem(LOCATION_SLOT, createEditItem(HeadUtils.CREATE_ICON, "§e§lꜱᴇᴛ ʟᴏᴄᴀᴛɪᴏɴ",
                "§aᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ ᴛᴏ ʏᴏᴜʀ ʟᴏᴄᴀᴛɪᴏɴ"));
        inv.setItem(LORE_SLOT, createLoreItem(warp));
        inv.setItem(EFFECTS_SLOT, createEffectsButton(warp));
        inv.setItem(SLOTPOS_SLOT, createSlotPosButton(warp));

        inv.setItem(BACK_SLOT, createEditItem(HeadUtils.BACK_ICON, "§e§lʙᴀᴄᴋ ᴛᴏ ᴀᴅᴍɪɴ ᴍᴇɴᴜ"));
        inv.setItem(DELETE_SLOT, createEditItem(HeadUtils.DELETE_ICON, "§c§lᴅᴇʟᴇᴛᴇ ᴡᴀʀᴘ",
                "§4ᴄʟɪᴄᴋ ᴛᴏ ᴅᴇʟᴇᴛᴇ ᴛʜɪꜱ ᴡᴀʀᴘ"));

        fillBorders(inv);
        player.openInventory(inv);
    }

    private ItemStack createLoreItem(Warp warp) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render("§e§lꜱᴇᴛ ᴄᴜꜱᴛᴏᴍ ʟᴏʀᴇ"));
        List<Component> lore = new ArrayList<>();
        List<String> current = warp.getCustomLore();
        if (current.isEmpty()) {
            lore.add(TextFormat.render("§7ᴄᴜʀʀᴇɴᴛ: §8(ɴᴏɴᴇ)"));
        } else {
            lore.add(TextFormat.render("§7ᴄᴜʀʀᴇɴᴛ:"));
            for (String line : current) {

                lore.add(TextFormat.renderTemplate("§8│ {line}", "{line}", line));
            }
        }
        lore.add(Component.empty());
        lore.add(TextFormat.render("§7ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ ᴠɪᴀ ᴄʜᴀᴛ"));
        lore.add(TextFormat.render("§8ᴜꜱᴇ §f|§8 ᴛᴏ ꜱᴇᴘᴀʀᴀᴛᴇ ʟɪɴᴇꜱ"));
        lore.add(TextFormat.render("§8§oclear§8 ᴛᴏ ʀᴇᴍᴏᴠᴇ · §8§ocancel§8 ᴛᴏ ᴀʙᴏʀᴛ"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEffectsButton(Warp warp) {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render("§e§lᴛᴇʟᴇᴘᴏʀᴛ ᴇꜰꜰᴇᴄᴛꜱ"));
        List<Component> lore = new ArrayList<>();
        lore.add(TextFormat.render("§7ᴍᴏᴅᴇ: " + (warp.isCustomEffects() ? "§a§lᴄᴜꜱᴛᴏᴍ" : "§7§lɢʟᴏʙᴀʟ")));
        lore.add(Component.empty());
        lore.add(TextFormat.render("§7ᴄʟɪᴄᴋ ᴛᴏ ᴄᴏɴꜰɪɢᴜʀᴇ ᴘᴇʀ-ᴡᴀʀᴘ"));
        lore.add(TextFormat.render("§7ᴘᴀʀᴛɪᴄʟᴇ & ꜱᴏᴜɴᴅ ᴏɴ ᴀʀʀɪᴠᴀʟ"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSlotPosButton(Warp warp) {
        ItemStack item = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render("§e§lᴘɪɴ ᴛᴏ ꜱʟᴏᴛ"));
        List<Component> lore = new ArrayList<>();
        lore.add(TextFormat.render("§7ᴄᴜʀʀᴇɴᴛ: " + (warp.getSlot() >= 0 ? "§a§lꜱʟᴏᴛ " + warp.getSlot() : "§7§lᴀᴜᴛᴏ")));
        lore.add(Component.empty());
        lore.add(TextFormat.render("§7ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴏᴏꜱᴇ ᴀ ꜱʟᴏᴛ"));
        lore.add(TextFormat.render("§7ɪɴ ᴛʜᴇ ᴘʟᴀʏᴇʀ ᴡᴀʀᴘ ᴍᴇɴᴜ"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createIconItem(Warp warp) {
        ItemStack icon = HeadUtils.cleanIcon(warp.getIcon());
        if (icon == null) {
            icon = HeadUtils.getHead(HeadUtils.WARP_ICON);
        }
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(TextFormat.render("§d§lᴄᴜꜱᴛᴏᴍ ɪᴄᴏɴ"));
        List<Component> lore = new ArrayList<>();
        lore.add(TextFormat.render("§7ᴅʀᴀɢ ᴀɴ ɪᴛᴇᴍ ꜰʀᴏᴍ ʏᴏᴜʀ ɪɴᴠᴇɴᴛᴏʀʏ"));
        lore.add(TextFormat.render("§7ᴏɴᴛᴏ ᴛʜɪꜱ ꜱʟᴏᴛ ᴛᴏ ꜱᴇᴛ ɪᴛ ᴀꜱ ᴛʜᴇ ᴡᴀʀᴘ ɪᴄᴏɴ"));
        meta.lore(lore);

        if (warp.isGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createDisplayNameItem(Warp warp) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render("§e§lꜱᴇᴛ ᴅɪꜱᴘʟᴀʏ ɴᴀᴍᴇ"));
        List<Component> lore = new ArrayList<>();

        String raw = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        lore.add(TextFormat.renderTemplate("§f§lᴄᴜʀʀᴇɴᴛ: {name}", "{name}", raw));
        lore.add(TextFormat.render("§7ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ ᴅɪꜱᴘʟᴀʏ ɴᴀᴍᴇ ᴠɪᴀ ᴄʜᴀᴛ"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleEditClick(int slot, Warp warp) {
        switch (slot) {
            case ENABLED_SLOT:
                warp.setEnabled(!warp.isEnabled());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                openWarpEditMenu(warp);
                break;
            case PERMISSION_SLOT:
                if (warp.getPermission() == null) {
                    warp.setPermission("ultimatewarps.warp." + warp.getName());
                } else {
                    warp.setPermission(null);
                }
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                openWarpEditMenu(warp);
                break;
            case DISPLAYNAME_SLOT:
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
            case EDIT_ICON_SLOT:

                break;
            case GLOW_SLOT:
                warp.setGlow(!warp.isGlow());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                openWarpEditMenu(warp);
                break;
            case COOLDOWN_SLOT:
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        openWarpEditMenu(warp);
                        return;
                    }
                    try {
                        int cd = Integer.parseInt(input.trim());
                        if (cd < 0) {
                            player.sendMessage(TextFormat.render("<#A98BFF>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴇɴᴛᴇʀ 0 ᴏʀ ᴍᴏʀᴇ ꜱᴇᴄᴏɴᴅꜱ."));
                            openWarpEditMenu(warp);
                            return;
                        }
                        warp.setCooldown(cd);
                        warp.save();
                        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextFormat.render("<#A98BFF>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴇɴᴛᴇʀ ꜱᴇᴄᴏɴᴅꜱ."));
                    }
                    openWarpEditMenu(warp);
                });
                break;
            case DELAY_SLOT:
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        openWarpEditMenu(warp);
                        return;
                    }
                    try {
                        int del = Integer.parseInt(input.trim());
                        if (del < 0) {
                            player.sendMessage(TextFormat.render("<#A98BFF>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴇɴᴛᴇʀ 0 ᴏʀ ᴍᴏʀᴇ ꜱᴇᴄᴏɴᴅꜱ."));
                            openWarpEditMenu(warp);
                            return;
                        }
                        warp.setDelay(del);
                        warp.save();
                        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextFormat.render("<#A98BFF>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ. ᴇɴᴛᴇʀ ꜱᴇᴄᴏɴᴅꜱ."));
                    }
                    openWarpEditMenu(warp);
                });
                break;
            case LOCATION_SLOT:
                warp.setLocation(player.getLocation());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                openWarpEditMenu(warp);
                break;
            case LORE_SLOT:
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        openWarpEditMenu(warp);
                        return;
                    }
                    if (input.equalsIgnoreCase("clear")) {
                        warp.setCustomLore(new ArrayList<>());
                    } else {
                        List<String> lines = new ArrayList<>();
                        for (String part : input.split("\\|")) {
                            lines.add(part.trim());
                        }
                        warp.setCustomLore(lines);
                    }
                    warp.save();
                    player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                    openWarpEditMenu(warp);
                });
                break;
            case EFFECTS_SLOT:
                openEffectsMenu(warp);
                break;
            case SLOTPOS_SLOT:
                openSlotPickerMenu(warp);
                break;
            case DELETE_SLOT:
                UltimateWarps.getInstance().getWarpManager().removeWarp(warp.getName());
                player.sendMessage(config.getMessage("warp-deleted", "name", warp.getName()));
                player.closeInventory();

                warps = new ArrayList<>(UltimateWarps.getInstance().getWarpManager().getAllWarps());
                if (page > 0 && page * warpSlots >= warps.size()) {
                    page--;
                }
                open(page);
                break;
            case BACK_SLOT:
                open(page);
                break;
        }
    }

    private void openEffectsMenu(Warp warp) {
        Component title = TextFormat.renderTemplate("<dark_gray>Effects: {name}", "{name}", warp.getName());
        Inventory inv = Bukkit.createInventory(this, 36, title);
        this.currentInventory = inv;
        putMenu(inv, MenuType.EFFECTS, warp);

        boolean custom = warp.isCustomEffects();

        inv.setItem(EFF_MASTER, createEditItem(custom ? HeadUtils.EFFECT_MODE_CUSTOM_ICON : HeadUtils.EFFECT_MODE_DEFAULT_ICON, "§e§lᴇꜰꜰᴇᴄᴛ ᴍᴏᴅᴇ",
                "§7ᴄᴜʀʀᴇɴᴛ: " + (custom ? "§a§lᴄᴜꜱᴛᴏᴍ" : "§7§lɢʟᴏʙᴀʟ"),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ᴜꜱᴇ " + (custom ? "ɢʟᴏʙᴀʟ ᴇꜰꜰᴇᴄᴛꜱ" : "ᴄᴜꜱᴛᴏᴍ ᴇꜰꜰᴇᴄᴛꜱ")));

        inv.setItem(EFF_P_ENABLED, createMaterialItem(Material.BLAZE_POWDER, "§e§lᴘᴀʀᴛɪᴄʟᴇ",
                "§7ᴇɴᴀʙʟᴇᴅ: " + (warp.isEffectParticleEnabled() ? "§a§lʏᴇꜱ" : "§c§lɴᴏ")));
        inv.setItem(EFF_P_TYPE, createMaterialItem(Material.FIREWORK_STAR, "§e§lᴘᴀʀᴛɪᴄʟᴇ ᴛʏᴘᴇ",
                "§7ᴄᴜʀʀᴇɴᴛ: §f" + warp.getEffectParticleType(),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴏᴏꜱᴇ"));
        inv.setItem(EFF_P_COUNT, createMaterialItem(Material.REPEATER, "§e§lᴘᴀʀᴛɪᴄʟᴇ ᴄᴏᴜɴᴛ",
                "§7ᴄᴜʀʀᴇɴᴛ: §f" + warp.getEffectParticleCount(),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ ᴠɪᴀ ᴄʜᴀᴛ"));
        inv.setItem(EFF_P_COLOR, createMaterialItem(Material.RED_DYE, "§e§lᴅᴜꜱᴛ ᴄᴏʟᴏʀ",
                "§7ᴄᴜʀʀᴇɴᴛ: §f" + warp.getEffectDustColor(),
                "§7ꜰᴏʀ ᴛʜᴇ §fDUST§7 ᴘᴀʀᴛɪᴄʟᴇ",
                "§7ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴏᴏꜱᴇ"));

        inv.setItem(EFF_S_ENABLED, createMaterialItem(Material.NOTE_BLOCK, "§e§lꜱᴏᴜɴᴅ",
                "§7ᴇɴᴀʙʟᴇᴅ: " + (warp.isEffectSoundEnabled() ? "§a§lʏᴇꜱ" : "§c§lɴᴏ")));
        inv.setItem(EFF_S_TYPE, createMaterialItem(Material.JUKEBOX, "§e§lꜱᴏᴜɴᴅ ᴛʏᴘᴇ",
                "§7ᴄᴜʀʀᴇɴᴛ: §f" + warp.getEffectSoundType(),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴏᴏꜱᴇ"));
        inv.setItem(EFF_S_VOLUME, createMaterialItem(Material.IRON_NUGGET, "§e§lꜱᴏᴜɴᴅ ᴠᴏʟᴜᴍᴇ",
                "§7ᴄᴜʀʀᴇɴᴛ: §f" + warp.getEffectSoundVolume(),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ ᴠɪᴀ ᴄʜᴀᴛ"));
        inv.setItem(EFF_S_PITCH, createMaterialItem(Material.GOLD_NUGGET, "§e§lꜱᴏᴜɴᴅ ᴘɪᴛᴄʜ",
                "§7ᴄᴜʀʀᴇɴᴛ: §f" + warp.getEffectSoundPitch(),
                "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ ᴠɪᴀ ᴄʜᴀᴛ"));

        inv.setItem(EFF_BACK, createEditItem(HeadUtils.BACK_ICON, "§e§lʙᴀᴄᴋ ᴛᴏ ᴇᴅɪᴛᴏʀ"));

        fillBorders(inv);
        player.openInventory(inv);
    }

    public void handleEffectsClick(int slot, Warp warp) {
        switch (slot) {
            case EFF_MASTER:
                if (!warp.isCustomEffects()) {
                    warp.setCustomEffects(true);

                    warp.seedEffectsFromGlobal(config);
                } else {
                    warp.setCustomEffects(false);
                }
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
                openEffectsMenu(warp);
                break;
            case EFF_BACK:
                openWarpEditMenu(warp);
                break;
            case EFF_P_ENABLED:
                if (requireCustom(warp)) {
                    warp.setEffectParticleEnabled(!warp.isEffectParticleEnabled());
                    saveAndReopenEffects(warp);
                }
                break;
            case EFF_S_ENABLED:
                if (requireCustom(warp)) {
                    warp.setEffectSoundEnabled(!warp.isEffectSoundEnabled());
                    saveAndReopenEffects(warp);
                }
                break;
            case EFF_P_TYPE:
                if (requireCustom(warp)) openParticlePickerMenu(warp);
                break;
            case EFF_P_COUNT:
                if (requireCustom(warp)) editParticleCount(warp);
                break;
            case EFF_P_COLOR:
                if (requireCustom(warp)) openColorPickerMenu(warp);
                break;
            case EFF_S_TYPE:
                if (requireCustom(warp)) openSoundPickerMenu(warp);
                break;
            case EFF_S_VOLUME:
                if (requireCustom(warp)) editSoundValue(warp, true);
                break;
            case EFF_S_PITCH:
                if (requireCustom(warp)) editSoundValue(warp, false);
                break;
        }
    }

    private boolean requireCustom(Warp warp) {
        if (!warp.isCustomEffects()) {
            player.sendMessage(TextFormat.render("<#A98BFF>ᴇɴᴀʙʟᴇ ᴄᴜꜱᴛᴏᴍ ᴇꜰꜰᴇᴄᴛꜱ ꜰɪʀꜱᴛ."));
            openEffectsMenu(warp);
            return false;
        }
        return true;
    }

    private void saveAndReopenEffects(Warp warp) {
        warp.save();
        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
        openEffectsMenu(warp);
    }

    private void editParticleType(Warp warp) {
        player.closeInventory();
        ChatInput.waitForInput(player, 60, input -> {
            if (input.equalsIgnoreCase("cancel")) { openEffectsMenu(warp); return; }
            try {
                Particle p = Particle.valueOf(input.trim().toUpperCase(Locale.ROOT));
                warp.setEffectParticleType(p.name());
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
            } catch (IllegalArgumentException e) {
                player.sendMessage(TextFormat.render("<#A98BFF>ᴜɴᴋɴᴏᴡɴ ᴘᴀʀᴛɪᴄʟᴇ: ").append(Component.text(input)));
            }
            openEffectsMenu(warp);
        });
    }

    private void editParticleCount(Warp warp) {
        player.closeInventory();
        ChatInput.waitForInput(player, 60, input -> {
            if (input.equalsIgnoreCase("cancel")) { openEffectsMenu(warp); return; }
            try {
                int c = Integer.parseInt(input.trim());
                if (c < 0) c = 0;
                warp.setEffectParticleCount(c);
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
            } catch (NumberFormatException e) {
                player.sendMessage(TextFormat.render("<#A98BFF>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ."));
            }
            openEffectsMenu(warp);
        });
    }

    private void editDustColor(Warp warp) {
        player.closeInventory();
        ChatInput.waitForInput(player, 60, input -> {
            if (input.equalsIgnoreCase("cancel")) { openEffectsMenu(warp); return; }
            String raw = input.trim();
            if (!raw.startsWith("#") && !raw.contains(",")) {
                player.sendMessage(TextFormat.render("<#A98BFF>ᴜꜱᴇ #RRGGBB ᴏʀ r,g,b ꜰᴏʀᴍᴀᴛ."));
                openEffectsMenu(warp);
                return;
            }
            warp.setEffectDustColor(raw);
            warp.save();
            player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
            openEffectsMenu(warp);
        });
    }

    private void editSoundType(Warp warp) {
        player.closeInventory();
        ChatInput.waitForInput(player, 60, input -> {
            if (input.equalsIgnoreCase("cancel")) { openEffectsMenu(warp); return; }
            Sound s = ConfigManager.parseSound(input.trim(), null);
            if (s == null) {
                player.sendMessage(TextFormat.render("<#A98BFF>ᴜɴᴋɴᴏᴡɴ ꜱᴏᴜɴᴅ: ").append(Component.text(input)));
            } else {
                String key = ConfigManager.soundKey(s);
                if (key != null) warp.setEffectSoundType(key);
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
            }
            openEffectsMenu(warp);
        });
    }

    private void editSoundValue(Warp warp, boolean volume) {
        player.closeInventory();
        ChatInput.waitForInput(player, 60, input -> {
            if (input.equalsIgnoreCase("cancel")) { openEffectsMenu(warp); return; }
            try {
                float v = Float.parseFloat(input.trim());
                if (v < 0) v = 0;
                if (volume) warp.setEffectSoundVolume(v); else warp.setEffectSoundPitch(v);
                warp.save();
                player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
            } catch (NumberFormatException e) {
                player.sendMessage(TextFormat.render("<#A98BFF>ɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ."));
            }
            openEffectsMenu(warp);
        });
    }

    private static volatile List<Particle> PARTICLE_LIST;

    private static synchronized List<Particle> allParticles() {
        if (PARTICLE_LIST == null) {
            List<Particle> list = new ArrayList<>(Arrays.asList(Particle.values()));
            list.sort(Comparator.comparing(Particle::name));
            PARTICLE_LIST = list;
        }
        return PARTICLE_LIST;
    }

    private static final Material[] COLOR_WOOLS = {
        Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL,
        Material.BROWN_WOOL, Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
        Material.LIME_WOOL, Material.GREEN_WOOL, Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL,
        Material.BLUE_WOOL, Material.PURPLE_WOOL, Material.MAGENTA_WOOL, Material.PINK_WOOL
    };
    private static final String[] COLOR_HEX = {
        "#F9FFFE", "#9D9D97", "#474F52", "#1D1D21",
        "#835432", "#B02E26", "#F9801D", "#FED83D",
        "#80C71F", "#5E7C16", "#169C9C", "#3AB3DA",
        "#3C44AA", "#8932B8", "#C74EBD", "#F38BAA"
    };
    private static final String[] COLOR_NAMES = {
        "ᴡʜɪᴛᴇ", "ʟɪɢʜᴛ ɢʀᴀʏ", "ɢʀᴀʏ", "ʙʟᴀᴄᴋ",
        "ʙʀᴏᴡɴ", "ʀᴇᴅ", "ᴏʀᴀɴɢᴇ", "ʏᴇʟʟᴏᴡ",
        "ʟɪᴍᴇ", "ɢʀᴇᴇɴ", "ᴄʏᴀɴ", "ʟɪɢʜᴛ ʙʟᴜᴇ",
        "ʙʟᴜᴇ", "ᴘᴜʀᴘʟᴇ", "ᴍᴀɢᴇɴᴛᴀ", "ᴘɪɴᴋ"
    };

    private void openParticlePickerMenu(Warp warp) {
        openParticlePickerMenu(warp, 0);
    }

    private void openParticlePickerMenu(Warp warp, int page) {
        List<Particle> all = allParticles();
        int totalPages = Math.max(1, (all.size() + PICKER_PER_PAGE - 1) / PICKER_PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        Component title = TextFormat.renderTemplate("<dark_gray>Particle: {name}", "{name}", warp.getName());
        Inventory inv = Bukkit.createInventory(this, PICKER_SIZE, title);
        this.currentInventory = inv;
        putMenu(inv, MenuType.EFFECT_PARTICLE, warp, page);

        String current = warp.getEffectParticleType();
        int start = page * PICKER_PER_PAGE;
        int end = Math.min(start + PICKER_PER_PAGE, all.size());
        for (int i = start; i < end; i++) {
            Particle p = all.get(i);
            boolean selected = p.name().equalsIgnoreCase(current);
            ItemStack item = createMaterialItem(Material.FIREWORK_STAR,
                    (selected ? "§a§l" : "§e§l") + p.name(),
                    selected ? "§a§lꜱᴇʟᴇᴄᴛᴇᴅ" : "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇʟᴇᴄᴛ");
            if (selected) item = withGlint(item);
            inv.setItem(i - start, item);
        }

        addPickerControls(inv, page, totalPages, "§7ꜰᴏʀ ᴀɴʏ ᴘᴀʀᴛɪᴄʟᴇ ɴᴏᴛ ʟɪꜱᴛᴇᴅ");
        fillBorders(inv);
        player.openInventory(inv);
    }

    public void handleParticlePickerClick(int slot, Warp warp, int page) {
        List<Particle> all = allParticles();
        int totalPages = Math.max(1, (all.size() + PICKER_PER_PAGE - 1) / PICKER_PER_PAGE);
        if (slot == PICK_PREV) { if (page > 0) openParticlePickerMenu(warp, page - 1); return; }
        if (slot == PICK_NEXT) { if (page < totalPages - 1) openParticlePickerMenu(warp, page + 1); return; }
        if (slot == PICK_CHAT) { editParticleType(warp); return; }
        if (slot == PICK_BACK) { openEffectsMenu(warp); return; }
        if (slot == PICK_INFO) return;
        if (slot < 0 || slot >= PICKER_PER_PAGE) return;
        int index = page * PICKER_PER_PAGE + slot;
        if (index >= all.size()) return;
        warp.setEffectParticleType(all.get(index).name());
        warp.save();
        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
        openEffectsMenu(warp);
    }

    private void openSoundPickerMenu(Warp warp) {
        openSoundPickerMenu(warp, 0);
    }

    private void openSoundPickerMenu(Warp warp, int page) {
        List<String> sounds = ConfigManager.allSoundKeys();
        int totalPages = Math.max(1, (sounds.size() + PICKER_PER_PAGE - 1) / PICKER_PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        Component title = TextFormat.renderTemplate("<dark_gray>Sound: {name}", "{name}", warp.getName());
        Inventory inv = Bukkit.createInventory(this, PICKER_SIZE, title);
        this.currentInventory = inv;
        putMenu(inv, MenuType.EFFECT_SOUND, warp, page);

        Sound cur = ConfigManager.parseSound(warp.getEffectSoundType(), null);
        String currentKey = ConfigManager.soundKey(cur);

        int start = page * PICKER_PER_PAGE;
        int end = Math.min(start + PICKER_PER_PAGE, sounds.size());
        for (int i = start; i < end; i++) {
            String s = sounds.get(i);
            boolean selected = s.equalsIgnoreCase(currentKey);
            ItemStack item = createMaterialItem(Material.NOTE_BLOCK,
                    (selected ? "§a§l" : "§e§l") + s,
                    selected ? "§a§lꜱᴇʟᴇᴄᴛᴇᴅ" : "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇʟᴇᴄᴛ & ᴘʀᴇᴠɪᴇᴡ");
            if (selected) item = withGlint(item);
            inv.setItem(i - start, item);
        }

        addPickerControls(inv, page, totalPages, "§7ꜰᴏʀ ᴀɴʏ ꜱᴏᴜɴᴅ ɴᴏᴛ ʟɪꜱᴛᴇᴅ");
        fillBorders(inv);
        player.openInventory(inv);
    }

    public void handleSoundPickerClick(int slot, Warp warp, int page) {
        List<String> sounds = ConfigManager.allSoundKeys();
        int totalPages = Math.max(1, (sounds.size() + PICKER_PER_PAGE - 1) / PICKER_PER_PAGE);
        if (slot == PICK_PREV) { if (page > 0) openSoundPickerMenu(warp, page - 1); return; }
        if (slot == PICK_NEXT) { if (page < totalPages - 1) openSoundPickerMenu(warp, page + 1); return; }
        if (slot == PICK_CHAT) { editSoundType(warp); return; }
        if (slot == PICK_BACK) { openEffectsMenu(warp); return; }
        if (slot == PICK_INFO) return;
        if (slot < 0 || slot >= PICKER_PER_PAGE) return;
        int index = page * PICKER_PER_PAGE + slot;
        if (index >= sounds.size()) return;
        String chosen = sounds.get(index);
        warp.setEffectSoundType(chosen);
        warp.save();

        Sound preview = ConfigManager.parseSound(chosen, null);
        if (preview != null) {
            player.playSound(player.getLocation(), preview,
                    warp.getEffectSoundVolume(), warp.getEffectSoundPitch());
        }
        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
        openEffectsMenu(warp);
    }

    private void addPickerControls(Inventory inv, int page, int totalPages, String chatHint) {
        if (page > 0) {
            inv.setItem(PICK_PREV, createEditItem(HeadUtils.PREV_ICON, "§e§lᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ"));
        }
        if (page < totalPages - 1) {
            inv.setItem(PICK_NEXT, createEditItem(HeadUtils.NEXT_ICON, "§e§lɴᴇxᴛ ᴘᴀɢᴇ"));
        }
        inv.setItem(PICK_CHAT, createMaterialItem(Material.WRITABLE_BOOK, "§e§lᴛʏᴘᴇ ɪɴ ᴄʜᴀᴛ", chatHint));
        inv.setItem(PICK_BACK, createEditItem(HeadUtils.BACK_ICON, "§e§lʙᴀᴄᴋ ᴛᴏ ᴇꜰꜰᴇᴄᴛꜱ"));
        inv.setItem(PICK_INFO, createMaterialItem(Material.PAPER,
                "§7ᴘᴀɢᴇ §f" + (page + 1) + "§7 / §f" + totalPages));
    }

    private void openColorPickerMenu(Warp warp) {
        int size = 27;
        Component title = TextFormat.renderTemplate("<dark_gray>Dust color: {name}", "{name}", warp.getName());
        Inventory inv = Bukkit.createInventory(this, size, title);
        this.currentInventory = inv;
        putMenu(inv, MenuType.EFFECT_COLOR, warp);

        String current = warp.getEffectDustColor();
        for (int i = 0; i < COLOR_WOOLS.length; i++) {
            boolean selected = COLOR_HEX[i].equalsIgnoreCase(current);
            ItemStack item = createMaterialItem(COLOR_WOOLS[i],
                    (selected ? "§a§l" : "§e§l") + COLOR_NAMES[i],
                    "§7" + COLOR_HEX[i],
                    "§7ꜰᴏʀ ᴛʜᴇ §fDUST§7 ᴘᴀʀᴛɪᴄʟᴇ",
                    selected ? "§a§lꜱᴇʟᴇᴄᴛᴇᴅ" : "§7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇʟᴇᴄᴛ");
            if (selected) item = withGlint(item);
            inv.setItem(i, item);
        }

        inv.setItem(size - 9, createEditItem(HeadUtils.BACK_ICON, "§e§lʙᴀᴄᴋ ᴛᴏ ᴇꜰꜰᴇᴄᴛꜱ"));
        inv.setItem(size - 1, createMaterialItem(Material.WRITABLE_BOOK, "§e§lᴛʏᴘᴇ ɪɴ ᴄʜᴀᴛ",
                "§7ꜰᴏʀ ᴀ ᴄᴜꜱᴛᴏᴍ §8#RRGGBB§7 ᴏʀ §8r,g,b"));

        fillBorders(inv);
        player.openInventory(inv);
    }

    public void handleColorPickerClick(int slot, Warp warp) {
        int size = 27;
        if (slot == size - 9) { openEffectsMenu(warp); return; }
        if (slot == size - 1) { editDustColor(warp); return; }
        if (slot < 0 || slot >= COLOR_WOOLS.length) return;
        warp.setEffectDustColor(COLOR_HEX[slot]);
        warp.save();
        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
        openEffectsMenu(warp);
    }

    private ItemStack withGlint(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openSlotPickerMenu(Warp warp) {
        UltimateWarps plugin = UltimateWarps.getInstance();
        int pickerRows = WarpGUI.loadConfiguredRows(plugin);
        List<Integer> pickerSlots = WarpGUI.loadConfiguredWarpSlots(plugin);
        int pickerSize = pickerRows * 9;

        Component title = TextFormat.renderTemplate("<dark_gray>Pin slot: {name}", "{name}", warp.getName());
        Inventory inv = Bukkit.createInventory(this, pickerSize, title);
        this.currentInventory = inv;
        putMenu(inv, MenuType.SLOT_PICKER, warp);

        Map<Integer, Warp> pinnedByOthers = new HashMap<>();
        for (Warp w : plugin.getWarpManager().getAllWarps()) {
            if (w.getName().equals(warp.getName())) continue;
            int s = w.getSlot();
            if (s >= 0 && pickerSlots.contains(s) && !pinnedByOthers.containsKey(s)) {
                pinnedByOthers.put(s, w);
            }
        }

        int[] controls = pickerControlSlots(pickerSize, pickerSlots);
        int backSlot = controls[0], resetSlot = controls[1];
        Set<Integer> warpSlotSet = new HashSet<>(pickerSlots);

        for (int slot = 0; slot < pickerSize; slot++) {
            if (slot == backSlot || slot == resetSlot) continue;
            if (!warpSlotSet.contains(slot)) {
                inv.setItem(slot, createMaterialItem(Material.GRAY_STAINED_GLASS_PANE, "§8ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ"));
            } else if (warp.getSlot() == slot) {
                inv.setItem(slot, createMaterialItem(Material.LIME_STAINED_GLASS_PANE, "§a§lᴘɪɴɴᴇᴅ ʜᴇʀᴇ",
                        "§7ꜱʟᴏᴛ §f" + slot,
                        "§7ᴜꜱᴇ §cʀᴇꜱᴇᴛ§7 ᴛᴏ ᴜɴᴘɪɴ"));
            } else if (pinnedByOthers.containsKey(slot)) {
                Warp other = pinnedByOthers.get(slot);
                ItemStack icon = HeadUtils.cleanIcon(other.getIcon());
                if (icon == null) icon = HeadUtils.getHead(HeadUtils.WARP_ICON);
                ItemMeta meta = icon.getItemMeta();
                String otherLabel = other.getDisplayName() != null ? other.getDisplayName() : other.getName();
                meta.displayName(TextFormat.renderTemplate("§c§lᴏᴄᴄᴜᴘɪᴇᴅ §7— {name}", "{name}", otherLabel));
                List<Component> lore = new ArrayList<>();
                lore.add(TextFormat.render("§7ꜱʟᴏᴛ §f" + slot));
                lore.add(TextFormat.render("§7ᴘɪɴɴᴇᴅ ʙʏ ᴀɴᴏᴛʜᴇʀ ᴡᴀʀᴘ"));
                meta.lore(lore);
                icon.setItemMeta(meta);
                inv.setItem(slot, icon);
            } else {
                inv.setItem(slot, createMaterialItem(Material.GREEN_STAINED_GLASS_PANE, "§a§lᴄʟɪᴄᴋ ᴛᴏ ᴘɪɴ ʜᴇʀᴇ",
                        "§7ꜱʟᴏᴛ §f" + slot));
            }
        }

        inv.setItem(backSlot, createEditItem(HeadUtils.BACK_ICON, "§e§lʙᴀᴄᴋ ᴛᴏ ᴇᴅɪᴛᴏʀ"));
        inv.setItem(resetSlot, createMaterialItem(Material.BARRIER, "§c§lʀᴇꜱᴇᴛ ᴛᴏ ᴀᴜᴛᴏ",
                "§7ᴜɴᴘɪɴ — ʟᴇᴛ ᴛʜɪꜱ ᴡᴀʀᴘ ꜰʟᴏᴡ ᴀᴜᴛᴏᴍᴀᴛɪᴄᴀʟʟʏ"));

        player.openInventory(inv);
    }

    public void handleSlotPickerClick(int slot, Warp warp) {
        UltimateWarps plugin = UltimateWarps.getInstance();
        int pickerRows = WarpGUI.loadConfiguredRows(plugin);
        List<Integer> pickerSlots = WarpGUI.loadConfiguredWarpSlots(plugin);
        int pickerSize = pickerRows * 9;
        int[] controls = pickerControlSlots(pickerSize, pickerSlots);
        int backSlot = controls[0], resetSlot = controls[1];

        if (slot == backSlot) {
            openWarpEditMenu(warp);
            return;
        }
        if (slot == resetSlot) {
            warp.setSlot(-1);
            warp.save();
            player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
            openWarpEditMenu(warp);
            return;
        }
        if (!pickerSlots.contains(slot)) return;

        for (Warp w : plugin.getWarpManager().getAllWarps()) {
            if (w.getName().equals(warp.getName())) continue;
            if (w.getSlot() == slot) {
                player.sendMessage(TextFormat.render("<#A98BFF>ᴛʜᴀᴛ ꜱʟᴏᴛ ɪꜱ ᴀʟʀᴇᴀᴅʏ ᴛᴀᴋᴇɴ ʙʏ ").append(Component.text(w.getName())).append(TextFormat.render("<#A98BFF>.")));
                return;
            }
        }

        warp.setSlot(slot);
        warp.save();
        player.sendMessage(config.getMessage("warp-edited", "name", warp.getName()));
        openWarpEditMenu(warp);
    }

    private int[] pickerControlSlots(int size, List<Integer> warpSlots) {
        int reset = size - 5;
        int back = -1;
        for (int i = size - 1; i >= 0; i--) {
            if (i != reset && !warpSlots.contains(i)) { back = i; break; }
        }
        if (back == -1) back = (reset == size - 1) ? size - 2 : size - 1;
        return new int[]{back, reset};
    }

    private ItemStack createMaterialItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextFormat.render(name));
        List<Component> loreList = new ArrayList<>();
        for (String s : lore) loreList.add(TextFormat.render(s));
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    public void setIcon(Warp warp, ItemStack cursorItem) {
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            warp.setIcon(HeadUtils.cleanIcon(cursorItem));
            warp.save();
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
        topMeta.displayName(Component.text(" "));
        topFiller.setItemMeta(topMeta);
        ItemStack middleFiller = new ItemStack(config.middleFillerMaterial());
        ItemMeta midMeta = middleFiller.getItemMeta();
        midMeta.displayName(Component.text(" "));
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

    private void putMenu(Inventory inv, MenuType type, Warp warp) {
        putMenu(inv, type, warp, 0);
    }

    private void putMenu(Inventory inv, MenuType type, Warp warp, int page) {
        menuStates.put(inv, new MenuState(type, warp, page));
    }

    public static MenuState getMenu(Inventory inv) { return menuStates.get(inv); }

    public static void removeMenu(Inventory inv) { menuStates.remove(inv); }

    public static void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof AdminGUI) {
                p.closeInventory();
            }
        }
        menuStates.clear();
    }
}
