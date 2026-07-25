package net.rainbowfurry.moderationManager.guis;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.models.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class AltAccountsMenu extends BaseMenu {

    private final UUID centerUUID;
    private final String centerName;
    private final int perPage = 21;
    private int currentPage = 0;
    private List<PlayerProfile> allAlts = new ArrayList<>();

    public AltAccountsMenu(ModerationManager plugin, UUID centerUUID, String centerName) {
        super(plugin, "<gradient:#009688:#4caf50><bold>Alt-Accounts</bold></gradient>", 6);
        this.centerUUID = centerUUID;
        this.centerName = centerName;
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();

        allAlts = new ArrayList<>();
        if (centerUUID != null) {
            PlayerProfile center = plugin.getDatabaseManager().getPlayerProfile(centerUUID);
            if (center != null && center.getCurrentIp() != null && !center.getCurrentIp().isEmpty()) {
                allAlts = plugin.getAltAccountManager().findAltAccounts(centerUUID, center.getCurrentIp());
            }
        } else {
            // Alt-Ansicht: Zeige alle Accounts der IPs der letzten Joins (Top 100)
            allAlts = plugin.getAltAccountManager().findAllWithSharedIps();
        }

        // Kopf des Ziels oben
        if (centerUUID != null && centerName != null) {
            setItem(4, skullUUID(centerUUID, centerName,
                    "<yellow>" + centerName + " <gray>(Scan-Ziel)",
                    List.of("<gray>IP: <white>" + getLastIpOf(centerUUID),
                            "<gray>Alt Accounts gefunden: <yellow>" + allAlts.size())), null);
        } else {
            setItem(4, makeItem(Material.BEACON, "<aqua>👥 Alle Alt-Account Gruppen",
                    List.of("<gray>Alle Spieler mit geteilten IPs",
                            "<gray>gefiltert nach den letzten Logins",
                            "",
                            "<gray>Insgesamt: <yellow>" + allAlts.size() + " Spieler"), true), null);
        }

        int maxPages = (int) Math.ceil((double) Math.max(1, allAlts.size()) / perPage);
        if (maxPages <= 0) maxPages = 1;
        if (currentPage >= maxPages) currentPage = maxPages - 1;
        int start = currentPage * perPage;
        int end = Math.min(start + perPage, allAlts.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            PlayerProfile alt = allAlts.get(i);
            if ((slot + 1) % 9 == 0) slot++;
            if (slot % 9 == 0) slot++;
            if (slot > 43) break;

            final UUID altId = alt.getUuid();
            final String altName = alt.getPlayerName();
            final boolean isCenter = alt.getUuid().equals(centerUUID);

            List<String> lore = new ArrayList<>();
            lore.add("<gray>UUID: <white>" + altId);
            lore.add("<gray>IP: <white>" + firstNonNull(alt.getCurrentIp(), "-"));
            lore.add("<gray>Erster Login: <white>" + format(alt.getFirstJoin()));
            lore.add("<gray>Letzter Login: <white>" + format(alt.getLastLogin()));
            if (isCenter) lore.add("<aqua>★ DIESER SPIELER (Scan-Ziel)");
            lore.add("");
            lore.add("<yellow>Linksklick: <white>Profil & Strafen");
            lore.add("<yellow>Rechtsklick: <white>Bestrafen (Punish-GUI)");

            final int fslot = slot;
            setItem(fslot, skullUUID(altId, altName,
                            (isCenter ? "<aqua>★ " : "<yellow>") + altName, lore),
                    e -> {
                        if (e.getWhoClicked() instanceof Player p) {
                            if (e.getClick().isRightClick()) {
                                new PunishMenu(plugin, altId, altName, false).open(p);
                            } else {
                                new PlayerInfoMenu(plugin, altId, altName, false).open(p);
                            }
                        }
                    });
            slot++;
        }

        // Seiten-Buttons
        setItem(45, makeItem(Material.ARROW, "<gold>← Zurück",
                List.of(centerUUID != null ? "<gray>Zum Spieler-Profil" : "<gray>Zum Hauptmenü"), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                if (centerUUID != null) {
                    new PlayerInfoMenu(plugin, centerUUID, centerName, false).open(p);
                } else {
                    new MainMenu(plugin).open(p);
                }
            }
        });

        if (currentPage > 0) {
            final int prev = currentPage - 1;
            setItem(47, makeItem(Material.BLAZE_ROD, "<yellow>← Seite " + (prev + 1),
                    List.of("<gray>Vorherige Seite"), true), e -> {
                currentPage = prev;
                if (e.getWhoClicked() instanceof Player p) refresh(p);
            });
        }

        setItem(49, makeItem(Material.PAPER, "<white>Seite <yellow>" + (currentPage + 1) + "<gray>/<yellow>" + maxPages,
                List.of("<gray>Spieler gesamt: <yellow>" + allAlts.size()), false), null);

        if (currentPage < maxPages - 1) {
            final int next = currentPage + 1;
            setItem(51, makeItem(Material.BLAZE_ROD, "<yellow>Seite " + (next + 1) + " →",
                    List.of("<gray>Nächste Seite"), true), e -> {
                currentPage = next;
                if (e.getWhoClicked() instanceof Player p) refresh(p);
            });
        }

        setItem(53, makeItem(Material.BARRIER, "<dark_red>❌ Schließen",
                List.of("<gray>Inventar schließen"), false),
                e -> e.getWhoClicked().closeInventory());

        if (allAlts.isEmpty()) {
            setItem(22, makeItem(Material.BARRIER, "<green>✔ Keine Alt-Accounts gefunden",
                    List.of("<gray>Keine Accounts mit gleicher IP.",
                            "<gray>Entweder neu, oder nur dieser Account auf der IP."), true), null);
        }
    }

    private String getLastIpOf(UUID id) {
        PlayerProfile p = plugin.getDatabaseManager().getPlayerProfile(id);
        if (p == null) return "-";
        return firstNonNull(p.getCurrentIp(), "-");
    }

    private String firstNonNull(String s, String def) { return s != null && !s.isEmpty() ? s : def; }

    private String format(long time) {
        if (time <= 0) return "-";
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(time),
                java.time.ZoneId.systemDefault()
        ).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
