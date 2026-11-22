package com.ultimatewarps.playerwarps.gui;

import com.ultimatewarps.TextFormat;
import com.ultimatewarps.HeadUtils;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import com.ultimatewarps.playerwarps.PlayerWarpManager;
import com.ultimatewarps.playerwarps.PlayerWarpsConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerWarpGUI implements Listener, InventoryHolder {

    private final UltimateWarps plugin;
    private final Player player;
    private Inventory gui;
    private int currentPage = 0;
    private boolean myWarpsOnly = false;
    private boolean favouritesOnly = false;
    private List<PlayerWarp> warps;
    private YamlConfiguration guiConfig;

    private Component title;
    private int rows;
    private boolean fillEmptySlots;
    private ItemStack fillItem;
    private Sound clickSound;
    private float clickSoundVolume;
    private float clickSoundPitch;

    private String warpNameFormat;
    private List<String> warpLoreFormat;
    private String ownWarpNameFormat;
    private List<String> ownWarpLoreFormat;

    private List<Integer> warpSlots;
    private Map<String, ButtonConfig> buttons;
    private Set<Integer> customItemSlots = new HashSet<>();
    private Set<Integer> occupiedWarpSlots = new HashSet<>();

    private final Map<UUID, Long> lastPageChangeTime = new HashMap<>();
    private static final Set<PlayerWarpGUI> activeInstances = new HashSet<>();
    private static final Map<UUID, PlayerWarpGUI> OPEN_GUIS = new HashMap<>();

    public PlayerWarpGUI(UltimateWarps plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        loadGuiConfig();
        refreshWarpList();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        activeInstances.add(this);
    }

    public static PlayerWarpGUI getOrCreate(UltimateWarps plugin, Player player) {
        return OPEN_GUIS.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerWarpGUI(plugin, player));
    }

    public void setFavouritesOnly(boolean favouritesOnly) {
        this.favouritesOnly = favouritesOnly;
        if (favouritesOnly) {
            this.myWarpsOnly = false;
        }
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }

    public void unregister() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        activeInstances.remove(this);
        if (player != null) {
            OPEN_GUIS.remove(player.getUniqueId(), this);
        }
    }

    public static void unregisterAll() {
        for (PlayerWarpGUI gui : new ArrayList<>(activeInstances)) {
            gui.unregister();
            if (gui.player != null && gui.player.isOnline()
                    && gui.player.getOpenInventory().getTopInventory().getHolder() == gui) {
                gui.player.closeInventory();
            }
        }
        activeInstances.clear();
        OPEN_GUIS.clear();
    }

    private void refreshWarpList() {
        PlayerWarpManager manager = plugin.getPlayerWarpManager();
        List<PlayerWarp> visible = manager.getVisibleWarps(player);
        if (myWarpsOnly) {
            visible = visible.stream()
                    .filter(w -> w.getOwnerId().equals(player.getUniqueId()))
                    .collect(Collectors.toList());
        } else if (favouritesOnly && plugin.getPlayerWarpsConfigManager().favouritesEnabled()) {
            visible = visible.stream()
                    .filter(w -> manager.isFavourite(player.getUniqueId(), w))
                    .collect(Collectors.toList());
        }
        this.warps = visible;
    }

    public void open(int page) {
        this.currentPage = page;
        open();
    }

    public void open() {
        refreshWarpList();
        createGUI();
        player.openInventory(gui);
    }

    private void loadGuiConfig() {
        File guiFile = new File(plugin.getDataFolder(), "playerwarps-gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("playerwarps-gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        ConfigurationSection guiSection = guiConfig.getConfigurationSection("gui");
        if (guiSection != null) {
            title = TextFormat.render(guiSection.getString("title", "&b&l Player Warps "));
            rows = Math.max(1, Math.min(6, guiSection.getInt("rows", 6)));
            fillEmptySlots = guiSection.getBoolean("fill-empty-slots", true);
        } else {
            title = TextFormat.render("&b&l Player Warps ");
            rows = 6;
            fillEmptySlots = true;
        }

        ConfigurationSection soundSection = guiConfig.getConfigurationSection("click-sound");
        if (soundSection != null) {
            clickSound = com.ultimatewarps.ConfigManager.parseSound(
                    soundSection.getString("sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
            clickSoundVolume = (float) soundSection.getDouble("volume", 1.0);
            clickSoundPitch = (float) soundSection.getDouble("pitch", 1.0);
        } else {
            clickSound = Sound.UI_BUTTON_CLICK;
            clickSoundVolume = 1.0f;
            clickSoundPitch = 1.0f;
        }

        fillItem = loadItem(guiConfig.getConfigurationSection("gui.fill-item"), "BLACK_STAINED_GLASS_PANE", " ");

        ConfigurationSection warpItemSection = guiConfig.getConfigurationSection("warp-item");
        warpNameFormat = warpItemSection != null ? warpItemSection.getString("name", "&b&l{warp_name}") : "&b&l{warp_name}";
        warpLoreFormat = warpItemSection != null ? warpItemSection.getStringList("lore") : new ArrayList<>();

        ConfigurationSection ownWarpItemSection = guiConfig.getConfigurationSection("own-warp-item");
        ownWarpNameFormat = ownWarpItemSection != null ? ownWarpItemSection.getString("name", "&e&l{warp_name}") : warpNameFormat;
        ownWarpLoreFormat = ownWarpItemSection != null ? ownWarpItemSection.getStringList("lore") : warpLoreFormat;

        ConfigurationSection warpSlotsSection = guiConfig.getConfigurationSection("warp-slots");
        if (warpSlotsSection != null) {
            String mode = warpSlotsSection.getString("mode", "auto");
            if (mode.equals("auto")) {
                int start = warpSlotsSection.getInt("auto-start", 9);
                int end = warpSlotsSection.getInt("auto-end", 44);
                warpSlots = new ArrayList<>();
                for (int i = start; i <= end; i++) warpSlots.add(i);
            } else {
                warpSlots = warpSlotsSection.getIntegerList("slots");
                Collections.sort(warpSlots);
            }
        } else {
            warpSlots = new ArrayList<>();
            for (int i = 9; i <= 44; i++) warpSlots.add(i);
        }
        int maxSlot = rows * 9;
        warpSlots = warpSlots.stream()
                .filter(s -> s >= 0 && s < maxSlot)
                .distinct()
                .collect(Collectors.toList());
        if (warpSlots.isEmpty()) {
            for (int i = 9; i < Math.min(45, maxSlot); i++) warpSlots.add(i);
        }

        buttons = new HashMap<>();
        ConfigurationSection buttonsSection = guiConfig.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String key : buttonsSection.getKeys(false)) {
                ConfigurationSection btnSection = buttonsSection.getConfigurationSection(key);
                if (btnSection != null && btnSection.getBoolean("enabled", true)) {
                    int slot = btnSection.getInt("slot", -1);
                    if (slot < 0 || slot >= maxSlot) continue;
                    ButtonConfig btn = new ButtonConfig();
                    btn.slot = slot;
                    btn.item = loadItem(btnSection, "BARRIER", key);
                    btn.hideIfNoPrevious = btnSection.getBoolean("hide-if-no-previous", false);
                    btn.hideIfNoNext = btnSection.getBoolean("hide-if-no-next", false);
                    btn.updateOnEachPage = btnSection.getBoolean("update-on-each-page", false);

                    btn.rawNameTemplate = btnSection.getString("name", key);
                    btn.rawLoreTemplate = btnSection.getStringList("lore");
                    buttons.put(key, btn);
                }
            }
        }

        // Ensure "favourite" button is present only if missing from YAML (respect enabled: false)
        if ((buttonsSection == null || !buttonsSection.contains("favourite")) && !buttons.containsKey("favourite") && 48 < maxSlot) {
            ButtonConfig favBtn = new ButtonConfig();
            favBtn.slot = 48;
            favBtn.item = new ItemStack(Material.NETHER_STAR);
            favBtn.rawNameTemplate = "&6&lꜰᴀᴠᴏᴜʀɪᴛᴇ: {favourite}";
            favBtn.rawLoreTemplate = List.of(
                    "&7ᴄʟɪᴄᴋ ᴛᴏ ᴠɪᴇᴡ ʏᴏᴜʀ",
                    "&7ꜰᴀᴠᴏᴜʀɪᴛᴇ ᴡᴀʀᴘs ʟɪsᴛ",
                    "",
                    "&eʀɪɢʜᴛ-ᴄʟɪᴄᴋ ᴀɴʏ ᴡᴀʀᴘ ɪɴ ᴛʜᴇ",
                    "&eɢᴜɪ ᴛᴏ ᴀᴅᴅ/ʀᴇᴍᴏᴠᴇ ꜰᴀᴠᴏᴜʀɪᴛᴇ"
            );
            buttons.put("favourite", favBtn);
        }
    }

    private ItemStack loadItem(ConfigurationSection section, String defaultMaterial, String defaultName) {
        if (section == null) return new ItemStack(Material.valueOf(defaultMaterial));
        Material material;
        try {
            material = Material.valueOf(section.getString("material", defaultMaterial).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            material = Material.valueOf(defaultMaterial);
        }

        ItemStack item;
        if (material == Material.PLAYER_HEAD && section.contains("head-texture")) {
            item = HeadUtils.getHead(section.getString("head-texture"));
            item.setAmount(Math.max(1, section.getInt("amount", 1)));
        } else {
            item = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name", defaultName);
            if (name != null && !name.isEmpty()) {
                meta.displayName(TextFormat.render(name));
            }
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.lore(lore.stream().map(TextFormat::render).collect(Collectors.toList()));
            }
            if (section.getBoolean("enchanted", false)) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void createGUI() {
        int size = rows * 9;
        gui = Bukkit.createInventory(this, size, title);
        occupiedWarpSlots.clear();

        if (warps.isEmpty()) {
            currentPage = 0;
            loadCustomItems();
            placeButtons();
            if (fillEmptySlots) {
                for (int i = 0; i < size; i++) {
                    if (gui.getItem(i) == null) gui.setItem(i, fillItem.clone());
                }
            }
            return;
        }

        int perPage = warpSlots.size();
        if (currentPage < 0) currentPage = 0;

        int start = currentPage * perPage;
        int end = Math.min(start + perPage, warps.size());
        if (start >= warps.size() && currentPage > 0) {
            currentPage = (warps.size() - 1) / perPage;
            start = currentPage * perPage;
            end = Math.min(start + perPage, warps.size());
        }

        if (start < warps.size() && start >= 0) {
            List<PlayerWarp> pageWarps = warps.subList(start, end);
            for (int i = 0; i < pageWarps.size() && i < warpSlots.size(); i++) {
                PlayerWarp warp = pageWarps.get(i);
                int slot = warpSlots.get(i);
                gui.setItem(slot, createWarpItem(warp));
                occupiedWarpSlots.add(slot);
            }
        }

        loadCustomItems();
        placeButtons();

        if (fillEmptySlots) {
            for (int i = 0; i < size; i++) {
                if (gui.getItem(i) == null) gui.setItem(i, fillItem.clone());
            }
        }
    }

    private ItemStack createWarpItem(PlayerWarp warp) {
        boolean owned = warp.getOwnerId().equals(player.getUniqueId());

        ItemStack customIcon = warp.getIcon();
        ItemStack item;
        if (customIcon != null) {
            item = customIcon;
        } else {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.getOwnerId());
                skullMeta.setOwningPlayer(owner);
                item.setItemMeta(skullMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        PlayerWarpsConfigManager pwConfig = plugin.getPlayerWarpsConfigManager();
        PlayerWarpManager manager = plugin.getPlayerWarpManager();

        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        String visibility = warp.isPublic() ? "&aPublic" : "&cPrivate";
        String ownerName = warp.getOwnerName() != null ? warp.getOwnerName() : "Unknown";
        String ratingLore = warp.getFormattedRatingLore(pwConfig);
        String costLore = warp.getFormattedCost(pwConfig);
        String whitelistStatus = warp.isWhitelistEnabled() ? "&aEnabled" : "&7Disabled";
        String favStatus = manager.isFavourite(player.getUniqueId(), warp) ? "&6★ Yes" : "&7☆ No";

        String nameFormat = owned ? ownWarpNameFormat : warpNameFormat;
        List<String> loreFormat = owned ? ownWarpLoreFormat : warpLoreFormat;

        meta.displayName(TextFormat.renderTemplate(nameFormat,
                "{warp_name}", displayLabel,
                "{owner}", ownerName,
                "{cooldown}", String.valueOf(pwConfig.cooldown()),
                "{delay}", String.valueOf(pwConfig.delay()),
                "{visibility}", visibility,
                "{rating}", ratingLore,
                "{cost}", costLore,
                "{whitelist_status}", whitelistStatus,
                "{favourite_status}", favStatus));

        List<Component> lore = new ArrayList<>();
        boolean hasRatingPlaceholder = loreFormat.stream().anyMatch(l -> l.contains("{rating}"));

        // Ensure rating is at the very first line of the warp lore when rating is enabled
        if (pwConfig.ratingEnabled() && !hasRatingPlaceholder) {
            lore.add(TextFormat.render(ratingLore));
        }

        for (String line : loreFormat) {
            if (!pwConfig.ratingEnabled() && line.trim().equals("{rating}")) {
                continue;
            }
            lore.add(TextFormat.renderTemplate(line,
                    "{warp_name}", displayLabel,
                    "{owner}", ownerName,
                    "{cooldown}", String.valueOf(pwConfig.cooldown()),
                    "{delay}", String.valueOf(pwConfig.delay()),
                    "{visibility}", visibility,
                    "{rating}", ratingLore,
                    "{cost}", costLore,
                    "{whitelist_status}", whitelistStatus,
                    "{favourite_status}", favStatus));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void loadCustomItems() {
        customItemSlots.clear();
        ConfigurationSection customSection = guiConfig.getConfigurationSection("custom-items");
        if (customSection == null) return;

        PlayerWarpsConfigManager pwConfig = plugin.getPlayerWarpsConfigManager();
        Set<Integer> buttonSlots = new HashSet<>();
        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            if (entry.getKey().equals("favourite") && !pwConfig.favouritesEnabled()) continue;
            buttonSlots.add(entry.getValue().slot);
        }

        for (String itemKey : customSection.getKeys(false)) {
            ConfigurationSection itemSection = customSection.getConfigurationSection(itemKey);
            if (itemSection != null && itemSection.getBoolean("enabled", true)) {
                ItemStack customItem = loadItem(itemSection, "BARRIER", itemKey);
                for (int slot : itemSection.getIntegerList("slots")) {
                    if (slot < 0 || slot >= rows * 9) continue;
                    if (occupiedWarpSlots.contains(slot)) continue;
                    if (buttonSlots.contains(slot)) continue;
                    gui.setItem(slot, customItem.clone());
                    customItemSlots.add(slot);
                }
            }
        }
    }

    private void placeButtons() {
        int perPage = warpSlots.size();
        int totalPages = warps.isEmpty() ? 1 : (int) Math.ceil((double) warps.size() / perPage);

        int trueOwnedCount = plugin.getPlayerWarpManager().countWarpsByOwner(player.getUniqueId());
        PlayerWarpsConfigManager pwConfig = plugin.getPlayerWarpsConfigManager();
        int limit = pwConfig.getWarpLimit(player);
        String limitDisplay = limit >= Integer.MAX_VALUE ? "\u221E" : String.valueOf(limit);

        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            String key = entry.getKey();
            ButtonConfig btn = entry.getValue();

            if (key.equals("favourite") && !pwConfig.favouritesEnabled()) continue;
            if (key.equals("previous-page") && btn.hideIfNoPrevious && currentPage == 0) continue;
            if (key.equals("next-page") && btn.hideIfNoNext && currentPage >= totalPages - 1) continue;
            if (occupiedWarpSlots.contains(btn.slot)) continue;

            ItemStack item = btn.item.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (btn.rawNameTemplate != null) {
                    String nameTemplate = btn.rawNameTemplate
                            .replace("{filter}", myWarpsOnly ? "&eMy Warps" : "&bAll Warps")
                            .replace("{favourite}", favouritesOnly ? "&aShowing Favourites" : "&7All Warps")
                            .replace("{current}", String.valueOf(currentPage + 1))
                            .replace("{total}", String.valueOf(totalPages))
                            .replace("{owned}", String.valueOf(trueOwnedCount))
                            .replace("{limit}", limitDisplay);
                    meta.displayName(TextFormat.render(nameTemplate));
                }

                if (btn.rawLoreTemplate != null && !btn.rawLoreTemplate.isEmpty()) {
                    List<Component> lore = btn.rawLoreTemplate.stream()
                            .map(line -> line.replace("{current}", String.valueOf(currentPage + 1))
                                    .replace("{total}", String.valueOf(totalPages))
                                    .replace("{owned}", String.valueOf(trueOwnedCount))
                                    .replace("{limit}", limitDisplay)
                                    .replace("{filter}", myWarpsOnly ? "&eMy Warps" : "&bAll Warps")
                                    .replace("{favourite}", favouritesOnly ? "&aShowing Favourites" : "&7All Warps"))
                            .map(TextFormat::render)
                            .collect(Collectors.toList());
                    meta.lore(lore);
                }
                item.setItemMeta(meta);
            }

            gui.setItem(btn.slot, item);
        }
    }

    private void handleClick(int slot, ClickType clickType) {
        PlayerWarpsConfigManager pwConfig = plugin.getPlayerWarpsConfigManager();

        if (!occupiedWarpSlots.contains(slot)) {
            int perPage = Math.max(1, warpSlots.size());
            int totalPages = warps.isEmpty() ? 1 : (int) Math.ceil((double) warps.size() / perPage);
            for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
                String key = entry.getKey();
                ButtonConfig btn = entry.getValue();
                if (key.equals("favourite") && !pwConfig.favouritesEnabled()) continue;
                if (key.equals("previous-page") && btn.hideIfNoPrevious && currentPage == 0) continue;
                if (key.equals("next-page") && btn.hideIfNoNext && currentPage >= totalPages - 1) continue;
                if (btn.slot == slot) {
                    handleButtonClick(key);
                    return;
                }
            }
            return;
        }

        if (customItemSlots.contains(slot)) return;

        int perPage = warpSlots.size();
        int indexInPage = warpSlots.indexOf(slot);
        if (indexInPage < 0) return;
        int index = currentPage * perPage + indexInPage;
        if (index < 0 || index >= warps.size()) return;

        PlayerWarp warp = warps.get(index);
        boolean owned = warp.getOwnerId().equals(player.getUniqueId());
        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();

        playClickSound();

        // Shift + Left-Click on own warp -> Edit GUI
        if (clickType == ClickType.SHIFT_LEFT && owned) {
            player.closeInventory();
            player.performCommand("playerwarps edit " + warp.getName());
            return;
        }

        // Shift + Right-Click -> Open Rate GUI (after visiting the warp, if not owner)
        if (clickType == ClickType.SHIFT_RIGHT) {
            if (!pwConfig.ratingEnabled()) {
                player.sendMessage(pwConfig.getRawMessage("subfeature-disabled"));
                return;
            }
            if (owned) {
                player.sendMessage(pwConfig.getRawMessage("cannot-rate-own-warp"));
                return;
            }
            if (!warp.hasVisited(player.getUniqueId())) {
                player.sendMessage(pwConfig.getRawMessage("must-visit-to-rate"));
                return;
            }
            player.closeInventory();
            new PlayerWarpRateGUI(player, warp, true).open();
            return;
        }

        // Right-Click (or Middle-Click) -> Add/Remove warp from Favourite Warps list
        if (clickType == ClickType.RIGHT || clickType == ClickType.MIDDLE) {
            if (!pwConfig.favouritesEnabled()) {
                return;
            }
            boolean added = plugin.getPlayerWarpManager().toggleFavourite(player.getUniqueId(), warp);
            player.sendMessage(pwConfig.getMessage(added ? "favourite-added" : "favourite-removed",
                    "name", displayLabel));
            open();
            return;
        }

        // Only explicit Left-Click triggers teleport (prevents Q/F/number-key accidental paid teleports)
        if (clickType != ClickType.LEFT) return;
        player.closeInventory();
        if (owned || warp.getOwnerName() == null || warp.getOwnerName().isBlank()) {
            player.performCommand("playerwarps warp " + warp.getName());
        } else {
            player.performCommand("playerwarps warp " + warp.getOwnerName() + " " + warp.getName());
        }
    }

    private void handleButtonClick(String buttonKey) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        boolean isPageButton = buttonKey.equals("previous-page") || buttonKey.equals("next-page");

        if (isPageButton && lastPageChangeTime.containsKey(uuid) && now - lastPageChangeTime.get(uuid) < 400) {
            return;
        }
        if (isPageButton) lastPageChangeTime.put(uuid, now);

        playClickSound();

        switch (buttonKey) {
            case "previous-page":
                if (currentPage > 0) { currentPage--; open(); }
                break;
            case "next-page": {
                int perPage = Math.max(1, warpSlots.size());
                int totalPages = warps.isEmpty() ? 1 : (int) Math.ceil((double) warps.size() / perPage);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    open();
                }
                break;
            }
            case "filter-toggle":
                myWarpsOnly = !myWarpsOnly;
                if (myWarpsOnly) favouritesOnly = false;
                currentPage = 0;
                open();
                break;
            case "favourite":
                favouritesOnly = !favouritesOnly;
                if (favouritesOnly) myWarpsOnly = false;
                currentPage = 0;
                open();
                break;
            case "create-warp":
                player.closeInventory();
                player.sendMessage(plugin.getPlayerWarpsConfigManager().parse(
                        "<yellow>Use <white>/playerwarps set <name></white> to create a warp at your current location.</yellow>"));
                break;
            case "close":
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void playClickSound() {
        if (clickSound != null) {
            player.playSound(player.getLocation(), clickSound, clickSoundVolume, clickSoundPitch);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() != this) return;

        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize() || !top.equals(gui)) return;

        ClickType clickType = event.getClick();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            handleClick(rawSlot, clickType);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
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
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(gui)) return;
        UUID uuid = player.getUniqueId();
        lastPageChangeTime.remove(uuid);
        OPEN_GUIS.remove(uuid);
        unregister();
    }

    private static class ButtonConfig {
        int slot;
        ItemStack item;
        boolean hideIfNoPrevious;
        boolean hideIfNoNext;
        boolean updateOnEachPage;
        String rawNameTemplate;
        List<String> rawLoreTemplate;
    }
}
