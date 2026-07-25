package net.rainbowfurry.moderationManager.managers;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatManager {

    private final ModerationManager plugin;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?:\\/\\/)?([\\w-]+\\.)+[a-z]{2,}(:[0-9]+)?(\\/[^\\s]*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_LINK_PATTERN = Pattern.compile(
            "(https?:\\/\\/)?\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b(?:[:\\/][^\\s]*)?", Pattern.CASE_INSENSITIVE);

    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> recentMessageTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> lastMessages = new ConcurrentHashMap<>();

    private volatile boolean chatLocked = false;
    private volatile boolean slowmodeEnabled = false;

    public ChatManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    // ===== Chat Lock / Slowmode (für GUI-Steuerung) =====
    public boolean isChatLocked() { return chatLocked; }
    public boolean isSlowmodeEnabled() { return slowmodeEnabled; }
    public void setSlowmodeEnabled(boolean on) { this.slowmodeEnabled = on; }

    public void setChatLocked(boolean locked, String by) {
        this.chatLocked = locked;
        String key = locked ? "chat-locked" : "chat-unlocked";
        plugin.getStaffManager().notifySimple(key,
                (locked ? "<red>🔒 Chat wurde GESPERRT" : "<green>🔓 Chat wurde FREIGEGEBEN")
                        + "<gray> von <yellow>" + by);
        for (var p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (locked) {
                MessageUtils.sendMessage(p, plugin.getConfigManager().getPrefix()
                        + "<red>🔒 Der Chat wurde gesperrt von <yellow>" + by);
            } else {
                MessageUtils.sendMessage(p, plugin.getConfigManager().getPrefix()
                        + "<green>🔓 Der Chat wurde freigegeben von <yellow>" + by);
            }
        }
    }

    public void clearChat(Player by) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 120; i++) sb.append("\n ");
        String clearStr = sb.toString();
        for (var p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("moderation.chat.bypass") || p.isOp()) {
                MessageUtils.sendMessage(p, plugin.getConfigManager().getPrefix()
                        + "<gray>Chat wurde geleert von <yellow>" + by.getName());
            } else {
                p.sendMessage(net.kyori.adventure.text.Component.text(clearStr));
                MessageUtils.sendMessage(p, plugin.getConfigManager().getPrefix()
                        + "<gray>Chat wurde geleert von <yellow>" + by.getName());
            }
        }
        plugin.getStaffManager().notifySimple("chat-clear",
                "<gray>Chat geleert von <yellow>" + by.getName());
    }

    public void handleChatEvent(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getPunishmentManager().hasBypass(player, "chat")) {
            return;
        }

        String message = event.getMessage();

        // Mute Check
        PunishmentManager pm = plugin.getPunishmentManager();
        var mute = pm.checkMuted(player.getUniqueId());
        if (mute != null) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.formatPunishment(
                    plugin.getConfigManager().getMuteMessageTemplate(),
                    mute.getReason(), mute.getOperatorName(),
                    String.valueOf(mute.getId()),
                    net.rainbowfurry.moderationManager.utils.DurationUtils.formatRemaining(mute.getEndAt()),
                    MessageUtils.formatDate(mute.getEndAt())
            ));
            return;
        }

        // Chat Lock
        if (chatLocked && !player.hasPermission("moderation.chat.bypass") && !player.isOp()) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, plugin.getConfigManager().getPrefix()
                    + "<red>Chat ist aktuell GESPERRT. Bitte warte bis er freigegeben wird.");
            return;
        }

        // Slowmode (zusätzlicher globaler Mindestabstand)
        ConfigManager cm = plugin.getConfigManager();
        if (slowmodeEnabled && !player.hasPermission("moderation.chat.bypass") && !player.isOp()) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            long slowDelay = cm.getSlowmodeDelay() * 1000L;
            Long last = lastMessageTime.get(uuid);
            if (last != null && (now - last) < slowDelay) {
                event.setCancelled(true);
                long wait = ((slowDelay - (now - last)) / 1000) + 1;
                MessageUtils.sendMessage(player, plugin.getConfigManager().getPrefix()
                        + "<gray>Slowmode aktiv: Bitte warte noch <yellow>" + wait + "s<gray>.");
                return;
            }
        }

        // 1. Swear Check
        SwearResult swearResult = filterSwear(player, message);
        if (swearResult.blocked) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, plugin.getConfigManager().getSwearMsg("blocked"));
            plugin.getStaffManager().notifySimple("blocked-swear",
                    "<red>Chat blockiert (Beleidigung):</red> <yellow>" + player.getName() +
                            "<gray>: <white>" + message);
            plugin.getAutoModManager().reportViolation(player, "chat-swear", message);
            return;
        } else if (swearResult.changed) {
            message = swearResult.modifiedMessage;
        }

        // 2. Link Check
        LinkResult linkResult = filterLinks(player, message);
        if (linkResult.blocked) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, plugin.getConfigManager().getLinkBlockedMsg());
            plugin.getStaffManager().notifySimple("blocked-link",
                    "<red>Chat blockiert (Link):</red> <yellow>" + player.getName() +
                            "<gray>: <white>" + message);
            plugin.getAutoModManager().reportViolation(player, "chat-links", message);
            return;
        }

        // 3. Spam Check
        SpamResult spamResult = checkSpam(player, message);
        if (spamResult.blocked) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, spamResult.reasonMessage);
            plugin.getStaffManager().notifySimple("blocked-spam",
                    "<red>Chat blockiert (Spam):</red> <yellow>" + player.getName() +
                            "<gray>: <white>" + message + "<br><gray>Grund: " + spamResult.reasonKey);
            plugin.getAutoModManager().reportViolation(player, "chat-spam", spamResult.reasonKey);
            return;
        } else if (spamResult.changed) {
            message = spamResult.modifiedMessage;
        }

        // Nachricht übernehmen
        event.setMessage(message);
    }

    public boolean checkCommandForSwear(Player player, String commandLine) {
        if (!plugin.getConfigManager().isAntiSwearEnabled()) return false;
        if (plugin.getPunishmentManager().hasBypass(player, "chat")) return false;
        if (!plugin.getConfigManager().isSwearCheckCommands()) return false;

        // Befehl ohne / holen
        String cmdPart = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        String[] parts = cmdPart.split("\\s+");
        if (parts.length == 0) return false;
        String cmdName = parts[0].toLowerCase();

        if (!plugin.getConfigManager().getSwearCheckedCommands().contains(cmdName)) return false;

        String argsPart = cmdPart.length() > cmdName.length() ? cmdPart.substring(cmdName.length()).trim() : "";
        SwearResult res = filterSwear(player, argsPart);
        return res.blocked;
    }

    // ================= SPAM =================
    public record SpamResult(boolean blocked, boolean changed, String modifiedMessage, String reasonMessage, String reasonKey) {}

    public SpamResult checkSpam(Player player, String message) {
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.isAntiSpamEnabled()) return new SpamResult(false, false, message, "", "");

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Message Delay
        Long last = lastMessageTime.get(uuid);
        if (last != null && (now - last) < cm.getSpamMessageDelay()) {
            return new SpamResult(true, false, message, cm.getSpamMsg("too-fast"), "too-fast");
        }
        lastMessageTime.put(uuid, now);

        // Messages per Minute
        List<Long> timestamps = recentMessageTimestamps.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>()));
        long oneMinuteAgo = now - 60_000L;
        timestamps.removeIf(t -> t < oneMinuteAgo);
        timestamps.add(now);
        if (timestamps.size() > cm.getMessagesPerMinute()) {
            return new SpamResult(true, false, message, cm.getSpamMsg("too-many"), "too-many");
        }

        // Flood Schutz (gleiche Zeichen)
        if (cm.isFloodProtectionEnabled()) {
            int maxSame = cm.getMaxSameCharacters();
            char[] arr = message.toCharArray();
            int streak = 1;
            for (int i = 1; i < arr.length; i++) {
                if (Character.toLowerCase(arr[i]) == Character.toLowerCase(arr[i-1])) {
                    streak++;
                    if (streak > maxSame) {
                        return new SpamResult(true, false, message, cm.getSpamMsg("flood"), "flood");
                    }
                } else {
                    streak = 1;
                }
            }
        }

        // Caps Lock
        if (cm.isCapsLockEnabled() && message.length() >= cm.getCapsMinLength()) {
            int letters = 0;
            int upper = 0;
            for (char c : message.toCharArray()) {
                if (Character.isLetter(c)) {
                    letters++;
                    if (Character.isUpperCase(c)) upper++;
                }
            }
            if (letters > 0 && (upper * 100 / letters) >= cm.getCapsPercentageThreshold()) {
                if (cm.isCapsAutoCorrect()) {
                    message = message.charAt(0) + message.substring(1).toLowerCase();
                    return new SpamResult(false, true, message, "", "caps-corrected");
                } else {
                    return new SpamResult(true, false, message, cm.getSpamMsg("caps-lock"), "caps-lock");
                }
            }
        }

        // Repeat Schutz
        if (cm.isRepeatProtectionEnabled()) {
            List<String> history = lastMessages.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>()));
            int maxRepeat = cm.getMaxRepeats();
            int count = 0;
            String msgNorm = message.toLowerCase().trim();
            for (String old : history) {
                if (old.equals(msgNorm) || similarity(old, msgNorm) > 0.85) count++;
            }
            history.add(msgNorm);
            while (history.size() > 5) history.remove(0);
            if (count >= maxRepeat) {
                return new SpamResult(true, false, message, cm.getSpamMsg("repeat"), "repeat");
            }
        }

        return new SpamResult(false, false, message, "", "");
    }

    private double similarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        String longer = s1.length() > s2.length() ? s1 : s2;
        String shorter = s1.length() > s2.length() ? s2 : s1;
        if (longer.isEmpty()) return 1.0;
        int distance = levenshtein(longer, shorter);
        return (longer.length() - distance) / (double) longer.length();
    }

    private int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j-1]), a.charAt(i-1) == b.charAt(j-1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    // ================= LINKS =================
    public record LinkResult(boolean blocked) {}

    public LinkResult filterLinks(Player player, String message) {
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.isLinkProtectionEnabled()) return new LinkResult(false);

        ConfigManager.LinkMode mode = cm.getLinkMode();
        if (mode == ConfigManager.LinkMode.NONE) return new LinkResult(false);

        Matcher urlMatcher = URL_PATTERN.matcher(message);
        Matcher ipMatcher = IP_LINK_PATTERN.matcher(message);

        boolean hasUrl = urlMatcher.find();
        boolean hasIpLink = cm.isBlockIpLinks() && ipMatcher.find();

        if (!hasUrl && !hasIpLink) return new LinkResult(false);

        List<String> whitelist = new ArrayList<>();
        List<String> blacklist = new ArrayList<>();
        if (mode == ConfigManager.LinkMode.WHITELIST) whitelist = cm.getLinkWhitelist();
        if (mode == ConfigManager.LinkMode.BLACKLIST) blacklist = cm.getLinkBlacklist();

        // Alle Links prüfen
        urlMatcher.reset();
        while (urlMatcher.find()) {
            String raw = urlMatcher.group().toLowerCase();
            String domain = extractDomain(raw);

            if (cm.isBlockIpLinks() && IP_LINK_PATTERN.matcher(raw).matches()) {
                return new LinkResult(true);
            }

            switch (mode) {
                case BLOCK_ALL:
                    return new LinkResult(true);
                case WHITELIST:
                    if (!isDomainInList(domain, whitelist)) return new LinkResult(true);
                    break;
                case BLACKLIST:
                    if (isDomainInList(domain, blacklist)) return new LinkResult(true);
                    break;
            }
        }

        if (hasIpLink) return new LinkResult(true);
        return new LinkResult(false);
    }

    private String extractDomain(String url) {
        String clean = url;
        if (clean.contains("://")) clean = clean.substring(clean.indexOf("://") + 3);
        if (clean.contains("/")) clean = clean.substring(0, clean.indexOf("/"));
        if (clean.contains(":")) clean = clean.substring(0, clean.indexOf(":"));
        return clean.toLowerCase();
    }

    private boolean isDomainInList(String domain, List<String> list) {
        for (String entry : list) {
            String e = entry.toLowerCase();
            if (domain.equals(e) || domain.endsWith("." + e)) return true;
        }
        return false;
    }

    // ================= SWEAR =================
    public record SwearResult(boolean blocked, boolean changed, String modifiedMessage) {}

    public SwearResult filterSwear(Player player, String message) {
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.isAntiSwearEnabled()) return new SwearResult(false, false, message);

        List<String> blacklist = cm.getSwearBlacklist();
        if (blacklist.isEmpty()) return new SwearResult(false, false, message);

        ConfigManager.SwearMode mode = cm.getSwearMode();
        String replacement = cm.getSwearReplacement();

        String lower = message.toLowerCase();
        boolean found = false;

        for (String word : blacklist) {
            String wordLower = word.toLowerCase();
            // Wort mit Wortgrenzen suchen
            Pattern p = Pattern.compile("\\b" + Pattern.quote(wordLower) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(lower);
            if (m.find()) found = true;
        }

        if (!found) return new SwearResult(false, false, message);

        switch (mode) {
            case BLOCK:
                return new SwearResult(true, false, message);
            case CENSOR:
            case REPLACE:
                String modified = message;
                for (String word : blacklist) {
                    Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
                    modified = p.matcher(modified).replaceAll(replacement);
                }
                return new SwearResult(false, true, modified);
        }
        return new SwearResult(false, false, message);
    }

    public void clearPlayerCache(Player player) {
        UUID id = player.getUniqueId();
        lastMessageTime.remove(id);
        recentMessageTimestamps.remove(id);
        lastMessages.remove(id);
        plugin.getAutoModManager().clearCachesFor(player);
    }
}
