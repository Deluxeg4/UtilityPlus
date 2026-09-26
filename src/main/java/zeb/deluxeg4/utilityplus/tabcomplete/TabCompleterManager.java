package zeb.deluxeg4.utilityplus.tabcomplete;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class TabCompleterManager implements TabCompleter {

    private static final List<String> CHAT_SUBS = Arrays.asList("on", "off", "pmon", "pmoff");
    private static final List<String> STOPNOW_ARGS = Arrays.asList(
            "10s", "30s", "1m", "5m", "10m", "30m", "1h", "now", "cancel", "time", "status"
    );
    private static final Set<String> PRIVATE_MESSAGE_COMMANDS = Set.of(
            "tell", "t", "msg", "w", "whisper", "pm"
    );

    /** Returns tab-completion suggestions for UtilityPlus commands. */
    @Override
    public List<String> onTabComplete(
            final CommandSender sender,
            final Command command,
            final String alias,
            final String[] args
    ) {
        // Use the registered command name instead of the typed alias. This keeps
        // completion working for aliases such as /t and /endersee as well.
        final String commandName = command.getName().toLowerCase(Locale.ROOT);

        if (commandName.equals("ping")) {
            if (args.length != 1) return Collections.emptyList();
            return onlinePlayers(sender, args[0]);
        }

        if (commandName.equals("pingall")) {
            return Collections.emptyList();
        }

        if (commandName.equals("stopnow")) {
            if (args.length != 1) return Collections.emptyList();
            return filter(STOPNOW_ARGS, args[0]);
        }

        if (commandName.equals("chat")) {
            if (args.length != 1) return Collections.emptyList();
            return filter(CHAT_SUBS, args[0]);
        }

        if (commandName.equals("ignore") || commandName.equals("ignorehard") || commandName.equals("ignoredeathmsgs")) {
            if (args.length != 1) return Collections.emptyList();
            return onlinePlayers(sender, args[0]);
        }

        if (commandName.equals("gmc")
                || commandName.equals("gms")
                || commandName.equals("gmsp")
                || commandName.equals("gma")
                || commandName.equals("invsee")
                || commandName.equals("enderchestsee")
                || commandName.equals("offlinetp")
                || commandName.equals("s")) {
            if (args.length != 1) return Collections.emptyList();
            return commandName.equals("invsee") || commandName.equals("enderchestsee") || commandName.equals("offlinetp")
                    ? knownPlayers(sender, args[0])
                    : onlinePlayers(sender, args[0]);
        }
        if (commandName.equals("ignorelist")
                || commandName.equals("togglechat")
                || commandName.equals("toggleprivatemsgs")
                || commandName.equals("toggledeathmsgs")
                || commandName.equals("toggledeathmsgshard")) {
            return Collections.emptyList();
        }

        if (commandName.equals("r")
                || commandName.equals("reply")
                || commandName.equals("l")
                || commandName.equals("last")
                || commandName.equals("kill")) {
            return Collections.emptyList();
        }
        if (PRIVATE_MESSAGE_COMMANDS.contains(commandName)) {
            if (args.length != 1) return Collections.emptyList();
            return onlinePlayers(sender, args[0]);
        }

        return Collections.emptyList();
    }

    private List<String> onlinePlayers(final CommandSender sender, final String input) {
        final String lowerInput = input.toLowerCase();
        final List<String> names = new ArrayList<>();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && player.equals(sender)) {
                continue;
            }
            if (player.getName().toLowerCase().startsWith(lowerInput)) {
                names.add(player.getName());
            }
        }
        return names;
    }

    private List<String> knownPlayers(final CommandSender sender, final String input) {
        final String lowerInput = input.toLowerCase(Locale.ROOT);
        final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if ((!(sender instanceof Player) || !player.equals(sender))
                    && player.getName().toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                names.add(player.getName());
            }
        }
        for (final OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            final String name = player.getName();
            if (name != null && player.hasPlayedBefore()
                    && (!(sender instanceof Player) || !player.getUniqueId().equals(((Player) sender).getUniqueId()))
                    && name.toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    private List<String> filter(final List<String> options, final String input) {
        final String lowerInput = input.toLowerCase();
        final List<String> result = new ArrayList<>();
        for (final String option : options) {
            if (option.startsWith(lowerInput)) {
                result.add(option);
            }
        }
        return result;
    }
}
