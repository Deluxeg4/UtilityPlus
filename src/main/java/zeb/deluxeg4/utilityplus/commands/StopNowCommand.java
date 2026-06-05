package zeb.deluxeg4.utilityplus.commands;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import zeb.deluxeg4.utilityplus.util.Messages;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StopNowCommand implements CommandExecutor {

    private static final int MAX_SECONDS = 3600;
    private static final int[] BROADCAST_SECONDS = {1800, 900, 600, 300, 120, 60, 30, 10};
    private static final DateTimeFormatter SHUTDOWN_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss z");

    private final JavaPlugin plugin;
    private ScheduledTask countdownTask;
    private int secondsLeft;
    private long shutdownAtMillis;
    private String shutdownBy;

    public StopNowCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("server.stop")) {
            Messages.send(sender, "&cYou don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        if (args.length > 1) {
            Messages.send(sender, "&cToo many arguments.");
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("cancel")) {
            cancelCountdown(sender);
            return true;
        }
        if (sub.equals("time") || sub.equals("status")) {
            sendStatus(sender);
            return true;
        }
        if (sub.equals("now")) {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
            shutdownBy = sender.getName();
            shutdownAtMillis = System.currentTimeMillis();
            broadcast("&cServer shutdown started now by &f" + shutdownBy
                    + "&c at &f" + formatShutdownAt() + "&c.");
            shutdownServer();
            return true;
        }

        try {
            startCountdown(parseTime(sub), sender);
        } catch (IllegalArgumentException ex) {
            Messages.send(sender, "&c" + ex.getMessage());
            sendUsage(sender, label);
        }
        return true;
    }

    private void cancelCountdown(CommandSender sender) {
        if (countdownTask == null) {
            Messages.send(sender, "&eThere is no active shutdown countdown.");
            return;
        }

        countdownTask.cancel();
        countdownTask = null;
        secondsLeft = 0;
        broadcast("&aServer shutdown scheduled for &f" + formatShutdownAt()
                + "&a by &f" + shutdownBy
                + "&a has been cancelled by &f" + sender.getName() + "&a.");
        shutdownAtMillis = 0L;
        shutdownBy = null;
    }

    private void sendStatus(CommandSender sender) {
        if (countdownTask == null) {
            Messages.send(sender, "&eThere is no active shutdown countdown.");
            return;
        }

        Messages.send(sender, "&eServer shutting down in &f" + formatTime(secondsLeft)
                + "&e at &f" + formatShutdownAt()
                + "&e. Requested by &f" + shutdownBy + "&e.");
    }

    private void startCountdown(int seconds, CommandSender sender) {
        if (countdownTask != null) {
            countdownTask.cancel();
            broadcast("&eShutdown countdown reset by " + sender.getName() + ".");
        }

        secondsLeft = seconds;
        shutdownBy = sender.getName();
        shutdownAtMillis = System.currentTimeMillis() + seconds * 1000L;
        broadcast("&cServer shutdown countdown started by &f" + shutdownBy
                + "&c. Server will close at &f" + formatShutdownAt() + "&c.");
        broadcastCountdown(secondsLeft);

        countdownTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> {
                    secondsLeft--;

                    if (secondsLeft <= 0) {
                        broadcast("&cServer is closing now. Requested by &f" + shutdownBy + "&c.");
                        task.cancel();
                        countdownTask = null;
                        shutdownServer();
                        return;
                    }

                    if (shouldBroadcast(secondsLeft)) {
                        broadcastCountdown(secondsLeft);
                    }
                },
                20L,
                20L
        );
    }

    private boolean shouldBroadcast(int seconds) {
        if (seconds <= 5) return true;
        for (int broadcastSecond : BROADCAST_SECONDS) {
            if (seconds == broadcastSecond) return true;
        }
        return false;
    }

    private void broadcastCountdown(int seconds) {
        broadcast("&cServer is shutting down in &f" + formatTime(seconds)
                + "&c at &f" + formatShutdownAt()
                + "&c. Requested by &f" + shutdownBy + "&c.");
    }

    private int parseTime(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Time is required.");
        }

        int totalSeconds = 0;
        int currentNumber = 0;
        boolean readingNumber = false;
        boolean hasUnit = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (Character.isDigit(ch)) {
                readingNumber = true;
                currentNumber = currentNumber * 10 + Character.digit(ch, 10);
                if (currentNumber > MAX_SECONDS) {
                    throw new IllegalArgumentException("Maximum allowed time is " + formatTime(MAX_SECONDS) + ".");
                }
                continue;
            }

            if (!readingNumber || currentNumber <= 0) {
                throw new IllegalArgumentException("Invalid time: " + input);
            }

            int multiplier = switch (ch) {
                case 'h' -> 3600;
                case 'm' -> 60;
                case 's' -> 1;
                default -> throw new IllegalArgumentException("Invalid time unit: " + ch);
            };
            hasUnit = true;
            totalSeconds += currentNumber * multiplier;
            currentNumber = 0;
            readingNumber = false;
        }

        if (readingNumber) {
            totalSeconds += currentNumber;
        }
        if (totalSeconds <= 0) {
            throw new IllegalArgumentException("Time must be greater than 0.");
        }
        if (totalSeconds > MAX_SECONDS) {
            throw new IllegalArgumentException("Maximum allowed time is " + formatTime(MAX_SECONDS) + ".");
        }
        if (!hasUnit && input.length() > 4) {
            throw new IllegalArgumentException("Use s, m, or h for long times. Example: 30s, 5m, 1h.");
        }

        return totalSeconds;
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds >= 3600 && totalSeconds % 3600 == 0) {
            return (totalSeconds / 3600) + "h";
        }
        if (totalSeconds >= 60 && totalSeconds % 60 == 0) {
            return (totalSeconds / 60) + "m";
        }
        if (totalSeconds >= 3600) {
            int h = totalSeconds / 3600;
            int remainder = totalSeconds % 3600;
            int m = remainder / 60;
            int s = remainder % 60;
            if (s == 0) return h + "h " + m + "m";
            if (m == 0) return h + "h " + s + "s";
            return h + "h " + m + "m " + s + "s";
        }
        if (totalSeconds >= 60) {
            int m = totalSeconds / 60;
            int s = totalSeconds % 60;
            return s == 0 ? m + "m" : m + "m " + s + "s";
        }
        return totalSeconds + "s";
    }

    private String formatShutdownAt() {
        long time = shutdownAtMillis > 0L ? shutdownAtMillis : System.currentTimeMillis();
        return SHUTDOWN_TIME_FORMAT.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
    }

    private void broadcast(String message) {
        plugin.getServer().broadcast(Messages.legacy(message));
    }

    private void shutdownServer() {
        plugin.getServer().shutdown();
    }

    private void sendUsage(CommandSender sender, String label) {
        Messages.send(sender, "&cUsage:");
        Messages.send(sender, "&c  /" + label + " <time>   &7Start countdown. Examples: 30s, 5m, 1h, 1h30m");
        Messages.send(sender, "&c  /" + label + " now      &7Shutdown immediately");
        Messages.send(sender, "&c  /" + label + " cancel   &7Cancel active countdown");
        Messages.send(sender, "&c  /" + label + " time     &7Show time remaining");
    }
}
