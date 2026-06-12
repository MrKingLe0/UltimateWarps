package com.ultimatewarps;

import com.ultimatewarps.commands.*;
import com.ultimatewarps.gui.AdminGUI;
import com.ultimatewarps.gui.WarpGUI;
import com.ultimatewarps.listeners.ChatListener;
import com.ultimatewarps.listeners.GUIListener;
import com.ultimatewarps.listeners.JoinListener;
import com.ultimatewarps.listeners.MoveListener;
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

        registerCommands();
        registerListeners();

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
        activeTeleports.values().forEach(TeleportTask::cancel);
        activeTeleports.clear();
        
        instance = null;
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
    }

    private void registerCommands() {
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand());
        getCommand("delspawn").setExecutor(new DelSpawnCommand());
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("warp").setTabCompleter(new WarpCommand(this));
        getCommand("setwarp").setExecutor(new SetWarpCommand());
        getCommand("delwarp").setExecutor(new DelWarpCommand());
        getCommand("warpsadmin").setExecutor(new WarpsAdminCommand());
        getCommand("ultimatewarps").setExecutor(new UltimateWarpsCommand());
        getCommand("ultimatewarps").setTabCompleter(new UltimateWarpsCommand());
        getCommand("spawnforce").setExecutor(new SpawnForceCommand(this));
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

    public Map<UUID, TeleportTask> getActiveTeleports() {
        return activeTeleports;
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        warpManager.loadWarps();
    }
}