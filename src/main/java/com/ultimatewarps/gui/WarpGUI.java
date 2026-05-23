package com.ultimatewarps.gui;
import com.ultimatewarps.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public class WarpGUI implements InventoryHolder {

    private final Player player;
    private final List<Warp> warps;
    private int page = 0;
    private final int size;
    private final int warpSlots;
    private final ConfigManager config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public WarpGUI(Player player) {
        this.player = player;
        this.config = UltimateWarps.getInstance().getConfigManager();
        this.size = config.guiSize();
        this.warpSlots = size - 18;
        this.warps = new ArrayList<>(UltimateWarps.getInstance().getWarpManager().getAllWarps());

        warps.removeIf(w ->
                !w.isEnabled() ||
                (w.getPermission() != null && !player.hasPermission(w.getPermission()))
        );
    }

    public void open(int page) {
        this.page = page;

        Component title = miniMessage.deserialize(config.warpGuiTitle());
        Inventory inv = Bukkit.createInventory(this, size, title);

        fillBorders(inv);

        int startIndex = page * warpSlots;
        int endIndex = Math.min(startIndex + warpSlots, warps.size());

        for (int i = startIndex; i < endIndex; i++) {

            int slot = 9 + (i - startIndex);
            Warp warp = warps.get(i);

            ItemStack head = warp.getIcon();

            if (head == null) {
                head = HeadUtils.getHead(HeadUtils.WARP_ICON);
            } else {
                head = head.clone();
            }

            ItemMeta meta = head.getItemMeta();

            String raw = warp.getDisplayName() != null
                    ? warp.getDisplayName()
                    : warp.getName();

            String display = ChatColor.translateAlternateColorCodes('&', raw);

            meta.setDisplayName("§6" + display);

            List<String> lore = new ArrayList<>();
            lore.add("§7ᴄᴏᴏʟᴅᴏᴡɴ: §f" + warp.getCooldown() + "s");
            lore.add("§7ᴅᴇʟᴀʏ: §f" + warp.getDelay() + "s");

            meta.setLore(lore);
            head.setItemMeta(meta);

            inv.setItem(slot, head);
        }

        if (page > 0) {
            inv.setItem(size - 9,
                    createNavButton(HeadUtils.PREV_ICON, "§eᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ"));
        }

        if (endIndex < warps.size()) {
            inv.setItem(size - 1,
                    createNavButton(HeadUtils.NEXT_ICON, "§eɴᴇxᴛ ᴘᴀɢᴇ"));
        }

        inv.setItem(size - 5, createCloseButton());

        player.openInventory(inv);
    }

    public void handleClick(int slot) {

        // Navigation
        if (slot == size - 9 && page > 0) {

            open(page - 1);

        } else if (slot == size - 1 && (page + 1) * warpSlots < warps.size()) {

            open(page + 1);

        } else if (slot == size - 5) {

            player.closeInventory();

        } else if (slot >= 9 && slot < 9 + warpSlots) {

            int index = page * warpSlots + (slot - 9);

            if (index < warps.size()) {

                Warp warp = warps.get(index);

                player.closeInventory();

                CooldownManager cm = UltimateWarps.getInstance().getCooldownManager();

                int cd = cm.getRemainingCooldown(
                        player,
                        "warp_" + warp.getName(),
                        cm.getEffectiveCooldown(player, warp.getCooldown())
                );

                if (cd > 0) {
                    player.sendMessage(config.getMessage("cooldown-active")
                            .replaceText(b ->
                                    b.matchLiteral("%seconds%")
                                            .replacement(String.valueOf(cd)))
                    );
                    return;
                }

                int delay = cm.getEffectiveDelay(player, warp.getDelay());
                TeleportTask task = new TeleportTask(player, warp.getLocation(), delay,
                        config.warpCancelOnMove(),
                        config.warpTitleMessage().replace("%warp%", warp.getName()),
                        config.warpSubtitleMessage(),
                        warp.getName());

                task.runTaskTimer(UltimateWarps.getInstance(), 0L, 20L);

                UltimateWarps.getInstance()
                        .getActiveTeleports()
                        .put(player.getUniqueId(), task);

                cm.applyCooldown(player, "warp_" + warp.getName());
            }
        }
    }

    // ---------- Helpers ----------

    private ItemStack createNavButton(String texture, String name) {

        ItemStack item = HeadUtils.getHead(texture);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createSpawnButton() {

        ItemStack item = HeadUtils.getHead(HeadUtils.SPAWN_ICON);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aᴛᴇʟᴇᴘᴏʀᴛ ᴛᴏ ꜱᴘᴀᴡɴ");

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createCloseButton() {

        ItemStack item = HeadUtils.getHead(HeadUtils.CLOSE_ICON);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cᴄʟᴏꜱᴇ");

        item.setItemMeta(meta);

        return item;
    }

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

    @Override
    public Inventory getInventory() {
        return null;
    }
}