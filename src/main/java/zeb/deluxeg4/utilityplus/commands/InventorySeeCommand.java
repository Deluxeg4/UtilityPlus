package zeb.deluxeg4.utilityplus.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.invsee.InventorySeeMode;

public final class InventorySeeCommand implements CommandExecutor {

    private final UtilityPlus plugin;

    public InventorySeeCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        InventorySeeMode mode = label.equalsIgnoreCase("invsee") ? InventorySeeMode.INVENTORY : InventorySeeMode.ENDER_CHEST;
        String permission = mode == InventorySeeMode.INVENTORY ? "utilityplus.invsee" : "utilityplus.enderchestsee";
        if (!player.hasPermission(permission)) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /" + label + " <player>");
            return true;
        }

        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[0]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot open your own inventory.");
            return true;
        }

        if (!target.isOnline() && !target.hasPlayedBefore() && !player.hasPermission("utilityplus.invsee.unseen")) {
            player.sendMessage("§cThat player has never joined this server.");
            return true;
        }

        if (mode == InventorySeeMode.INVENTORY) {
            plugin.getInventorySeeSessionManager().open(target, player);
        } else {
            plugin.getEnderChestSeeSessionManager().open(target, player);
        }
        return true;
    }
}
