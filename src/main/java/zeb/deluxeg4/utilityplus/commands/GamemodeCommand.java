package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GamemodeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player target;
        GameMode mode;

        // Determine gamemode from command label
        switch (label.toLowerCase()) {
            case "gmc":
                mode = GameMode.CREATIVE;
                break;
            case "gms":
                mode = GameMode.SURVIVAL;
                break;
            case "gmsp":
                mode = GameMode.SPECTATOR;
                break;
            case "gma":
                mode = GameMode.ADVENTURE;
                break;
            default:
                sender.sendMessage("§cUnknown gamemode command.");
                return true;
        }

        // Determine target player
        if (args.length >= 1) {
            // Target specified
            target = sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
        } else {
            // Self target
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player. Usage: /" + label + " <player>");
                return true;
            }
            target = (Player) sender;
        }

        String modeName = mode.name().toLowerCase();
        String displayMode = modeName.substring(0, 1).toUpperCase() + modeName.substring(1);
        UtilityPlus plugin = JavaPlugin.getPlugin(UtilityPlus.class);

        if (target == sender) {
            target.setGameMode(mode);
            target.sendMessage("§aYour gamemode has been changed to §e" + displayMode + "§a!");
        } else {
            String senderName = sender.getName();
            PaperFoliaTasks.runForPlayer(plugin, target, () -> {
                target.setGameMode(mode);
                target.sendMessage("§aYour gamemode has been changed to §e" + displayMode + " §aby §e" + senderName + "§a!");
                PaperFoliaTasks.runForSender(plugin, sender, () ->
                        sender.sendMessage("§aChanged §e" + target.getName() + "§a's gamemode to §e" + displayMode + "§a!"));
            });
        }

        return true;
    }
}
