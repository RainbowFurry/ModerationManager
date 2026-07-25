package net.rainbowfurry.moderationManager.guis;

import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ChatControlMenu extends BaseMenu {

    public ChatControlMenu(ModerationManager plugin) {
        super(plugin, "<gradient:#2196f3:#00bcd4><bold>Chat-Kontrolle</bold></gradient>", 5);
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();

        boolean locked = plugin.getChatManager().isChatLocked();
        boolean slowmode = plugin.getChatManager().isSlowmodeEnabled();
        int slowmodeSec = plugin.getConfigManager().getSlowmodeDelay();
        // Staff-Chat ist pro Spieler - Default Anzeige ohne Check
        boolean staffChat = false;

        // Clear Chat
        setItem(10, makeItem(Material.IRON_SHOVEL,
                "<yellow>🧹 Chat leeren (Clear)",
                List.of(
                        "<gray>Sendet 100 leere Zeilen an",
                        "<gray>alle Spieler (wie /cc).",
                        "",
                        "<yellow>Klick: Chat leeren"
                ), false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                plugin.getChatManager().clearChat(p);
                refresh(p);
            }
        });

        // Lock / Unlock Chat
        setItem(13, makeItem(locked ? Material.RED_CONCRETE : Material.GREEN_CONCRETE,
                locked ? "<red>🔒 Chat GESPERRT (klicken zum öffnen)"
                       : "<green>🔓 Chat FREI (klicken zum sperren)",
                List.of(
                        "<gray>Wenn gesperrt, können nur Spieler",
                        "<gray>mit <yellow>moderation.chat.bypass<gray> schreiben.",
                        "",
                        "<gray>Aktuell: " + (locked ? "<red>GESPERRT" : "<green>FREI"),
                        "",
                        "<yellow>Klicken: " + (locked ? "Freigeben" : "Sperren")
                ), true), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                plugin.getChatManager().setChatLocked(!locked, p.getName());
                refresh(p);
            }
        });

        // Slowmode
        setItem(16, makeItem(slowmode ? Material.CLOCK : Material.REPEATER,
                slowmode ? "<aqua>⏳ Slowmode: AN (" + slowmodeSec + "s)"
                         : "<gray>⏳ Slowmode: AUS",
                List.of(
                        "<gray>Verlangsame den Chat.",
                        "<gray>Jeder Spieler kann nur alle",
                        "<yellow>" + slowmodeSec + " Sekunden <gray>eine Nachricht senden.",
                        "",
                        "<gray>Aktuell: " + (slowmode ? "<green>AKTIV" : "<red>INAKTIV"),
                        "",
                        "<yellow>Klicken: " + (slowmode ? "Deaktivieren" : "Aktivieren")
                ), slowmode), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                plugin.getChatManager().setSlowmodeEnabled(!slowmode);
                Bukkit.broadcast(plugin.getConfigManager().getPrefix()
                                + (plugin.getChatManager().isSlowmodeEnabled()
                                        ? "Slowmode <green>aktiviert <gray>(" + slowmodeSec + "s)."
                                        : "Slowmode <red>deaktiviert<gray>."),
                        "moderation.chat.notify");
                refresh(p);
            }
        });

        // Staff-Chat
        setItem(29, makeItem(staffChat ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD,
                staffChat ? "<aqua>📡 Staff-Chat: DEIN DEFAULT CHAT"
                         : "<white>📡 Staff-Chat: NUR VIA /sc",
                List.of(
                        "<gray>Wenn AKTIV: alles was du schreibst",
                        "<gray>geht automatisch in den Staff-Chat",
                        "<gray>(ohne /sc).",
                        "",
                        "<gray>Aktuell: " + (staffChat ? "<green>★ Auto-Send" : "<gray>/sc manuell"),
                        "",
                        "<yellow>Klicken: " + (staffChat ? "Deaktivieren" : "Aktivieren (für dich)")
                ), staffChat), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                plugin.getStaffManager().toggleStaffChat(p);
                refresh(p);
            }
        });

        // Zeile 3: Zurück
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
}
