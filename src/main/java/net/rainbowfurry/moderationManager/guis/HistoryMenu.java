package net.rainbowfurry.moderationManager.guis;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.managers.ConfigManager;
import net.rainbowfurry.moderationManager.models.Punishment;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class HistoryMenu extends BaseMenu {

    private static final String DELETE_PERM = "moderation.punishments.delete";
    private final UUID targetUUID;
    private final String targetName;
    private final int perPage = 5;
    private int currentPage = 0;
    private List<Punishment> history = new ArrayList<>();
    private Player opener;

    public HistoryMenu(ModerationManager plugin, UUID targetUUID, String targetName) {
        super(plugin,
                plugin.getConfigManager().getGuiTitle("history-menu",
                                "<gradient:#9c27b0:#e91e63><bold>📜 Straf-Historie: %name%</bold></gradient>")
                        .replace("%name%", escape(targetName)),
                plugin.getConfigManager().getGuiRows("history-menu", 6));
        this.targetUUID = targetUUID;
        this.targetName = targetName != null ? targetName : "?";
    }

    private static String escape(String s) {
        if (s == null) return "?";
        return s.replace("<", "＜").replace(">", "＞");
    }

    @Override
    public void open(Player player) {
        this.opener = player;
        super.open(player);
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();
        history = plugin.getPunishmentManager().getPunishments(targetUUID);
        ConfigManager cfg = plugin.getConfigManager();

        int maxPages = (int) Math.ceil((double) history.size() / perPage);
        if (maxPages <= 0) maxPages = 1;
        if (currentPage >= maxPages) currentPage = maxPages - 1;

        boolean canDelete = opener != null && opener.hasPermission(DELETE_PERM);

        String skullName = cfg.guiItemName("skull-default-name", "<yellow>%name% <gray>(Profil)")
                .replace("%name%", targetName);
        List<String> skullLore = new ArrayList<>();
        for (String line : List.of(cfg.guiItemLine("skull-lore-count", null,
                "<gray>Strafen gesamt: <yellow>%count%"))) {
            skullLore.add(MessageUtils.applyPercentPlaceholders(line, "count", String.valueOf(history.size())));
        }
        setItem(4, skullUUID(targetUUID, targetName, skullName, skullLore), null);

        int start = currentPage * perPage;
        int slot = 19;

        for (int i = 0; i < perPage && (start + i) < history.size(); i++) {
            Punishment pun = history.get(start + i);
            final Punishment fpun = pun;
            Consumer<InventoryClickEvent> handler = e -> handlePunishmentClick(e, fpun);
            setItem(slot, makePunishmentItem(pun, start + i + 1, canDelete), handler);
            slot += 2;
            if (slot > 43) break;
        }

        Material backMat = ConfigManager.safeMaterial(cfg.guiItemMaterial("back-arrow"), Material.ARROW);
        String backName = cfg.guiItemName("back-arrow", "<gold>← Zurück");
        List<String> backLore = cfg.guiItemLore("back-arrow");
        setItem(45, makeItem(backMat, backName, backLore, false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new PlayerInfoMenu(plugin, targetUUID, targetName, false).open(p);
            }
        });

        if (currentPage > 0) {
            final int prev = currentPage - 1;
            String pageLabel = MessageUtils.applyPercentPlaceholders(
                    cfg.getMessage("history-prev", "<yellow>← Seite %page%"), "page", String.valueOf(prev + 1));
            setItem(47, makeItem(Material.BLAZE_ROD, pageLabel,
                    List.of("<gray>Vorherige Seite"), true), e -> {
                currentPage = prev;
                if (e.getWhoClicked() instanceof Player p) refresh(p);
            });
        }

        String pageInfo = MessageUtils.applyPercentPlaceholders(
                cfg.getMessage("history-page-info", "<white>Seite <yellow>%current<gray>/<yellow>%max%"),
                "current", String.valueOf(currentPage + 1),
                "max", String.valueOf(maxPages));
        String pageTotal = MessageUtils.applyPercentPlaceholders(
                cfg.getMessage("history-page-total", "<gray>Einträge gesamt: <yellow>%total%"),
                "total", String.valueOf(history.size()));
        setItem(49, makeItem(Material.PAPER, pageInfo,
                List.of(pageTotal), false), null);

        if (currentPage < maxPages - 1) {
            final int next = currentPage + 1;
            String pageLabel = MessageUtils.applyPercentPlaceholders(
                    cfg.getMessage("history-next", "<yellow>Seite %page% →"), "page", String.valueOf(next + 1));
            setItem(51, makeItem(Material.BLAZE_ROD, pageLabel,
                    List.of("<gray>Nächste Seite"), true), e -> {
                currentPage = next;
                if (e.getWhoClicked() instanceof Player p) refresh(p);
            });
        }

        Material closeMat = ConfigManager.safeMaterial(cfg.guiItemMaterial("close-barrier"), Material.BARRIER);
        String closeName = cfg.guiItemName("close-barrier", "<dark_red>❌ Schließen");
        List<String> closeLore = cfg.guiItemLore("close-barrier");
        setItem(53, makeItem(closeMat, closeName, closeLore, false),
                e -> e.getWhoClicked().closeInventory());

        if (history.isEmpty()) {
            String emptyName = cfg.getMessage("history-empty-name", "<green>✔ Keine Strafen gefunden");
            String emptyLore = cfg.getMessage("history-empty-lore",
                    "<gray>Spieler hat noch keine Bestrafungen erhalten.");
            setItem(22, makeItem(Material.BARRIER, emptyName, List.of(emptyLore), true), null);
        }
    }

    private void handlePunishmentClick(InventoryClickEvent e, Punishment pun) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ConfigManager cfg = plugin.getConfigManager();
        if (p.hasPermission(DELETE_PERM) && e.isShiftClick()) {
            boolean ok = plugin.getPunishmentManager().deletePunishment(pun.getId(), p.getName());
            if (ok) {
                MessageUtils.sendMessage(p, cfg.getPrefix() + MessageUtils.applyPercentPlaceholders(
                        cfg.getMessage("punishment-deleted",
                                "<green>Strafe #%id% (%type%) wurde gelöscht!"),
                        "id", String.valueOf(pun.getId()),
                        "type", pun.getType().name()));
            } else {
                MessageUtils.sendMessage(p, cfg.getPrefix() + MessageUtils.applyPercentPlaceholders(
                        cfg.getMessage("punishment-delete-failed",
                                "<red>Strafe #%id% konnte nicht gelöscht werden."),
                        "id", String.valueOf(pun.getId())));
            }
            if (p != null) refresh(p);
            return;
        }
        if (p.hasPermission(DELETE_PERM)) {
            MessageUtils.sendMessage(p, MessageUtils.applyPercentPlaceholders(
                    cfg.getMessage("vanish-staff-note",
                            "<gray><i>Shift+Klick: Strafe #%id% löschen"),
                    "id", String.valueOf(pun.getId())));
        } else {
            MessageUtils.sendMessage(p, cfg.getPrefix() + cfg.getMessage("punishment-no-delete-perm",
                    "<red>Keine Rechte zum Löschen von Strafen."));
        }
    }

    private org.bukkit.inventory.ItemStack makePunishmentItem(Punishment pun, int globalIdx, boolean canDelete) {
        ConfigManager cfg = plugin.getConfigManager();
        Material m = cfg.getGuiHistoryTypeMaterial(pun.getType().name().toLowerCase());
        if (m == null || m.isAir()) {
            m = switch (pun.getType()) {
                case WARN -> Material.YELLOW_BANNER;
                case KICK -> Material.IRON_BOOTS;
                case MUTE, TEMPMUTE -> Material.MUSIC_DISC_CHIRP;
                case BAN, TEMPBAN -> Material.IRON_DOOR;
                case UNBAN, UNMUTE -> Material.LIME_DYE;
            };
        }
        boolean active = pun.isActive();
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.applyPercentPlaceholders(
                cfg.getGuiHistoryLore("id", "<gray>#%id% <dark_gray>- <white>%type%"),
                "id", String.valueOf(pun.getId()),
                "type", pun.getType().name()));
        lore.add(cfg.getGuiHistoryStatus(active));
        lore.add("");
        lore.add(MessageUtils.applyPercentPlaceholders(
                cfg.getGuiHistoryLore("reason", "<gray>Grund: <white>%reason%"),
                "reason", pun.getReason()));
        lore.add(MessageUtils.applyPercentPlaceholders(
                cfg.getGuiHistoryLore("operator", "<gray>Von: <yellow>%name%"),
                "name", pun.getOperatorName()));
        lore.add(MessageUtils.applyPercentPlaceholders(
                cfg.getGuiHistoryLore("date", "<gray>Datum: <white>%date%"),
                "date", format(pun.getStart())));
        if (pun.getType() != Punishment.Type.KICK && pun.getType() != Punishment.Type.WARN) {
            String endVal = (pun.getEndAt() == Long.MAX_VALUE || pun.getEndAt() == -1)
                    ? cfg.getGuiHistoryLore("end-permanent", "<red>PERMANENT")
                    : format(pun.getEndAt());
            lore.add(MessageUtils.applyPercentPlaceholders(
                    cfg.getGuiHistoryLore("end", "<gray>Bis: %value%"),
                    "value", endVal));
        }
        if (canDelete) {
            lore.addAll(cfg.getGuiHistoryDeleteHint());
        }
        return makeItem(m,
                "<" + (active ? "red" : "green") + ">" + pun.getType().name() + " <gray>#" + pun.getId(),
                lore, active);
    }

    private String format(long time) {
        if (time <= 0) return "-";
        if (time == Long.MAX_VALUE) return "∞";
        return java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(time),
                java.time.ZoneId.systemDefault()).format(
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
