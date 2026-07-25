package net.rainbowfurry.moderationManager.guis;

import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

public class PlayerSelectorMenu extends BaseMenu {

    public record Target(UUID uniqueId, String name, boolean online) {}

    private final Consumer<Target> onSelect;
    private final int itemsPerPage = 28;
    private int currentPage = 0;
    private final List<Target> allTargets = new ArrayList<>();

    public PlayerSelectorMenu(ModerationManager plugin) {
        this(plugin, null);
    }

    public PlayerSelectorMenu(ModerationManager plugin, Consumer<Target> onSelect) {
        super(plugin, "<gradient:#ff9800:#ff5722><bold>Spieler auswählen</bold></gradient>", 6);
        this.onSelect = onSelect; // null = Default: PlayerInfoMenu öffnen
        // Online Players zuerst
        for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            allTargets.add(new Target(p.getUniqueId(), p.getName(), true));
        }
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();

        int maxPages = (int) Math.ceil((double) allTargets.size() / itemsPerPage);
        if (maxPages <= 0) maxPages = 1;
        if (currentPage >= maxPages) currentPage = maxPages - 1;

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allTargets.size());
        int slotIndex = 10;

        for (int i = start; i < end; i++) {
            Target t = allTargets.get(i);
            if ((slotIndex + 1) % 9 == 0) slotIndex++;
            if (slotIndex % 9 == 0) slotIndex++;

            List<String> lore = new ArrayList<>();
            lore.add(t.online ? "<green>● Online</green>" : "<gray>● Offline");
            lore.add("<gray>UUID: <white>" + t.uniqueId);
            lore.add("");
            lore.add(onSelect != null
                    ? "<yellow>Klick: Auswählen & Weiter"
                    : "<yellow>Klick: Spieler-Info öffnen");

            setItem(slotIndex, skullUUID(t.uniqueId, t.name,
                    "<" + (t.online ? "green" : "gray") + ">" + t.name, lore), e -> {
                if (e.getWhoClicked() instanceof Player p) {
                    p.closeInventory();
                    // Wenn Consumer vorhanden: ausführen
                    if (onSelect != null) {
                        onSelect.accept(t);
                    } else {
                        new PlayerInfoMenu(plugin, t.uniqueId, t.name, false).open(p);
                    }
                }
            });
            slotIndex++;
        }

        // Zurück Button
        setItem(45, makeItem(Material.ARROW, "<gold>← Zurück zum Hauptmenü",
                Collections.singletonList("<gray>Öffnet das Moderation Hauptmenü"), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new MainMenu(plugin).open(p);
            }
        });

        // Vorherige Seite
        if (currentPage > 0) {
            final int prev = currentPage - 1;
            setItem(47, makeItem(Material.BLAZE_ROD, "<yellow>← Seite " + (prev + 1),
                    List.of("<gray>Vorherige Seite anzeigen"), true), e -> {
                if (e.getWhoClicked() instanceof Player p) {
                    currentPage = prev;
                    refresh(p);
                }
            });
        }

        // Seiten Info
        setItem(49, makeItem(Material.PAPER,
                "<white>Seite <yellow>" + (currentPage + 1) + "<gray> / <yellow>" + maxPages,
                List.of("<gray>Spieler gesamt: <yellow>" + allTargets.size(),
                        "",
                        "<gray>Leere Seite? Alle Spieler sind online.",
                        "<gold>Tipp: Nutze /pi <Name> für Offline Spieler"), false), null);

        // Nächste Seite
        if (currentPage < maxPages - 1) {
            final int next = currentPage + 1;
            setItem(51, makeItem(Material.BLAZE_ROD, "<yellow>Seite " + (next + 1) + " →",
                    List.of("<gray>Nächste Seite anzeigen"), true), e -> {
                if (e.getWhoClicked() instanceof Player p) {
                    currentPage = next;
                    refresh(p);
                }
            });
        }

        // Schließen
        setItem(53, makeItem(Material.BARRIER, "<dark_red>❌ Schließen",
                Collections.singletonList("<gray>Inventar schließen"), false),
                e -> e.getWhoClicked().closeInventory());
    }
}
