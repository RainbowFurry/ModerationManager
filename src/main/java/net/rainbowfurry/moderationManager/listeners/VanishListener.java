package net.rainbowfurry.moderationManager.listeners;

import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class VanishListener implements Listener {

    private final ModerationManager plugin;

    public VanishListener(ModerationManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfigManager().isVanishEnabled()) return;
        if (!plugin.getConfigManager().isVanishGodMode()) return;
        if (!(event.getEntity() instanceof Player p)) return;
        if (plugin.getStaffManager().isVanished(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfigManager().isVanishEnabled()) return;
        if (!plugin.getConfigManager().isVanishNoDrops()) return;
        if (plugin.getStaffManager().isVanished(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfigManager().isVanishEnabled()) return;
        if (!plugin.getConfigManager().isVanishNoDrops()) return;
        if (!(event.getEntity() instanceof Player p)) return;
        if (plugin.getStaffManager().isVanished(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
