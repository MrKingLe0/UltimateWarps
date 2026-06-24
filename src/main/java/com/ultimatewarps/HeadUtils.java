package com.ultimatewarps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class HeadUtils {

    @SuppressWarnings("deprecation")
    public static ItemStack getHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "customhead");
        PlayerTextures textures = profile.getTextures();

        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            String skinUrl = json.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url").getAsString();
            textures.setSkin(new URL(skinUrl));
        } catch (Exception e) {
            UltimateWarps.getInstance().getLogger().warning(
                "Invalid custom head texture – using default Steve head instead."
            );
            return head;
        }

        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    // ---------------------------------------------------------------------------------
    //  Your custom head textures
    // ---------------------------------------------------------------------------------
    public static final String WARP_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDQxYTAyNTMwMDBjMmIyOGQzODY0MjZiZDM4YTRmMmQ3NmViY2RkMDdmNjc1ZTExYjNlNjgxNGM2YThhMmVhIn19fQ==";
    public static final String SPAWN_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTJkN2E3NTFlYjA3MWUwOGRiYmM5NWJjNWQ5ZDY2ZTVmNTFkYzY3MTI2NDBhZDJkZmEwM2RlZmJiNjhhN2YzYSJ9fX0=";
    public static final String CREATE_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA1NmJjMTI0NGZjZmY5OTM0NGYxMmFiYTQyYWMyM2ZlZTZlZjZlMzM1MWQyN2QyNzNjMTU3MjUzMWYifX19";
    public static final String DELETE_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ2NWY4MGJmMDJiNDA4ODg1OTg3YjAwOTU3Y2E1ZTllYjg3NGMzZmE4ODMwNTA5OTU5N2EzMzNhMzM2ZWUxNSJ9fX0=";
    public static final String SETTINGS_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjViNjNhYzA4OWY3N2M2M2RkYjI0YjQ4ZjRmYjU0YzE4NzQzMDJlZjYxYjJkMjI5NWFmZGI2ZDQ2YjNhMjkyNyJ9fX0=";
    public static final String CLOSE_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTljZGI5YWYzOGNmNDFkYWE1M2JjOGNkYTc2NjVjNTA5NjMyZDE0ZTY3OGYwZjE5ZjI2M2Y0NmU1NDFkOGEzMCJ9fX0=";
    public static final String PREV_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjllYTFkODYyNDdmNGFmMzUxZWQxODY2YmNhNmEzMDQwYTA2YzY4MTc3Yzc4ZTQyMzE2YTEwOThlNjBmYjdkMyJ9fX0=";
    public static final String NEXT_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODI3MWE0NzEwNDQ5NWUzNTdjM2U4ZTgwZjUxMWE5ZjEwMmIwNzAwY2E5Yjg4ZTg4Yjc5NWQzM2ZmMjAxMDVlYiJ9fX0=";
    public static final String BACK_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQxMzNmNmFjM2JlMmUyNDk5YTc4NGVmYWRjZmZmZWI5YWNlMDI1YzM2NDZhZGE2N2YzNDE0ZTVlZjMzOTQifX19";
    public static final String COOLDOWN_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Y1YjFkYzdhZjExZWJiNDAyNTJjN2I4NDhkNGM1Njc0ZTY0ODdkMzgzMzE5MmE1ZTYyNGFlZjU3ZjQ3YWYwZCJ9fX0=";
    public static final String DELAY_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmZlN2Q0NjMyMjQ3N2Q2MWQ0MWMxODc4OGY1YzFhZmQyNGVkNTI2ZWIzZWQ4NDEyN2YyMTJlMjUxNWIxODgzIn19fQ==";
    public static final String ENABLED_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTk2Yzg3OWJkOWE3YzExMWZlYmQ0NTM4NGU3MjdhZjdjZDE2MGZjOGQ5YTZkMjlhNTFhYjU3NTRiNzlmMzJkZiJ9fX0=";
    public static final String PERMISSION_ICON = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWMzYWFmMTEzN2U1NzgzY2UyNzE0MDJkMDE1YWM4MzUyOWUyYzNmNTNkOTFmNjlhYjYyOWY2YTk1NjVmZWU3OCJ9fX0=";
}