package zeb.deluxeg4.utilityplus.listeners;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerQuitEvent;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

import java.util.Map;

public final class InventorySeeListener implements Listener {
    private final UtilityPlus plugin;

    public InventorySeeListener(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.getInventorySeeSessionManager().isReadOnlySession(player, event.getView().getTopInventory())
                || plugin.getEnderChestSeeSessionManager().isReadOnlySession(player, event.getView().getTopInventory())) {
            event.setCancelled(true);
            return;
        }
        boolean invsee = plugin.getInventorySeeSessionManager().isSessionInventory(player, event.getView().getTopInventory());
        boolean enderSee = plugin.getEnderChestSeeSessionManager().isSessionInventory(player, event.getView().getTopInventory());
        if (invsee || enderSee) {
            if (isPlaceholder(event.getCursor())) {
                event.setCancelled(true);
                player.setItemOnCursor(new ItemStack(Material.AIR));
                return;
            }
            if (event.getClickedInventory() == event.getView().getTopInventory() && invsee) {
                ItemStack incoming = incomingItem(event, player);
                if (!plugin.getInventorySeeSessionManager().canPlaceInSlot(player, event.getRawSlot(), incoming)) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (event.getClickedInventory() == event.getView().getTopInventory() && isPlaceholder(event.getCurrentItem())) {
                event.setCancelled(true);
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR && !cursor.isEmpty()) {
                    if (invsee && !plugin.getInventorySeeSessionManager().canPlaceInSlot(player, event.getRawSlot(), cursor)) {
                        return;
                    }
                    event.setCurrentItem(cursor.clone());
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    syncNextTick(player);
                }
                return;
            }
        }
        if ((invsee || enderSee) && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory()
                && plugin.getInventorySeeSessionManager().isLockedSlot(player, event.getRawSlot())) {
            event.setCancelled(true);
            return;
        }
        if (invsee) {
            syncNextTick(player);
            return;
        }
        if (enderSee) {
            syncNextTick(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.getInventorySeeSessionManager().isReadOnlySession(player, event.getView().getTopInventory())
                || plugin.getEnderChestSeeSessionManager().isReadOnlySession(player, event.getView().getTopInventory())) {
            event.setCancelled(true);
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (plugin.getInventorySeeSessionManager().isLockedSlot(player, rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
        boolean invsee = plugin.getInventorySeeSessionManager().isSessionInventory(player, event.getView().getTopInventory());
        if (invsee) {
            for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
                if (!plugin.getInventorySeeSessionManager().canPlaceInSlot(player, entry.getKey(), entry.getValue())) {
                    event.setCancelled(true);
                    return;
                }
            }
            syncNextTick(player);
            return;
        }
        if (plugin.getEnderChestSeeSessionManager().isSessionInventory(player, event.getView().getTopInventory())) {
            syncNextTick(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            plugin.getInventorySeeSessionManager().markDirtyIfOffline(player, event.getInventory());
            plugin.getEnderChestSeeSessionManager().markDirtyIfOffline(player, event.getInventory());
            plugin.getInventorySeeSessionManager().applyViewerChanges(player);
            plugin.getEnderChestSeeSessionManager().applyViewerChanges(player);
            plugin.getInventorySeeSessionManager().close(player.getUniqueId());
            plugin.getEnderChestSeeSessionManager().close(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getInventorySeeSessionManager().close(event.getPlayer().getUniqueId());
        plugin.getEnderChestSeeSessionManager().close(event.getPlayer().getUniqueId());
    }

    private void syncNextTick(Player player) {
        PaperFoliaTasks.runForPlayerDelayed(plugin, player, task -> {
            plugin.getInventorySeeSessionManager().applyViewerChanges(player);
            plugin.getEnderChestSeeSessionManager().applyViewerChanges(player);
            if (plugin.getInventorySeeSessionManager().isPlaceholderItem(player.getItemOnCursor())
                    || plugin.getEnderChestSeeSessionManager().isPlaceholderItem(player.getItemOnCursor())) {
                player.setItemOnCursor(new org.bukkit.inventory.ItemStack(Material.AIR));
            }
        }, 1L);
    }

    private boolean isPlaceholder(ItemStack item) {
        return plugin.getInventorySeeSessionManager().isPlaceholderItem(item)
                || plugin.getEnderChestSeeSessionManager().isPlaceholderItem(item);
    }

    private ItemStack incomingItem(InventoryClickEvent event, Player player) {
        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton < 9) {
                return player.getInventory().getItem(hotbarButton);
            }
            return null;
        }
        return event.getCursor();
    }
}
