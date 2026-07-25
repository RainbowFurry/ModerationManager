package net.rainbowfurry.moderationManager.listeners;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.managers.ChatManager;
import net.rainbowfurry.moderationManager.managers.StaffManager;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class ChatListener implements Listener {

    private final ModerationManager plugin;

    public ChatListener(ModerationManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        StaffManager sm = plugin.getStaffManager();

        // Staff Chat Check (Toggle Mode)
        if (plugin.getConfigManager().isStaffChatEnabled()
                && sm.hasStaffChatToggled(player.getUniqueId())
                && player.hasPermission("moderation.staffchat")) {
            event.setCancelled(true);
            sm.sendStaffChat(player.getUniqueId(), player.getName(), event.getMessage());
            return;
        }

        // Full Chat Filter
        plugin.getChatManager().handleChatEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        String cmdName = cmd.split("\\s+")[0].toLowerCase();

        // Staff Chat via /sc <Nachricht> nicht doppelt filtern
        if ("staffchat".equalsIgnoreCase(cmdName) || "sc".equalsIgnoreCase(cmdName)) {
            return;
        }

        // OP Abuse: Blockierte Befehle für nicht erlaubte OPs
        if (plugin.getProtectionManager().isCommandBlockedForOp(player, command)) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, plugin.getConfigManager().getOpMsg("blocked-command"));
            plugin.getStaffManager().notifySimple("op-change",
                    "<gradient:#e53935:#b71c1c>OP-COMMAND BLOCKED</gradient> <yellow>" + player.getName() +
                            "<gray> hat versucht <white>" + command + " <gray>auszuführen!");
            return;
        }

        // Command Monitor
        if (plugin.getProtectionManager().shouldMonitorCommand(command)) {
            plugin.getProtectionManager().logMonitoredCommand(player, command);
        }

        // OP / DEOP Befehle loggen (mit Ausführer!)
        if (("op".equalsIgnoreCase(cmdName) || "deop".equalsIgnoreCase(cmdName))
                && plugin.getConfigManager().isLogOpChanges()) {
            boolean isOp = "op".equalsIgnoreCase(cmdName);
            String[] parts = cmd.split("\\s+");
            if (parts.length >= 2) {
                String targetName = parts[1];
                Player target = Bukkit.getPlayerExact(targetName);
                UUID targetUUID;
                String targetDisplayName;
                if (target != null) {
                    targetUUID = target.getUniqueId();
                    targetDisplayName = target.getName();
                } else {
                    targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
                    targetDisplayName = targetName;
                }
                UUID opUUID = player.getUniqueId();
                String opName = player.getName();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getConnectionListener().registerOpChange(opUUID, opName, targetUUID, targetDisplayName, isOp);
                }, 2L);
            }
        }

        // Swear Check in Befehlen (/msg etc.)
        ChatManager cm = plugin.getChatManager();
        if (cm.checkCommandForSwear(player, command)) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, plugin.getConfigManager().getSwearMsg("blocked"));
        }
    }
}
