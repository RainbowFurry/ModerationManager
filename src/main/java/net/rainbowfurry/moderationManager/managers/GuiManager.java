package net.rainbowfurry.moderationManager.managers;

import net.rainbowfurry.moderationManager.guis.BaseMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class GuiManager {

    private final Map<Inventory, BaseMenu> openMenus = new HashMap<>();

    public GuiManager() {}

    public void register(Inventory inv, BaseMenu menu) {
        openMenus.put(inv, menu);
    }

    public void unregister(Inventory inv) {
        openMenus.remove(inv);
    }

    public BaseMenu getMenu(Inventory inv) {
        return openMenus.get(inv);
    }

    public void closeAll() {
        for (Inventory inv : openMenus.keySet()) {
            if (inv.getViewers() != null) {
                for (var hv : inv.getViewers()) {
                    if (hv instanceof Player p) {
                        p.closeInventory();
                    }
                }
            }
        }
        openMenus.clear();
    }
}
