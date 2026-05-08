package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillCommand implements CommandExecutor {

    public static final String SELF_KILL_METADATA = "utilityplus-self-kill";

    private final UtilityPlus plugin;
    private final Set<UUID> pendingConfirmation = ConcurrentHashMap.newKeySet();

    public KillCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("utilityplus.kill")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (pendingConfirmation.remove(uuid)) {
            player.setMetadata(SELF_KILL_METADATA, new FixedMetadataValue(plugin, true));
            player.setHealth(0);
            return true;
        }

        pendingConfirmation.add(uuid);
        player.sendMessage("§eType /kill again to confirm.");
        PaperFoliaTasks.runForPlayerDelayed(plugin, player, task -> pendingConfirmation.remove(uuid), 200L);
        return true;
    }
}
