package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class BroadcastCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public BroadcastCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.broadcast")) {
            Messages.send(sender, "<red>You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            Messages.send(sender, "<red>Usage: /" + label + " <message>");
            return true;
        }

        String prefix = plugin.getConfig().getString("broadcast.prefix", "<gold><bold>[BROADCAST]<reset> ");
        String message = String.join(" ", args);

        PaperFoliaTasks.broadcast(plugin, Messages.parse(prefix + message));

        return true;
    }
}
