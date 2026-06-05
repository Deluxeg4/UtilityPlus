package zeb.deluxeg4.utilityplus.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import zeb.deluxeg4.utilityplus.util.Messages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("pingall")) {
            return handlePingAll(sender);
        }

        if (args.length == 0) {
            return handleSelfPing(sender);
        }

        return handleTargetPing(sender, args[0]);
    }

    private boolean handleSelfPing(CommandSender sender) {
        if (!sender.hasPermission("utilityplus.ping")) {
            Messages.send(sender, "&cYou don't have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "&cConsole must use /ping <player> or /pingall.");
            return true;
        }
        sender.sendMessage(Component.text()
                .append(prefix())
                .append(Component.space())
                .append(Component.text("Your ping: ", NamedTextColor.GRAY))
                .append(coloredPing(player.getPing()))
                .append(Component.text("ms", NamedTextColor.GRAY))
                .build());
        return true;
    }

    private boolean handleTargetPing(CommandSender sender, String targetName) {
        if (!sender.hasPermission("utilityplus.ping.others")) {
            Messages.send(sender, "&cYou don't have permission to view other players' ping.");
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            Messages.send(sender, "&cPlayer &e" + targetName + "&c is not online.");
            return true;
        }

        sender.sendMessage(Component.text()
                .append(prefix())
                .append(Component.space())
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .append(Component.text("'s ping: ", NamedTextColor.GRAY))
                .append(coloredPing(target.getPing()))
                .append(Component.text("ms", NamedTextColor.GRAY))
                .build());
        return true;
    }

    private boolean handlePingAll(CommandSender sender) {
        if (!sender.hasPermission("utilityplus.ping.others")) {
            Messages.send(sender, "&cYou don't have permission to view all player pings.");
            return true;
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparingInt(Player::getPing));

        if (players.isEmpty()) {
            Messages.send(sender, "&eNo players online.");
            return true;
        }

        int average = (int) Math.round(players.stream().mapToInt(Player::getPing).average().orElse(0));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text()
                .append(prefix())
                .append(Component.space())
                .append(Component.text("Player pings", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(Component.space())
                .append(Component.text("(" + players.size() + " players)", NamedTextColor.DARK_GRAY))
                .build());
        sender.sendMessage(line());

        for (Player player : players) {
            sender.sendMessage(Component.text()
                    .append(Component.space())
                    .append(Component.text("-", NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(coloredPing(player.getPing()))
                    .append(Component.text("ms", NamedTextColor.GRAY))
                    .build());
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text()
                .append(Component.text("Average ping", NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(coloredPing(average))
                .append(Component.text("ms", NamedTextColor.GRAY))
                .build());
        return true;
    }

    private Component prefix() {
        return Component.text("[UtilityPlus]", NamedTextColor.AQUA);
    }

    private Component line() {
        return Component.text("-------------------------", NamedTextColor.DARK_GRAY);
    }

    private Component coloredPing(int ping) {
        NamedTextColor color = ping < 100
                ? NamedTextColor.GREEN
                : ping < 200 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        return Component.text(ping, color);
    }
}
