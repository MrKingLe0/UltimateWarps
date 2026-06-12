package com.ultimatewarps.gui;

import com.ultimatewarps.HeadUtils;
import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.Warp;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private String title;
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
    
    // Store custom item slots to make them non-clickable
    private Set<Integer> customItemSlots = new HashSet<>();
    
    // Sound cooldown to prevent multiple sounds
    private final Map<UUID, Long> lastSoundTime = new HashMap<>();
    
    // Prevent duplicate listener registration
    private static boolean listenerRegistered = false;
    
    public WarpGUI(UltimateWarps plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.warps = new ArrayList<>(plugin.getWarpManager().getAccessibleWarps(player));
        loadGuiConfig();
        
        // Only register once
        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
    }
    
    public WarpGUI(Player player) {
        this(UltimateWarps.getInstance(), player);
    }
    
    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < (rows * 9);
    }
    
    public void open(int page) {
        this.currentPage = page;
        open();
    }
    
    private void loadGuiConfig() {
        File guiFile = new File(plugin.getDataFolder(), "warps-gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("warps-gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        
        // Load GUI settings
        ConfigurationSection guiSection = guiConfig.getConfigurationSection("gui");
        if (guiSection != null) {
            title = ChatColor.translateAlternateColorCodes('&', guiSection.getString("title", "&d&l Ultimate Warps "));
            rows = guiSection.getInt("rows", 6);
            if (rows < 1) rows = 1;
            if (rows > 6) rows = 6;
            fillEmptySlots = guiSection.getBoolean("fill-empty-slots", true);
        } else {
            title = ChatColor.translateAlternateColorCodes('&', "&d&l Ultimate Warps ");
            rows = 6;
            fillEmptySlots = true;
        }
        
        // Load click sound settings
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
        
        // Load fill item
        fillItem = loadItem(guiConfig.getConfigurationSection("gui.fill-item"), "GRAY_STAINED_GLASS_PANE", " ");
        
        // Load warp item template
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
        
        // Load warp slots configuration
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
            }
        } else {
            warpSlots = new ArrayList<>();
            for (int i = 0; i <= 44; i++) warpSlots.add(i);
        }
        
        // Load buttons
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
        
        // Use HeadUtils for player heads with textures
        if (material == Material.PLAYER_HEAD && section.contains("head-texture")) {
            String texture = section.getString("head-texture");
            item = HeadUtils.getHead(texture);
        } else {
            item = new ItemStack(material, section.getInt("amount", 1));
        }
        
        ItemMeta meta = item.getItemMeta();
        
        String name = section.getString("name", defaultName);
        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> coloredLore = lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
            meta.setLore(coloredLore);
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
        
        // Prevent sound spam - only play every 200ms
        if (lastSoundTime.containsKey(uuid) && now - lastSoundTime.get(uuid) < 200) {
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
        
        // Get all button slots first so we don't mark them as custom items
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
                    if (slot >= 0 && slot < rows * 9) {
                        gui.setItem(slot, customItem.clone());
                        // Only mark as custom item if it's NOT a button slot
                        if (!buttonSlots.contains(slot)) {
                            customItemSlots.add(slot);
                        }
                    }
                }
            }
        }
    }
    
    public void handleClick(int slot) {
        // Don't process clicks on custom decorative items
        if (customItemSlots.contains(slot)) {
            return;
        }
        
        warps = new ArrayList<>(plugin.getWarpManager().getAccessibleWarps(player));
        
        // Check buttons first
        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            ButtonConfig btn = entry.getValue();
            if (btn.slot == slot && isValidSlot(btn.slot)) {
                playClickSound(player, btn.clickSound);
                handleButtonClick(player, entry.getKey(), btn);
                return;
            }
        }
        
        // Check warp slots - FIX: Make sure slot numbers are correct
        // Debug output to see what slots are being clicked
        plugin.getLogger().info("Clicked slot: " + slot);
        plugin.getLogger().info("Warp slots: " + warpSlots);
        plugin.getLogger().info("Current page: " + currentPage);
        plugin.getLogger().info("Total warps: " + warps.size());
        
        if (warpSlots.contains(slot)) {
            int index = currentPage * warpSlots.size() + warpSlots.indexOf(slot);
            plugin.getLogger().info("Calculated index: " + index);
            if (index < warps.size()) {
                playClickSound(player, null);
                player.closeInventory();
                player.performCommand("warp " + warps.get(index).getName());
            }
        } else {
            plugin.getLogger().info("Slot " + slot + " is not in warp slots list");
        }
    }
    
    public void open() {
        createGUI();
        player.openInventory(gui);
    }
    
    private void createGUI() {
        int size = rows * 9;
        gui = Bukkit.createInventory(null, size, title);
        
        warps = new ArrayList<>(plugin.getWarpManager().getAccessibleWarps(player));
        
        // Safety check: if no warps, just show empty GUI
        if (warps.isEmpty()) {
            // Fill with filler and place buttons only
            if (fillEmptySlots) {
                for (int i = 0; i < size; i++) {
                    gui.setItem(i, fillItem.clone());
                }
            }
            loadCustomItems();
            placeButtons();
            player.openInventory(gui);
            return;
        }
        
        int warpsPerPage = warpSlots.size();
        
        // Prevent negative page
        if (currentPage < 0) {
            currentPage = 0;
        }
        
        int startIndex = currentPage * warpsPerPage;
        int endIndex = Math.min(startIndex + warpsPerPage, warps.size());
        
        // If startIndex is out of bounds, go to last valid page
        if (startIndex >= warps.size() && currentPage > 0) {
            currentPage = (warps.size() - 1) / warpsPerPage;
            startIndex = currentPage * warpsPerPage;
            endIndex = Math.min(startIndex + warpsPerPage, warps.size());
        }
        
        // Place warp items
        if (startIndex < warps.size() && startIndex >= 0) {
            List<Warp> pageWarps = warps.subList(startIndex, endIndex);
            for (int i = 0; i < pageWarps.size() && i < warpSlots.size(); i++) {
                Warp warp = pageWarps.get(i);
                int slot = warpSlots.get(i);
                gui.setItem(slot, createWarpItem(warp));
            }
        }
        
        // Fill empty warp slots with filler
        if (fillEmptySlots) {
            for (int slot : warpSlots) {
                if (gui.getItem(slot) == null) {
                    gui.setItem(slot, fillItem.clone());
                }
            }
        }
        
        // Load custom decorative items
        loadCustomItems();
        
        // Place buttons
        placeButtons();
        
        // Fill remaining empty slots
        if (fillEmptySlots) {
            for (int i = 0; i < size; i++) {
                if (gui.getItem(i) == null && !customItemSlots.contains(i) && !isButtonSlot(i)) {
                    gui.setItem(i, fillItem.clone());
                }
            }
        }
    }
    
    private boolean isButtonSlot(int slot) {
        for (ButtonConfig btn : buttons.values()) {
            if (btn.slot == slot) {
                return true;
            }
        }
        return false;
    }
    
    private ItemStack createWarpItem(Warp warp) {
        ItemStack item = HeadUtils.getHead(warpHeadTexture);
        ItemMeta meta = item.getItemMeta();
        
        // Apply warp name
        String name = warpNameFormat.replace("{warp_name}", warp.getName())
            .replace("{cooldown}", String.valueOf(warp.getCooldown()))
            .replace("{delay}", String.valueOf(warp.getDelay()));
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        
        // Apply lore
        List<String> coloredLore = new ArrayList<>();
        for (String line : warpLoreFormat) {
            String processed = line.replace("{warp_name}", warp.getName())
                .replace("{cooldown}", String.valueOf(warp.getCooldown()))
                .replace("{delay}", String.valueOf(warp.getDelay()));
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', processed));
        }
        meta.setLore(coloredLore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private void placeButtons() {
        int totalPages = Math.max(1, (int) Math.ceil((double) warps.size() / warpSlots.size()));
        
        for (Map.Entry<String, ButtonConfig> entry : buttons.entrySet()) {
            String key = entry.getKey();
            ButtonConfig btn = entry.getValue();
            
            if (!isValidSlot(btn.slot)) {
                continue;
            }
            
            // Check visibility conditions
            if (key.equals("previous-page") && btn.hideIfNoPrevious && currentPage == 0) {
                continue;
            }
            if (key.equals("next-page") && btn.hideIfNoNext && currentPage >= totalPages - 1) {
                continue;
            }
            
            ItemStack item = btn.item.clone();
            
            // Update dynamic items (like page info)
            if (btn.updateOnEachPage && item.getItemMeta() != null) {
                ItemMeta meta = item.getItemMeta();
                String displayName = meta.getDisplayName();
                displayName = displayName.replace("{current}", String.valueOf(currentPage + 1))
                    .replace("{total}", String.valueOf(totalPages));
                meta.setDisplayName(displayName);
                
                if (meta.getLore() != null) {
                    List<String> lore = meta.getLore().stream()
                        .map(line -> line.replace("{current}", String.valueOf(currentPage + 1))
                            .replace("{total}", String.valueOf(totalPages)))
                        .collect(Collectors.toList());
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
            }
            
            gui.setItem(btn.slot, item);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!event.getView().getTitle().equals(title)) return;
        event.setCancelled(true);
        handleClick(event.getRawSlot());
    }
    
    private void handleButtonClick(Player clicker, String buttonKey, ButtonConfig btn) {
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
    }
}