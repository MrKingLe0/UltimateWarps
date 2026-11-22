package com.ultimatewarps.playerwarps.integration;

import com.ultimatewarps.UltimateWarps;
import com.ultimatewarps.playerwarps.PlayerWarp;
import com.ultimatewarps.playerwarps.PlayerWarpsConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EconomyBridge {

    private static final Pattern STRICT_BALANCE = Pattern.compile("^([-+]?\\d+(?:\\.\\d+)?)\\s*([kmbt])?$", Pattern.CASE_INSENSITIVE);

    private EconomyBridge() {}

    public static boolean isPlaceholderAPIAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                && classExists("me.clip.placeholderapi.PlaceholderAPI");
    }

    public static boolean isVaultAvailable() {
        return getVaultEconomyProvider() != null;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object getVaultEconomyProvider() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")
                    && classExists("net.milkbowl.vault.economy.Economy")) {
                Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
                Object rsp = Bukkit.getServicesManager().getRegistration(econClass);
                if (rsp != null) {
                    return rsp.getClass().getMethod("getProvider").invoke(rsp);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Resolves the visitor's balance. Prefers Vault Economy API via reflection for exact
     * numeric accuracy, falling back to the configured PlaceholderAPI selector.
     * Returns Double.NaN if neither is available.
     */
    public static double getPlayerBalance(Player player, String placeholder) {
        Object econ = getVaultEconomyProvider();
        if (econ != null) {
            try {
                Method getBalance = econ.getClass().getMethod("getBalance", OfflinePlayer.class);
                Object bal = getBalance.invoke(econ, player);
                if (bal instanceof Number num && Double.isFinite(num.doubleValue())) {
                    return num.doubleValue();
                }
            } catch (Throwable ignored) {
            }
        }

        if (isPlaceholderAPIAvailable() && placeholder != null && !placeholder.isBlank()) {
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Method setPlaceholders;
                String resolved;
                try {
                    setPlaceholders = papiClass.getMethod("setPlaceholders", Player.class, String.class);
                    resolved = (String) setPlaceholders.invoke(null, player, placeholder);
                } catch (NoSuchMethodException e) {
                    setPlaceholders = papiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
                    resolved = (String) setPlaceholders.invoke(null, player, placeholder);
                }
                if (resolved != null && !resolved.equals(placeholder)) {
                    double parsed = parseBalanceString(resolved);
                    if (Double.isFinite(parsed)) {
                        return parsed;
                    }
                }
            } catch (Throwable t) {
                UltimateWarps.getInstance().getLogger().warning(
                        "Failed to parse balance placeholder '" + placeholder + "': " + t.getMessage());
            }
        }

        return Double.NaN;
    }

    public static double parseBalanceString(String raw) {
        if (raw == null) return Double.NaN;
        String stripped = raw.replaceAll("(?i)[§&][0-9A-FK-ORX]", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[$€£¥]", "")
                .trim();
        if (stripped.isEmpty()) return Double.NaN;

        // Handle European decimal comma (e.g. "10,50") vs thousands separators ("1,234.56")
        if (stripped.contains(",") && !stripped.contains(".")) {
            if (stripped.matches("^[-+]?\\d+,\\d{1,2}(\\s*[kmbtKMBT])?$")) {
                stripped = stripped.replace(",", ".");
            } else {
                stripped = stripped.replace(",", "");
            }
        } else {
            stripped = stripped.replace(",", "");
        }

        // Extract leading numeric token with optional single k/m/b/t suffix (ignoring trailing currency words)
        Matcher m = Pattern.compile("^([-+]?\\d+(?:\\.\\d+)?)\\s*([kmbt])?(?:\\s+.*)?$", Pattern.CASE_INSENSITIVE).matcher(stripped);
        if (!m.matches()) {
            m = STRICT_BALANCE.matcher(stripped);
            if (!m.matches()) return Double.NaN;
        }

        double base;
        try {
            base = Double.parseDouble(m.group(1));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }

        double multiplier = 1.0;
        String suffix = m.group(2);
        if (suffix != null && !suffix.isEmpty()) {
            switch (suffix.toLowerCase(Locale.ROOT)) {
                case "k" -> multiplier = 1_000.0;
                case "m" -> multiplier = 1_000_000.0;
                case "b" -> multiplier = 1_000_000_000.0;
                case "t" -> multiplier = 1_000_000_000_000.0;
            }
        }

        double result = base * multiplier;
        return Double.isFinite(result) ? result : Double.NaN;
    }

    public static double roundCost(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) return 0.0;
        return Math.round(amount * 100.0) / 100.0;
    }

    public static String formatAmount(double amount) {
        double rounded = roundCost(amount);
        if (rounded == (long) rounded) {
            return String.valueOf((long) rounded);
        }
        return String.format(Locale.US, "%.2f", rounded);
    }

    /**
     * Atomically withdraws the warp cost from the visitor and pays the warp owner.
     * Uses Vault Economy API when available so failed withdrawals never mint money.
     * Returns true if payment succeeded.
     */
    public static boolean chargeAndPay(Player visitor, PlayerWarp warp, PlayerWarpsConfigManager config) {
        double cost = roundCost(warp.getTeleportCost());
        if (cost <= 0) return true;

        Object econ = getVaultEconomyProvider();
        if (econ != null) {
            try {
                Method hasMethod = econ.getClass().getMethod("has", OfflinePlayer.class, double.class);
                Object hasBal = hasMethod.invoke(econ, visitor, cost);
                if (hasBal instanceof Boolean b && !b) {
                    return false;
                }

                Method withdraw = econ.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                Object withdrawResp = withdraw.invoke(econ, visitor, cost);
                if (!isVaultSuccess(withdrawResp)) {
                    return false;
                }

                OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.getOwnerId());
                Method deposit = econ.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
                Object depositResp = deposit.invoke(econ, owner, cost);
                if (!isVaultSuccess(depositResp)) {
                    // Refund visitor if deposit failed
                    deposit.invoke(econ, visitor, cost);
                    return false;
                }
                return true;
            } catch (Throwable t) {
                UltimateWarps.getInstance().getLogger().warning(
                        "Vault payment failed for warp " + warp.getName() + ": " + t.getMessage());
                return false;
            }
        }

        // Console command fallback when Vault is not installed
        String amountStr = formatAmount(cost);
        String ownerName = warp.getOwnerName() != null ? warp.getOwnerName().trim() : "";
        String visitorName = visitor.getName();

        String takeCmd = prepareCommand(config.getTakeCommand(), ownerName, visitorName, amountStr);
        String giveCmd = prepareCommand(config.getGiveCommand(), ownerName, visitorName, amountStr);

        if (takeCmd.isEmpty()) return false;
        boolean took = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), takeCmd);
        if (!took) return false;

        if (!giveCmd.isEmpty()) {
            if (ownerName.isEmpty()) {
                UltimateWarps.getInstance().getLogger().warning(
                        "Skipped give-command for warp " + warp.getName() + " because ownerName is blank.");
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);
            }
        }
        return true;
    }

    /**
     * Refunds a Vault payment if a subsequent teleport was cancelled by another plugin.
     */
    public static void refund(Player visitor, PlayerWarp warp) {
        double cost = roundCost(warp.getTeleportCost());
        if (cost <= 0) return;

        Object econ = getVaultEconomyProvider();
        if (econ != null) {
            try {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.getOwnerId());
                Method withdraw = econ.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                Method deposit = econ.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
                withdraw.invoke(econ, owner, cost);
                deposit.invoke(econ, visitor, cost);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void executePaymentCommands(Player visitor, PlayerWarp warp, PlayerWarpsConfigManager config) {
        chargeAndPay(visitor, warp, config);
    }

    private static boolean isVaultSuccess(Object response) {
        if (response == null) return false;
        try {
            Object ok = response.getClass().getMethod("transactionSuccess").invoke(response);
            return ok instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String prepareCommand(String rawCommand, String warpOwner, String visitor, String amount) {
        if (rawCommand == null || rawCommand.isBlank()) return "";
        String cmd = rawCommand.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1).trim();
        }
        boolean hasAmountPlaceholder = cmd.contains("%amount%");
        cmd = cmd.replace("%warpowner%", warpOwner)
                 .replace("%visitor%", visitor)
                 .replace("%amount%", amount);
        if (!hasAmountPlaceholder) {
            cmd = cmd + " " + amount;
        }
        return cmd;
    }
}
