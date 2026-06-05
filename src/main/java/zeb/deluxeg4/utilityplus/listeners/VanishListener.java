package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.commands.VanishCommand;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class VanishListener implements Listener {

    private final UtilityPlus plugin;
    private final VanishCommand vanishCommand;

    public VanishListener(UtilityPlus plugin, VanishCommand vanishCommand) {
        this.plugin = plugin;
        this.vanishCommand = vanishCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        refreshVanishFor(joiningPlayer, true);

        // Re-send hide packets after the join spawn packets have settled.
        PaperFoliaTasks.runForPlayerDelayed(plugin, joiningPlayer, task -> refreshVanishFor(joiningPlayer, false), 2L);
        PaperFoliaTasks.runForPlayerDelayed(plugin, joiningPlayer, task -> refreshVanishFor(joiningPlayer, false), 20L);
    }

    private void refreshVanishFor(Player joiningPlayer, boolean notify) {
        vanishCommand.hideVanishedPlayersFrom(joiningPlayer);

        if (vanishCommand.isVanished(joiningPlayer.getUniqueId())) {
            vanishCommand.applyPersistentVanish(joiningPlayer);
            if (notify) {
                Messages.send(joiningPlayer, "&7You are still vanished.");
            }
        }
    }
}
