package net.rainbowfurry.moderationManager.managers;

import net.kyori.adventure.text.Component;
import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.models.PlayerProfile;
import net.rainbowfurry.moderationManager.models.Punishment;
import net.rainbowfurry.moderationManager.utils.DurationUtils;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import net.rainbowfurry.moderationManager.utils.UUIDUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class PunishmentManager {

    private final ModerationManager plugin;

    public PunishmentManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    // ============ BAN ============
    public long ban(String targetName, UUID operatorUUID, String operatorName, String reason) {
        return ban(UUIDUtils.getUUID(targetName), targetName, operatorUUID, operatorName, reason, -1);
    }

    public long tempban(String targetName, UUID operatorUUID, String operatorName, String reason, long durationMillis) {
        return ban(UUIDUtils.getUUID(targetName), targetName, operatorUUID, operatorName, reason,
                durationMillis <= 0 ? -1 : System.currentTimeMillis() + durationMillis);
    }

    public long ban(UUID targetUUID, String targetName, UUID operatorUUID, String operatorName, String reason, long endAt) {
        PlayerProfile profile = getOrCreateProfile(targetUUID, targetName);
        profile.incTotalBans();

        Punishment p = new Punishment(-1,
                endAt <= 0 ? Punishment.Type.BAN : Punishment.Type.TEMPBAN,
                targetUUID, targetName, operatorUUID, operatorName,
                reason != null ? reason : "Kein Grund",
                System.currentTimeMillis(), endAt, true, null);

        long id = plugin.getDatabaseManager().savePunishment(p);

        // Online Player kicken
        Player online = Bukkit.getPlayer(targetUUID);
        if (online != null) {
            String duration = DurationUtils.formatDuration(endAt <= 0 ? -1 : endAt - System.currentTimeMillis());
            String unbanDate = MessageUtils.formatDate(endAt);
            Component kickScreen = MessageUtils.formatPunishment(
                    plugin.getConfigManager().getBanScreenTemplate(),
                    reason, operatorName != null ? operatorName : "System",
                    String.valueOf(id), duration, unbanDate
            );
            online.kick(kickScreen);
        }

        plugin.getDatabaseManager().savePlayerProfile(profile);
        plugin.getStaffManager().notifyPunishment(p);
        return id;
    }

    public void unban(String targetName, UUID operatorUUID, String operatorName) {
        PlayerProfile profile = plugin.getDatabaseManager().getPlayerProfileByName(targetName);
        UUID targetUUID = profile != null ? profile.getUuid() : UUIDUtils.getUUID(targetName);
        plugin.getDatabaseManager().unbanAllFor(targetUUID, operatorName, operatorUUID);
    }

    // ============ KICK ============
    public long kick(String targetName, UUID operatorUUID, String operatorName, String reason) {
        Player player = Bukkit.getPlayerExact(targetName);
        if (player == null) return -1;

        PlayerProfile profile = getOrCreateProfile(player.getUniqueId(), player.getName());
        profile.incTotalKicks();

        Punishment p = new Punishment(-1, Punishment.Type.KICK,
                player.getUniqueId(), player.getName(), operatorUUID, operatorName,
                reason != null ? reason : "Kein Grund", System.currentTimeMillis(), 0, false, null);
        long id = plugin.getDatabaseManager().savePunishment(p);

        Component kickScreen = MessageUtils.formatPunishment(
                plugin.getConfigManager().getKickScreenTemplate(),
                reason, operatorName != null ? operatorName : "System",
                String.valueOf(id), "-", "-"
        );
        player.kick(kickScreen);

        plugin.getDatabaseManager().savePlayerProfile(profile);
        plugin.getStaffManager().notifyPunishment(p);
        return id;
    }

    // ============ MUTE ============
    public long mute(String targetName, UUID operatorUUID, String operatorName, String reason) {
        return mute(UUIDUtils.getUUID(targetName), targetName, operatorUUID, operatorName, reason, -1);
    }

    public long tempmute(String targetName, UUID operatorUUID, String operatorName, String reason, long durationMillis) {
        return mute(UUIDUtils.getUUID(targetName), targetName, operatorUUID, operatorName, reason,
                durationMillis <= 0 ? -1 : System.currentTimeMillis() + durationMillis);
    }

    public long mute(UUID targetUUID, String targetName, UUID operatorUUID, String operatorName, String reason, long endAt) {
        PlayerProfile profile = getOrCreateProfile(targetUUID, targetName);
        profile.incTotalMutes();

        Punishment p = new Punishment(-1,
                endAt <= 0 ? Punishment.Type.MUTE : Punishment.Type.TEMPMUTE,
                targetUUID, targetName, operatorUUID, operatorName,
                reason != null ? reason : "Kein Grund", System.currentTimeMillis(), endAt, true, null);
        long id = plugin.getDatabaseManager().savePunishment(p);

        Player online = Bukkit.getPlayer(targetUUID);
        if (online != null) {
            MessageUtils.sendMessage(online, "<red>Du wurdest gemutet! " +
                    "Grund: <white>" + reason + "<red> Dauer: <white>" +
                    DurationUtils.formatDuration(endAt <= 0 ? -1 : endAt - System.currentTimeMillis()));
        }

        plugin.getDatabaseManager().savePlayerProfile(profile);
        plugin.getStaffManager().notifyPunishment(p);
        return id;
    }

    public void unmute(String targetName, UUID operatorUUID, String operatorName) {
        PlayerProfile profile = plugin.getDatabaseManager().getPlayerProfileByName(targetName);
        UUID targetUUID = profile != null ? profile.getUuid() : UUIDUtils.getUUID(targetName);
        plugin.getDatabaseManager().unmuteAllFor(targetUUID, operatorName, operatorUUID);
    }

    // ============ WARN ============
    public long warn(String targetName, UUID operatorUUID, String operatorName, String reason) {
        UUID targetUUID = UUIDUtils.getUUID(targetName);
        PlayerProfile profile = getOrCreateProfile(targetUUID, targetName);
        profile.incTotalWarns();

        Punishment p = new Punishment(-1, Punishment.Type.WARN,
                targetUUID, targetName, operatorUUID, operatorName,
                reason != null ? reason : "Kein Grund", System.currentTimeMillis(), 0, true, null);
        long id = plugin.getDatabaseManager().savePunishment(p);

        Player online = Bukkit.getPlayer(targetUUID);
        if (online != null) {
            MessageUtils.sendMessage(online, "<gradient:#ff9800:#f44336><bold>WARNUNG</bold></gradient> " +
                    "<red>Du hast eine Verwarnung erhalten!<br>Grund: <white>" + reason);
        }

        plugin.getDatabaseManager().savePlayerProfile(profile);
        plugin.getStaffManager().notifyPunishment(p);

        // Auto Thresholds prüfen
        checkAutoThresholds(targetUUID, targetName);
        return id;
    }

    public void checkAutoThresholds(UUID targetUUID, String targetName) {
        ConfigManager cm = plugin.getConfigManager();
        int warns = plugin.getDatabaseManager().countActiveWarns(targetUUID);
        if (warns <= 0) return;
        plugin.getAutoModManager().triggerDirect(targetUUID, targetName, "warn-thresholds", warns,
                warns + " aktive Warnungen");
    }

    // Auto-Punishment nach Schlüssel (spam, swear, link, etc.)
    public void handleViolation(UUID targetUUID, String targetName, String category, ConfigurationSection violationsSection) {
        if (violationsSection == null) return;

        int activeWarns = plugin.getDatabaseManager().countActiveWarns(targetUUID);
        List<?> list = violationsSection.getList(".");
        if (list == null) return;

        String action = null;
        String reason = category + " Verstoß";
        long duration = -1;
        int triggerLevel = 0;

        for (Object obj : list) {
            if (!(obj instanceof ConfigurationSection cs)) continue;
            int warns = cs.getInt("warns", -1);
            if (warns > 0 && activeWarns + 1 >= warns && warns > triggerLevel) {
                triggerLevel = warns;
                action = cs.getString("action", "warn");
                reason = cs.getString("reason", reason);
                duration = cs.getLong("duration-minutes", -1) * 60_000L;
            }
        }

        // Mindestens 1 Warn immer
        warn(targetName, null, "System", reason);

        if (action != null) {
            switch (action.toLowerCase()) {
                case "mute":
                    mute(targetUUID, targetName, null, "System", reason, duration > 0 ? System.currentTimeMillis() + duration : -1);
                    break;
                case "ban":
                    ban(targetUUID, targetName, null, "System", reason, duration > 0 ? System.currentTimeMillis() + duration : -1);
                    break;
                case "kick":
                    kick(targetName, null, "System", reason);
                    break;
            }
        }
    }

    // Check ob Spieler gebannt ist (inkl. abgelaufen)
    public Punishment checkBanned(UUID uuid, String name) {
        Punishment ban = plugin.getDatabaseManager().getActiveBan(uuid);
        if (ban == null && name != null) ban = plugin.getDatabaseManager().getActiveBanByName(name);
        if (ban == null) return null;

        if (ban.isExpired()) {
            ban.setActive(false);
            plugin.getDatabaseManager().updatePunishment(ban);
            return null;
        }
        return ban;
    }

    public Punishment checkMuted(UUID uuid) {
        Punishment mute = plugin.getDatabaseManager().getActiveMute(uuid);
        if (mute == null) return null;
        if (mute.isExpired()) {
            mute.setActive(false);
            plugin.getDatabaseManager().updatePunishment(mute);
            return null;
        }
        return mute;
    }

    // ============ Player Profile ============
    public PlayerProfile getOrCreateProfile(UUID uuid, String name) {
        PlayerProfile profile = plugin.getDatabaseManager().getPlayerProfile(uuid);
        if (profile == null) {
            profile = new PlayerProfile(uuid, name != null ? name : "Unknown");
            plugin.getDatabaseManager().savePlayerProfile(profile);
        } else if (name != null && !name.equals(profile.getPlayerName())) {
            // Name Update - speichern
            profile.setPlayerName(name);
            plugin.getDatabaseManager().savePlayerProfile(profile);
        }
        return profile;
    }

    public boolean hasBypass(CommandSender sender) {
        return sender.hasPermission("moderation.bypass");
    }

    public boolean hasBypass(Player player, String subPermission) {
        return player.hasPermission("moderation.bypass") || player.hasPermission("moderation.bypass." + subPermission);
    }

    @SuppressWarnings("deprecation")
    public OfflinePlayer getOfflinePlayerByName(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    // ============ GUI Integration: Einheitliche Methoden für GUIs ============

    public List<Punishment> getPunishments(UUID targetUUID) {
        int limit = plugin.getConfigManager().getMaxShownPunishments();
        if (limit <= 0) limit = 100;
        return plugin.getDatabaseManager().getPunishmentHistory(targetUUID, limit);
    }

    public Punishment getActiveBan(UUID uuid) {
        return checkBanned(uuid, null);
    }

    public Punishment getActiveMute(UUID uuid) {
        return checkMuted(uuid);
    }

    public long addPunishment(Punishment punishment) {
        if (punishment == null) return -1;
        UUID targetUUID = punishment.getTargetUUID();
        String targetName = punishment.getTargetName();
        UUID operatorUUID = punishment.getOperatorUUID();
        String operatorName = punishment.getOperatorName();
        String reason = punishment.getReason();
        long endAt = punishment.getEndAt();
        Punishment.Type type = punishment.getType();

        return switch (type) {
            case WARN -> warn(targetName != null ? targetName : "", operatorUUID, operatorName, reason);
            case KICK -> kick(targetName != null ? targetName : "", operatorUUID, operatorName, reason);
            case MUTE, TEMPMUTE -> mute(targetUUID, targetName, operatorUUID, operatorName, reason, endAt);
            case BAN, TEMPBAN -> ban(targetUUID, targetName, operatorUUID, operatorName, reason, endAt);
            default -> {
                long id = plugin.getDatabaseManager().savePunishment(punishment);
                plugin.getStaffManager().notifyPunishment(punishment);
                yield id;
            }
        };
    }

    public void removePunishment(long punishmentId, String operatorName) {
        Punishment p = plugin.getDatabaseManager().getPunishmentById(punishmentId);
        if (p == null) return;

        // Deaktiviere die spezifische Strafe
        plugin.getDatabaseManager().deactivatePunishmentById(punishmentId);

        // Erstelle einen Unban/Unmute-Eintrag in der Historie (optional)
        Punishment.Type revType = switch (p.getType()) {
            case BAN, TEMPBAN -> Punishment.Type.UNBAN;
            case MUTE, TEMPMUTE -> Punishment.Type.UNMUTE;
            default -> null;
        };
        if (revType != null) {
            Punishment rev = new Punishment(-1, revType, p.getTargetUUID(), p.getTargetName(),
                    null, operatorName, "Aufgehoben (ID #" + punishmentId + ")",
                    System.currentTimeMillis(), 0, false, null);
            plugin.getDatabaseManager().savePunishment(rev);
        }
    }

    public long executeKick(Player target, String reason, String operatorName) {
        return kick(target.getName(), null, operatorName, reason);
    }

    public boolean deletePunishment(long punishmentId, String operatorName) {
        Punishment p = plugin.getDatabaseManager().getPunishmentById(punishmentId);
        if (p == null) return false;
        Punishment.Type t = p.getType();
        boolean ok = plugin.getDatabaseManager().deletePunishmentById(punishmentId);
        if (ok) {
            plugin.getStaffManager().notifySimple("punishment-deleted",
                    "<red>🗑️ Strafe gelöscht:</red> <gray>ID #" + punishmentId + " (" + t.name() + ")" +
                            "<gray> von <yellow>" + (operatorName != null ? operatorName : "Unknown"));
        }
        return ok;
    }
}
