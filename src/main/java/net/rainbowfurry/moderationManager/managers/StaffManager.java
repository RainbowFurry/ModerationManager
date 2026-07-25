package net.rainbowfurry.moderationManager.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.models.Punishment;
import net.rainbowfurry.moderationManager.utils.DurationUtils;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

public class StaffManager {

    private final ModerationManager plugin;
    private final Set<UUID> staffChatToggled = new HashSet<>();
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public StaffManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    // ================= NOTIFICATIONS =================
    public void notifyPunishment(Punishment p) {
        ConfigManager cm = plugin.getConfigManager();
        String typeKey = switch (p.getType()) {
            case BAN, TEMPBAN -> "ban";
            case KICK -> "kick";
            case MUTE, TEMPMUTE -> "mute";
            case WARN -> "warn";
            default -> null;
        };
        if (typeKey == null || !cm.isNotificationEnabled(typeKey)) return;

        String action = p.getTypeDisplayName();
        String duration = p.getEndAt() <= 0 ? "Permanent" : DurationUtils.formatDuration(p.getEndAt() - p.getCreatedAt());
        String msg = "<gradient:#6a11cb:#2575fc><bold>" + action + "</bold></gradient> " +
                "<yellow>" + p.getTargetName() + "</yellow>" +
                "<gray> von <gold>" + p.getOperatorName() + "</gold>" +
                "<gray> | Dauer: <aqua>" + duration + "</aqua>" +
                "<gray> | Grund: <white>" + p.getReason();

        broadcastStaffNotification(msg);
    }

    public void notifySimple(String notifyKey, String miniMessage) {
        if (!plugin.getConfigManager().isNotificationEnabled(notifyKey)) return;
        broadcastStaffNotification(miniMessage);
    }

    private void broadcastStaffNotification(String miniMessage) {
        ConfigManager cm = plugin.getConfigManager();
        MiniMessage mm = MiniMessage.miniMessage();
        String finalMsg = cm.getPrefix() + miniMessage;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("moderation.notify")) {
                p.sendMessage(mm.deserialize(finalMsg));
                if (cm.isNotificationSound()) {
                    try {
                        String soundName = cm.getNotificationSoundType();
                        // Paper 1.21+: Registry API statt Sound.valueOf (deprecated)
                        NamespacedKey key = NamespacedKey.minecraft(soundName.toLowerCase().replace('_', '.'));
                        Sound sound = Registry.SOUNDS.get(key);
                        // Fallback: falls Registry nichts findet (z.B. altes Format),
                        // versuche Bukkit Sound.byName via NamespacedKey.fromString
                        if (sound == null) {
                            sound = Registry.SOUNDS.get(NamespacedKey.fromString(soundName.toLowerCase()));
                        }
                        if (sound != null) {
                            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        // Auch an Konsole ausgeben
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(cm.getPrefix() + miniMessage));
    }

    // ================= STAFF CHAT =================
    public void sendStaffChat(UUID senderUUID, String senderName, String message) {
        if (!plugin.getConfigManager().isStaffChatEnabled()) return;

        ConfigManager cm = plugin.getConfigManager();
        String format = cm.getStaffChatFormat();
        String resolved = net.rainbowfurry.moderationManager.utils.MessageUtils.applyPercentPlaceholders(format,
                "player", senderName != null ? senderName : "Unknown",
                "message", message != null ? message : ""
        );
        TagResolver resolver = TagResolver.resolver(
                Placeholder.parsed("player", senderName != null ? senderName : "Unknown"),
                Placeholder.parsed("message", message != null ? message : "")
        );
        MiniMessage mm = MiniMessage.miniMessage();
        Component component = mm.deserialize(resolved, resolver);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(cm.getStaffChatPermission())) {
                p.sendMessage(component);
            }
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    public boolean toggleStaffChat(UUID uuid) {
        if (staffChatToggled.contains(uuid)) {
            staffChatToggled.remove(uuid);
            return false;
        } else {
            staffChatToggled.add(uuid);
            return true;
        }
    }

    public boolean toggleStaffChat(Player player) {
        boolean on = toggleStaffChat(player.getUniqueId());
        MessageUtils.sendMessage(player, plugin.getConfigManager().getPrefix()
                + (on ? "<green>Staff-Chat jetzt für dich als DEFAULT-CHAT aktiviert. Alles was du schreibst geht nur an Staff!"
                     : "<gray>Staff-Chat wieder als NORMAL-MODUS. Nutze /sc <Nachricht> um Staff zu schreiben."));
        return on;
    }

    public boolean isStaffChatToggled(Player player) {
        return hasStaffChatToggled(player.getUniqueId());
    }

    public boolean hasStaffChatToggled(UUID uuid) {
        return staffChatToggled.contains(uuid);
    }

    public Set<UUID> getStaffChatToggled() {
        return Collections.unmodifiableSet(staffChatToggled);
    }

    // ================= VANISH =================
    public boolean toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();
        ConfigManager cm = plugin.getConfigManager();
        if (vanishedPlayers.contains(uuid)) {
            vanishedPlayers.remove(uuid);
            player.removeMetadata("vanished", plugin);

            // Wieder einblenden
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }

            if (cm.isVanishHideFromTablist()) {
                player.setInvisible(false);
            }
            if (cm.isVanishGodMode()) {
                player.setInvulnerable(false);
            }

            MessageUtils.sendMessage(player, cm.getVanishMsg("unvanished"));
            return false;
        } else {
            vanishedPlayers.add(uuid);
            player.setMetadata("vanished", new FixedMetadataValue(plugin, true));

            // Ausblenden bei Spielern ohne Perm
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("moderation.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }

            if (cm.isVanishHideFromTablist()) {
                player.setInvisible(true);
            }
            if (cm.isVanishGodMode()) {
                player.setInvulnerable(true);
            }

            MessageUtils.sendMessage(player, cm.getVanishMsg("vanished"));
            return true;
        }
    }

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanishedPlayers);
    }

    // Beim Join eines neuen Spielers die verschwundenen ausblenden
    public void applyVanishForNewPlayer(Player joining) {
        if (joining.hasPermission("moderation.vanish.see")) return;
        for (UUID vanishedId : vanishedPlayers) {
            Player v = Bukkit.getPlayer(vanishedId);
            if (v != null && v.isOnline()) joining.hidePlayer(plugin, v);
        }
    }

    // Clear Chat
    public void clearChat(org.bukkit.command.CommandSender sender) {
        clearChat(sender, sender.getName());
    }

    public void clearChat(org.bukkit.command.CommandSender sender, String displayName) {
        String[] emptyLines = new String[150];
        Arrays.fill(emptyLines, " ");
        MiniMessage mm = MiniMessage.miniMessage();
        for (String line : emptyLines) {
            Component lineC = mm.deserialize(line);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("moderation.bypass.clearchat")) {
                    p.sendMessage(lineC);
                }
            }
            Bukkit.getConsoleSender().sendMessage(lineC);
        }
        MessageUtils.sendRaw(Bukkit.getConsoleSender(),
                "<gradient:#4caf50:#009688><bold>CHAT GELEERT</bold></gradient> " +
                        "<gray>Von: <gold>" + displayName);
        broadcastStaffNotification("<gradient:#4caf50:#009688><bold>CHAT GELEERT</bold></gradient> " +
                "<gray>Von: <gold>" + displayName);
    }
}
