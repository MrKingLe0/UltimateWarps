package com.ultimatewarps.gui;

import com.ultimatewarps.HeadUtils;
import com.ultimatewarps.TextFormat;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.Warp;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class WarpGUI implements Listener {

    private final UltimateWarps plugin;
    private final Player player;
    private Inventory gui;
    private int currentPage = 0;
    private List<Warp> warps;
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
    private String warpHeadTexture;

    private List<Integer> warpSlots;

    private Map<String, ButtonConfig> buttons;

    private Set<Integer> customItemSlots = new HashSet<>();

    private Set<Integer> occupiedWarpSlots = new HashSet<>();

    private final Map<Integer, Warp> slotToWarp = new HashMap<>();

    private int totalPages = 1;

    private final Map<UUID, Long> lastSoundTime = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Long> lastPageChangeTime = new HashMap<>();

    private static final Set<WarpGUI> activeInstances = new HashSet<>();
    private static final Map<UUID, WarpGUI> OPEN_GUIS = new HashMap<>();

    public WarpGUI(UltimateWarps plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.warps = new ArrayList<>(plugin.getWarpManager().getAccessibleWarps(player));
        loadGuiConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        activeInstances.add(this);
    }

    public WarpGUI(Player player) {
        this(UltimateWarps.getInstance(), player);
    }
    public static WarpGUI getOrCreate(UltimateWarps plugin, Player player) {
        return OPEN_GUIS.computeIfAbsent(
            player.getUniqueId(),
            uuid -> new WarpGUI(plugin, player)
        );
    }

    public void unregister() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        activeInstances.remove(this);
    }

    public static void unregisterAll() {
        for (WarpGUI gui : new ArrayList<>(activeInstances)) {
            gui.unregister();
        }
        activeInstances.clear();
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < (rows * 9);
    }

    public void open(int page) {
        this.currentPage = page;
        open();
    }

    public void open() {
        createGUI();
        player.openInventory(gui);
    }

    private void loadGuiConfig() {
        guiConfig = loadGuiFile(plugin);

        ConfigurationSection guiSection = guiConfig.getConfigurationSection("gui");
        if (guiSection != null) {
            title = TextFormat.render(guiSection.getString("title", "&d&l Ultimate Warps "));
            rows = parseRows(guiConfig);
            fillEmptySlots = guiSection.getBoolean("fill-empty-slots", true);
        } else {
            title = TextFormat.render("&d&l Ultimate Warps ");
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

        fillItem = loadItem(guiConfig.getConfigurationSection("gui.fill-item"), "GRAY_STAINED_GLASS_PANE", " ");

        ConfigurationSection warpItemSection = guiConfig.getConfigurationSection("warp-item");
        if (warpItemSection != null) {
            warpNameFormat = warpItemSection.getString("name", "&6&l{warp_name}");
            warpLoreFormat = warpItemSection.getStringList("lore");
            warpHeadTexture = warpItemSection.getString("head-texture", HeadUtils.WARP_ICON);
        } else {
            warpNameFormat = "&6&l{warp_name}";
            warpLoreFormat = new ArrayList<>();
            warpHeadTexture = HeadUtils.WARP_ICON;
        }

        warpSlots = parseWarpSlots(guiConfig);

        buttons = new HashMap<>();
        ConfigurationSection buttonsSection = guiConfig.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonKey : buttonsSection.getKeys(false)) {
                ConfigurationSection btnSection = buttonsSection.getConfigurationSection(buttonKey);
                if (btnSection != null && btnSection.getBoolean("enabled", true)) {
                    ButtonConfig btnConfig = new ButtonConfig();
                    btnConfig.slot = btnSection.getInt("slot");
                    btnConfig.item = loadItem(btnSection, "BARRIER", buttonKey);
                    btnConfig.hideIfNoPrevious = btnSection.getBoolean("hide-if-no-previous", false);
                    btnConfig.hideIfNoNext = btnSection.getBoolean("hide-if-no-next", false);
                    btnConfig.updateOnEachPage = btnSection.getBoolean("update-on-each-page", false);
                    btnConfig.command = btnSection.getString("command", null);
                    btnConfig.closeOnClick = btnSection.getBoolean("close-on-click", false);
                    btnConfig.clickSound = btnSection.getString("click-sound", null);

                    btnConfig.rawNameTemplate = btnSection.getString("name", buttonKey);
                    btnConfig.rawLoreTemplate = btnSection.getStringList("lore");
                    buttons.put(buttonKey, btnConfig);
                }
            }
        }
    }

    private static YamlConfiguration loadGuiFile(UltimateWarps plugin) {
        File guiFile = new File(plugin.getDataFolder(), "warps-gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("warps-gui.yml", false);
        }
        return YamlConfiguration.loadConfiguration(guiFile);
    }

    private static int parseRows(YamlConfiguration cfg) {
        ConfigurationSection guiSection = cfg.getConfigurationSection("gui");
        int r = guiSection != null ? guiSection.getInt("rows", 6) : 6;
        if (r < 1) r = 1;
        if (r > 6) r = 6;
        return r;
    }

    private static List<Integer> parseWarpSlots(YamlConfiguration cfg) {
        ConfigurationSection warpSlotsSection = cfg.getConfigurationSection("warp-slots");
        List<Integer> slots = new ArrayList<>();
        if (warpSlotsSection != null) {
            String mode = warpSlotsSection.getString("mode", "auto");
            if (mode.equals("auto")) {
                int autoStartSlot = warpSlotsSection.getInt("auto-start", 0);
                int autoEndSlot = warpSlotsSection.getInt("auto-end", 44);
                for (int i = autoStartSlot; i <= autoEndSlot; i++) slots.add(i);
            } else {
                slots = warpSlotsSection.getIntegerList("slots");
                Collections.sort(slots);
            }
        } else {
            for (int i = 0; i <= 44; i++) slots.add(i);
        }
        return slots;
    }

    public static int loadConfiguredRows(UltimateWarps plugin) {
        return parseRows(loadGuiFile(plugin));
    }

    public static List<Integer> loadConfiguredWarpSlots(UltimateWarps plugin) {
        return parseWarpSlots(loadGuiFile(plugin));
    }

    private ItemStack loadItem(ConfigurationSection section, String defaultMaterial, String defaultName) {
        if (section == null) {
            return new ItemStack(Material.valueOf(defaultMaterial));
        }

        String materialName = section.getString("material", defaultMaterial);
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.valueOf(defaultMaterial);
        }

        ItemStack item;

        if (material == Material.PLAYER_HEAD && section.contains("head-texture")) {
            String texture = section.getString("head-texture");
            item = HeadUtils.getHead(texture);
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
            List<Component> renderedLore = lore.stream()
                .map(TextFormat::render)
                .collect(Collectors.toList());
            meta.lore(renderedLore);
        }

        if (section.getBoolean("enchanted", false)) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private void playClickSound(Player player, String customSound) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastSoundTime.containsKey(uuid) && now - lastSoundTime.get(uuid) < 400) {
            return;
        }
        lastSoundTime.put(uuid, now);

        String soundToPlay = customSound != null ? customSound : clickSound;
        Sound sound = null;

        try {
            sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(soundToPlay.toLowerCase()));
            if (sound == null) {
                sound = Sound.UI_BUTTON_CLICK;
            }
        } catch (Exception e) {
            sound = Sound.UI_BUTTON_CLICK;
        }

        if (sound != null) {
            player.playSound(player.getLocation(), sound, clickSoundVolume, clickSoundPitch);
        }
    }

    private void loadCustomItems() {
        customItemSlots.clear();
        ConfigurationSection customSection = guiConfig.getConfigurationSection("custom-items");
        if (customSection == null) return;

        Set<Integer> buttonSlots = new HashSet<>();
        for (ButtonConfig btn : buttons.values()) {
            buttonSlots.add(btn.slot);
        }

        for (String itemKey : customSection.getKeys(false)) {
            ConfigurationSection itemSection = customSection.getConfigurationSection(itemKey);
            if (itemSection != null && itemSection.getBoolean("enabled", true)) {
                ItemStack customItem = loadItem(itemSection, "BARRIER", itemKey);
                List<Integer> slots = itemSection.getIntegerList("slots");
                for (int slot : slots) {
                    if (slot < 0 || slot >= rows * 9) continue;
                    if (occupiedWarpSlots.contains(slot)) {

                        continue;
                    }
                    gui.setItem(slot, customItem.clone());
                    if (!buttonSlots.contains(slot)) {
                        customItemSlots.add(slot);
                    }
                }
            }
        }
    }

    public void handleClick(int slot) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastClickTime.containsKey(uuid) && now - lastClickTime.get(uuid) < 400) {
            return;
        }
        lastClickTime.put(uuid, now);

        if (customItemSlots.contains(slot)) {
            return;
        }

        Warp clickedWarp = slotToWarp.get(slot);
        if (clickedWarp != null) {
            playClickSound(player, null);
            player.closeInventory();
            player.performCommand("warp " + clickedWarp.getName());
            return;
        }

        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            ButtonConfig btn = entry.getValue();
            if (btn.slot == slot && isValidSlot(btn.slot)) {
                playClickSound(player, btn.clickSound);
                handleButtonClick(player, entry.getKey(), btn);
                return;
            }
        }
    }

    private void createGUI() {
        int size = rows * 9;
        gui = Bukkit.createInventory(null, size, title);
        occupiedWarpSlots.clear();
        slotToWarp.clear();

        warps = new ArrayList<>(plugin.getWarpManager().getAccessibleWarps(player));

        if (warps.isEmpty()) {
            totalPages = 1;
            loadCustomItems();
            placeButtons();
            if (fillEmptySlots) {
                for (int i = 0; i < size; i++) {
                    if (gui.getItem(i) == null) {
                        gui.setItem(i, fillItem.clone());
                    }
                }
            }
            return;
        }

        LinkedHashMap<Integer, Warp> pins = new LinkedHashMap<>();
        List<Warp> flow = new ArrayList<>();
        for (Warp w : warps) {
            int s = w.getSlot();
            if (s >= 0 && warpSlots.contains(s) && !pins.containsKey(s)) {
                pins.put(s, w);
            } else {
                flow.add(w);
            }
        }

        int per = warpSlots.size();
        int firstCap = 0;
        if (per == 0) {

            totalPages = 1;
        } else {

            firstCap = Math.max(0, per - pins.size());
            int remaining = Math.max(0, flow.size() - firstCap);
            totalPages = 1 + (int) Math.ceil(remaining / (double) per);
        }

        if (currentPage < 0) currentPage = 0;
        if (currentPage > totalPages - 1) currentPage = totalPages - 1;

        if (per > 0) {
            if (currentPage == 0) {

                for (Map.Entry<Integer, Warp> e : pins.entrySet()) {
                    place(e.getKey(), e.getValue());
                }
                int fi = 0;
                for (int slot : warpSlots) {
                    if (pins.containsKey(slot)) continue;
                    if (fi >= flow.size()) break;
                    place(slot, flow.get(fi++));
                }
            } else {

                int fi = firstCap + (currentPage - 1) * per;
                for (int slot : warpSlots) {
                    if (fi >= flow.size()) break;
                    place(slot, flow.get(fi++));
                }
            }
        }

        loadCustomItems();
        placeButtons();

        if (fillEmptySlots) {
            for (int i = 0; i < size; i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, fillItem.clone());
                }
            }
        }
    }

    private void place(int slot, Warp warp) {
        gui.setItem(slot, createWarpItem(warp));
        occupiedWarpSlots.add(slot);
        slotToWarp.put(slot, warp);
    }

    private ItemStack createWarpItem(Warp warp) {

        ItemStack customIcon = HeadUtils.cleanIcon(warp.getIcon());
        ItemStack item = customIcon != null ? customIcon : HeadUtils.getHead(warpHeadTexture);
        ItemMeta meta = item.getItemMeta();

        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();

        meta.displayName(renderTemplate(warpNameFormat, displayLabel, warp.getCooldown(), warp.getDelay()));

        List<Component> renderedLore = new ArrayList<>();

        for (String line : warp.getCustomLore()) {
            renderedLore.add(renderTemplate(line, displayLabel, warp.getCooldown(), warp.getDelay()));
        }
        for (String line : warpLoreFormat) {
            renderedLore.add(renderTemplate(line, displayLabel, warp.getCooldown(), warp.getDelay()));
        }
        meta.lore(renderedLore);

        if (warp.isGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private Component renderTemplate(String template, String displayLabel, int cooldown, int delay) {
        return TextFormat.renderTemplate(template,
            "{warp_name}", displayLabel,
            "{cooldown}", String.valueOf(cooldown),
            "{delay}", String.valueOf(delay));
    }

    private void placeButtons() {

        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            String key = entry.getKey();
            ButtonConfig btn = entry.getValue();

            if (!isValidSlot(btn.slot)) {
                continue;
            }

            if (key.equals("previous-page") && btn.hideIfNoPrevious && currentPage == 0) {
                continue;
            }
            if (key.equals("next-page") && btn.hideIfNoNext && currentPage >= totalPages - 1) {
                continue;
            }

            if (occupiedWarpSlots.contains(btn.slot)) {
                continue;
            }

            ItemStack item = btn.item.clone();

            if (btn.updateOnEachPage) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (btn.rawNameTemplate != null) {
                        String substituted = btn.rawNameTemplate
                            .replace("{current}", String.valueOf(currentPage + 1))
                            .replace("{total}", String.valueOf(totalPages));
                        meta.displayName(TextFormat.render(substituted));
                    }
                    if (btn.rawLoreTemplate != null && !btn.rawLoreTemplate.isEmpty()) {
                        List<Component> lore = btn.rawLoreTemplate.stream()
                            .map(line -> line.replace("{current}", String.valueOf(currentPage + 1))
                                .replace("{total}", String.valueOf(totalPages)))
                            .map(TextFormat::render)
                            .collect(Collectors.toList());
                        meta.lore(lore);
                    }
                    item.setItemMeta(meta);
                }
            }

            gui.setItem(btn.slot, item);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId()))
            return;

        if (!event.getInventory().equals(gui))
            return;

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= gui.getSize())
            return;

        handleClick(event.getRawSlot());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(gui)) return;

        UUID uuid = player.getUniqueId();

        lastClickTime.remove(uuid);
        lastPageChangeTime.remove(uuid);
        lastSoundTime.remove(uuid);

        OPEN_GUIS.remove(uuid);
        unregister();
    }

    private void handleButtonClick(Player clicker, String buttonKey, ButtonConfig btn) {
        UUID uuid = clicker.getUniqueId();
        long now = System.currentTimeMillis();
        boolean isPageButton = buttonKey.equals("previous-page") || buttonKey.equals("next-page");

        if (isPageButton && lastPageChangeTime.containsKey(uuid) && now - lastPageChangeTime.get(uuid) < 400) {
            return;
        }
        if (isPageButton) {
            lastPageChangeTime.put(uuid, now);
        }

        switch (buttonKey) {
            case "previous-page":
                if (currentPage > 0) {
                    currentPage--;
                    open();
                }
                break;
            case "next-page":
                currentPage++;
                open();
                break;
            case "spawn":
                clicker.closeInventory();
                clicker.performCommand("spawn");
                break;
            case "close":
                clicker.closeInventory();
                break;
            case "refresh":
                warps = new ArrayList<>(plugin.getWarpManager().getAccessibleWarps(player));
                open();
                break;
            default:
                if (btn != null && btn.command != null && !btn.command.isEmpty()) {
                    if (btn.closeOnClick) {
                        clicker.closeInventory();
                    }
                    clicker.performCommand(btn.command);
                }
                break;
        }
    }

    private class ButtonConfig {
        int slot;
        ItemStack item;
        boolean hideIfNoPrevious;
        boolean hideIfNoNext;
        boolean updateOnEachPage;
        String command;
        boolean closeOnClick;
        String clickSound;
        String rawNameTemplate;
        List<String> rawLoreTemplate;
    }
}
