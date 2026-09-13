package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class STapwarp implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("utilityplus.summon")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /s <player>");
            return true;
        }

        Player target = player.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot summon yourself.");
            return true;
        }

        UtilityPlus plugin = JavaPlugin.getPlugin(UtilityPlus.class);
        String targetName = target.getName();
        String playerName = player.getName();

        PaperFoliaTasks.teleport(target, player.getLocation(), plugin, success -> {
            if (!success) {
                PaperFoliaTasks.runForSender(plugin, player, () -> player.sendMessage("§cSummon failed."));
                return;
            }
            //target.sendMessage("§aYou have been summoned by §e" + playerName + "§a!");
            PaperFoliaTasks.runForSender(plugin, player, () ->
                    player.sendMessage("§aYou have §e" + targetName + " §ato your location!"));
        });

        return true;
    }
}
