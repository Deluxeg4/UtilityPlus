package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
            Messages.send(sender, "<red>You don't have permission to use this command.");
            return true;
        }

        sender.sendMessage(gradient("Server uptime: " + formatUptime()));
        return true;
    }

    private Component gradient(String text) {
        final int startRed = 0x6E;
        final int startGreen = 0x9A;
        final int startBlue = 0xC7;
        final int endRed = 0x72;
        final int endGreen = 0xF1;
        final int endBlue = 0xCC;
        final int[] characters = text.codePoints().toArray();
        final Component result = Component.empty();

        Component gradient = result;
        for (int index = 0; index < characters.length; index++) {
            final double progress = characters.length <= 1 ? 0.0D : (double) index / (characters.length - 1);
            final int red = (int) Math.round(startRed + (endRed - startRed) * progress);
            final int green = (int) Math.round(startGreen + (endGreen - startGreen) * progress);
            final int blue = (int) Math.round(startBlue + (endBlue - startBlue) * progress);
            gradient = gradient.append(Component.text(new String(Character.toChars(characters[index])))
                    .color(TextColor.color(red, green, blue)));
        }
        return gradient;
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
