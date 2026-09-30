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
            Messages.send(sender, "<red>You don't have permission!");
            return true;
        }

        Messages.send(sender, "<yellow>Reloading UtilityPlus...");

        plugin.getSpawnManager().saveData();

        plugin.reloadConfig();
        Messages.copyMessageDefaults();

        plugin.getSpawnManager().reload();
        plugin.getChatManager().reload();
        plugin.getDeathMessageManager().reload();
        plugin.getTabListManager().reload();
        plugin.getAnnouncementManager().reload();

        Messages.send(sender, "<green><bold>UtilityPlus reloaded!");
        Messages.send(sender, "<gray>config.yml <green>OK  <gray>spawn <green>OK  <gray>announcement <green>OK");
        return true;
    }
}
