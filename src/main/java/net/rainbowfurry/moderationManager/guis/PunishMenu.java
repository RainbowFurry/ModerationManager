package net.rainbowfurry.moderationManager.guis;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.managers.ConfigManager;
import net.rainbowfurry.moderationManager.models.Punishment;
import net.rainbowfurry.moderationManager.utils.DurationUtils;
import net.rainbowfurry.moderationManager.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PunishMenu extends BaseMenu {

    private final UUID targetUUID;
    private final String targetName;
    private final boolean showBackToInfo;

    private Punishment.Type selectedType = null;
    private Long selectedDurationMillis = null;
    private String selectedReason = null;

    public PunishMenu(ModerationManager plugin, UUID targetUUID, String targetName, boolean backToInfo) {
        super(plugin,
                plugin.getConfigManager().getGuiTitle("punish-menu",
                        "<gradient:#f44336:#ff9800><bold>⚔️ Strafen: %name%</bold></gradient>")
                        .replace("%name%", escape(targetName)),
                plugin.getConfigManager().getGuiRows("punish-menu", 6));
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.showBackToInfo = backToInfo;
    }

    private static String escape(String s) {
        if (s == null) return "Unbekannt";
        return s.replace("<", "＜").replace(">", "＞");
    }

    @Override
    protected void build() {
        createInventory();
        fillBorder();

        ConfigManager cfg = plugin.getConfigManager();

        setItem(10, typeButton(Punishment.Type.WARN, "warn"),
                e -> selectType(Punishment.Type.WARN, e.getWhoClicked()));
        setItem(12, typeButton(Punishment.Type.KICK, "kick"),
                e -> selectType(Punishment.Type.KICK, e.getWhoClicked()));
        setItem(14, typeButton(Punishment.Type.MUTE, "mute"),
                e -> selectType(Punishment.Type.MUTE, e.getWhoClicked()));
        setItem(16, typeButton(Punishment.Type.BAN, "ban"),
                e -> selectType(Punishment.Type.BAN, e.getWhoClicked()));

        List<ConfigManager.DurationPreset> durations = cfg.getPunishmentPresetDurations();
        List<String> reasons = cfg.getPunishmentPresetReasons();

        int slot = 19;
        for (int i = 0; i < durations.size(); i++) {
            while ((slot + 1) % 9 == 0 || slot % 9 == 0) slot++;
            if (slot > 34) break;
            ConfigManager.DurationPreset dp = durations.get(i);
            final long durationMillis = dp.durationMs == -1 ? Long.MAX_VALUE : dp.durationMs;
            final String durLabel = dp.label;
            final Material mat = dp.material;
            final boolean isDurSelected = Objects.equals(selectedDurationMillis, durationMillis);
            final int fslot = slot;
            String name = cfg.getGuiPunishDurPrefix(isDurSelected)
                    + MessageUtils.applyPercentPlaceholders(durLabel, "label", durLabel);
            List<String> lore = List.of(MessageUtils.applyPercentPlaceholders(
                    cfg.getGuiPunishDurLore(isDurSelected), "label", durLabel));
            setItem(fslot, makeItem(mat, name, lore, isDurSelected),
                    e -> selectDuration(durationMillis, durLabel, e.getWhoClicked()));
            slot++;
        }

        int rslot = 29;
        int placed = 0;
        for (int i = 0; i < reasons.size() && placed < 8; i++) {
            while ((rslot + 1) % 9 == 0 || rslot % 9 == 0) rslot++;
            if (rslot > 43) break;
            final String reason = reasons.get(i);
            final boolean sel = reason.equals(selectedReason);
            List<String> lore = new ArrayList<>();
            for (String line : cfg.getGuiPunishReasonLore(sel)) {
                lore.add(MessageUtils.applyPercentPlaceholders(line, "reason", reason));
            }
            String name = cfg.getGuiPunishReasonPrefix(sel) + truncate(reason, 28);
            setItem(rslot, makeItem(cfg.getGuiPunishReasonMaterial(sel), name, lore, sel),
                    e -> selectReason(reason, e.getWhoClicked()));
            rslot++;
            placed++;
        }

        List<String> bookLore = new ArrayList<>();
        bookLore.add(MessageUtils.applyPercentPlaceholders(cfg.guiItemLine("status-book", "lore-line-type", "<gray>Typ: %type%"),
                "type", displayType(selectedType)));
        bookLore.add(MessageUtils.applyPercentPlaceholders(cfg.guiItemLine("status-book", "lore-line-duration", "<gray>Dauer: %duration%"),
                "duration", displayDuration()));
        bookLore.add(MessageUtils.applyPercentPlaceholders(cfg.guiItemLine("status-book", "lore-line-reason", "<gray>Grund: %reason%"),
                "reason", displayReason(selectedReason)));
        bookLore.add("");
        bookLore.add(MessageUtils.applyPercentPlaceholders(cfg.guiItemLine("status-book", "lore-line-target", "<gray>Ziel: <white>%target%"),
                "target", targetName));
        String bookName = cfg.guiItemName("status-book", "<gold>📝 Aktuelle Auswahl");
        setItem(38, makeItem(ConfigManager.safeMaterial(cfg.guiItemMaterial("status-book"), Material.WRITABLE_BOOK),
                bookName, bookLore, false), null);

        boolean canApply = selectedType != null && selectedReason != null
                && (selectedType == Punishment.Type.WARN
                    || selectedType == Punishment.Type.KICK
                    || selectedDurationMillis != null);

        if (canApply) {
            Material m = ConfigManager.safeMaterial(cfg.guiItemMaterial("apply-enabled"), Material.EMERALD_BLOCK);
            String title = cfg.guiItemName("apply-enabled", "<green><bold>✔ BESTRAFUNG DURCHFÜHREN");
            List<String> applyLore = new ArrayList<>();
            for (String l : cfg.guiApplyEnabledLore()) {
                applyLore.add(MessageUtils.applyPercentPlaceholders(l,
                        "target", targetName,
                        "type", displayType(selectedType),
                        "duration", displayDuration(),
                        "reason", displayReason(selectedReason)));
            }
            setItem(40, makeItem(m, title, applyLore, true),
                    e -> applyPunishment(e.getWhoClicked()));
        } else {
            Material m = ConfigManager.safeMaterial(cfg.guiItemMaterial("apply-disabled"), Material.REDSTONE_BLOCK);
            String title = cfg.guiItemName("apply-disabled", "<red><bold>✘ Bitte erst alles auswählen");
            List<String> lore = cfg.guiItemLore("apply-disabled");
            if (lore == null || lore.isEmpty()) {
                lore = List.of(
                        "<dark_red>Bitte wähle:",
                        "<dark_red>1. Straf-Typ (oben: Zeile 1)",
                        "<dark_red>2. Dauer (Zeile 2-3: bei Mute/Ban)",
                        "<dark_red>3. Grund (Zeile 4-5: Mitte)");
            }
            setItem(40, makeItem(m, title, lore, false), null);
        }

        Material resetMat = ConfigManager.safeMaterial(cfg.guiItemMaterial("reset-selection"), Material.RED_CONCRETE);
        String resetName = cfg.guiItemName("reset-selection", "<reset><red>❌ Auswahl zurücksetzen");
        List<String> resetLore = cfg.guiItemLore("reset-selection");
        setItem(42, makeItem(resetMat, resetName, resetLore, false),
                e -> {
                    selectedType = null; selectedDurationMillis = null; selectedReason = null;
                    if (e.getWhoClicked() instanceof Player p) refresh(p);
                });

        Material backMat = ConfigManager.safeMaterial(cfg.guiItemMaterial("back-arrow"), Material.ARROW);
        String backName = cfg.guiItemName("back-arrow", "<gold>← Zurück");
        List<String> backLore = cfg.guiItemLore("back-arrow");
        setItem(45, makeItem(backMat, backName, backLore, false), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                if (showBackToInfo) {
                    new PlayerInfoMenu(plugin, targetUUID, targetName, false).open(p);
                } else {
                    new PlayerSelectorMenu(plugin).open(p);
                }
            }
        });

        Material closeMat = ConfigManager.safeMaterial(cfg.guiItemMaterial("close-barrier"), Material.BARRIER);
        String closeName = cfg.guiItemName("close-barrier", "<dark_red>❌ Schließen");
        List<String> closeLore = cfg.guiItemLore("close-barrier");
        setItem(53, makeItem(closeMat, closeName, closeLore, false),
                e -> e.getWhoClicked().closeInventory());
    }

    private ItemStack typeButton(Punishment.Type t, String cfgKey) {
        ConfigManager cfg = plugin.getConfigManager();
        boolean sel = t == selectedType;
        List<String> lore = new ArrayList<>();
        lore.add(sel ? "<green>✔ Ausgewählt" : "<yellow>Klicken zum Auswählen");
        lore.addAll(cfg.getGuiPunishTypeLore(cfgKey));
        String prefix = sel ? "<green>✔ " : "";
        return makeItem(cfg.getGuiPunishTypeMaterial(cfgKey),
                prefix + cfg.getGuiPunishTypeName(cfgKey, t.name()),
                lore, sel);
    }

    private void selectType(Punishment.Type t, org.bukkit.inventory.InventoryHolder who) {
        this.selectedType = t;
        if (t == Punishment.Type.WARN) {
            this.selectedDurationMillis = Long.MAX_VALUE;
        } else if (t == Punishment.Type.KICK) {
            this.selectedDurationMillis = 0L;
        }
        if (who instanceof Player p) refresh(p);
    }

    private void selectDuration(long durationMillis, String label, org.bukkit.inventory.InventoryHolder who) {
        this.selectedDurationMillis = durationMillis;
        if (who instanceof Player p) refresh(p);
    }

    private void selectReason(String reason, org.bukkit.inventory.InventoryHolder who) {
        this.selectedReason = reason;
        if (who instanceof Player p) refresh(p);
    }

    private String displayType(Punishment.Type t) {
        if (t == null) return "<red>(nicht ausgewählt)";
        return switch (t) {
            case WARN -> "<yellow>⚠️ Warnung";
            case KICK -> "<white>🥾 Kick";
            case MUTE, TEMPMUTE -> "<aqua>🔇 Mute";
            case BAN, TEMPBAN ->  "<red>🚫 Ban";
            case UNBAN -> "<green>🔓 Unban";
            case UNMUTE -> "<green>🔊 Unmute";
        };
    }

    private String displayDuration() {
        if (selectedType == null) return "<red>(nicht ausgewählt)";
        if (selectedType == Punishment.Type.KICK) return "<white>(Einmalig)";
        if (selectedType == Punishment.Type.WARN) return "<white>(Verwarnung)";
        if (selectedDurationMillis == null) return "<red>(bitte Dauer auswählen)";
        if (selectedDurationMillis == Long.MAX_VALUE) return "<red>Permanent";
        return "<yellow>" + DurationUtils.formatDuration(selectedDurationMillis);
    }

    private String displayReason(String r) {
        if (r == null) return "<red>(nicht ausgewählt)";
        return "<white>" + r;
    }

    private void applyPunishment(org.bukkit.inventory.InventoryHolder who) {
        if (!(who instanceof Player p)) return;
        if (selectedType == null || selectedReason == null) return;
        ConfigManager cfg = plugin.getConfigManager();

        final long now = System.currentTimeMillis();
        final long endAt;
        final Punishment.Type actualType;

        if (selectedType == Punishment.Type.KICK) {
            Player target = org.bukkit.Bukkit.getPlayer(targetUUID);
            if (target == null) {
                MessageUtils.sendMessage(p, cfg.getMessage("kick-offline-error",
                        "<red>Kick nur bei Online-Spielern möglich! Spieler ist offline."));
                return;
            }
            plugin.getPunishmentManager().executeKick(target, selectedReason, p.getName());
            p.closeInventory();
            MessageUtils.sendMessage(p, cfg.getPrefix() + MessageUtils.applyPercentPlaceholders(
                    cfg.getMessage("kick-applied", "<green>Kick durchgeführt: <white>%target%"),
                    "target", targetName));
            return;
        }

        switch (selectedType) {
            case WARN:
                endAt = 0L;
                actualType = Punishment.Type.WARN;
                break;
            case MUTE:
                if (selectedDurationMillis == null) return;
                if (selectedDurationMillis == Long.MAX_VALUE) {
                    endAt = -1L;
                    actualType = Punishment.Type.MUTE;
                } else {
                    endAt = now + selectedDurationMillis;
                    actualType = Punishment.Type.TEMPMUTE;
                }
                break;
            case BAN:
                if (selectedDurationMillis == null) return;
                if (selectedDurationMillis == Long.MAX_VALUE) {
                    endAt = -1L;
                    actualType = Punishment.Type.BAN;
                } else {
                    endAt = now + selectedDurationMillis;
                    actualType = Punishment.Type.TEMPBAN;
                }
                break;
            default:
                endAt = 0L;
                actualType = selectedType;
        }

        boolean active = switch (actualType) {
            case WARN, MUTE, TEMPMUTE, BAN, TEMPBAN -> true;
            case KICK, UNBAN, UNMUTE -> false;
        };

        Punishment pun = new Punishment(0, targetUUID, targetName, actualType,
                0L, selectedReason, p.getName(), now, endAt, active);
        plugin.getPunishmentManager().addPunishment(pun);

        p.closeInventory();
        MessageUtils.sendMessage(p, cfg.getPrefix() + MessageUtils.applyPercentPlaceholders(
                cfg.getMessage("punishment-applied",
                        "<green>Bestrafung angewendet: <white>%type% <gray>gegen <white>%target%"),
                "type", actualType.name(),
                "target", targetName));
    }

    private String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }
}
