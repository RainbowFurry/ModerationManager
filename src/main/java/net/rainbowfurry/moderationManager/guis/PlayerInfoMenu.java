package net.rainbowfurry.moderationManager.guis;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.models.PlayerProfile;
import net.rainbowfurry.moderationManager.models.Punishment;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerInfoMenu extends BaseMenu {

    private final UUID targetUUID;
    private final String targetName;
    private final boolean punishMode;

    public PlayerInfoMenu(ModerationManager plugin, UUID targetUUID, String targetName, boolean punishMode) {
        super(plugin, "<gradient:#2196f3:#21cbf3><bold>Spieler-Profil</bold></gradient>", 6);
        this.targetUUID = targetUUID;
        this.targetName = targetName != null ? targetName : "Unbekannt";
        this.punishMode = punishMode;
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();

        PlayerProfile profile = plugin.getDatabaseManager().getPlayerProfile(targetUUID);
        List<Punishment> history = plugin.getPunishmentManager().getPunishments(targetUUID);
        Punishment activeBan = plugin.getPunishmentManager().getActiveBan(targetUUID);
        Punishment activeMute = plugin.getPunishmentManager().getActiveMute(targetUUID);

        // Kopf
        String displayName = profile != null ? profile.getPlayerName() : targetName;
        List<String> headLore = new ArrayList<>();
        headLore.add("<gray>Name: <white>" + displayName);
        headLore.add("<gray>UUID: <white>" + targetUUID);
        if (profile != null) {
            headLore.add("<gray>Erster Login: <white>" + format(profile.getFirstJoin()));
            headLore.add("<gray>Letzter Login: <white>" + format(profile.getLastLogin()));
            headLore.add("<gray>Letzte IP: <white>" + firstNonNull(profile.getCurrentIp(), "-"));
            long playtimeSec = profile.getPlaytimeMillis() / 1000;
            headLore.add("<gray>Spielzeit: <aqua>" + formatPlaytime(playtimeSec));
        }
        headLore.add("<gray>Gesamt Strafen: <yellow>" + history.size());
        if (activeBan != null) {
            headLore.add("<red>⚠️ AKTIV GEBANNT</red>");
        }
        if (activeMute != null) {
            headLore.add("<gold>⚠️ AKTIV GEMUTET</gold>");
        }
        setItem(10, skullUUID(targetUUID, displayName, "<yellow>" + displayName, headLore), null);

        // Stats
        long warns = history.stream().filter(p -> p.getType() == Punishment.Type.WARN).count();
        long mutes = history.stream().filter(p -> p.getType() == Punishment.Type.MUTE).count();
        long kicks = history.stream().filter(p -> p.getType() == Punishment.Type.KICK).count();
        long bans = history.stream().filter(p -> p.getType() == Punishment.Type.BAN).count();
        long staffP = history.stream().filter(Punishment::isActive).count();

        setItem(13, makeItem(Material.NETHER_STAR, "<gold>📊 Straf-Statistiken",
                List.of(
                        "<gray>Warns: <yellow>" + warns,
                        "<gray>Mutes: <yellow>" + mutes,
                        "<gray>Kicks: <yellow>" + kicks,
                        "<gray>Bans: <red>" + bans,
                        "",
                        "<gray>AKTIVE Strafen: <red>" + staffP
                ), true), null);

        // Punish Button
        setItem(16, makeItem(Material.IRON_SWORD, "<red>🎯 Spieler bestrafen",
                List.of(
                        "<gray>Klicken: Straf-GUI öffnen",
                        "<gray>Ban / Mute / Warn / Kick",
                        "<gray>Zeit-Auswahl & Gründe"
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new PunishMenu(plugin, targetUUID, displayName, false).open(p);
            }
        });

        // Row 2: History + Alts
        setItem(20, makeItem(Material.WRITTEN_BOOK, "<blue>📖 Straf-Vergangenheit",
                List.of(
                        "<yellow>" + history.size() + " <gray>Einträge",
                        "",
                        "<yellow>Klicken: History (geblättert) anzeigen"
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new HistoryMenu(plugin, targetUUID, displayName).open(p);
            }
        });

        setItem(22, makeItem(Material.BEACON, "<aqua>👥 Alt-Accounts",
                List.of(
                        "<gray>Alle Accounts, die von der",
                        "<gray>gleichen IP wie",
                        "<white>" + displayName + "<gray> verbunden haben.",
                        "",
                        "<yellow>Klicken: Alt-Accounts anzeigen"
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new AltAccountsMenu(plugin, targetUUID, displayName).open(p);
            }
        });

        setItem(24, makeItem(Material.FIREWORK_ROCKET, "<light_purple>🚀 Schnell-Aktionen",
                List.of(
                        "<gray>Direkte Aktionen ohne Menü:",
                        "",
                        "<yellow>Linksklick: <white>Warnung 'Verwarnung'",
                        "<yellow>Rechtsklick: <white>Kick 'Verwarnung'",
                        "<yellow>Shift-Links: <white>Mute 1 Stunde - Spam",
                        "<yellow>Shift-Rechts: <white>Ban 7 Tage - Cheating"
                ), true), e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            String opName = p.getName();
            switch (e.getClick()) {
                case LEFT -> plugin.getPunishmentManager().addPunishment(
                        new Punishment(0, targetUUID, displayName, Punishment.Type.WARN, 1L, "Verwarnung", opName, System.currentTimeMillis(), Long.MAX_VALUE, false));
                case RIGHT -> {
                    Player t = org.bukkit.Bukkit.getPlayer(targetUUID);
                    if (t != null) plugin.getPunishmentManager().executeKick(t, "Verwarnung", opName);
                    else {
                        plugin.getPunishmentManager().addPunishment(
                                new Punishment(0, targetUUID, displayName, Punishment.Type.KICK, 1L, "Verwarnung", opName, System.currentTimeMillis(), System.currentTimeMillis() + 1L, false));
                        p.sendMessage(ChatColor.GRAY + "Kick nicht möglich: Spieler offline (als Warn gespeichert).");
                    }
                }
                case SHIFT_LEFT -> plugin.getPunishmentManager().addPunishment(
                        new Punishment(0, targetUUID, displayName, Punishment.Type.MUTE, 1L, "Spam / Chat Missbrauch", opName, System.currentTimeMillis(), System.currentTimeMillis() + 3_600_000, false));
                case SHIFT_RIGHT -> plugin.getPunishmentManager().addPunishment(
                        new Punishment(0, targetUUID, displayName, Punishment.Type.BAN, 1L, "Cheating / Hacking", opName, System.currentTimeMillis(), System.currentTimeMillis() + 7L * 24 * 3_600_000, false));
                default -> { /* nothing */ }
            }
            // Refresh this menu after action
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> new PlayerInfoMenu(plugin, targetUUID, displayName, false).open(p));
        });

        // Row 3: Deaktivieren (Unmute / Unban)
        if (activeMute != null) {
            setItem(30, makeItem(Material.MUSIC_DISC_CHIRP, "<green>🔊 Mute aufheben",
                    List.of("<gray>Aktiver Mute #<yellow>" + activeMute.getId() +
                            "<gray> - <white>" + activeMute.getReason(),
                            "",
                            "<yellow>Klicken: <green>Mute entfernen"), true), e -> {
                if (e.getWhoClicked() instanceof Player p) {
                    plugin.getPunishmentManager().removePunishment(activeMute.getId(), p.getName());
                    p.closeInventory();
                    Bukkit.getScheduler().runTask(plugin, () -> new PlayerInfoMenu(plugin, targetUUID, displayName, false).open(p));
                }
            });
        } else {
            setItem(30, makeItem(Material.MUSIC_DISC_CHIRP, "<gray>🔊 Mute aufheben",
                    List.of("<dark_gray>Spieler hat keinen aktiven Mute"), false), null);
        }

        if (activeBan != null) {
            setItem(32, makeItem(Material.TRIPWIRE_HOOK, "<green>⛓️ Ban aufheben",
                    List.of("<gray>Aktiver Ban #<yellow>" + activeBan.getId() +
                            "<gray> - <white>" + activeBan.getReason(),
                            "",
                            "<yellow>Klicken: <green>Ban aufheben (unban)"), true), e -> {
                if (e.getWhoClicked() instanceof Player p) {
                    plugin.getPunishmentManager().removePunishment(activeBan.getId(), p.getName());
                    p.closeInventory();
                    Bukkit.getScheduler().runTask(plugin, () -> new PlayerInfoMenu(plugin, targetUUID, displayName, false).open(p));
                }
            });
        } else {
            setItem(32, makeItem(Material.TRIPWIRE_HOOK, "<gray>⛓️ Ban aufheben",
                    List.of("<dark_gray>Spieler hat keinen aktiven Ban"), false), null);
        }

        // Row 4: Zurück + Schließen
        setItem(45, makeItem(Material.ARROW, "<gold>← Zurück",
                List.of(punishMode ? "<gray>Zurück zum Straf-Menü" : "<gray>Zurück zum Spieler Selektor"), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                if (punishMode) {
                    new PunishMenu(plugin, targetUUID, displayName, true).open(p);
                } else {
                    new PlayerSelectorMenu(plugin).open(p);
                }
            }
        });

        setItem(49, makeItem(Material.COMPASS, "<white>🔎 Anderen Spieler suchen",
                List.of("<gray>Öffne den Spieler-Selektor"), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new PlayerSelectorMenu(plugin).open(p);
            }
        });

        setItem(53, makeItem(Material.BARRIER, "<dark_red>❌ Schließen",
                List.of("<gray>Inventar schließen"), false), e -> e.getWhoClicked().closeInventory());
    }

    private static class Bukkit {
        static org.bukkit.scheduler.BukkitScheduler getScheduler() { return org.bukkit.Bukkit.getScheduler(); }
    }

    private String firstNonNull(String s, String def) { return s != null && !s.isEmpty() ? s : def; }

    private String format(long time) {
        if (time <= 0) return "-";
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(time),
                java.time.ZoneId.systemDefault()
        ).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private String formatPlaytime(long secs) {
        long days = secs / 86_400; long hours = (secs % 86_400) / 3_600;
        long mins = (secs % 3_600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        sb.append(mins).append("m");
        return sb.toString();
    }
}
