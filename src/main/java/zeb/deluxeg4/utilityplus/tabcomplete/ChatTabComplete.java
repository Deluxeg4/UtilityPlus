package zeb.deluxeg4.utilityplus.tabcomplete;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /chat <on|off|pmon|pmoff>
 * /chatsettings — no args
 */
public class ChatTabComplete implements TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("on", "off", "pmon", "pmoff");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (alias.equalsIgnoreCase("chatsettings")) return Collections.emptyList();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(input)) result.add(sub);
            }
            return result;
        }
        return Collections.emptyList();
    }
}
