package net.rainbowfurry.moderationManager.commands;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.managers.PunishmentManager;
import net.rainbowfurry.moderationManager.managers.StaffManager;
import net.rainbowfurry.moderationManager.models.IPLog;
import net.rainbowfurry.moderationManager.models.PlayerProfile;
import net.rainbowfurry.moderationManager.models.Punishment;
import net.rainbowfurry.moderationManager.utils.DurationUtils;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import net.rainbowfurry.moderationManager.utils.UUIDUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandManager {

    private final ModerationManager plugin;

    public CommandManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        register("ban", this::cmdBan);
        register("tempban", this::cmdTempban);
        register("unban", this::cmdUnban);
        register("kick", this::cmdKick);
        register("mute", this::cmdMute);
        register("tempmute", this::cmdTempmute);
        register("unmute", this::cmdUnmute);
        register("warn", this::cmdWarn);
        register("playerinfo", this::cmdPlayerinfo);
        register("staffchat", this::cmdStaffchat);
        register("vanish", this::cmdVanish);
        register("mod", this::cmdMod);
        register("clearchat", this::cmdClearchat);
        register("modreload", this::cmdReload);
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(new CommandAdapter(executor, name, plugin));
        } else {
            plugin.getLogger().warning("Konnte Command /" + name + " nicht registrieren!");
        }
    }

    @FunctionalInterface
    public interface CommandExecutor {
        boolean execute(CommandSender sender, String label, String[] args);
    }

    private static class CommandAdapter implements org.bukkit.command.CommandExecutor {
        private final CommandExecutor executor;
        private final String label;
        private final ModerationManager plugin;

        CommandAdapter(CommandExecutor executor, String label, ModerationManager plugin) {
            this.executor = executor;
            this.label = label;
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
            return executor.execute(sender, label, args);
        }
    }

    // ================ COMMANDS ================

    private boolean cmdBan(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.ban")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /ban <Spieler> <Grund>");
            return true;
        }
        String target = args[0];
        String reason = MessageUtils.joinArgs(args, 1);
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        long id = plugin.getPunishmentManager().ban(target, opUuid, opName, reason);
        MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde gebannt (ID #" + id + ")");
        return true;
    }

    private boolean cmdTempban(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.tempban")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /tempban <Spieler> <Zeit z.B. 1d12h> <Grund>");
            return true;
        }
        String target = args[0];
        long duration = DurationUtils.parseDuration(args[1]);
        String reason = MessageUtils.joinArgs(args, 2);
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        long id = plugin.getPunishmentManager().tempban(target, opUuid, opName, reason, duration);
        MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde für <aqua>" +
                DurationUtils.formatDuration(duration) + " <green>gebannt (ID #" + id + ")");
        return true;
    }

    private boolean cmdUnban(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.unban")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /unban <Spieler>");
            return true;
        }
        String target = args[0];
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        plugin.getPunishmentManager().unban(target, opUuid, opName);
        MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde entbannt.");
        return true;
    }

    private boolean cmdKick(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.kick")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /kick <Spieler> <Grund>");
            return true;
        }
        String target = args[0];
        String reason = MessageUtils.joinArgs(args, 1);
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        long id = plugin.getPunishmentManager().kick(target, opUuid, opName, reason);
        if (id == -1) {
            MessageUtils.sendMessage(sender, "<red>Spieler nicht gefunden! Muss online sein!");
        } else {
            MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde gekickt (ID #" + id + ")");
        }
        return true;
    }

    private boolean cmdMute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.mute")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /mute <Spieler> <Grund>");
            return true;
        }
        String target = args[0];
        String reason = MessageUtils.joinArgs(args, 1);
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        long id = plugin.getPunishmentManager().mute(target, opUuid, opName, reason);
        MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde gemutet (ID #" + id + ")");
        return true;
    }

    private boolean cmdTempmute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.tempmute")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /tempmute <Spieler> <Zeit z.B. 1h30m> <Grund>");
            return true;
        }
        String target = args[0];
        long duration = DurationUtils.parseDuration(args[1]);
        String reason = MessageUtils.joinArgs(args, 2);
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        long id = plugin.getPunishmentManager().tempmute(target, opUuid, opName, reason, duration);
        MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde für <aqua>" +
                DurationUtils.formatDuration(duration) + " <green>gemutet (ID #" + id + ")");
        return true;
    }

    private boolean cmdUnmute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.unmute")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /unmute <Spieler>");
            return true;
        }
        String target = args[0];
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        plugin.getPunishmentManager().unmute(target, opUuid, opName);
        MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde entmutet.");
        return true;
    }

    private boolean cmdWarn(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.warn")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /warn <Spieler> <Grund>");
            return true;
        }
        String target = args[0];
        String reason = MessageUtils.joinArgs(args, 1);
        UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;
        String opName = sender.getName();
        long id = plugin.getPunishmentManager().warn(target, opUuid, opName, reason);
        MessageUtils.sendMessage(sender, "<green>Spieler <yellow>" + target + " <green>wurde verwarnt (ID #" + id + ")");
        return true;
    }

    private boolean cmdPlayerinfo(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.playerinfo")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "<red>Benutzung: /playerinfo <Spieler>");
            return true;
        }
        String targetName = args[0];
        UUID targetUuid = UUIDUtils.getUUID(targetName);

        PunishmentManager pm = plugin.getPunishmentManager();
        PlayerProfile profile = plugin.getDatabaseManager().getPlayerProfile(targetUuid);
        if (profile == null) {
            profile = plugin.getDatabaseManager().getPlayerProfileByName(targetName);
        }
        if (profile == null) {
            // Immer noch nichts - neues minimales Profil
            profile = new PlayerProfile(targetUuid, targetName);
        }
        // Final target name/uuid (falls via name gefunden)
        UUID finalUuid = profile.getUuid() != null ? profile.getUuid() : targetUuid;
        String finalName = profile.getPlayerName() != null ? profile.getPlayerName() : targetName;

        // Wenn Spieler: GUI öffnen (außer er gibt --chat explizit an)
        if (sender instanceof Player p) {
            boolean useChat = false;
            for (String a : args) {
                if ("--chat".equals(a) || "-c".equals(a)) { useChat = true; break; }
            }
            if (!useChat) {
                new net.rainbowfurry.moderationManager.guis.PlayerInfoMenu(plugin, finalUuid, finalName, false).open(p);
                return true;
            }
        }

        // Aktualisiere Name falls nötig
        String displayName = finalName;

        // Online / Status
        Player onlinePlayer = Bukkit.getPlayer(targetUuid);
        Punishment activeBan = pm.checkBanned(targetUuid, null);
        Punishment activeMute = pm.checkMuted(targetUuid);

        String status;
        if (activeBan != null) {
            status = "<gradient:#ff0000:#b71c1c>GEBANNT</gradient> " +
                    "<gray>(noch " + DurationUtils.formatRemaining(activeBan.getEndAt()) + ")";
        } else if (onlinePlayer != null && onlinePlayer.isOnline()) {
            if (plugin.getStaffManager().isVanished(onlinePlayer.getUniqueId())) {
                status = "<gradient:#9d50bb:#6e48aa>VANISHED</gradient>";
            } else {
                status = "<gradient:#56ab2f:#a8e063>ONLINE</gradient>";
            }
        } else if (activeMute != null) {
            status = "<gradient:#7b1fa2:#00bcd4>GEMUTET</gradient> " +
                    "<gray>(noch " + DurationUtils.formatRemaining(activeMute.getEndAt()) + ")";
        } else {
            status = "<gray>OFFLINE</gray>";
        }

        // Spielzeit
        long playtime = profile.getPlaytimeMillis();
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            playtime += (System.currentTimeMillis() - profile.getLastLogin());
        }
        String playtimeStr = DurationUtils.formatDuration(playtime);

        // Header
        MiniMessage mm = MiniMessage.miniMessage();
        String headerTpl = plugin.getConfigManager().getPlayerInfoHeader();
        headerTpl = MessageUtils.applyPercentPlaceholders(headerTpl,
                "name", displayName,
                "uuid", targetUuid.toString(),
                "status", status,
                "first_join", MessageUtils.formatDate(profile.getFirstJoin()),
                "last_login", MessageUtils.formatDate(profile.getLastLogin()),
                "playtime", playtimeStr
        );
        TagResolver headerResolver = TagResolver.resolver(
                Placeholder.parsed("name", displayName),
                Placeholder.parsed("uuid", targetUuid.toString()),
                Placeholder.parsed("status", status),
                Placeholder.parsed("first_join", MessageUtils.formatDate(profile.getFirstJoin())),
                Placeholder.parsed("last_login", MessageUtils.formatDate(profile.getLastLogin())),
                Placeholder.parsed("playtime", playtimeStr)
        );
        sender.sendMessage(mm.deserialize(headerTpl, headerResolver));

        // Summary
        String summaryTpl = plugin.getConfigManager().getPlayerInfoSummaryFormat();
        summaryTpl = MessageUtils.applyPercentPlaceholders(summaryTpl,
                "bans", String.valueOf(profile.getTotalBans()),
                "mutes", String.valueOf(profile.getTotalMutes()),
                "warns", String.valueOf(profile.getTotalWarns()),
                "kicks", String.valueOf(profile.getTotalKicks())
        );
        TagResolver summaryResolver = TagResolver.resolver(
                Placeholder.parsed("bans", String.valueOf(profile.getTotalBans())),
                Placeholder.parsed("mutes", String.valueOf(profile.getTotalMutes())),
                Placeholder.parsed("warns", String.valueOf(profile.getTotalWarns())),
                Placeholder.parsed("kicks", String.valueOf(profile.getTotalKicks()))
        );
        sender.sendMessage(mm.deserialize(summaryTpl, summaryResolver));

        // Aktive Strafen
        if (activeBan != null) {
            sender.sendMessage(mm.deserialize("<gradient:#e53935:#b71c1c><bold>AKTIV GEBANNT:</bold></gradient> " +
                    "<gray>ID #" + activeBan.getId() + " | " +
                    "Grund: <white>" + activeBan.getReason() + " | " +
                    "Von: <gold>" + activeBan.getOperatorName() + " | " +
                    "Bis: <yellow>" + MessageUtils.formatDate(activeBan.getEndAt())));
        }
        if (activeMute != null) {
            sender.sendMessage(mm.deserialize("<gradient:#7b1fa2:#00bcd4><bold>AKTIV GEMUTET:</bold></gradient> " +
                    "<gray>ID #" + activeMute.getId() + " | " +
                    "Grund: <white>" + activeMute.getReason() + " | " +
                    "Von: <gold>" + activeMute.getOperatorName() + " | " +
                    "Bis: <yellow>" + MessageUtils.formatDate(activeMute.getEndAt())));
        }

        // History
        int limit = plugin.getConfigManager().getMaxShownPunishments();
        List<Punishment> history = plugin.getDatabaseManager().getPunishmentHistory(targetUuid, limit);
        StringBuilder histList = new StringBuilder();
        for (Punishment p : history) {
            String color = switch (p.getType()) {
                case BAN, TEMPBAN -> "<red>";
                case MUTE, TEMPMUTE -> "<dark_purple>";
                case WARN -> "<gold>";
                case KICK -> "<yellow>";
                case UNBAN, UNMUTE -> "<green>";
            };
            String state = p.isActive() && !p.isExpired() ? "<red>[AKTIV]</red> " : "";
            histList.append("<dark_gray>»</dark_gray> ")
                    .append(state)
                    .append(color).append(p.getTypeDisplayName()).append("</color>")
                    .append(" <gray>ID #").append(p.getId()).append("</gray>")
                    .append(" <gray>(").append(MessageUtils.formatDate(p.getCreatedAt())).append(")")
                    .append(" | Grund: <white>").append(p.getReason()).append("</white>")
                    .append(" | Von: <gold>").append(p.getOperatorName()).append("</gold><br>");
        }
        String historyTemplate = plugin.getConfigManager().getPlayerInfoHistoryFormat();
        historyTemplate = MessageUtils.applyPercentPlaceholders(historyTemplate,
                "anzahl", String.valueOf(history.size()),
                "history_list", histList.toString()
        );
        TagResolver historyResolver = TagResolver.resolver(
                Placeholder.parsed("anzahl", String.valueOf(history.size())),
                Placeholder.parsed("history_list", histList.toString())
        );
        sender.sendMessage(mm.deserialize(historyTemplate, historyResolver));

        // Alt Accounts
        List<IPLog> altAccounts = plugin.getAltAccountManager().getAltAccounts(targetUuid);
        altAccounts.removeIf(i -> i.getPlayerUUID().equals(targetUuid));
        if (!altAccounts.isEmpty()) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getPlayerInfoAltHeader()));
            for (IPLog alt : altAccounts) {
                String altTpl = plugin.getConfigManager().getPlayerInfoAltFormat();
                altTpl = MessageUtils.applyPercentPlaceholders(altTpl,
                        "name", alt.getPlayerName(),
                        "ip", alt.getIp(),
                        "last_seen", MessageUtils.formatDate(alt.getLastSeen())
                );
                TagResolver altRes = TagResolver.resolver(
                        Placeholder.parsed("name", alt.getPlayerName()),
                        Placeholder.parsed("ip", alt.getIp()),
                        Placeholder.parsed("last_seen", MessageUtils.formatDate(alt.getLastSeen()))
                );
                sender.sendMessage(mm.deserialize(altTpl, altRes));
            }
        }

        // Footer
        sender.sendMessage(mm.deserialize(plugin.getConfigManager().getPlayerInfoFooter()));

        return true;
    }

    private boolean cmdStaffchat(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.staffchat")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        if (args.length == 0) {
            // Toggle Mode
            if (sender instanceof Player p) {
                boolean toggled = plugin.getStaffManager().toggleStaffChat(p.getUniqueId());
                MessageUtils.sendMessage(sender, toggled
                        ? "<green>Staff Chat aktiviert. Alle Nachrichten gehen nun an das Team."
                        : "<red>Staff Chat deaktiviert.");
            } else {
                MessageUtils.sendMessage(sender, "<red>Benutzung: /sc <Nachricht>");
            }
            return true;
        }
        String message = String.join(" ", args);
        plugin.getStaffManager().sendStaffChat(
                sender instanceof Player p ? p.getUniqueId() : null,
                sender.getName(), message);
        return true;
    }

    private boolean cmdVanish(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.sendMessage(sender, "<red>Nur Spieler können Vanish nutzen!");
            return true;
        }
        if (!sender.hasPermission("moderation.vanish")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        plugin.getStaffManager().toggleVanish(p);
        return true;
    }

    private boolean cmdMod(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.mod")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        // GUI kann nur Spieler öffnen
        if (!(sender instanceof Player p)) {
            MessageUtils.sendMessage(sender, "<red>Dieses GUI-Menü kann nur als Spieler genutzt werden.");
            return true;
        }
        if (args.length == 0) {
            new net.rainbowfurry.moderationManager.guis.MainMenu(plugin).open(p);
            return true;
        }
        // /mod <Spieler> [punish|history|alts]
        String targetName = args[0];
        UUID targetUuid = UUIDUtils.getUUID(targetName);
        PlayerProfile profile = plugin.getDatabaseManager().getPlayerProfile(targetUuid);
        if (profile == null) {
            profile = plugin.getDatabaseManager().getPlayerProfileByName(targetName);
        }
        if (profile == null) {
            // Versuche: Offline-Player via Bukkit
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(targetName);
            if (op != null) {
                targetUuid = op.getUniqueId();
                targetName = op.getName() != null ? op.getName() : targetName;
            }
        } else {
            targetUuid = profile.getUuid();
            targetName = profile.getPlayerName();
        }
        String sub = args.length >= 2 ? args[1].toLowerCase() : "";
        switch (sub) {
            case "punish" -> new net.rainbowfurry.moderationManager.guis.PunishMenu(plugin, targetUuid, targetName, false).open(p);
            case "history" -> new net.rainbowfurry.moderationManager.guis.HistoryMenu(plugin, targetUuid, targetName).open(p);
            case "alts" -> new net.rainbowfurry.moderationManager.guis.AltAccountsMenu(plugin, targetUuid, targetName).open(p);
            default -> new net.rainbowfurry.moderationManager.guis.PlayerInfoMenu(plugin, targetUuid, targetName, false).open(p);
        }
        return true;
    }

    private boolean cmdClearchat(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.clearchat")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        plugin.getStaffManager().clearChat(sender);
        return true;
    }

    private boolean cmdReload(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("moderation.reload")) {
            MessageUtils.sendMessage(sender, "<red>Du hast keine Rechte dazu!");
            return true;
        }
        plugin.getConfigManager().reload();
        MessageUtils.sendMessage(sender, "<green>Moderation Config wurde neu geladen!");
        return true;
    }
}
