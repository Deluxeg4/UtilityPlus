package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final UtilityPlus plugin;

    public ReloadCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.reload")) {
            Messages.send(sender, "&cYou don't have permission!");
            return true;
        }

        Messages.send(sender, "&eReloading UtilityPlus...");

        plugin.getSpawnManager().saveData();

        plugin.reloadConfig();

        plugin.getSpawnManager().reload();
        plugin.getChatManager().reload();
        plugin.getDeathMessageManager().reload();
        plugin.getTabListManager().reload();
        plugin.getAnnouncementManager().reload();

        Messages.send(sender, "&a&lUtilityPlus reloaded!");
        Messages.send(sender, "&7config.yml &aOK  &7spawn &aOK  &7announcement &aOK");
        return true;
    }
}
