package com.ultimatewarps;

import com.ultimatewarps.commands.*;
import com.ultimatewarps.gui.AdminGUI;
import com.ultimatewarps.gui.WarpGUI;
import com.ultimatewarps.listeners.ChatListener;
import com.ultimatewarps.listeners.GUIListener;
import com.ultimatewarps.listeners.JoinListener;
import com.ultimatewarps.listeners.MoveListener;
import com.ultimatewarps.playerwarps.PlayerWarpManager;
import com.ultimatewarps.playerwarps.PlayerWarpTeleportTask;
import com.ultimatewarps.playerwarps.PlayerWarpsConfigManager;
import com.ultimatewarps.playerwarps.commands.PlayerWarpsCommand;
import com.ultimatewarps.playerwarps.gui.PlayerWarpGUI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UltimateWarps extends JavaPlugin {

    private static UltimateWarps instance;
    private ConfigManager configManager;
    private WarpManager warpManager;
    private CooldownManager cooldownManager;
    private EffectManager effectManager;
    private final Map<UUID, TeleportTask> activeTeleports = new HashMap<>();

    // ===== Player Warps - kept as entirely separate fields/managers from admin warps =====
    private PlayerWarpsConfigManager playerWarpsConfigManager;
    private PlayerWarpManager playerWarpManager;
    private final Map<UUID, PlayerWarpTeleportTask> activePlayerWarpTeleports = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!new File(getDataFolder(), "warps-gui.yml").exists()) {
            saveResource("warps-gui.yml", false);
        }
        configManager = new ConfigManager(this);
        warpManager = new WarpManager(this);
        warpManager.loadWarps();
        cooldownManager = new CooldownManager();
        effectManager = new EffectManager(this);

        // Player warps - separate config file, separate storage folder, separate manager.
        // Initialized after the admin-warp managers above so getPlayerWarpManager() etc.
        // are never called before they exist, but everything here is otherwise fully
        // independent of the admin warp system.
        playerWarpsConfigManager = new PlayerWarpsConfigManager(this);
        playerWarpManager = new PlayerWarpManager(this);
        playerWarpManager.loadAll();

        registerCommands();
        registerListeners();

        // Improvement: sweep expired cooldown entries every 10 minutes so the cooldown
        // map doesn't grow forever on a long-running server with many unique players.
        // Only removes entries that have already expired, so it can never be used to
        // dodge an active cooldown.
        Bukkit.getScheduler().runTaskTimer(this, () -> cooldownManager.purgeExpired(), 20L * 60 * 10, 20L * 60 * 10);

        Bukkit.getConsoleSender().sendMessage(
                "\n" +
                "§d ==============================================================================\n" +
                "§b   __  ______  _            __        _      __                \n" +
                "§b  / / / / / /_(_)_ _  ___ _/ /____   | | /| / /__ ________  ___\n" +
                "§b / /_/ / / __/ /  ' \\/ _ `/ __/ -_)  | |/ |/ / _ `/ __/ _ \\/_-<\n" +
                "§b \\____/_/\\__/_/_/_/_/\\_,_/\\__/\\__/   |__/|__/\\_,_/_/ / .__/___/\n" +
                "§b                                                     /_/         \n\n" +
                "§d =============================================================================="
        );
        Bukkit.getConsoleSender().sendMessage("§dUltimateWarps v" + getDescription().getVersion() + " enabled!\n§aBy §1King_Le0_");
    }

    @Override
    public void onDisable() {
        if (warpManager != null) {
            warpManager.saveAllWarps();
        }
        if (playerWarpManager != null) {
            playerWarpManager.saveAll();
        }
        // Bug fix: iterating activeTeleports.values() while each cancel() call removes its
        // own entry from that same map throws ConcurrentModificationException. Snapshot the
        // tasks first, and do this before nulling out `instance` since TeleportMap#cancel()
        // looks the plugin instance back up internally.
        new java.util.ArrayList<>(activeTeleports.values()).forEach(TeleportTask::cancel);
        activeTeleports.clear();

        new java.util.ArrayList<>(activePlayerWarpTeleports.values()).forEach(PlayerWarpTeleportTask::cancel);
        activePlayerWarpTeleports.clear();
        
        Bukkit.getConsoleSender().sendMessage(
                "\n"+
                "§d ==============================================================================\n" +
                "§b   __  ______  _            __        _      __                \n" +
                "§b  / / / / / /_(_)_ _  ___ _/ /____   | | /| / /__ ________  ___\n" +
                "§b / /_/ / / __/ /  ' \\/ _ `/ __/ -_)  | |/ |/ / _ `/ __/ _ \\/_-<\n" +
                "§b \\____/_/\\__/_/_/_/_/\\_,_/\\__/\\__/   |__/|__/\\_,_/_/ / .__/___/\n" +
                "§b                                                     /_/         \n\n" +
                "§d =============================================================================="
        );
        Bukkit.getConsoleSender().sendMessage("§cUltimateWarps disabled!");
        instance = null;
    }

    private WarpCommand warpCommand;
    private SpawnCommand spawnCommand;

    private void registerCommands() {
        spawnCommand = new SpawnCommand(this);
        getCommand("spawn").setExecutor(spawnCommand);
        getCommand("setspawn").setExecutor(new SetSpawnCommand());
        getCommand("delspawn").setExecutor(new DelSpawnCommand());
        // Improvement: this used to construct two separate WarpCommand instances - one
        // as the executor, a different one as the tab completer - so they never shared
        // state. Harmless today since tab-complete doesn't read the cooldown-message
        // maps, but it's wasted allocation and a trap for future changes. Same fix
        // applied to UltimateWarpsCommand below.
        warpCommand = new WarpCommand(this);
        getCommand("warp").setExecutor(warpCommand);
        getCommand("warp").setTabCompleter(warpCommand);
        getCommand("setwarp").setExecutor(new SetWarpCommand());
        DelWarpCommand delWarpCommand = new DelWarpCommand();
        getCommand("delwarp").setExecutor(delWarpCommand);
        getCommand("delwarp").setTabCompleter(delWarpCommand);
        getCommand("warpsadmin").setExecutor(new WarpsAdminCommand());
        UltimateWarpsCommand uwarpsCommand = new UltimateWarpsCommand();
        getCommand("ultimatewarps").setExecutor(uwarpsCommand);
        getCommand("ultimatewarps").setTabCompleter(uwarpsCommand);
        getCommand("spawnforce").setExecutor(new SpawnForceCommand(this));

        // Player warps - entirely separate command tree from the admin warp commands above.
        PlayerWarpsCommand playerWarpsCommand = new PlayerWarpsCommand(this);
        getCommand("playerwarps").setExecutor(playerWarpsCommand);
        getCommand("playerwarps").setTabCompleter(playerWarpsCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
    }

    public static UltimateWarps getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public WarpCommand getWarpCommand() {
        return warpCommand;
    }

    public SpawnCommand getSpawnCommand() {
        return spawnCommand;
    }

    public Map<UUID, TeleportTask> getActiveTeleports() {
        return activeTeleports;
    }

    public PlayerWarpsConfigManager getPlayerWarpsConfigManager() {
        return playerWarpsConfigManager;
    }

    public PlayerWarpManager getPlayerWarpManager() {
        return playerWarpManager;
    }

    public Map<UUID, PlayerWarpTeleportTask> getActivePlayerWarpTeleports() {
        return activePlayerWarpTeleports;
    }

    public void reloadPlugin() {
        WarpGUI.unregisterAll();
        com.ultimatewarps.playerwarps.gui.PlayerWarpGUI.unregisterAll();

        reloadConfig();
        configManager.reload();
        warpManager.loadWarps();

        playerWarpsConfigManager.reload();
        playerWarpManager.loadAll();
    }
}