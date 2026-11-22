package com.ultimatewarps.playerwarps.gui;

import com.ultimatewarps.ChatInput;
import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import com.ultimatewarps.playerwarps.PlayerWarpManager;
import com.ultimatewarps.playerwarps.PlayerWarpsConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerWarpListGUI implements InventoryHolder, Listener {

    public enum Mode {
        WHITELIST,
        BLACKLIST
    }

    private final Player player;
    private final PlayerWarp warp;
    private final Mode mode;
    private Inventory inventory;
    private int currentPage = 0;
    private List<UUID> entries = new ArrayList<>();
    private boolean registered = false;

    public PlayerWarpListGUI(Player player, PlayerWarp warp, Mode mode) {
        this.player = player;
        this.warp = warp;
        this.mode = mode;
    }

    public static void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof PlayerWarpListGUI) {
                p.closeInventory();
            }
        }
    }

    private PlayerWarp resolveLiveWarp() {
        return UltimateWarps.getInstance().getPlayerWarpManager().getWarp(warp.getOwnerId(), warp.getName());
    }

    public void open(int page) {
        this.currentPage = Math.max(0, page);
        open();
    }

    public void open() {
        if (!player.isOnline()) return;
        PlayerWarp live = resolveLiveWarp();
        if (live == null) {
            player.sendMessage(UltimateWarps.getInstance().getPlayerWarpsConfigManager().getRawMessage("warp-not-found"));
            return;
        }
        if (!registered) {
            UltimateWarps.getInstance().getServer().getPluginManager()
                    .registerEvents(this, UltimateWarps.getInstance());
            registered = true;
        }

        entries = new ArrayList<>(mode == Mode.WHITELIST ? live.getWhitelist() : live.getBlacklist());
        int perPage = 36; // slots 9..44
        int totalPages = entries.isEmpty() ? 1 : (int) Math.ceil((double) entries.size() / perPage);
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        String titlePrefix = mode == Mode.WHITELIST ? "Whitelist: " : "Blacklist: ";
        NamedTextColor titleColor = mode == Mode.WHITELIST ? NamedTextColor.GREEN : NamedTextColor.RED;
        inventory = Bukkit.createInventory(this, 54, Component.text(titlePrefix + live.getName(), titleColor));

        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.text(" "));
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 9; i++) inventory.setItem(i, border.clone());
        for (int i = 45; i < 54; i++) inventory.setItem(i, border.clone());

        int start = currentPage * perPage;
        int end = Math.min(start + perPage, entries.size());
        for (int i = start; i < end; i++) {
            UUID targetId = entries.get(i);
            int slot = 9 + (i - start);
            inventory.setItem(slot, createPlayerHeadItem(live, targetId));
        }

        if (currentPage > 0) {
            inventory.setItem(45, createButton(Material.ARROW, "&b← Previous Page",
                    "&7Click to go to page " + currentPage));
        }

        if (mode == Mode.WHITELIST) {
            boolean enabled = live.isWhitelistEnabled();
            inventory.setItem(47, createButton(
                    enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                    "&e&lWhitelist: " + (enabled ? "&aEnabled" : "&cDisabled"),
                    "&7When enabled, only whitelisted players",
                    "&7(plus you and staff) can visit this warp.",
                    "",
                    "&eClick to toggle"
            ));
        }

        inventory.setItem(49, createButton(
                Material.EMERALD,
                mode == Mode.WHITELIST ? "&a&l+ Add Whitelisted Player" : "&c&l+ Add Blacklisted Player",
                "&7Click to type a player's name in chat",
                "&7to add them to this list.",
                "",
                "&8(Cannot add yourself or server staff)"
        ));

        inventory.setItem(51, createButton(Material.BARRIER, "&c&lBack to Edit Menu",
                "&7Return to warp settings"));

        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createButton(Material.ARROW, "&bNext Page →",
                    "&7Click to go to page " + (currentPage + 2)));
        }

        player.openInventory(inventory);
    }

    private ItemStack createPlayerHeadItem(PlayerWarp live, UUID targetId) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        String playerName = live.getKnownPlayerName(targetId);
        if (meta != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetId);
            try {
                meta.setOwningPlayer(op);
            } catch (Exception ignored) {}
            meta.displayName(TextFormat.render("&e&l" + playerName));
            List<Component> lore = new ArrayList<>();
            lore.add(TextFormat.render("&7UUID: &8" + targetId));
            lore.add(Component.empty());
            lore.add(TextFormat.render(mode == Mode.WHITELIST
                    ? "&cClick to remove from whitelist"
                    : "&cClick to remove from blacklist"));
            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextFormat.render(name));
            List<Component> lore = new ArrayList<>();
            for (String l : loreLines) {
                lore.add(TextFormat.render(l));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() != this) return;

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize() || !top.equals(inventory)) return;

        Bukkit.getScheduler().runTask(UltimateWarps.getInstance(), () -> {
            if (!player.isOnline()) return;
            handleAction(rawSlot);
        });
    }

    private void handleAction(int rawSlot) {
        PlayerWarpsConfigManager config = UltimateWarps.getInstance().getPlayerWarpsConfigManager();
        PlayerWarpManager manager = UltimateWarps.getInstance().getPlayerWarpManager();

        if ((mode == Mode.WHITELIST && !config.whitelistEnabled())
                || (mode == Mode.BLACKLIST && !config.blacklistEnabled())) {
            player.closeInventory();
            player.sendMessage(config.getRawMessage("subfeature-disabled"));
            return;
        }

        PlayerWarp live = resolveLiveWarp();
        if (live == null) {
            player.closeInventory();
            player.sendMessage(config.getRawMessage("warp-not-found"));
            return;
        }

        if (rawSlot >= 9 && rawSlot <= 44) {
            int index = currentPage * 36 + (rawSlot - 9);
            if (index >= 0 && index < entries.size()) {
                UUID targetId = entries.get(index);
                String targetName = live.getKnownPlayerName(targetId);
                if (mode == Mode.WHITELIST) {
                    live.removeWhitelistedPlayer(targetId);
                    live.save();
                    player.sendMessage(config.getMessage("whitelist-removed",
                            "player", targetName, "name", live.getName()));
                } else {
                    live.removeBlacklistedPlayer(targetId);
                    live.save();
                    player.sendMessage(config.getMessage("blacklist-removed",
                            "player", targetName, "name", live.getName()));
                }
                open();
            }
            return;
        }

        switch (rawSlot) {
            case 45 -> {
                if (currentPage > 0) {
                    currentPage--;
                    open();
                }
            }
            case 47 -> {
                if (mode == Mode.WHITELIST) {
                    live.setWhitelistEnabled(!live.isWhitelistEnabled());
                    live.save();
                    player.sendMessage(config.getMessage("whitelist-toggled",
                            "name", live.getName(),
                            "status", live.isWhitelistEnabled() ? "enabled" : "disabled"));
                    open();
                }
            }
            case 49 -> {
                player.closeInventory();
                ChatInput.waitForInput(player, 60, input -> {
                    PlayerWarp current = resolveLiveWarp();
                    if (current == null) {
                        player.sendMessage(config.getRawMessage("warp-not-found"));
                        return;
                    }
                    String trimmed = input.trim();
                    if (!trimmed.equalsIgnoreCase("cancel") && !trimmed.isEmpty()) {
                        OfflinePlayer target = Bukkit.getPlayerExact(trimmed);
                        if (target == null) {
                            target = Bukkit.getOfflinePlayerIfCached(trimmed);
                        }
                        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                            player.sendMessage(Component.text("Player not found: " + trimmed, NamedTextColor.RED));
                        } else {
                            String resolvedName = target.getName() != null ? target.getName() : trimmed;
                            if (target.getUniqueId().equals(current.getOwnerId())) {
                                player.sendMessage(config.getRawMessage("cannot-list-self"));
                            } else if (manager.isStaff(target)) {
                                player.sendMessage(config.getRawMessage("cannot-list-staff"));
                            } else if (mode == Mode.WHITELIST) {
                                current.addWhitelistedPlayer(target.getUniqueId(), resolvedName);
                                current.save();
                                player.sendMessage(config.getMessage("whitelist-added",
                                        "player", resolvedName, "name", current.getName()));
                            } else {
                                current.addBlacklistedPlayer(target.getUniqueId(), resolvedName);
                                current.save();
                                player.sendMessage(config.getMessage("blacklist-added",
                                        "player", resolvedName, "name", current.getName()));
                            }
                        }
                    }
                    open();
                });
            }
            case 51 -> new PlayerWarpEditGUI(player, live).open();
            case 53 -> {
                int totalPages = entries.isEmpty() ? 1 : (int) Math.ceil((double) entries.size() / 36.0);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    open();
                }
            }
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
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
