package net.rainbowfurry.moderationManager.managers;

import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final ModerationManager plugin;
    private FileConfiguration config;

    public ConfigManager(ModerationManager plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        addMissingDefaults();
    }

    private void addMissingDefaults() {
        try {
            Reader reader = new InputStreamReader(plugin.getResource("config.yml"), StandardCharsets.UTF_8);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
            plugin.saveConfig();
        } catch (Exception ignored) {}
    }

    public void reload() {
        loadConfig();
    }

    public FileConfiguration getConfig() { return config; }

    public static Material safeMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) return fallback;
        try {
            Material m = Material.matchMaterial(name);
            return m != null ? m : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    // ===== GETTER =====

    public String getPrefix() {
        return config.getString("general.prefix", "<gradient:#6a11cb:#2575fc>Moderation</gradient><dark_gray> • ");
    }

    public String getLanguage() { return config.getString("general.language", "de"); }
    public int getMaxShownPunishments() { return config.getInt("general.max-shown-punishments", 20); }
    public boolean isBstats() { return config.getBoolean("general.bstats", true); }

    // Database
    public String getDatabaseFilename() { return config.getString("database.filename", "moderation.db"); }

    // Alt Accounts
    public boolean isAltDetectionEnabled() { return config.getBoolean("alt-accounts.enabled", true); }
    public boolean isAltAutoDetect() { return config.getBoolean("alt-accounts.auto-detect", true); }
    public int getMaxAccountsPerIp() { return config.getInt("alt-accounts.max-accounts-per-ip", 3); }
    public boolean isLogIpChanges() { return config.getBoolean("alt-accounts.log-ip-changes", true); }
    public boolean isNotifyStaffOnAltJoin() { return config.getBoolean("alt-accounts.notify-staff-on-alt-join", true); }
    public List<String> getIgnoredIps() { return config.getStringList("alt-accounts.ignored-ips"); }
    public boolean isVpnDetection() { return config.getBoolean("alt-accounts.vpn-detection", true); }
    public int getVpnThreshold() { return config.getInt("alt-accounts.vpn-threshold", 70); }
    public boolean isVpnAutoBan() { return config.getBoolean("alt-accounts.vpn-autoban", false); }
    public String getVpnBanReason() { return config.getString("alt-accounts.vpn-ban-reason", "VPN/Proxy Verbindungen sind nicht erlaubt!"); }
    public long getVpnBanDuration() { return config.getLong("alt-accounts.vpn-ban-duration", -1) * 60_000L; }

    // Anti Spam
    public boolean isAntiSpamEnabled() { return config.getBoolean("anti-spam.enabled", true); }
    public long getSpamMessageDelay() { return config.getLong("anti-spam.message-delay", 800); }
    public int getMessagesPerMinute() { return config.getInt("anti-spam.messages-per-minute", 6); }
    public boolean isCapsLockEnabled() { return config.getBoolean("anti-spam.caps-lock.enabled", true); }
    public int getCapsPercentageThreshold() { return config.getInt("anti-spam.caps-lock.percentage-threshold", 60); }
    public int getCapsMinLength() { return config.getInt("anti-spam.caps-lock.min-length", 5); }
    public boolean isCapsAutoCorrect() { return config.getBoolean("anti-spam.caps-lock.auto-correct", true); }
    public boolean isRepeatProtectionEnabled() { return config.getBoolean("anti-spam.repeat-protection.enabled", true); }
    public int getMaxRepeats() { return config.getInt("anti-spam.repeat-protection.max-repeats", 2); }
    public boolean isFloodProtectionEnabled() { return config.getBoolean("anti-spam.flood-protection.enabled", true); }
    public int getMaxSameCharacters() { return config.getInt("anti-spam.flood-protection.max-same-characters", 5); }
    public ConfigurationSection getSpamViolations() { return config.getConfigurationSection("anti-spam.auto-punishments.violations"); }
    public String getSpamMsg(String key) { return config.getString("anti-spam.messages." + key, ""); }

    // Link Schutz
    public enum LinkMode { WHITELIST, BLACKLIST, NONE, BLOCK_ALL }
    public boolean isLinkProtectionEnabled() { return config.getBoolean("link-protection.enabled", true); }
    public LinkMode getLinkMode() {
        try { return LinkMode.valueOf(config.getString("link-protection.mode", "WHITELIST").toUpperCase()); }
        catch (Exception e) { return LinkMode.WHITELIST; }
    }
    public List<String> getLinkWhitelist() { return config.getStringList("link-protection.whitelist"); }
    public List<String> getLinkBlacklist() { return config.getStringList("link-protection.blacklist"); }
    public boolean isBlockIpLinks() { return config.getBoolean("link-protection.block-ip-links", true); }
    public String getLinkBypassPermission() { return config.getString("link-protection.bypass-permission", "moderation.bypass.links"); }
    public boolean isLinkAutoPunish() { return config.getBoolean("link-protection.auto-punishments.enabled", true); }
    public ConfigurationSection getLinkPunishments() { return config.getConfigurationSection("link-protection.auto-punishments"); }
    public String getLinkBlockedMsg() { return config.getString("link-protection.messages.blocked", ""); }

    // Anti Swear
    public enum SwearMode { REPLACE, BLOCK, CENSOR }
    public boolean isAntiSwearEnabled() { return config.getBoolean("anti-swear.enabled", true); }
    public SwearMode getSwearMode() {
        try { return SwearMode.valueOf(config.getString("anti-swear.mode", "BLOCK").toUpperCase()); }
        catch (Exception e) { return SwearMode.BLOCK; }
    }
    public List<String> getSwearBlacklist() { return config.getStringList("anti-swear.blacklist"); }
    public String getSwearReplacement() { return config.getString("anti-swear.replacement", "****"); }
    public boolean isSwearCheckCommands() { return config.getBoolean("anti-swear.check-commands", true); }
    public List<String> getSwearCheckedCommands() { return config.getStringList("anti-swear.checked-commands"); }
    public boolean isSwearAutoPunish() { return config.getBoolean("anti-swear.auto-punishments.enabled", true); }
    public ConfigurationSection getSwearViolations() { return config.getConfigurationSection("anti-swear.auto-punishments.violations"); }
    public String getSwearMsg(String key) { return config.getString("anti-swear.messages." + key, ""); }

    // DDoS
    public boolean isDdosEnabled() { return config.getBoolean("ddos-protection.enabled", true); }
    public int getMaxConnectionsPerIp() { return config.getInt("ddos-protection.max-connections-per-ip", 3); }
    public int getJoinsPerSecond() { return config.getInt("ddos-protection.joins-per-second", 1); }
    public int getTotalJoinsPerMinute() { return config.getInt("ddos-protection.total-joins-per-minute", 60); }
    public boolean isPacketRateLimit() { return config.getBoolean("ddos-protection.packet-rate-limit.enabled", true); }
    public int getMaxPacketsPerSecond() { return config.getInt("ddos-protection.packet-rate-limit.max-packets-per-second", 50); }
    public List<String> getDdosWhitelistIps() { return config.getStringList("ddos-protection.whitelisted-ips"); }
    public List<String> getDdosBlacklistIps() { return config.getStringList("ddos-protection.blacklisted-ips"); }
    public long getDdosTempBlockMinutes() { return config.getLong("ddos-protection.temp-block-time", 30); }
    public String getDdosMsg(String key) { return config.getString("ddos-protection.messages." + key, ""); }
    public int getSlowmodeDelay() { return config.getInt("anti-spam.message-delay", 3000) / 1000; }
    public int getDdosWindowSeconds() { return 60; }
    public int getDdosWindow() { return getDdosWindowSeconds(); }
    public int getDdosConnectionLimit() { return getMaxConnectionsPerIp(); }

    // Raid
    public boolean isRaidEnabled() { return config.getBoolean("raid-detection.enabled", true); }
    public int getRaidWindow() { return getRaidTimeframeSeconds(); }
    public int getRaidThreshold() { return config.getInt("raid-detection.threshold", 15); }
    public int getRaidTimeframeSeconds() { return config.getInt("raid-detection.timeframe-seconds", 10); }
    public String getRaidAutoAction() { return config.getString("raid-detection.auto-action", "LOCKDOWN"); }
    public int getRaidActionDurationMinutes() { return config.getInt("raid-detection.action-duration", 10); }
    public boolean isRaidNotifyStaff() { return config.getBoolean("raid-detection.notify-staff", true); }
    public boolean isRaidBanNewPlayers() { return config.getBoolean("raid-detection.ban-new-players-during-raid", false); }
    public String getRaidMsg(String key) { return config.getString("raid-detection.messages." + key, ""); }

    // OP Abuse
    public boolean isOpAbuseEnabled() { return config.getBoolean("op-abuse.enabled", true); }
    public boolean isLogOpChanges() { return config.getBoolean("op-abuse.log-op-changes", true); }
    public List<String> getBlockedOpCommands() { return config.getStringList("op-abuse.blocked-op-commands"); }
    public List<String> getAllowedOpPlayers() { return config.getStringList("op-abuse.allowed-op-players"); }
    public boolean isAutoDeop() { return config.getBoolean("op-abuse.auto-deop", true); }
    public List<String> getMonitoredCommands() { return config.getStringList("op-abuse.monitored-commands"); }
    public boolean isOpNotifyStaff() { return config.getBoolean("op-abuse.notify-staff", true); }
    public String getOpMsg(String key) { return config.getString("op-abuse.messages." + key, ""); }

    // Auto Rules / Warn Thresholds
    public int getMuteAtWarns() { return config.getInt("auto-rules.warn-thresholds.mute-at-warns", 5); }
    public int getBanAtWarns() { return config.getInt("auto-rules.warn-thresholds.ban-at-warns", 10); }
    public int getThresholdMuteDurationMinutes() { return config.getInt("auto-rules.warn-thresholds.mute-duration-minutes", 30); }
    public long getThresholdBanDurationMinutes() { return config.getLong("auto-rules.warn-thresholds.ban-duration-minutes", 4320); }

    // ============ AUTO MOD ============
    public static class AutoModRule {
        public final int triggerLevel;
        public final String action;
        public final String reason;
        public final long durationMinutes;
        public final long cooldownMinutes;
        public AutoModRule(int triggerLevel, String action, String reason,
                           long durationMinutes, long cooldownMinutes) {
            this.triggerLevel = triggerLevel;
            this.action = action;
            this.reason = reason;
            this.durationMinutes = durationMinutes;
            this.cooldownMinutes = cooldownMinutes;
        }
    }

    public boolean isAutoModEnabled() { return config.getBoolean("auto-mod.enabled", true); }
    public String getAutoModOperatorName() { return config.getString("auto-mod.operator-name", "AutoMod"); }
    public String getAutoModBypassPermission() { return config.getString("auto-mod.bypass-permission", "moderation.automod.bypass"); }
    public boolean isAutoModNotifyStaff() { return config.getBoolean("auto-mod.notify-staff", true); }
    public boolean isAutoModCategoryEnabled(String category) {
        return config.getBoolean("auto-mod.categories." + category + ".enabled", true);
    }
    public List<AutoModRule> getAutoModRules(String category) {
        List<AutoModRule> result = new ArrayList<>();
        String path = "auto-mod.categories." + category + ".rules";
        List<?> list = config.getList(path);
        if (list != null) {
            for (Object o : list) {
                int tl = -1; String action = "warn"; String reason = category;
                long dur = -1; long cool = 0;
                if (o instanceof ConfigurationSection cs) {
                    tl = cs.getInt("at-violations", -1);
                    action = cs.getString("action", "warn");
                    reason = cs.getString("reason", reason);
                    dur = cs.getLong("duration-minutes", -1);
                    cool = cs.getLong("cooldown-minutes", 0);
                } else if (o instanceof java.util.Map<?, ?> map) {
                    Object tlO = map.get("at-violations"); if (tlO instanceof Number n) tl = n.intValue();
                    Object aO = map.get("action"); if (aO != null) action = String.valueOf(aO);
                    Object rO = map.get("reason"); if (rO != null) reason = String.valueOf(rO);
                    Object dO = map.get("duration-minutes"); if (dO instanceof Number n) dur = n.longValue();
                    Object cO = map.get("cooldown-minutes"); if (cO instanceof Number n) cool = n.longValue();
                }
                if (tl > 0) result.add(new AutoModRule(tl, action, reason, dur, cool));
            }
        }
        return result;
    }
    public String getAutoModMessage(String category, String key, String def) {
        String v = config.getString("auto-mod.categories." + category + ".messages." + key);
        return v != null ? v : def;
    }

    // Punishment Screens + Presets
    public String getBanScreenTemplate() { return config.getString("punishments.ban-screen", ""); }
    public String getKickScreenTemplate() { return config.getString("punishments.kick-screen", ""); }
    public String getMuteMessageTemplate() { return config.getString("punishments.mute-message", ""); }

    public List<String> getPunishmentPresetReasons() {
        List<String> list = config.getStringList("punishments.presets.reasons");
        if (list == null || list.isEmpty()) {
            return List.of("Spam / Chat Missbrauch", "Beleidigung", "Werbung", "Trolling",
                    "Bug Ausnutzung", "Cheating / Hacking", "Bannumgehung", "Sonstiges");
        }
        return list;
    }

    public static class DurationPreset {
        public final String label;
        public final long durationMs;
        public final Material material;
        public DurationPreset(String label, long durationMs, Material material) {
            this.label = label;
            this.durationMs = durationMs;
            this.material = material;
        }
    }

    public List<DurationPreset> getPunishmentPresetDurations() {
        List<DurationPreset> result = new ArrayList<>();
        List<?> list = config.getList("punishments.presets.durations");
        if (list != null) {
            for (Object o : list) {
                if (o instanceof ConfigurationSection section) {
                    String label = section.getString("label", "?");
                    long ms = section.getLong("duration-ms", -1);
                    Material mat = safeMaterial(section.getString("material"), Material.PAPER);
                    result.add(new DurationPreset(label, ms, mat));
                } else if (o instanceof java.util.Map<?, ?> map) {
                    Object labelObj = map.get("label");
                    Object durationObj = map.get("duration-ms");
                    Object materialObj = map.get("material");
                    String label = labelObj != null ? String.valueOf(labelObj) : "?";
                    long ms;
                    try {
                        if (durationObj instanceof Number n) ms = n.longValue();
                        else ms = durationObj != null ? Long.parseLong(String.valueOf(durationObj)) : -1L;
                    } catch (Exception e) { ms = -1L; }
                    Material mat = safeMaterial(materialObj != null ? String.valueOf(materialObj) : null, Material.PAPER);
                    result.add(new DurationPreset(label, ms, mat));
                }
            }
        }
        if (result.isEmpty()) {
            result.add(new DurationPreset("5 Min", 300_000L, Material.GOLD_INGOT));
            result.add(new DurationPreset("15 Min", 900_000L, Material.COPPER_BLOCK));
            result.add(new DurationPreset("1 Stunde", 3_600_000L, Material.GOLD_BLOCK));
            result.add(new DurationPreset("6 Stunden", 21_600_000L, Material.LAPIS_BLOCK));
            result.add(new DurationPreset("1 Tag", 86_400_000L, Material.IRON_BLOCK));
            result.add(new DurationPreset("7 Tage", 604_800_000L, Material.COAL_BLOCK));
            result.add(new DurationPreset("30 Tage", 2_592_000_000L, Material.REDSTONE_BLOCK));
            result.add(new DurationPreset("Permanent", -1L, Material.BEDROCK));
        }
        return result;
    }

    // Staff Chat
    public boolean isStaffChatEnabled() { return config.getBoolean("staff-chat.enabled", true); }
    public String getStaffChatFormat() { return config.getString("staff-chat.format", ""); }
    public String getStaffChatPermission() { return config.getString("staff-chat.permission", "moderation.staffchat"); }
    public boolean isStaffChatToggleMode() { return config.getBoolean("staff-chat.toggle-mode", true); }
    public String getStaffChatToggleIndicator() { return config.getString("staff-chat.toggle-indicator", "<green>✓</green>"); }

    // Vanish
    public boolean isVanishEnabled() { return config.getBoolean("vanish.enabled", true); }
    public boolean isVanishHideFromTablist() { return config.getBoolean("vanish.effects.hide-from-tablist", true); }
    public boolean isVanishGodMode() { return config.getBoolean("vanish.effects.god-mode", true); }
    public boolean isVanishNoDrops() { return config.getBoolean("vanish.effects.no-drops", true); }
    public boolean isVanishSilent() { return config.getBoolean("vanish.effects.silent", true); }
    public String getVanishMsg(String key) { return config.getString("vanish.messages." + key, ""); }

    // PlayerInfo
    public String getPlayerInfoHeader() { return config.getString("playerinfo.header", ""); }
    public String getPlayerInfoSummaryFormat() { return config.getString("playerinfo.summary.format", ""); }
    public String getPlayerInfoHistoryFormat() { return config.getString("playerinfo.history-format", ""); }
    public String getPlayerInfoAltHeader() { return config.getString("playerinfo.alt-accounts.header", ""); }
    public String getPlayerInfoAltFormat() { return config.getString("playerinfo.alt-accounts.format", ""); }
    public String getPlayerInfoFooter() { return config.getString("playerinfo.footer", ""); }

    // Notifications
    public boolean isNotificationEnabled(String type) { return config.getBoolean("notifications.enabled." + type, true); }
    public boolean isNotificationSound() { return config.getBoolean("notifications.sound", true); }
    public String getNotificationSoundType() { return config.getString("notifications.sound-type", "ENTITY_EXPERIENCE_ORB_PICKUP"); }

    // ============ GUI ============

    public Material getGuiFillBorderMaterial() {
        return safeMaterial(config.getString("gui.fill-border-material"), Material.GRAY_STAINED_GLASS_PANE);
    }
    public Material getGuiFillGlassMaterial() {
        return safeMaterial(config.getString("gui.fill-glass-material"), Material.BLACK_STAINED_GLASS_PANE);
    }

    // GUI Items
    public String guiItemMaterial(String key) { return config.getString("gui.items." + key + ".material", "PAPER"); }
    public String guiItemName(String key, String def) {
        String v = config.getString("gui.items." + key + ".name");
        return v != null ? v : def;
    }
    public List<String> guiItemLore(String key) {
        List<String> l = config.getStringList("gui.items." + key + ".lore");
        return l != null ? l : Collections.emptyList();
    }
    public String guiItemLine(String key, String subkey, String def) {
        String v = config.getString("gui.items." + key + "." + subkey);
        return v != null ? v : def;
    }
    public List<String> guiItemLines(String key, String subkey, String... def) {
        List<String> l = config.getStringList("gui.items." + key + "." + subkey);
        if (l == null || l.isEmpty()) {
            return List.of(def);
        }
        return l;
    }
    public List<String> guiApplyEnabledLore() {
        List<String> l = config.getStringList("gui.items.apply-enabled.lore-apply");
        if (l == null || l.isEmpty()) {
            return List.of("");
        }
        return l;
    }

    // GUI Menu Titles / Sizes
    public String getGuiTitle(String menu, String def) {
        String v = config.getString("gui." + menu + ".title");
        return v != null ? v : def;
    }
    public int getGuiRows(String menu, int def) {
        return Math.max(1, Math.min(6, config.getInt("gui." + menu + ".rows", def)));
    }

    // PunishMenu
    public Material getGuiPunishTypeMaterial(String type) {
        return safeMaterial(config.getString("gui.punish-menu.type-buttons." + type + ".material"), Material.DIAMOND_SWORD);
    }
    public String getGuiPunishTypeName(String type, String def) {
        String v = config.getString("gui.punish-menu.type-buttons." + type + ".name");
        return v != null ? v : def;
    }
    public List<String> getGuiPunishTypeLore(String type) {
        List<String> l = config.getStringList("gui.punish-menu.type-buttons." + type + ".lore");
        return l != null ? l : Collections.emptyList();
    }
    public String getGuiPunishDurPrefix(boolean sel) {
        return config.getString("gui.punish-menu.duration-item.prefix-" + (sel ? "selected" : "unselected"), sel ? "<green>✔ " : "<white>");
    }
    public String getGuiPunishDurLore(boolean sel) {
        return config.getString("gui.punish-menu.duration-item.lore-" + (sel ? "selected" : "unselected"),
                sel ? "<green>Ausgewählt: %label%" : "<yellow>Klicken: %label% auswählen");
    }
    public String getGuiPunishReasonPrefix(boolean sel) {
        return config.getString("gui.punish-menu.reason-item.prefix-" + (sel ? "selected" : "unselected"),
                sel ? "<green>✔ " : "<yellow>");
    }
    public List<String> getGuiPunishReasonLore(boolean sel) {
        String k = sel ? "selected" : "unselected";
        List<String> l = config.getStringList("gui.punish-menu.reason-item.lore-" + k);
        if (l == null || l.isEmpty()) {
            return sel ? List.of("<green>Ausgewählt", "<gray>Grund: <white>%reason%")
                       : List.of("<yellow>Klicken zum Auswählen", "<gray>Grund: <white>%reason%");
        }
        return l;
    }
    public Material getGuiPunishReasonMaterial(boolean sel) {
        return safeMaterial(
                config.getString("gui.punish-menu.reason-item." + (sel ? "selected" : "unselected") + "-material"),
                sel ? Material.ENCHANTED_BOOK : Material.BOOK);
    }

    // HistoryMenu
    public String getGuiHistoryStatus(boolean active) {
        return config.getString("gui.history-menu.lore-status-" + (active ? "active" : "inactive"),
                active ? "<red>✘ AKTIV" : "<green>✔ Abgelaufen / Rückgängig");
    }
    public String getGuiHistoryLore(String key, String def) {
        String v = config.getString("gui.history-menu.lore-" + key);
        return v != null ? v : def;
    }
    public List<String> getGuiHistoryDeleteHint() {
        List<String> l = config.getStringList("gui.history-menu.lore-delete-hint");
        if (l == null || l.isEmpty()) {
            return List.of("", "<red><bold>SHIFT+CLICK</bold><red>: Eintrag dauerhaft löschen");
        }
        return l;
    }
    public Material getGuiHistoryTypeMaterial(String lowerType) {
        return safeMaterial(config.getString("gui.history-menu.type-material." + lowerType), Material.PAPER);
    }

    // Messages (Top-Level)
    public String getMessage(String key, String def) {
        String v = config.getString("messages." + key);
        return v != null ? v : def;
    }
    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }
}

