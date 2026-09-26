package zeb.deluxeg4.utilityplus.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import zeb.deluxeg4.utilityplus.util.Messages;
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
        if (args.length > 0) {
            return true;
        }
        sender.sendMessage("");
        Component link = Component.text("2b2t-th.org/commands")
                .color(NamedTextColor.GOLD)
                .clickEvent(ClickEvent.openUrl("https://2b2t-th.org/commands"));

        sender.sendMessage(link);
        return true;
    }
}
