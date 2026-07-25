package net.rainbowfurry.moderationManager.managers;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.models.IPLog;
import net.rainbowfurry.moderationManager.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AltAccountManager {

    private final ModerationManager plugin;

    public AltAccountManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    public void registerPlayer(Player player) {
        if (!plugin.getConfigManager().isAltDetectionEnabled()) return;

        InetSocketAddress address = player.getAddress();
        if (address == null) return;

        String ip = address.getAddress().getHostAddress();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        PlayerProfile profile = plugin.getPunishmentManager().getOrCreateProfile(uuid, name);

        // IP-Änderung erkennen
        String oldIp = profile.getCurrentIp();
        if (oldIp != null && !oldIp.isEmpty() && !oldIp.equals(ip) && plugin.getConfigManager().isLogIpChanges()) {
            plugin.getStaffManager().notifySimple("ip-change", "<gold>IP-Wechsel:</gold> <yellow>" + name +
                    "<gray> wechselte IP von <white>" + oldIp + "<gray> zu <white>" + ip);
        }
        profile.setCurrentIp(ip);
        profile.setLastLogin(System.currentTimeMillis());
        plugin.getDatabaseManager().savePlayerProfile(profile);

        // VPN/Proxy-Check (async)
        if (plugin.getConfigManager().isVpnDetection() && !plugin.getConfigManager().getIgnoredIps().contains(ip)) {
            checkVpnAsync(uuid, name, ip);
        }

        // IP-Log erstellen
        plugin.getDatabaseManager().logIp(uuid, name, ip, false);

        // Alt Accounts suchen und ggf. melden
        if (plugin.getConfigManager().isNotifyStaffOnAltJoin() && !plugin.getConfigManager().getIgnoredIps().contains(ip)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<IPLog> alts = plugin.getDatabaseManager().findPlayersByIp(ip);
                long distinctPlayers = alts.stream().map(IPLog::getPlayerUUID).distinct().count();
                if (distinctPlayers > plugin.getConfigManager().getMaxAccountsPerIp()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<gradient:#ff9800:#f44336><bold>ALT ACCOUNT ERKANNT!</bold></gradient><br>");
                    sb.append("<gold>").append(name).append("</gold> <gray>benutzt die IP </gray><white>").append(ip).append("</white><br>");
                    sb.append("<gray>Weitere Accounts mit dieser IP (").append(distinctPlayers).append("):<br>");
                    for (IPLog alt : alts) {
                        if (!alt.getPlayerUUID().equals(uuid)) {
                            sb.append("  <dark_gray>»</dark_gray> <red>").append(alt.getPlayerName()).append("</red> ");
                        }
                    }
                    plugin.getStaffManager().notifySimple("alt-join", sb.toString());
                }
            });
        }
    }

    public List<IPLog> getAltAccounts(UUID targetUUID) {
        PlayerProfile profile = plugin.getDatabaseManager().getPlayerProfile(targetUUID);
        if (profile == null || profile.getCurrentIp() == null) return List.of();
        return plugin.getDatabaseManager().findPlayersByIp(profile.getCurrentIp());
    }

    public List<IPLog> getAltAccounts(String ip) {
        return plugin.getDatabaseManager().findPlayersByIp(ip);
    }

    /**
     * Gibt alle Spieler-Profile zurück, die die gleiche IP wie targetUUID hatten/haben.
     * Sortiert: aktuellstes LastLogin zuerst.
     */
    public List<PlayerProfile> findAltAccounts(UUID targetUUID, String ip) {
        List<PlayerProfile> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        if (ip != null && !ip.isEmpty()) {
            for (IPLog l : plugin.getDatabaseManager().findPlayersByIp(ip)) {
                if (seen.add(l.getPlayerUUID())) {
                    PlayerProfile pr = plugin.getDatabaseManager().getPlayerProfile(l.getPlayerUUID());
                    if (pr != null) result.add(pr);
                }
            }
        }
        // Target selbst an erster Stelle, falls gefunden
        PlayerProfile target = plugin.getDatabaseManager().getPlayerProfile(targetUUID);
        if (target != null) {
            result.removeIf(p -> p.getUuid().equals(targetUUID));
            result.add(0, target);
        }
        result.sort((a, b) -> Long.compare(b.getLastLogin(), a.getLastLogin()));
        return result;
    }

    /**
     * Gibt alle Spieler-Profile zurück, bei denen die IP mehrfach verwendet wurde
     * (für "globale Alt-Ansicht"). Sortiert nach LastLogin absteigend, begrenzt auf 100.
     */
    public List<PlayerProfile> findAllWithSharedIps() {
        Set<UUID> candidates = new HashSet<>();
        // Alle IP-Logs holen und nach IPs mit >1 Spielern suchen
        List<IPLog> allLogs = plugin.getDatabaseManager().findAllIpLogs();
        Set<String> sharedIps = new HashSet<>();
        java.util.Map<String, Set<UUID>> ipToPlayers = new java.util.HashMap<>();
        for (IPLog l : allLogs) {
            if (l.getIp() == null || l.getIp().isBlank()) continue;
            Set<UUID> set = ipToPlayers.computeIfAbsent(l.getIp(), k -> new HashSet<>());
            set.add(l.getPlayerUUID());
            if (set.size() > 1) sharedIps.add(l.getIp());
        }
        Set<UUID> ids = new HashSet<>();
        for (String ip : sharedIps) {
            Set<UUID> s = ipToPlayers.get(ip);
            if (s != null) ids.addAll(s);
        }
        List<PlayerProfile> profiles = new ArrayList<>();
        for (UUID id : ids) {
            PlayerProfile pr = plugin.getDatabaseManager().getPlayerProfile(id);
            if (pr != null) profiles.add(pr);
        }
        profiles.sort((a, b) -> Long.compare(b.getLastLogin(), a.getLastLogin()));
        // Limit
        if (profiles.size() > 200) profiles = new ArrayList<>(profiles.subList(0, 200));
        return profiles;
    }

    // VPN/Proxy Check (nutzt ip-api.com, kostenlos, limitiert auf 45 Anfragen/min)
    private void checkVpnAsync(UUID playerUUID, String playerName, String ip) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("http://ip-api.com/json/" + ip + "?fields=status,proxy,hosting,query");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) content.append(line);
                in.close();

                String response = content.toString();
                boolean isProxy = extractBoolean(response, "proxy");
                boolean isHosting = extractBoolean(response, "hosting");
                boolean vpn = isProxy || isHosting;

                // IP-Log updaten
                plugin.getDatabaseManager().logIp(playerUUID, playerName, ip, vpn);

                if (vpn) {
                    plugin.getStaffManager().notifySimple("vpn-detect",
                            "<gradient:#9c27b0:#e91e63><bold>VPN/PROXY ERKANNT</bold></gradient><br>" +
                                    "<gold>Spieler:</gold> <yellow>" + playerName + "</yellow><br>" +
                                    "<gold>IP:</gold> <white>" + ip + "</white><br>" +
                                    "<gray>Proxy=" + isProxy + " Hosting=" + isHosting);

                    if (plugin.getConfigManager().isVpnAutoBan()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getAutoModManager().triggerDirect(playerUUID, playerName,
                                    "vpn", 1, "IP: " + ip);
                        });
                    }
                }
            } catch (Exception ignored) {
                // Silent, falls API nicht erreichbar
            }
        });
    }

    private static final Pattern BOOL_FIELD = Pattern.compile("\"(\\w+)\":\\s*(true|false)", Pattern.CASE_INSENSITIVE);

    private static boolean extractBoolean(String json, String field) {
        Matcher m = BOOL_FIELD.matcher(json);
        while (m.find()) {
            if (field.equalsIgnoreCase(m.group(1))) {
                return Boolean.parseBoolean(m.group(2));
            }
        }
        return false;
    }
}
