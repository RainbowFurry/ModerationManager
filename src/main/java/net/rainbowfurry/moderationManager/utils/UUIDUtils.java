package net.rainbowfurry.moderationManager.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UUIDUtils {

    public static UUID getUUID(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) return player.getUniqueId();

        // OfflinePlayer
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    public static String getName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) return player.getName();
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    public static UUID parseUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (Exception e) {
            return getUUID(input);
        }
    }

    public static boolean isOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }

    public static Player getOnlinePlayer(String nameOrUUID) {
        Player player = Bukkit.getPlayerExact(nameOrUUID);
        if (player != null) return player;

        // try by uuid
        try {
            UUID uuid = UUID.fromString(nameOrUUID);
            return Bukkit.getPlayer(uuid);
        } catch (Exception ignored) {}
        return null;
    }
}
