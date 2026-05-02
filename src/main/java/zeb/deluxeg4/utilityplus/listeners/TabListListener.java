package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.managers.TabListManager;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class TabListListener implements Listener {

    private final Plugin plugin;
    private final TabListManager tabListManager;

    public TabListListener(Plugin plugin, TabListManager tabListManager) {
        this.plugin = plugin;
        this.tabListManager = tabListManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PaperFoliaTasks.runForPlayerDelayed(plugin, event.getPlayer(), task -> {
            if (event.getPlayer().isOnline()) {
                tabListManager.update(event.getPlayer());
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tabListManager.onPlayerQuit(event.getPlayer());
    }
}
