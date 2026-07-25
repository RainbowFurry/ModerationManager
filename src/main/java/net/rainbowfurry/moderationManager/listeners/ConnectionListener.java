package net.rainbowfurry.moderationManager.listeners;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.managers.ProtectionManager;
import net.rainbowfurry.moderationManager.managers.StaffManager;
import net.rainbowfurry.moderationManager.models.Punishment;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionListener implements Listener {

    private final ModerationManager plugin;
    private final Map<UUID, Boolean> lastKnownOpState = new HashMap<>();

    public ConnectionListener(ModerationManager plugin) {
        this.plugin = plugin;
        startOpMonitor();
    }

    private void startOpMonitor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfigManager().isLogOpChanges() && !plugin.getConfigManager().isAutoDeop()) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();
                boolean currentOp = p.isOp();
                Boolean wasOp = lastKnownOpState.put(id, currentOp);

                if (wasOp != null && wasOp != currentOp && plugin.getConfigManager().isLogOpChanges()) {
                    // Zustandsänderung erkannt (durch /op, /deop, Plugin etc.)
                    plugin.getProtectionManager().logOpChange(
                            id, p.getName(),
                            null, "System",
                            currentOp
                    );
                }

                if (currentOp && plugin.getConfigManager().isAutoDeop()) {
                    plugin.getProtectionManager().checkAndFixOpAbuse(p);
                }
            }
        }, 20L, 20L);
    }

    public void registerOpChange(UUID operatorUUID, String operatorName,
                                 UUID targetUUID, String targetName,
                                 boolean isNowOp) {
        lastKnownOpState.put(targetUUID, isNowOp);
        plugin.getProtectionManager().logOpChange(
                targetUUID, targetName, operatorUUID, operatorName, isNowOp);
        if (isNowOp && plugin.getConfigManager().isAutoDeop()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOp()) {
                    plugin.getProtectionManager().checkAndFixOpAbuse(target);
                }
            }, 5L);
        }
    }

    // PreLogin - DDoS, Raid, Lockdown, IP-Blacklist, Ban-Check (Name)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        plugin.getProtectionManager().handlePreLogin(event);
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        // Ban Check (Name-basiert, falls UUID unbekannt)
        Punishment ban = plugin.getPunishmentManager().checkBanned(event.getUniqueId(), event.getName());
        if (ban != null) {
            String duration = ban.getEndAt() <= 0 ? "Permanent"
                    : net.rainbowfurry.moderationManager.utils.DurationUtils.formatRemaining(ban.getEndAt());
            String unbanDate = MessageUtils.formatDate(ban.getEndAt());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    MessageUtils.formatPunishment(
                            plugin.getConfigManager().getBanScreenTemplate(),
                            ban.getReason(),
                            ban.getOperatorName(),
                            String.valueOf(ban.getId()),
                            duration,
                            unbanDate
                    ));
        }
    }

    // PlayerLogin (leicht später, OP Abuse Check)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        ProtectionManager pm = plugin.getProtectionManager();

        // OP Abuse - Auto DEOP
        pm.checkAndFixOpAbuse(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        StaffManager sm = plugin.getStaffManager();

        // Alt-Account & IP-Handling
        plugin.getAltAccountManager().registerPlayer(player);

        // Vanish: verschwundene Spieler für neue ausblenden
        sm.applyVanishForNewPlayer(player);

        // Falls Spieler vanished bleiben soll nach Rejoin
        if (sm.isVanished(player.getUniqueId())) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("moderation.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }
            if (plugin.getConfigManager().isVanishHideFromTablist()) player.setInvisible(true);
            if (plugin.getConfigManager().isVanishGodMode()) player.setInvulnerable(true);
        }

        // Silent Join wenn vanished
        if (sm.isVanished(player.getUniqueId()) && plugin.getConfigManager().isVanishSilent()) {
            event.joinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Cache vom Chat-Manager aufräumen
        plugin.getChatManager().clearPlayerCache(player);

        // Spielzeit updaten
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var profile = plugin.getDatabaseManager().getPlayerProfile(player.getUniqueId());
            if (profile != null) {
                long now = System.currentTimeMillis();
                profile.setLastLogout(now);
                long session = now - profile.getLastLogin();
                if (session > 0) profile.addPlaytime(session);
                plugin.getDatabaseManager().savePlayerProfile(profile);
            }
        });

        // Staff Chat Toggle Entfernen
        StaffManager sm = plugin.getStaffManager();
        if (sm.hasStaffChatToggled(player.getUniqueId())) {
            sm.toggleStaffChat(player.getUniqueId());
        }

        // Silent Quit wenn vanished
        if (sm.isVanished(player.getUniqueId()) && plugin.getConfigManager().isVanishSilent()) {
            event.quitMessage(null);
        }
    }
}
