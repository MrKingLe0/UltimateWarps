package com.ultimatewarps.playerwarps.gui;

import com.ultimatewarps.ChatInput;
import com.ultimatewarps.HeadUtils;
import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import com.ultimatewarps.playerwarps.PlayerWarpsConfigManager;
import com.ultimatewarps.playerwarps.integration.EconomyBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

    public PlayerWarpEditGUI(Player player, PlayerWarp warp) {
        this.player = player;
        this.warp = warp;
    }

    public static void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof PlayerWarpEditGUI) {
                p.closeInventory();
            }
        }
    }

    private PlayerWarp resolveLiveWarp() {
        return UltimateWarps.getInstance().getPlayerWarpManager().getWarp(warp.getOwnerId(), warp.getName());
    }

    public void open() {
        if (!player.isOnline()) return;
        PlayerWarp live = resolveLiveWarp();
        if (live == null) {
            player.sendMessage(UltimateWarps.getInstance().getPlayerWarpsConfigManager().getRawMessage("warp-not-found"));
            return;
        }
        if (!registered) {
            UltimateWarps.getInstance().getServer().getPluginManager().registerEvents(this, UltimateWarps.getInstance());
            registered = true;
        }

        Component title = Component.text("Edit: " + live.getName(), NamedTextColor.DARK_AQUA);
        inventory = Bukkit.createInventory(this, 45, title);

        fillBorder();
        inventory.setItem(10, visibilityItem(live));
        inventory.setItem(12, renameItem(live));
        inventory.setItem(14, descriptionItem(live));
        inventory.setItem(16, iconItem(live));

        PlayerWarpsConfigManager config = UltimateWarps.getInstance().getPlayerWarpsConfigManager();
        if (config.moneyEnabled()) {
            inventory.setItem(28, moneyItem(live));
        }
        if (config.whitelistEnabled()) {
            inventory.setItem(30, whitelistToggleItem(live));
            inventory.setItem(31, whitelistPlayersItem(live));
        }
        if (config.blacklistEnabled()) {
            inventory.setItem(32, blacklistPlayersItem(live));
        }
        inventory.setItem(34, deleteItem());

        player.openInventory(inventory);
    }

    private void fillBorder() {
        ItemStack filler = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack named(Material material, String name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextFormat.render(name));
            meta.lore(java.util.Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component line(String text) {
        return TextFormat.render(text);
    }

    private ItemStack visibilityItem(PlayerWarp w) {
        boolean isPublic = w.isPublic();
        return named(isPublic ? Material.LIME_DYE : Material.GRAY_DYE,
                "&e&lVisibility: " + (isPublic ? "&aPublic" : "&cPrivate"),
                line("&7Public warps show up for everyone"),
                line("&7in the player warps browser."),
                line("&7Private warps only show up for you"),
                line("&7and server staff."),
                Component.empty(),
                line("&eClick to toggle"));
    }

    private ItemStack renameItem(PlayerWarp w) {
        String display = w.getDisplayName() != null ? w.getDisplayName() : w.getName();
        return named(Material.NAME_TAG, "&e&lSet Display Name",
                TextFormat.renderTemplate("&fCurrent: {value}", "{value}", PlayerWarpsConfigManager.sanitizeUntrusted(display)),
                line("&7Click to change via chat"));
    }

    private ItemStack descriptionItem(PlayerWarp w) {
        String description = w.getDescription() != null ? w.getDescription() : "&7(none)";
        return named(Material.WRITABLE_BOOK, "&e&lSet Description",
                TextFormat.renderTemplate("&fCurrent: {value}", "{value}", PlayerWarpsConfigManager.sanitizeUntrusted(description)),
                line("&7Click to change via chat"));
    }

    private ItemStack iconItem(PlayerWarp w) {
        ItemStack icon = HeadUtils.cleanIcon(w.getIcon());
        if (icon == null) {
            icon = new ItemStack(Material.PLAYER_HEAD);
        }
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(TextFormat.render("&d&lCustom Icon"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextFormat.render("&7Drag an item from your inventory"));
            lore.add(TextFormat.render("&7onto this slot to set it as the icon"));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack moneyItem(PlayerWarp w) {
        boolean enabled = w.isMoneyRequired();
        String costStr = EconomyBridge.formatAmount(w.getTeleportCost());
        return named(Material.GOLD_INGOT,
                "&6&lTeleport Cost: " + (enabled ? "&a$" + costStr : "&cDisabled"),
                line("&7Status: " + (enabled ? "&aEnabled" : "&cDisabled")),
                line("&7Amount: &e$" + costStr),
                line("&7(Owner & server staff don't pay)"),
                Component.empty(),
                line("&eLeft-Click to toggle on/off"),
                line("&bRight-Click to set cost via chat"));
    }

    private ItemStack whitelistToggleItem(PlayerWarp w) {
        boolean enabled = w.isWhitelistEnabled();
        return named(enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                "&e&lWhitelist Mode: " + (enabled ? "&aEnabled" : "&cDisabled"),
                line("&7When enabled, only whitelisted players"),
                line("&7can teleport to this warp."),
                Component.empty(),
                line("&eClick to toggle"));
    }

    private ItemStack whitelistPlayersItem(PlayerWarp w) {
        int count = w.getWhitelist().size();
        return named(Material.PAPER,
                "&a&lWhitelisted Players &7(" + count + ")",
                line("&7View, add, or remove players"),
                line("&7on your warp's whitelist."),
                Component.empty(),
                line("&eClick to open Whitelist GUI"));
    }

    private ItemStack blacklistPlayersItem(PlayerWarp w) {
        int count = w.getBlacklist().size();
        return named(Material.COAL,
                "&c&lBlacklisted Players &7(" + count + ")",
                line("&7View, add, or remove players"),
                line("&7on your warp's blacklist."),
                line("&8(Blocks players even if whitelist is off)"),
                Component.empty(),
                line("&eClick to open Blacklist GUI"));
    }

    private ItemStack deleteItem() {
        return named(Material.BARRIER, "&c&lDelete Warp",
                line("&4Click to permanently delete this warp"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() != this) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot >= top.getSize()) {
            if (event.isShiftClick()
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
        if (rawSlot < 0 || !top.equals(inventory)) return;

        final ItemStack cursorSnapshot = (event.getCursor() != null && event.getCursor().getType() != Material.AIR)
                ? event.getCursor().clone()
                : null;
        final boolean rightOrShift = event.isRightClick() || event.isShiftClick();

        Bukkit.getScheduler().runTask(UltimateWarps.getInstance(), () -> {
            if (!player.isOnline()) return;
            handleAction(rawSlot, cursorSnapshot, rightOrShift);
        });
    }

    private void handleAction(int rawSlot, ItemStack cursorSnapshot, boolean rightOrShift) {
        PlayerWarpsConfigManager config = UltimateWarps.getInstance().getPlayerWarpsConfigManager();
        PlayerWarp live = resolveLiveWarp();
        if (live == null) {
            player.closeInventory();
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return;
        }

        switch (rawSlot) {
            case 10 -> {
                live.setPublic(!live.isPublic());
                live.save();
                player.sendMessage(Component.text("Visibility updated.", NamedTextColor.GREEN));
                open();
            }
            case 12 -> {
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    PlayerWarp current = resolveLiveWarp();
                    if (current == null) {
                        player.sendMessage(config.getRawMessage("warp-not-found"));
                        return;
                    }
                    if (!input.equalsIgnoreCase("cancel")) {
                        String sanitized = PlayerWarpsConfigManager.sanitizeUntrusted(input.trim());
                        if (sanitized.length() > 64) sanitized = sanitized.substring(0, 64);
                        current.setDisplayName(sanitized);
                        current.save();
                        player.sendMessage(Component.text("Display name updated.", NamedTextColor.GREEN));
                    }
                    open();
                });
            }
            case 14 -> {
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    PlayerWarp current = resolveLiveWarp();
                    if (current == null) {
                        player.sendMessage(config.getRawMessage("warp-not-found"));
                        return;
                    }
                    if (!input.equalsIgnoreCase("cancel")) {
                        String sanitized = PlayerWarpsConfigManager.sanitizeUntrusted(input.trim());
                        if (sanitized.length() > 128) sanitized = sanitized.substring(0, 128);
                        current.setDescription(sanitized);
                        current.save();
                        player.sendMessage(Component.text("Description updated.", NamedTextColor.GREEN));
                    }
                    open();
                });
            }
            case 16 -> {
                if (cursorSnapshot != null && cursorSnapshot.getType() != Material.AIR) {
                    ItemStack cleaned = HeadUtils.cleanIcon(cursorSnapshot);
                    if (cleaned != null) {
                        live.setIcon(cleaned);
                        live.save();
                        player.sendMessage(Component.text("Icon updated.", NamedTextColor.GREEN));
                        open();
                    }
                }
            }
            case 28 -> {
                if (!config.moneyEnabled()) return;
                if (!player.hasPermission(config.getMoneyPermission())
                        && !UltimateWarps.getInstance().getPlayerWarpManager().isStaff(player)) {
                    player.sendMessage(config.getRawMessage("no-permission"));
                    return;
                }
                if (rightOrShift) {
                    player.closeInventory();
                    ChatInput.waitForInput(player, 60, input -> {
                        PlayerWarp current = resolveLiveWarp();
                        if (current == null) {
                            player.sendMessage(config.getRawMessage("warp-not-found"));
                            return;
                        }
                        if (!input.equalsIgnoreCase("cancel")) {
                            try {
                                double val = Double.parseDouble(input.trim());
                                if (!Double.isFinite(val) || val < 0) {
                                    player.sendMessage(config.getRawMessage("invalid-amount"));
                                } else {
                                    double rounded = EconomyBridge.roundCost(val);
                                    current.setTeleportCost(rounded);
                                    current.setMoneyRequired(rounded > 0);
                                    current.save();
                                    player.sendMessage(config.getMessage("money-cost-set",
                                            "name", current.getName(),
                                            "amount", EconomyBridge.formatAmount(rounded)));
                                }
                            } catch (NumberFormatException e) {
                                player.sendMessage(config.getRawMessage("invalid-amount"));
                            }
                        }
                        open();
                    });
                } else {
                    live.setMoneyRequired(!live.isMoneyRequired());
                    live.save();
                    player.sendMessage(config.getMessage("money-toggled",
                            "name", live.getName(),
                            "status", live.isMoneyRequired() ? "enabled" : "disabled"));
                    open();
                }
            }
            case 30 -> {
                if (!config.whitelistEnabled()) return;
                live.setWhitelistEnabled(!live.isWhitelistEnabled());
                live.save();
                player.sendMessage(config.getMessage("whitelist-toggled",
                        "name", live.getName(),
                        "status", live.isWhitelistEnabled() ? "enabled" : "disabled"));
                open();
            }
            case 31 -> {
                if (!config.whitelistEnabled()) return;
                new PlayerWarpListGUI(player, live, PlayerWarpListGUI.Mode.WHITELIST).open(0);
            }
            case 32 -> {
                if (!config.blacklistEnabled()) return;
                new PlayerWarpListGUI(player, live, PlayerWarpListGUI.Mode.BLACKLIST).open(0);
            }
            case 34 -> {
                player.closeInventory();
                UltimateWarps.getInstance().getPlayerWarpManager().removeWarp(live.getOwnerId(), live.getName());
                player.sendMessage(Component.text("Player warp '" + live.getName() + "' deleted.", NamedTextColor.GREEN));
            }
            default -> {}
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() != this) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < top.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        unregister();
    }

    private void unregister() {
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
