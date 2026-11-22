package com.ultimatewarps.playerwarps.gui;

import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import com.ultimatewarps.playerwarps.PlayerWarpsConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.util.ArrayList;
import java.util.List;

public class PlayerWarpRateGUI implements InventoryHolder, Listener {

    private final Player player;
    private final PlayerWarp warp;
    private final boolean returnToBrowser;
    private Inventory inventory;
    private boolean registered = false;

    public PlayerWarpRateGUI(Player player, PlayerWarp warp) {
        this(player, warp, false);
    }

    public PlayerWarpRateGUI(Player player, PlayerWarp warp, boolean returnToBrowser) {
        this.player = player;
        this.warp = warp;
        this.returnToBrowser = returnToBrowser;
    }

    public static void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof PlayerWarpRateGUI) {
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
            UltimateWarps.getInstance().getServer().getPluginManager()
                    .registerEvents(this, UltimateWarps.getInstance());
            registered = true;
        }

        String displayLabel = live.getDisplayName() != null ? live.getDisplayName() : live.getName();
        String ownerName = live.getOwnerName() != null ? live.getOwnerName() : "Unknown";
        Component title = Component.text("Rate Warp: " + live.getName(), NamedTextColor.GOLD);
        inventory = Bukkit.createInventory(this, 27, title);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.text(" "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        PlayerWarpsConfigManager config = UltimateWarps.getInstance().getPlayerWarpsConfigManager();

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(TextFormat.render("&e&l" + PlayerWarpsConfigManager.sanitizeUntrusted(displayLabel)));
            List<Component> lore = new ArrayList<>();
            lore.add(TextFormat.render(live.getFormattedRatingLore(config)));
            lore.add(TextFormat.render("&7Owner: &f" + ownerName));
            int existing = live.getPlayerRating(player.getUniqueId());
            if (existing > 0) {
                lore.add(TextFormat.render("&7Your rating: &e" + existing + " ★"));
            }
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(4, info);

        int currentRating = live.getPlayerRating(player.getUniqueId());
        for (int stars = 1; stars <= 5; stars++) {
            int slot = 10 + stars; // slots 11..15
            boolean selected = currentRating == stars;
            Material mat = stars <= currentRating ? Material.NETHER_STAR : Material.GOLD_NUGGET;
            ItemStack starItem = new ItemStack(mat, stars);
            ItemMeta meta = starItem.getItemMeta();
            if (meta != null) {
                StringBuilder starSymbols = new StringBuilder();
                for (int i = 1; i <= 5; i++) {
                    starSymbols.append(i <= stars ? "★" : "☆");
                }
                meta.displayName(TextFormat.render("&e&l" + stars + " Star" + (stars > 1 ? "s" : "") + " &6" + starSymbols));
                List<Component> lore = new ArrayList<>();
                if (selected) {
                    lore.add(TextFormat.render("&aCurrently selected"));
                } else {
                    lore.add(TextFormat.render("&7Click to rate this warp &e" + stars + " ★"));
                }
                meta.lore(lore);
                starItem.setItemMeta(meta);
            }
            inventory.setItem(slot, starItem);
        }

        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(TextFormat.render(returnToBrowser ? "&c&lBack" : "&c&lClose"));
            closeBtn.setItemMeta(closeMeta);
        }
        inventory.setItem(22, closeBtn);

        player.openInventory(inventory);
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
        if (rawSlot >= 11 && rawSlot <= 15) {
            if (!config.ratingEnabled()) {
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

            int stars = rawSlot - 10;
            live.setRating(player.getUniqueId(), stars);
            live.save();

            String displayLabel = live.getDisplayName() != null ? live.getDisplayName() : live.getName();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            player.sendMessage(config.getMessage("warp-rated",
                    "name", displayLabel,
                    "stars", String.valueOf(stars)));

            player.closeInventory();
            if (returnToBrowser) {
                PlayerWarpGUI.getOrCreate(UltimateWarps.getInstance(), player).open();
            }
        } else if (rawSlot == 22) {
            player.closeInventory();
            if (returnToBrowser) {
                PlayerWarpGUI.getOrCreate(UltimateWarps.getInstance(), player).open();
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
