package zeb.deluxeg4.utilityplus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public class UptimeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("utilityplus.uptime")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        sender.sendMessage("§3Server uptime: §e" + formatUptime());
        return true;
    }

    private String formatUptime() {
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(
                ManagementFactory.getRuntimeMXBean().getUptime());

        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder uptime = new StringBuilder();
        if (days > 0) uptime.append(days).append("d ");
        if (hours > 0) uptime.append(hours).append("h ");
        if (minutes > 0) uptime.append(minutes).append("m ");
        uptime.append(seconds).append("s");
        return uptime.toString();
    }
}
