package com.ultimatewarps;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.ultimatewarps.commands.DelSpawnCommand;
import com.ultimatewarps.commands.DelWarpCommand;
import com.ultimatewarps.commands.SetSpawnCommand;
import com.ultimatewarps.commands.SetWarpCommand;
import com.ultimatewarps.commands.SpawnCommand;
import com.ultimatewarps.commands.SpawnForceCommand;
import com.ultimatewarps.commands.UltimateWarpsCommand;
import com.ultimatewarps.commands.WarpCommand;
import com.ultimatewarps.commands.WarpsAdminCommand;
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
import com.ultimatewarps.playerwarps.gui.PlayerWarpEditGUI;
import com.ultimatewarps.playerwarps.gui.PlayerWarpGUI;
import com.ultimatewarps.playerwarps.gui.PlayerWarpListGUI;
import com.ultimatewarps.playerwarps.gui.PlayerWarpRateGUI;

public final class UltimateWarps extends JavaPlugin {

    private static UltimateWarps instance;
    private ConfigManager configManager;
    private WarpManager warpManager;
    private CooldownManager cooldownManager;
    private EffectManager effectManager;
    private final Map<UUID, TeleportTask> activeTeleports = new HashMap<>();

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

        playerWarpsConfigManager = new PlayerWarpsConfigManager(this);
        playerWarpManager = new PlayerWarpManager(this);
        playerWarpManager.loadAll();

        registerCommands();
        registerListeners();

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
        Bukkit.getConsoleSender().sendMessage(TextFormat.render("§dUltimateWarps v" + getPluginMeta().getVersion() + " enabled!\n§aBy §1King_Le0_"));
    }

    @Override
    public void onDisable() {
        closeAllGuis();
        ChatInput.clearAll();

        if (warpManager != null) {
            warpManager.saveAllWarps();
        }
        if (playerWarpManager != null) {
            playerWarpManager.saveAll();
        }

        new ArrayList<>(activeTeleports.values()).forEach(TeleportTask::cancel);
        activeTeleports.clear();

        new ArrayList<>(activePlayerWarpTeleports.values()).forEach(PlayerWarpTeleportTask::cancel);
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

    private void closeAllGuis() {
        AdminGUI.closeAll();
        WarpGUI.unregisterAll();
        PlayerWarpGUI.unregisterAll();
        PlayerWarpEditGUI.closeAll();
        PlayerWarpListGUI.closeAll();
        PlayerWarpRateGUI.closeAll();
    }

    private WarpCommand warpCommand;
    private SpawnCommand spawnCommand;

    private void registerCommands() {
        spawnCommand = new SpawnCommand(this);
        getCommand("spawn").setExecutor(spawnCommand);
        getCommand("setspawn").setExecutor(new SetSpawnCommand());
        getCommand("delspawn").setExecutor(new DelSpawnCommand());

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
    public static boolean isAdmin(CommandSender sender) {
        return sender != null && (sender.isOp() || sender.hasPermission("ultimatewarps.admin"));
    }

    public static boolean isPlayerWarpsAdmin(CommandSender sender) {
        return sender != null && (sender.isOp()
                || sender.hasPermission("ultimatewarps.playerwarps.admin")
                || sender.hasPermission("ultimatewarps.admin"));
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
        closeAllGuis();
        ChatInput.clearAll();

        new ArrayList<>(activeTeleports.values()).forEach(TeleportTask::cancel);
        activeTeleports.clear();
        new ArrayList<>(activePlayerWarpTeleports.values()).forEach(PlayerWarpTeleportTask::cancel);
        activePlayerWarpTeleports.clear();

        configManager.reload();
        warpManager.loadWarps();

        playerWarpsConfigManager.reload();
        playerWarpManager.loadAll();
    }
}
