package zeb.deluxeg4.utilityplus.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.invsee.OfflineInventoryLoader;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

import java.io.IOException;

public final class OfflineTpCommand implements CommandExecutor {
    private final UtilityPlus plugin;

    public OfflineTpCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <player>");
            return true;
        }

        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && online == null) {
            player.sendMessage(ChatColor.RED + "That player has never joined this server.");
            return true;
        }

        try {
            Location location = online != null
                    ? online.getLocation()
                    : OfflineInventoryLoader.loadLastLocation(target.getUniqueId());
            if (location == null) {
                player.sendMessage(ChatColor.RED + "Could not find a last location in a loaded world for that player.");
                return true;
            }
            PaperFoliaTasks.teleport(player, location, plugin, success -> {
                if (success) {
                    PaperFoliaTasks.send(plugin, player, ChatColor.GREEN + "Teleported to "
                            + (target.getName() == null ? args[0] : target.getName()) + "'s last location.");
                } else {
                    PaperFoliaTasks.send(plugin, player, ChatColor.RED + "Teleport failed.");
                }
            });
        } catch (IOException exception) {
            plugin.getLogger().warning("[OfflineTP] Could not read playerdata for " + args[0] + ": " + exception.getMessage());
            player.sendMessage(ChatColor.RED + "Could not read that player's last location.");
        }
        return true;
    }
}
