package net.rainbowfurry.moderationManager.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;

public abstract class BaseMenu {

    protected final ModerationManager plugin;
    protected final MiniMessage mm = MiniMessage.miniMessage();
    protected final String title;
    protected final int size;
    protected Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();

    public BaseMenu(ModerationManager plugin, String titleMini, int rows) {
        this.plugin = plugin;
        this.title = titleMini;
        this.size = rows * 9;
    }

    protected abstract void build();

    public void open(Player player) {
        // Inventory bei dem ersten open() erstellen und registrieren,
        // danach NIE wieder neu erstellen - nur noch clear() + neu befuellen!
        // Sonst verlieren wir die GuiManager Registrierung (Inventory Objekt = Key!)
        boolean firstTime = (inventory == null);
        build();
        if (firstTime) {
            plugin.getGuiManager().register(inventory, this);
        }
        player.openInventory(inventory);
    }

    protected Inventory createInventory() {
        if (this.inventory == null) {
            // 1. Mal: Inventory neu anlegen
            this.inventory = Bukkit.createInventory(null, size, mm.deserialize(title));
        } else {
            // Folgeaufrufe: ALTES Inventory leeren (gleiches Objekt!)
            this.inventory.clear();
        }
        // WICHTIG: Click-Handler MUESSEN bei jedem build() zurueckgesetzt werden,
        // sonst haengen Handler aus vorherigen Builds noch drin!
        clickHandlers.clear();
        return this.inventory;
    }

    // ============ SICHERER REFRESH: Inventory NEU BEFÜLLEN ohne CLOSE-EVENT zu triggern ============
    // (kein p.openInventory() - das wuerde InventoryCloseEvent feuern und wir werden aus
    // dem GuiManager.openMenus ENTFERNT -> danach keine Clicks mehr!)
    protected void refresh(Player player) {
        build();
        if (player != null && inventory != null) {
            // Player.updateInventory() erzwingt Client-Seitige Synchronisation (ohne close/reopen)
            try {
                player.updateInventory();
            } catch (Throwable ignored) {
                // Ignorieren - setItem() hat das sollte bereits ueber setItem Pakete die Items schon uebertragen
            }
        }
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        if (inventory == null) return;
        inventory.setItem(slot, item);
        if (onClick != null) {
            clickHandlers.put(slot, onClick);
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;
        int slot = event.getSlot();
        Consumer<InventoryClickEvent> h = clickHandlers.get(slot);
        if (h != null) {
            try {
                h.accept(event);
            } catch (Exception ex) {
                ex.printStackTrace();
                if (event.getWhoClicked() instanceof Player p) {
                    p.sendMessage(ChatColor.RED + "Fehler bei der Aktion: " + ex.getMessage());
                }
            }
        }
    }

    protected void fillGlass() {
        ItemStack pane = makeItem(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", Collections.emptyList(), false);
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, pane);
            }
        }
    }

    protected void fillBorder() {
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray> ", Collections.emptyList(), false);
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= (size - 9) || i % 9 == 0 || (i + 1) % 9 == 0) {
                if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                    inventory.setItem(i, pane);
                }
            }
        }
    }

    public Inventory getInventory() { return inventory; }

    // ==================== HELPER: Item Stack Builder ====================

    protected ItemStack makeItem(Material material, String nameMini, List<String> loreMini, boolean enchant) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(nameMini));
            if (loreMini != null && !loreMini.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreMini) {
                    lore.add(mm.deserialize(line));
                }
                meta.lore(lore);
            }
            if (enchant) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack skull(String ownerName, String nameMini, List<String> loreMini) {
        ItemStack skull = makeItem(Material.PLAYER_HEAD, nameMini, loreMini, false);
        ItemMeta meta = skull.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
            sm.setOwner(ownerName);
            skull.setItemMeta(sm);
        }
        return skull;
    }

    protected ItemStack skullUUID(java.util.UUID ownerUUID, String ownerName, String nameMini, List<String> loreMini) {
        ItemStack skull = makeItem(Material.PLAYER_HEAD, nameMini, loreMini, false);
        ItemMeta meta = skull.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUUID);
            if (ownerName != null && !ownerName.isEmpty()) {
                // Setze zuerst Name - dann wird der Kopf im Cache gefunden
                sm.setOwner(ownerName);
            } else {
                sm.setOwningPlayer(op);
            }
            skull.setItemMeta(sm);
        }
        return skull;
    }

    protected int paginatedSlot(int pageIndex, int itemsPerPage, int firstSlot) {
        int columnsPerRow = itemsPerPage <= 7 ? itemsPerPage : 7;
        int row = firstSlot / 9;
        int col = firstSlot % 9;
        return (row + (pageIndex / columnsPerRow)) * 9 + (col + (pageIndex % columnsPerRow));
    }
}
