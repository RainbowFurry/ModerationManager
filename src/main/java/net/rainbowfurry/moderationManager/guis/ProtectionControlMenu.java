package net.rainbowfurry.moderationManager.guis;

import net.kyori.adventure.text.Component;
import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ProtectionControlMenu extends BaseMenu {

    public ProtectionControlMenu(ModerationManager plugin) {
        super(plugin, "<gradient:#ff1744:#ff6d00><bold>Schutz & Lag-Kontrolle</bold></gradient>", 5);
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();

        boolean lockdown = plugin.getProtectionManager().isLockdown();
        long until = plugin.getProtectionManager().getLockdownUntil();
        String lockdownEnd = lockdown
                ? (until == Long.MAX_VALUE ? "<red>∞ PERMANENT"
                        : "<white>" + format(until) + " <gray>(" + secsRemaining(until) + ")")
                : "<gray>-";

        // Lockdown an/aus
        setItem(10, makeItem(lockdown ? Material.RED_TERRACOTTA : Material.GREEN_TERRACOTTA,
                lockdown ? "<red>🚨 LOCKDOWN: AKTIV (klicken für Stop)"
                         : "<green>🚨 LOCKDOWN: INAKTIV (klicken für Start - 10min)",
                List.of(
                        "<gray>Bei Lockdown können nur Spieler",
                        "<gray>mit OP oder Permission",
                        "<yellow>moderation.bypass.raid <gray>den Server betreten.",
                        "",
                        "<gray>Aktuell: " + (lockdown ? "<red>AKTIV bis: " + lockdownEnd : "<green>AUS"),
                        "",
                        "<yellow>Linksklick: " + (lockdown ? "Lockdown beenden" : "Lockdown für 10 Min starten"),
                        "<yellow>Shift-Klick: " + (lockdown ? "" : "PERMANENTER Lockdown starten")
                ), lockdown), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                if (lockdown) {
                    plugin.getProtectionManager().endLockdown(p.getName());
                } else {
                    boolean perm = e.getClick().isShiftClick();
                    long dur = perm ? Long.MAX_VALUE : 600_000L;
                    plugin.getProtectionManager().startLockdown(dur, p.getName());
                }
                refresh(p);
            }
        });

        // Items clearen
        setItem(13, makeItem(Material.ITEM_FRAME,
                "<yellow>📦 Dropped Items entfernen",
                List.of(
                        "<gray>Entfernt alle Ground-Items auf",
                        "<gray>allen geladenen Welten / Chunks.",
                        "",
                        "<yellow>Klicken: Alle Items entfernen"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                int removed = clearDroppedItems();
                p.sendMessage(plugin.getConfigManager().getPrefix()
                        + "Es wurden <yellow>" + removed + " <gray>gedroppte Items entfernt.");
                refresh(p);
            }
        });

        // Mobs clearen
        setItem(16, makeItem(Material.SPAWNER,
                "<red>👾 Mobs entfernen (Tiere + Monster außer Villager)",
                List.of(
                        "<gray>Entfernt alle Mobs (keine Spieler,",
                        "<gray>keine Fahrzeuge, keine NPCs/Villager).",
                        "",
                        "<yellow>Klicken: Mobs entfernen"
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                int removed = clearMobs();
                p.sendMessage(plugin.getConfigManager().getPrefix()
                        + "Es wurden <yellow>" + removed + " <gray>Mobs entfernt.");
                refresh(p);
            }
        });

        // Lag-Clear Alles
        setItem(28, makeItem(Material.TNT,
                "<red>💥 Lag Clear: Items + Mobs",
                List.of(
                        "<gray>Items entfernen + Mobs entfernen",
                        "<gray>in einem Klick.",
                        "",
                        "<yellow>Klicken: Lag-Clear durchführen"
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                int a = clearDroppedItems();
                int b = clearMobs();
                p.sendMessage(plugin.getConfigManager().getPrefix()
                        + "Lag-Clear: <yellow>" + a + " <gray>Items, <yellow>" + b + " <gray>Mobs entfernt.");
                Bukkit.broadcast(Component.text(""), "moderation.lag.notify");
                Bukkit.broadcast(plugin.getConfigManager().getPrefix()
                        + "<yellow>Lag-Clear<gray> durchgeführt von <green>" + p.getName()
                        + "<gray> (<yellow>" + a + "<gray> Items, <yellow>" + b + "<gray> Mobs)",
                        "moderation.lag.notify");
                refresh(p);
            }
        });

        // Status Zeile
        setItem(31, makeItem(Material.BOOK, "<blue>🛡️ Schutz-Status",
                List.of(
                        "<gray>DDoS-Schutz: " + (plugin.getConfigManager().isDdosEnabled() ? "<green>AN" : "<red>AUS"),
                        "<gray>Raid-Erkennung-Schwelle: <yellow>" + plugin.getConfigManager().getRaidThreshold() + " / "
                                + plugin.getConfigManager().getRaidWindow() + "s",
                        "<gray>Lockdown: " + (lockdown ? "<red>AKTIV bis " + lockdownEnd : "<green>NEIN"),
                        "",
                        "<gray>Verbindungstimeout IP: <yellow>" + plugin.getConfigManager().getDdosConnectionLimit() + " / "
                                + plugin.getConfigManager().getDdosWindow() + "s"
                ), false), null);

        // Zeile 4: Zurück
        setItem(36, makeItem(Material.ARROW, "<gold>← Zum Hauptmenü",
                Collections.singletonList("<gray>Moderation Manager"), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new MainMenu(plugin).open(p);
            }
        });
        setItem(44, makeItem(Material.BARRIER, "<dark_red>❌ Schließen",
                Collections.singletonList("<gray>Inventar schließen"), false),
                e -> e.getWhoClicked().closeInventory());
    }

    private int clearDroppedItems() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntitiesByClass(Item.class)) {
                e.remove();
                count++;
            }
        }
        return count;
    }

    private int clearMobs() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getLivingEntities()) {
                if (e instanceof Player) continue;
                if (e.getType() == EntityType.VILLAGER) continue;
                if (e.getType() == EntityType.ARMOR_STAND) continue;
                if (e.getType().name().contains("MINECART")) continue;
                if (e.getType().name().startsWith("BOAT")) continue;
                e.remove();
                count++;
            }
        }
        return count;
    }

    private String format(long time) {
        if (time <= 0) return "-";
        if (time == Long.MAX_VALUE) return "∞";
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(time),
                java.time.ZoneId.systemDefault()
        ).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private String secsRemaining(long end) {
        if (end == Long.MAX_VALUE) return "permanent";
        long diff = end - System.currentTimeMillis();
        if (diff < 0) return "abgelaufen";
        long s = diff / 1000;
        long d = s / 86_400; long h = (s % 86_400) / 3_600; long m = (s % 3_600) / 60; long sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        sb.append(sec).append("s übrig");
        return sb.toString();
    }
}
