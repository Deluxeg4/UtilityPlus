package zeb.deluxeg4.utilityplus.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

public final class PendingInventoryOrderListener implements Listener {
    private final UtilityPlus plugin;

    public PendingInventoryOrderListener(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PaperFoliaTasks.runForPlayerDelayed(plugin, player, task -> {
            if (!player.isOnline()) {
                return;
            }
            if (plugin.getPendingInventoryOrderManager().apply(player)) {
                plugin.getLogger().info("[InvSee] Applied pending offline inventory order for " + player.getName());
            }
        }, 1L);
    }
}
