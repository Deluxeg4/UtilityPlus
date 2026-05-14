package zeb.deluxeg4.utilityplus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import zeb.deluxeg4.utilityplus.UtilityPlus;

public class HelpCommand implements CommandExecutor {

    private final UtilityPlus plugin;

    public HelpCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//        if (!sender.hasPermission("utilityplus.helps")) {
//            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
//            return true;
//        }
        if (args.length > 0) {
            return true;
        }
        sender.sendMessage("");
        sender.sendMessage("§62b2t-th.org/commands");
        return true;
    }
}
