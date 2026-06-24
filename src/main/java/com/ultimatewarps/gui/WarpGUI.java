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
    
    // GUI settings
    private Component title;
    private int rows;
    private boolean fillEmptySlots;
    private ItemStack fillItem;
    private String clickSound;
    private float clickSoundVolume;
    private float clickSoundPitch;
    
    // Warp item settings
    private String warpNameFormat;
    private List<String> warpLoreFormat;
    private String warpHeadTexture;
    
    // Warp slots
    private List<Integer> warpSlots;
    
    // Button positions and items
    private Map<String, ButtonConfig> buttons;
    
    // Store custom item slots
    private Set<Integer> customItemSlots = new HashSet<>();

    // Bug fix: slots that actually hold a real warp item on the current page, as opposed
    // to a warp-slot that's merely empty/filled with a placeholder. Used so custom items
    // and buttons can correctly claim genuinely-empty warp slots without ever overwriting
    // an actual warp icon.
    private Set<Integer> occupiedWarpSlots = new HashSet<>();
    
    // Cooldowns - 400ms (0.4 seconds) per player
    private final Map<UUID, Long> lastSoundTime = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Long> lastPageChangeTime = new HashMap<>();
    
    // Track active instances
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
        File guiFile = new File(plugin.getDataFolder(), "warps-gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("warps-gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        
        ConfigurationSection guiSection = guiConfig.getConfigurationSection("gui");
        if (guiSection != null) {
            title = TextFormat.render(guiSection.getString("title", "&d&l Ultimate Warps "));
            rows = guiSection.getInt("rows", 6);
            if (rows < 1) rows = 1;
            if (rows > 6) rows = 6;
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
        
        ConfigurationSection warpSlotsSection = guiConfig.getConfigurationSection("warp-slots");
        if (warpSlotsSection != null) {
            String warpSlotMode = warpSlotsSection.getString("mode", "auto");
            if (warpSlotMode.equals("auto")) {
                int autoStartSlot = warpSlotsSection.getInt("auto-start", 0);
                int autoEndSlot = warpSlotsSection.getInt("auto-end", 44);
                warpSlots = new ArrayList<>();
                for (int i = autoStartSlot; i <= autoEndSlot; i++) {
                    warpSlots.add(i);
                }
            } else {
                warpSlots = warpSlotsSection.getIntegerList("slots");
                Collections.sort(warpSlots);
            }
        } else {
            warpSlots = new ArrayList<>();
            for (int i = 0; i <= 44; i++) warpSlots.add(i);
        }
        
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
                    // Bug fix: {current}/{total} substitution used to run on the legacy
                    // string form (meta.getDisplayName()/getLore()) AFTER the item was
                    // already built with the new Component-based loadItem(). That mismatch
                    // either lost MiniMessage styling (round-tripping through the legacy
                    // string form flattens it) or simply didn't see the placeholders if
                    // they'd already been escaped during MiniMessage parsing. Keeping the
                    // raw template strings here lets per-page substitution happen BEFORE
                    // rendering to a Component, exactly once, the same way warp names work.
                    btnConfig.rawNameTemplate = btnSection.getString("name", buttonKey);
                    btnConfig.rawLoreTemplate = btnSection.getStringList("lore");
                    buttons.put(buttonKey, btnConfig);
                }
            }
        }
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
        
        // 400ms sound cooldown
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
        
        // Bug fix: custom items used to unconditionally overwrite whatever was already in
        // a slot, including a real warp icon that had just been placed there. If a server
        // admin's warp-slots range overlapped a custom-items slot list (e.g. a wider
        // warp-slots range than the shipped default, or a custom border placed on the
        // wrong slots), warps would silently disappear from the GUI with no warning.
        // Custom items now only fill slots that are a) a warp slot with no warp on the
        // current page, or b) not a warp slot at all (pure decoration/buttons area).
        for (String itemKey : customSection.getKeys(false)) {
            ConfigurationSection itemSection = customSection.getConfigurationSection(itemKey);
            if (itemSection != null && itemSection.getBoolean("enabled", true)) {
                ItemStack customItem = loadItem(itemSection, "BARRIER", itemKey);
                List<Integer> slots = itemSection.getIntegerList("slots");
                for (int slot : slots) {
                    if (slot < 0 || slot >= rows * 9) continue;
                    if (occupiedWarpSlots.contains(slot)) {
                        // A real warp icon is on this slot - don't clobber it.
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
        
        // 400ms click cooldown (0.4 seconds)
        if (lastClickTime.containsKey(uuid) && now - lastClickTime.get(uuid) < 400) {
            return;
        }
        lastClickTime.put(uuid, now);
        
        if (customItemSlots.contains(slot)) {
            return;
        }
        
        // Bug fix: this used to re-fetch `warps` fresh from the accessible-warps list on
        // every click. That's dangerous: the slot a player just clicked was rendered
        // against the LIST AT RENDER TIME (in createGUI()), not whatever the list looks
        // like right now. If a warp was added/removed/became inaccessible between render
        // and click, this re-fetch could shift indices and either silently no-op (index
        // now out of bounds) or - worse - resolve the click to a completely different
        // warp than the one the player actually saw and clicked on. The `warps` field is
        // already kept correct by createGUI() every time the menu is (re)opened; reusing
        // it here keeps clicks consistent with what's on screen.
        
        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            ButtonConfig btn = entry.getValue();
            if (btn.slot == slot && isValidSlot(btn.slot)) {
                playClickSound(player, btn.clickSound);
                handleButtonClick(player, entry.getKey(), btn);
                return;
            }
        }
        
        if (warpSlots.contains(slot)) {
            int index = currentPage * warpSlots.size() + warpSlots.indexOf(slot);
            if (index < warps.size()) {
                playClickSound(player, null);
                player.closeInventory();
                player.performCommand("warp " + warps.get(index).getName());
            }
        }
    }
    
    private void createGUI() {
        int size = rows * 9;
        gui = Bukkit.createInventory(null, size, title);
        occupiedWarpSlots.clear();
        
        warps = new ArrayList<>(plugin.getWarpManager().getAccessibleWarps(player));
        
        if (warps.isEmpty()) {
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
        
        int warpsPerPage = warpSlots.size();
        
        if (currentPage < 0) {
            currentPage = 0;
        }
        
        int startIndex = currentPage * warpsPerPage;
        int endIndex = Math.min(startIndex + warpsPerPage, warps.size());
        
        if (startIndex >= warps.size() && currentPage > 0) {
            currentPage = (warps.size() - 1) / warpsPerPage;
            startIndex = currentPage * warpsPerPage;
            endIndex = Math.min(startIndex + warpsPerPage, warps.size());
        }
        
        if (startIndex < warps.size() && startIndex >= 0) {
            List<Warp> pageWarps = warps.subList(startIndex, endIndex);
            for (int i = 0; i < pageWarps.size() && i < warpSlots.size(); i++) {
                Warp warp = pageWarps.get(i);
                int slot = warpSlots.get(i);
                gui.setItem(slot, createWarpItem(warp));
                occupiedWarpSlots.add(slot);
            }
        }
        
        // Bug fix: custom items and buttons used to be placed after warp slots were
        // already backfilled with fill-item placeholders, and would unconditionally
        // overwrite whatever was there - including real warp icons, if their configured
        // slots ever overlapped the warp-slots range. Now custom items/buttons go first
        // and are only blocked by an *actual* warp (occupiedWarpSlots), and the
        // fill-item pass runs last to mop up whatever is still empty.
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
    
    private ItemStack createWarpItem(Warp warp) {
        // Bug fix: this used to always build a fresh item from the globally configured
        // default head texture and never looked at warp.getIcon() at all, so a custom
        // icon set via /warpsadmin was saved/reloaded correctly but never actually shown
        // in the player-facing warp GUI - it always rendered the default head instead.
        ItemStack customIcon = warp.getIcon();
        ItemStack item = customIcon != null ? customIcon : HeadUtils.getHead(warpHeadTexture);
        ItemMeta meta = item.getItemMeta();
        
        // Bug fix: {warp_name} was always substituted with warp.getName() (the internal
        // file name), completely ignoring warp.getDisplayName() - the custom name set via
        // /warpsadmin's "Set Display Name" button. That value was saved and reloaded
        // correctly, just never read here, so it had no visible effect in the GUI.
        String displayLabel = warp.getDisplayName() != null ? warp.getDisplayName() : warp.getName();
        
        // Bug fix: this used to substitute {warp_name} as a raw string into the template
        // BEFORE rendering, then render the combined string once. That breaks the moment
        // the template uses one format (e.g. '&' codes, like the default
        // "&6&l{warp_name}") and the display name uses another (MiniMessage tags, e.g.
        // "<gradient:red:blue>Castle") - per Adventure's own guidance, mixing MiniMessage
        // and legacy formatting in one string has no supported, correct behavior.
        // Rendering the template and the display label SEPARATELY (each in its own
        // correct format) and composing them as Component children sidesteps the problem
        // entirely - there's no restriction on mixing styles across a Component tree,
        // only within a single raw string handed to one parser.
        meta.displayName(renderTemplate(warpNameFormat, displayLabel, warp.getCooldown(), warp.getDelay()));
        
        List<Component> renderedLore = new ArrayList<>();
        for (String line : warpLoreFormat) {
            renderedLore.add(renderTemplate(line, displayLabel, warp.getCooldown(), warp.getDelay()));
        }
        meta.lore(renderedLore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Renders a GUI template string (which may use '&' codes, '§' codes, or MiniMessage
     * tags) with {warp_name}/{cooldown}/{delay} placeholders, substituting the warp's
     * display name as a SEPARATELY rendered Component rather than splicing it into the
     * template as a raw string. This is what lets a '&'-coded template (the shipped
     * default, e.g. "&6&l{warp_name}") and a MiniMessage-tagged display name (e.g.
     * "<gradient:red:blue>Castle") both render correctly together - each half is parsed
     * in its own correct format, then composed at the Component level, where mixing
     * styles is fully supported (unlike mixing them within one raw string).
     */
    /**
     * Renders a GUI template string (which may use '&' codes, '§' codes, or MiniMessage
     * tags) with {warp_name}/{cooldown}/{delay} placeholders. See
     * TextFormat.renderTemplate() for why each placeholder is rendered as its own
     * separate Component instead of being spliced into the template as a raw string.
     */
    private Component renderTemplate(String template, String displayLabel, int cooldown, int delay) {
        return TextFormat.renderTemplate(template,
            "{warp_name}", displayLabel,
            "{cooldown}", String.valueOf(cooldown),
            "{delay}", String.valueOf(delay));
    }
    
    private void placeButtons() {
        int totalPages = Math.max(1, (int) Math.ceil((double) warps.size() / warpSlots.size()));
        
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
            // Bug fix: same overwrite hazard as custom items - if a button's configured
            // slot is ever misconfigured to overlap a warp slot, don't let it hide a real
            // warp icon.
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
        
        // 400ms page change cooldown
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