package net.rainbowfurry.moderationManager.listeners;

import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.guis.BaseMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class MenuInventoryListener implements Listener {

    private final ModerationManager plugin;

    public MenuInventoryListener(ModerationManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null || event.getView().getTopInventory() == null) return;
        Inventory top = event.getView().getTopInventory();
        BaseMenu menu = plugin.getGuiManager().getMenu(top);
        if (menu != null) {
            menu.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null) return;
        plugin.getGuiManager().unregister(inv);
    }
}
