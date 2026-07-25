package net.rainbowfurry.moderationManager.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoModManager {

    private final ModerationManager plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private static final class PlayerState {
        int count = 0;
        int lastTriggeredRule = 0;
        long lastActionAt = 0L;
    }

    private final Map<String, Map<UUID, PlayerState>> stateByCategory = new ConcurrentHashMap<>();

    public AutoModManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    public record ViolationResult(boolean punished, String action, String reason, long ruleLevel) {}

    public ViolationResult reportViolation(Player player, String category, String reasonText) {
        if (player == null) return null;
        return reportViolation(player.getUniqueId(), player.getName(), category, reasonText);
    }

    public ViolationResult reportViolation(UUID targetUUID, String targetName, String category, String reasonText) {
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.isAutoModEnabled()) return null;
        if (!cm.isAutoModCategoryEnabled(category)) return null;

        if (targetUUID != null) {
            Player online = Bukkit.getPlayer(targetUUID);
            if (online != null && (online.hasPermission(cm.getAutoModBypassPermission())
                    || online.hasPermission("moderation.bypass")
                    || online.isOp())) {
                return null;
            }
        }

        List<ConfigManager.AutoModRule> rules = cm.getAutoModRules(category);
        if (rules.isEmpty()) return null;

        PlayerState state = null;
        if (targetUUID != null) {
            state = stateByCategory
                    .computeIfAbsent(category, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(targetUUID, k -> new PlayerState());
            state.count++;
        }

        int currentCount = state != null ? state.count : 1;
        ConfigManager.AutoModRule match = bestRule(rules, currentCount);

        if (match == null) {
            return new ViolationResult(false, "", reasonText != null ? reasonText : "", currentCount);
        }

        if (state != null && state.lastTriggeredRule >= match.triggerLevel) {
            if (match.cooldownMinutes > 0
                    && (System.currentTimeMillis() - state.lastActionAt) < match.cooldownMinutes * 60_000L) {
                return new ViolationResult(false, match.action, match.reason, currentCount);
            }
        }

        PunishmentManager pm = plugin.getPunishmentManager();
        String opName = cm.getAutoModOperatorName();
        String reason = match.reason != null && !match.reason.isBlank() ? match.reason
                : (reasonText != null ? reasonText : category + " Verstoß");

        applyAction(pm, match, targetUUID, targetName, opName, reason);

        if (state != null) {
            state.lastTriggeredRule = Math.max(state.lastTriggeredRule, match.triggerLevel);
            state.lastActionAt = System.currentTimeMillis();
        }

        if (cm.isAutoModNotifyStaff()) {
            notifyStaff(category, targetUUID, targetName, match, currentCount, reasonText);
        }

        return new ViolationResult(true, match.action, reason, match.triggerLevel);
    }

    public ViolationResult triggerDirect(Player player, String category, int wantedLevel, String reasonText) {
        if (player == null) return null;
        return triggerDirect(player.getUniqueId(), player.getName(), category, wantedLevel, reasonText);
    }

    public ViolationResult triggerDirect(UUID targetUUID, String targetName, String category,
                                         int wantedLevel, String reasonText) {
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.isAutoModEnabled()) return null;
        if (!cm.isAutoModCategoryEnabled(category)) return null;

        if (targetUUID != null) {
            Player online = Bukkit.getPlayer(targetUUID);
            if (online != null && (online.hasPermission(cm.getAutoModBypassPermission())
                    || online.hasPermission("moderation.bypass")
                    || online.isOp())) {
                return null;
            }
        }

        List<ConfigManager.AutoModRule> rules = cm.getAutoModRules(category);
        if (rules.isEmpty()) return null;

        ConfigManager.AutoModRule match = bestRule(rules, wantedLevel);
        if (match == null) {
            return new ViolationResult(false, "", reasonText != null ? reasonText : "", wantedLevel);
        }

        PunishmentManager pm = plugin.getPunishmentManager();
        String opName = cm.getAutoModOperatorName();
        String reason = match.reason != null && !match.reason.isBlank() ? match.reason
                : (reasonText != null ? reasonText : category);

        applyAction(pm, match, targetUUID, targetName, opName, reason);

        if (cm.isAutoModNotifyStaff()) {
            notifyStaff(category, targetUUID, targetName, match, wantedLevel, reasonText);
        }

        return new ViolationResult(true, match.action, reason, match.triggerLevel);
    }

    private void applyAction(PunishmentManager pm, ConfigManager.AutoModRule match,
                             UUID targetUUID, String targetName, String opName, String reason) {
        switch (match.action.toLowerCase(Locale.ROOT)) {
            case "warn" -> pm.warn(targetName != null ? targetName : "", null, opName, reason);
            case "mute", "tempmute" -> {
                long endAt;
                if (match.durationMinutes <= 0) endAt = -1L;
                else endAt = System.currentTimeMillis() + match.durationMinutes * 60_000L;
                if (targetUUID != null) {
                    pm.mute(targetUUID, targetName, null, opName, reason, endAt);
                } else if (targetName != null) {
                    pm.mute(targetName, null, opName, reason);
                }
            }
            case "ban", "tempban" -> {
                long endAt;
                if (match.durationMinutes <= 0) endAt = -1L;
                else endAt = System.currentTimeMillis() + match.durationMinutes * 60_000L;
                if (targetUUID != null) {
                    pm.ban(targetUUID, targetName, null, opName, reason, endAt);
                } else if (targetName != null) {
                    if (match.durationMinutes <= 0) {
                        pm.ban(targetName, null, opName, reason);
                    } else {
                        pm.tempban(targetName, null, opName, reason, match.durationMinutes * 60_000L);
                    }
                }
            }
            case "kick" -> pm.kick(targetName != null ? targetName : "", null, opName, reason);
            default -> pm.warn(targetName != null ? targetName : "", null, opName, reason);
        }
    }

    private ConfigManager.AutoModRule bestRule(List<ConfigManager.AutoModRule> rules, int count) {
        ConfigManager.AutoModRule best = null;
        for (ConfigManager.AutoModRule r : rules) {
            if (count >= r.triggerLevel && (best == null || r.triggerLevel > best.triggerLevel)) {
                best = r;
            }
        }
        return best;
    }

    private void notifyStaff(String category, UUID targetUUID, String targetName,
                             ConfigManager.AutoModRule rule, int count, String extra) {
        String name = targetName != null ? targetName : (targetUUID != null ? targetUUID.toString() : "Unbekannt");
        String line = "<gradient:#ff1744:#b71c1c><bold>AUTOMOD</bold></gradient> "
                + "<gray>Kategorie:</gray> <yellow>" + category + "</yellow><br>"
                + "<gray>Spieler:</gray> <white>" + name + "</white><br>"
                + "<gray>Verstöße:</gray> <red>" + count + "</red> "
                + "<gray>Aktion:</gray> <aqua>" + rule.action.toUpperCase(Locale.ROOT) + "</aqua> "
                + "<gray>Grund:</gray> <white>" + rule.reason + "</white>"
                + (extra != null && !extra.isBlank() ? "<br><gray>Details:</gray> <white>" + truncate(extra, 200) : "");

        Component component;
        try {
            component = mm.deserialize(plugin.getConfigManager().getPrefix() + line);
        } catch (Exception e) {
            component = Component.text("[AutoMod] " + category + ": " + name + " → " + rule.action);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("moderation.staff") || p.hasPermission("moderation.automod.notify") || p.isOp()) {
                try {
                    p.sendMessage(component);
                } catch (Throwable ignored) {}
            }
        }
        try {
            Bukkit.getConsoleSender().sendMessage(component);
        } catch (Throwable ignored) {}
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }

    public void resetViolations(UUID playerId, String category) {
        if (playerId == null) return;
        Map<UUID, PlayerState> m = stateByCategory.get(category);
        if (m != null) m.remove(playerId);
    }

    public void resetAllForPlayer(UUID playerId) {
        if (playerId == null) return;
        for (Map<UUID, PlayerState> m : stateByCategory.values()) {
            m.remove(playerId);
        }
    }

    public void clearCachesFor(Player player) {
        if (player != null) resetAllForPlayer(player.getUniqueId());
    }

    public int getViolationCount(UUID playerId, String category) {
        if (playerId == null) return 0;
        Map<UUID, PlayerState> m = stateByCategory.get(category);
        if (m == null) return 0;
        PlayerState s = m.get(playerId);
        return s != null ? s.count : 0;
    }
}
