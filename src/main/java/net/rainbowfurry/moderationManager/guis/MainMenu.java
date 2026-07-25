package net.rainbowfurry.moderationManager.guis;

import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class MainMenu extends BaseMenu {

    public MainMenu(ModerationManager plugin) {
        super(plugin, "<gradient:#6a11cb:#2575fc><bold>Moderation Manager</bold></gradient>", 6);
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();

        // Zeile 1 (Mitte): Spieler-Menü

        setItem(10, makeItem(Material.PLAYER_HEAD, "<yellow>👥 Spieler suchen / auswählen",
                List.of(
                        "<gray>Öffne ein Menü, in dem du",
                        "<gray>einen Spieler auswählen kannst.",
                        "<gray>Zeigt dir Online-Spieler als Köpfe.",
                        "",
                        "<green>Klicken: Spieler-Selektor öffnen"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new PlayerSelectorMenu(plugin).open(p);
            }
        });

        setItem(13, makeItem(Material.KNOWLEDGE_BOOK, "<blue>📊 Spieler-Info (via Namen)",
                List.of(
                        "<gray>Bekomme alle Infos zu",
                        "<gray>einem bestimmten Spieler:",
                        "<gray>• UUID, IP, Spielzeit, Erster Login",
                        "<gray>• Warns, Bans, Mutes, Kicks",
                        "<gray>• Alt-Accounts (gleiche IP)",
                        "",
                        "<gold>Tipp: Auch via /pi <Name>"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                p.closeInventory();
                // Später via AnvilGUI - jetzt kurz Command via chat (als Fallback)
                p.sendMessage(plugin.getConfigManager().getPrefix() +
                        "<gray>Benutze <yellow>/pi <Spielername><gray> oder klicke auf Spieler-Selektor (links).");
                // Wir delegieren an Command, dort ist die Logic
                Bukkit.dispatchCommand(p, "playerinfo "); // trigger help
            }
        });

        setItem(16, makeItem(Material.SHIELD, "<red>🎯 Straf-Menü",
                List.of(
                        "<gray>Bestrafe einen Spieler:",
                        "<gray>• Warn / Kick / Mute / Ban",
                        "<gray>• Mit Zeit-Auswahl",
                        "<gray>• Mit vordefinierten Gründen",
                        "",
                        "<gold>Tipp: /punish <Name>"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new PlayerSelectorMenu(plugin, (target) ->
                        new PunishMenu(plugin, target.uniqueId(), target.name(), true).open(p)
                ).open(p);
            }
        });

        // Zeile 2: Funktionen

        setItem(20, makeItem(Material.CHEST, "<light_purple>⚖️ Straf-Vergangenheit",
                List.of(
                        "<gray>Historie aller Bestrafungen",
                        "<gray>mit Pagination (Blättern).",
                        "",
                        "<gold>Tipp: /history <Name>"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new PlayerSelectorMenu(plugin, (target) ->
                        new HistoryMenu(plugin, target.uniqueId(), target.name()).open(p)
                ).open(p);
            }
        });

        setItem(22, makeItem(Material.BEACON, "<aqua>👥 Alt-Accounts Übersicht",
                List.of(
                        "<gray>Alle Spieler mit gleicher IP",
                        "<gray>zu einem Account anzeigen.",
                        "",
                        "<gold>Tipp: /alts <Name>"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new PlayerSelectorMenu(plugin, (target) ->
                        new AltAccountsMenu(plugin, target.uniqueId(), target.name()).open(p)
                ).open(p);
            }
        });

        setItem(24, makeItem(Material.COMPARATOR, "<red>🎓 Alt-Accounts global suchen",
                List.of(
                        "<gray>Nach Accounts suchen",
                        "<gray>mit identischer IP.",
                        "",
                        "<gold>Öffnet: Alt-Accounts Menü"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new AltAccountsMenu(plugin, null, null).open(p);
            }
        });

        // Zeile 3: Chat & Schutz

        setItem(30, makeItem(Material.PAPER, "<white>💬 Chat-Kontrolle",
                List.of(
                        "<gray>Steuere den Chat:",
                        "<gray>• /cc - Clear Chat",
                        "<gray>• Chat sperren / entsperren",
                        "<gray>• Slowmode an / aus",
                        "<gray>• Staff-Chat toggeln",
                        "",
                        "<green>Klicken: Chat-Menü öffnen"
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new ChatControlMenu(plugin).open(p);
            }
        });

        setItem(32, makeItem(Material.COMMAND_BLOCK, "<red>🛡️ Schutz & Raid Kontrolle",
                List.of(
                        "<gray>Steuere den Schutz:",
                        "<gray>• Lockdown / Raid Modus an/aus",
                        "<gray>• Items & Mobs clearen (Anti-Lag)",
                        "<gray>• DDoS-Status einsehen",
                        "",
                        "<green>Klicken: Schutz-Menü öffnen"
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new ProtectionControlMenu(plugin).open(p);
            }
        });

        // Zeile 4: Sonstiges

        setItem(40, makeItem(Material.BARRIER, "<dark_red>❌ Menü schließen",
                List.of("<gray>Schließt das Inventar."), false),
                e -> e.getWhoClicked().closeInventory());
    }

    @SuppressWarnings("deprecation")
    private static class Bukkit {
        static boolean dispatchCommand(org.bukkit.command.CommandSender s, String cmd) {
            return org.bukkit.Bukkit.dispatchCommand(s, cmd);
        }
    }
}
