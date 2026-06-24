package com.ultimatewarps.playerwarps.gui;

import com.ultimatewarps.TextFormat;
import com.ultimatewarps.HeadUtils;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
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
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The player-warps browser GUI. Entirely separate inventory/listener/layout file from
 * the admin WarpGUI - the two features share no GUI state, so a layout change to one
 * can never affect the other.
 */
public class PlayerWarpGUI implements Listener {

    private final UltimateWarps plugin;
    private final Player player;
    private Inventory gui;
    private int currentPage = 0;
    private boolean myWarpsOnly = false;
    private List<PlayerWarp> warps;
    private YamlConfiguration guiConfig;

    private Component title;
    private int rows;
    private boolean fillEmptySlots;
    private ItemStack fillItem;
    private String clickSound;
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

    public void unregister() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        activeInstances.remove(this);
    }

    public static void unregisterAll() {
        for (PlayerWarpGUI gui : new ArrayList<>(activeInstances)) {
            gui.unregister();
        }
        activeInstances.clear();
    }

    private void refreshWarpList() {
        List<PlayerWarp> visible = plugin.getPlayerWarpManager().getVisibleWarps(player);
        if (myWarpsOnly) {
            visible = visible.stream()
                    .filter(w -> w.getOwnerId().equals(player.getUniqueId()))
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
            clickSound = soundSection.getString("sound", "UI_BUTTON_CLICK");
            clickSoundVolume = (float) soundSection.getDouble("volume", 1.0);
            clickSoundPitch = (float) soundSection.getDouble("pitch", 1.0);
        } else {
            clickSound = "UI_BUTTON_CLICK";
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
        if (warpSlots.isEmpty()) {
            // Defensive: an empty list here would make warpsPerPage 0 and divide-by-zero
            // when computing total pages, if a server owner's config ever ends up empty.
            for (int i = 9; i <= 44; i++) warpSlots.add(i);
        }

        buttons = new HashMap<>();
        ConfigurationSection buttonsSection = guiConfig.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String key : buttonsSection.getKeys(false)) {
                ConfigurationSection btnSection = buttonsSection.getConfigurationSection(key);
                if (btnSection != null && btnSection.getBoolean("enabled", true)) {
                    ButtonConfig btn = new ButtonConfig();
                    btn.slot = btnSection.getInt("slot");
                    btn.item = loadItem(btnSection, "BARRIER", key);
                    btn.hideIfNoPrevious = btnSection.getBoolean("hide-if-no-previous", false);
                    btn.hideIfNoNext = btnSection.getBoolean("hide-if-no-next", false);
                    btn.updateOnEachPage = btnSection.getBoolean("update-on-each-page", false);
                    // Bug fix: substitution placeholders ({current}/{total}/{filter}) used
                    // to run on the legacy string form AFTER the item was already built as
                    // a Component via loadItem() - same mismatch as the admin WarpGUI had.
                    // Keeping the raw template here lets substitution happen before the one
                    // and only render pass.
                    btn.rawNameTemplate = btnSection.getString("name", key);
                    btn.rawLoreTemplate = btnSection.getStringList("lore");
                    buttons.put(key, btn);
                }
            }
        }
    }

    private ItemStack loadItem(ConfigurationSection section, String defaultMaterial, String defaultName) {
        if (section == null) return new ItemStack(Material.valueOf(defaultMaterial));
        Material material;
        try {
            material = Material.valueOf(section.getString("material", defaultMaterial));
        } catch (IllegalArgumentException e) {
            material = Material.valueOf(defaultMaterial);
        }

        // Bug fix: this never read "head-texture" from the config at all, so every
        // PLAYER_HEAD button (previous-page, next-page, etc.) always rendered as a plain
        // default Steve head no matter what base64 texture was configured. Same fix as
        // the admin WarpGUI's loadItem() - route through HeadUtils.getHead() whenever the
        // material is a player head and a texture is actually configured.
        ItemStack item;
        if (material == Material.PLAYER_HEAD && section.contains("head-texture")) {
            item = HeadUtils.getHead(section.getString("head-texture"));
            item.setAmount(section.getInt("amount", 1));
        } else {
            item = new ItemStack(material, section.getInt("amount", 1));
        }

        ItemMeta meta = item.getItemMeta();
        String name = section.getString("name", defaultName);
        if (name != null && !name.isEmpty()) {
            meta.displayName(TextFormat.render(name));
        }
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(TextFormat::render).collect(Collectors.toList()));
        }
        item.setItemMeta(meta);
        return item;
    }

    private void createGUI() {
        int size = rows * 9;
        gui = Bukkit.createInventory(null, size, title);
        occupiedWarpSlots.clear();

        if (warps.isEmpty()) {
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
            // Default icon: the owner's own head, a nice personal touch that needs no
            // texture configuration and is always available since every owner has a
            // real Minecraft skin.
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.getOwnerId());
                skullMeta.setOwningPlayer(owner);
                item.setItemMeta(skullMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        String visibility = warp.isPublic() ? "&aPublic" : "&cPrivate";
        String ownerName = warp.getOwnerName() != null ? warp.getOwnerName() : "Unknown";

        String nameFormat = owned ? ownWarpNameFormat : warpNameFormat;
        List<String> loreFormat = owned ? ownWarpLoreFormat : warpLoreFormat;

        // Bug fix: name/lore used to splice the display name, owner name, and visibility
        // text into the template as raw strings and render the combined string once. That
        // breaks the moment the template uses one format (e.g. '&' codes, like the
        // default templates) and the display name uses another (MiniMessage tags) - per
        // Adventure's own guidance, mixing MiniMessage and legacy formatting in one string
        // has no supported, correct behavior. TextFormat.renderTemplate() renders each
        // placeholder's value as its own Component and composes them, sidestepping the
        // problem entirely.
        PlayerWarpsConfigManager pwConfig = plugin.getPlayerWarpsConfigManager();
        meta.displayName(TextFormat.renderTemplate(nameFormat,
                "{warp_name}", displayLabel,
                "{owner}", ownerName,
                "{cooldown}", String.valueOf(pwConfig.cooldown()),
                "{delay}", String.valueOf(pwConfig.delay()),
                "{visibility}", visibility));

        List<Component> lore = new ArrayList<>();
        for (String line : loreFormat) {
            lore.add(TextFormat.renderTemplate(line,
                    "{warp_name}", displayLabel,
                    "{owner}", ownerName,
                    "{cooldown}", String.valueOf(pwConfig.cooldown()),
                    "{delay}", String.valueOf(pwConfig.delay()),
                    "{visibility}", visibility));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void loadCustomItems() {
        customItemSlots.clear();
        ConfigurationSection customSection = guiConfig.getConfigurationSection("custom-items");
        if (customSection == null) return;

        Set<Integer> buttonSlots = new HashSet<>();
        for (ButtonConfig btn : buttons.values()) buttonSlots.add(btn.slot);

        for (String itemKey : customSection.getKeys(false)) {
            ConfigurationSection itemSection = customSection.getConfigurationSection(itemKey);
            if (itemSection != null && itemSection.getBoolean("enabled", true)) {
                ItemStack customItem = loadItem(itemSection, "BARRIER", itemKey);
                for (int slot : itemSection.getIntegerList("slots")) {
                    if (slot < 0 || slot >= rows * 9) continue;
                    if (occupiedWarpSlots.contains(slot)) continue;
                    gui.setItem(slot, customItem.clone());
                    if (!buttonSlots.contains(slot)) customItemSlots.add(slot);
                }
            }
        }
    }

    private void placeButtons() {
        int perPage = warpSlots.size();
        int totalPages = warps.isEmpty() ? 1 : (int) Math.ceil((double) warps.size() / perPage);
        // For the info button we always want the player's TRUE total warp count
        // regardless of the current "my warps only" filter, so go straight to the
        // manager rather than counting the (possibly filtered) `warps` list.
        int trueOwnedCount = plugin.getPlayerWarpManager().countWarpsByOwner(player.getUniqueId());
        int limit = plugin.getPlayerWarpsConfigManager().getWarpLimit(player);
        String limitDisplay = limit >= Integer.MAX_VALUE ? "\u221E" : String.valueOf(limit);

        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            String key = entry.getKey();
            ButtonConfig btn = entry.getValue();

            if (key.equals("previous-page") && btn.hideIfNoPrevious && currentPage == 0) continue;
            if (key.equals("next-page") && btn.hideIfNoNext && currentPage >= totalPages - 1) continue;
            if (occupiedWarpSlots.contains(btn.slot)) continue;

            ItemStack item = btn.item.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (btn.rawNameTemplate != null) {
                    // Bug fix: only {filter} was ever substituted here - {current}/{total}
                    // (used by the info button's name: "ᴘᴀɢᴇ {current}/{total}") had no
                    // substitution at all and showed up as the literal placeholder text in
                    // game. Every dynamic token a button name might use is now substituted
                    // here in one place, the same way the lore substitution below already
                    // handles all four of its tokens.
                    String nameTemplate = btn.rawNameTemplate
                            .replace("{filter}", myWarpsOnly ? "&eMy Warps" : "&bAll Warps")
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
                                    .replace("{filter}", myWarpsOnly ? "&eMy Warps" : "&bAll Warps"))
                            .map(TextFormat::render)
                            .collect(Collectors.toList());
                    meta.lore(lore);
                }
                item.setItemMeta(meta);
            }

            gui.setItem(btn.slot, item);
        }
    }

    private void handleClick(int slot, boolean shiftClick) {
        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            if (entry.getValue().slot == slot) {
                handleButtonClick(entry.getKey());
                return;
            }
        }

        if (customItemSlots.contains(slot)) return;
        if (!occupiedWarpSlots.contains(slot)) return;

        int perPage = warpSlots.size();
        int indexInPage = warpSlots.indexOf(slot);
        if (indexInPage < 0) return;
        int index = currentPage * perPage + indexInPage;
        if (index < 0 || index >= warps.size()) return;

        PlayerWarp warp = warps.get(index);
        boolean owned = warp.getOwnerId().equals(player.getUniqueId());

        playClickSound();

        if (shiftClick && owned) {
            player.closeInventory();
            player.performCommand("playerwarps edit " + warp.getName());
            return;
        }

        player.closeInventory();
        if (owned) {
            player.performCommand("playerwarps warp " + warp.getName());
        } else {
            // Visit someone else's warp through the owner-qualified path so a name
            // collision between two different players' warps can never resolve wrong.
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

        switch (buttonKey) {
            case "previous-page":
                if (currentPage > 0) { currentPage--; open(); }
                break;
            case "next-page":
                currentPage++;
                open();
                break;
            case "filter-toggle":
                myWarpsOnly = !myWarpsOnly;
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
        try {
            player.playSound(player.getLocation(), Sound.valueOf(clickSound), clickSoundVolume, clickSoundPitch);
        } catch (IllegalArgumentException ignored) {
            // invalid sound name in config - skip silently, this is purely cosmetic
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(gui)) return;

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= gui.getSize()) return;

        handleClick(event.getRawSlot(), event.isShiftClick());
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
