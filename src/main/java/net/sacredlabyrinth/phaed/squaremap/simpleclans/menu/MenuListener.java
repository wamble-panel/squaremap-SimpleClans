package net.sacredlabyrinth.phaed.squaremap.simpleclans.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Keeps menu items in the menu and routes clicks to it. */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof IconMenu menu)) {
            return;
        }
        event.setCancelled(true); // includes shift-clicks and hotbar swaps from the player's own inventory
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            menu.click(event.getSlot());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof IconMenu) {
            event.setCancelled(true);
        }
    }

    /** Closes every open menu, e.g. before a reload swaps out the icons it shows. */
    public static void closeAll(@NotNull Plugin plugin) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder(false) instanceof IconMenu) {
                player.closeInventory();
            }
        }
    }
}
