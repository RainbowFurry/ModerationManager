package net.rainbowfurry.moderationManager.managers;

import net.kyori.adventure.text.Component;
import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.models.OPLogEntry;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProtectionManager {

    private final ModerationManager plugin;
    private final Map<String, Long> tempBlockedIps = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> connectionAttempts = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> joinTimestamps = new ConcurrentHashMap<>();
    private final List<Long> globalJoinTimestamps = Collections.synchronizedList(new ArrayList<>());

    private final AtomicBoolean lockdown = new AtomicBoolean(false);
    private volatile long lockdownUntil = 0;
    private final List<Long> raidWindow = Collections.synchronizedList(new ArrayList<>());

    public ProtectionManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    // =========== DDoS / PreLogin ===========
    public void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        ConfigManager cm = plugin.getConfigManager();

        InetAddress addr = event.getAddress();
        if (addr == null) return;
        String ip = addr.getHostAddress();
        long now = System.currentTimeMillis();

        // --- DDoS-Prüfung (nur, wenn aktiviert) ---
        if (cm.isDdosEnabled()) {
            // Blacklist
            if (cm.getDdosBlacklistIps().contains(ip)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        Component.text("Deine IP ist permanent blockiert."));
                return;
            }

            // Whitelist überspringt DDoS-Prüfung (aber nicht Lockdown!)
            boolean whitelistedIp = cm.getDdosWhitelistIps().contains(ip);

            if (!whitelistedIp) {
                // Temp-Block
                Long blockUntil = tempBlockedIps.get(ip);
                if (blockUntil != null && blockUntil > now) {
                    long minutes = Math.max(1, (blockUntil - now) / 60_000);
                    String msg = cm.getDdosMsg("connection-limit").replace("%time%", String.valueOf(minutes));
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtils.parse(msg));
                    return;
                }
                if (blockUntil != null) tempBlockedIps.remove(ip);

                // 1. Verbindungsversuche pro IP
                List<Long> attempts = connectionAttempts.computeIfAbsent(ip, k -> Collections.synchronizedList(new ArrayList<>()));
                attempts.removeIf(t -> t < (now - 60_000L));
                attempts.add(now);
                if (attempts.size() > cm.getMaxConnectionsPerIp()) {
                    long blockMinutes = cm.getDdosTempBlockMinutes();
                    tempBlockedIps.put(ip, now + blockMinutes * 60_000L);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            MessageUtils.parse(cm.getDdosMsg("connection-limit").replace("%time%", String.valueOf(blockMinutes))));
                    return;
                }

                // 2. Joins pro Sekunde pro IP
                List<Long> ipJoins = joinTimestamps.computeIfAbsent(ip, k -> Collections.synchronizedList(new ArrayList<>()));
                ipJoins.removeIf(t -> t < (now - 1000L));
                ipJoins.add(now);
                if (ipJoins.size() > cm.getJoinsPerSecond()) {
                    long blockMinutes = cm.getDdosTempBlockMinutes();
                    tempBlockedIps.put(ip, now + blockMinutes * 60_000L);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            MessageUtils.parse(cm.getDdosMsg("connection-limit").replace("%time%", String.valueOf(blockMinutes))));
                    return;
                }

                // 3. Globale Joins pro Minute (Server Überlastung)
                synchronized (globalJoinTimestamps) {
                    globalJoinTimestamps.removeIf(t -> t < (now - 60_000L));
                    globalJoinTimestamps.add(now);
                    if (globalJoinTimestamps.size() > cm.getTotalJoinsPerMinute()) {
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                                MessageUtils.parse(cm.getDdosMsg("too-many-joins")));
                        return;
                    }
                }
            }
        }

        // --- Raid Detection (auch OHNE DDoS-Schutz durchführen!) ---
        if (cm.isRaidEnabled()) {
            detectRaid(event, now);
        }

        // --- Lockdown / Raid-Modus (auch OHNE DDoS-Schutz durchführen!) ---
        if (lockdown.get()) {
            if (lockdownUntil > 0 && now > lockdownUntil) {
                endLockdown();
            } else {
                UUID uuid = event.getUniqueId();
                boolean bypass = false;

                // Player ist vielleicht schonmal auf dem Server gewesen - OP-Status abfragen
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.isOp()) {
                    bypass = true;
                }

                // Online: Permission checken (besser als PreLogin-Abfrage über OfflinePlayer)
                if (!bypass) {
                    Player alreadyOnline = Bukkit.getPlayer(uuid);
                    if (alreadyOnline != null && alreadyOnline.hasPermission("moderation.bypass.raid")) {
                        bypass = true;
                    }
                }

                if (!bypass) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            MessageUtils.parse(cm.getRaidMsg("lockdown")));
                }
            }
        }
    }

    private void detectRaid(AsyncPlayerPreLoginEvent event, long now) {
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.isRaidEnabled()) return;

        int currentWindowSize;
        synchronized (raidWindow) {
            raidWindow.removeIf(t -> t < (now - cm.getRaidTimeframeSeconds() * 1000L));
            raidWindow.add(now);
            currentWindowSize = raidWindow.size();
        }

        if (!lockdown.get() && currentWindowSize >= cm.getRaidThreshold()) {
            // startLockdown nutzt Bukkit-API -> auf Main-Scheduler
            Bukkit.getScheduler().runTask(plugin, () -> startLockdown(cm.getRaidActionDurationMinutes(), currentWindowSize));
        }
    }

    private void startLockdown(int durationMinutes, int joinCount) {
        startLockdown(durationMinutes <= 0 ? Long.MAX_VALUE : durationMinutes * 60_000L, "System [Raid-Erkennung]", joinCount);
    }

    public void startLockdown(long durationMillis, String operatorName) {
        startLockdown(durationMillis, operatorName, -1);
    }

    private void startLockdown(long durationMillis, String operatorName, int joinCount) {
        ConfigManager cm = plugin.getConfigManager();
        String action = cm.getRaidAutoAction();

        lockdown.set(true);
        lockdownUntil = durationMillis <= 0 ? 0 : (durationMillis == Long.MAX_VALUE ? Long.MAX_VALUE : System.currentTimeMillis() + durationMillis);

        String msg = cm.getRaidMsg("raid-detected").replace("%op%", operatorName);
        Bukkit.broadcast(MessageUtils.parsePrefix(msg), "moderation.notify");
        String staffMsg = (joinCount > 0
                ? msg + "<br><gray>Joins: <yellow>" + joinCount + "<br><gray>von: <yellow>" + operatorName
                : msg + "<br><gray>Manuell gestartet von: <yellow>" + operatorName);
        plugin.getStaffManager().notifySimple("raid", staffMsg);

        // KICK_NEW: Kick alle Spieler die in den letzten N Minuten neu beigetreten sind
        if ("KICK_NEW".equalsIgnoreCase(action)) {
            long threshold = System.currentTimeMillis() - 5 * 60_000L;
            for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                if (p.hasPermission("moderation.bypass.raid") || p.isOp()) continue;
                if (p.getFirstPlayed() > threshold) {
                    p.kick(MessageUtils.parse(cm.getRaidMsg("lockdown")));
                }
            }
        }

        // WHITELIST_ONLY: Server vorübergehend in Whitelist-Modus
        if ("WHITELIST_ONLY".equalsIgnoreCase(action)) {
            Bukkit.setWhitelist(true);
        }
    }

    public void endLockdown() {
        endLockdown("System [Auto-Timeout]");
    }

    public void endLockdown(String operatorName) {
        // Auf Main Thread ausführen
        if (!Bukkit.isPrimaryThread()) {
            final String name = operatorName;
            Bukkit.getScheduler().runTask(plugin, () -> endLockdown(name));
            return;
        }
        lockdown.set(false);
        lockdownUntil = 0;
        synchronized (raidWindow) {
            raidWindow.clear();
        }
        String msg = plugin.getConfigManager().getRaidMsg("raid-ended").replace("%op%", operatorName);
        Bukkit.broadcast(MessageUtils.parsePrefix(msg), "moderation.notify");
        plugin.getStaffManager().notifySimple("raid-ended", msg + "<br><gray>aufgehoben von: <yellow>" + operatorName);
        // Whitelist ggf. wieder deaktivieren (wurde von uns aktiviert)
        Bukkit.setWhitelist(false);
    }

    public boolean isLockdown() { return lockdown.get(); }
    public long getLockdownUntil() { return lockdownUntil; }

    // =========== OP Abuse ===========
    public boolean isOpAllowed(Player player) {
        if (!plugin.getConfigManager().isOpAbuseEnabled()) return true;
        if (!player.isOp()) return true;

        List<String> allowed = plugin.getConfigManager().getAllowedOpPlayers();
        if (allowed.isEmpty()) return true;

        return allowed.contains(player.getName()) || allowed.contains(player.getUniqueId().toString());
    }

    public void checkAndFixOpAbuse(Player player) {
        if (!plugin.getConfigManager().isOpAbuseEnabled()) return;
        if (!player.isOp()) return;
        if (!plugin.getConfigManager().isAutoDeop()) return;

        if (!isOpAllowed(player)) {
            player.setOp(false);
            MessageUtils.sendMessage(player, plugin.getConfigManager().getOpMsg("op-removed"));

            OPLogEntry entry = new OPLogEntry(-1, player.getUniqueId(), player.getName(),
                    null, "AutoSchutz", "AUTO-DEOP",
                    "Spieler war nicht in OP-Whitelist", System.currentTimeMillis());
            plugin.getDatabaseManager().logOpAction(entry);

            plugin.getStaffManager().notifySimple("op-change",
                    "<gradient:#e53935:#b71c1c><bold>OP ABUSE ERKANNT</bold></gradient><br>" +
                            "<red>Der Spieler <yellow>" + player.getName() + "</yellow> wurde automatisch DE-OPED, da er nicht auf der OP-Whitelist steht!");
        }
    }

    public boolean isCommandBlockedForOp(Player player, String command) {
        if (!plugin.getConfigManager().isOpAbuseEnabled()) return false;
        if (!player.isOp()) return false;
        if (isOpAllowed(player)) return false;

        String cmd = command.toLowerCase().startsWith("/") ? command.substring(1).toLowerCase().split("\\s+")[0]
                : command.toLowerCase().split("\\s+")[0];
        return plugin.getConfigManager().getBlockedOpCommands().contains(cmd);
    }

    public boolean shouldMonitorCommand(String command) {
        if (!plugin.getConfigManager().isOpAbuseEnabled()) return false;
        String cmd = command.toLowerCase().startsWith("/") ? command.substring(1).toLowerCase().split("\\s+")[0]
                : command.toLowerCase().split("\\s+")[0];
        return plugin.getConfigManager().getMonitoredCommands().contains(cmd);
    }

    public void logMonitoredCommand(Player player, String command) {
        OPLogEntry entry = new OPLogEntry(-1, player.getUniqueId(), player.getName(),
                player.getUniqueId(), player.getName(), "CMD-MONITOR", command, System.currentTimeMillis());
        plugin.getDatabaseManager().logOpAction(entry);

        plugin.getStaffManager().notifySimple("command-monitor",
                "<gold>Command Monitor:</gold> <yellow>" + player.getName() +
                        "<gray> hat <white>" + command + " <gray>ausgeführt");
    }

    public void logOpChange(UUID targetUUID, String targetName, UUID operatorUUID, String operatorName, boolean isOp) {
        OPLogEntry entry = new OPLogEntry(-1, targetUUID, targetName,
                operatorUUID, operatorName, isOp ? "OP" : "DE-OP",
                "OP-Status geändert", System.currentTimeMillis());
        plugin.getDatabaseManager().logOpAction(entry);

        if (plugin.getConfigManager().isOpNotifyStaff()) {
            plugin.getStaffManager().notifySimple("op-change",
                    "<gradient:#ff9800:#f44336><bold>OP CHANGE</bold></gradient> " +
                            "<red>" + operatorName + "<gray> hat <yellow>" + targetName +
                            "<gray> den Status <gold>" + (isOp ? "OP" : "DE-OP") + " <gray>gegeben");
        }
    }
}
