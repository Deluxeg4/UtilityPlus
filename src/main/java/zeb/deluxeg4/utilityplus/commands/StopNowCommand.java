package zeb.deluxeg4.utilityplus.commands;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        if (args.length > 1) {
            sender.sendMessage(ChatColor.RED + "Too many arguments.");
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
            broadcast(ChatColor.RED + "Server shutdown started now by "
                    + ChatColor.WHITE + shutdownBy
                    + ChatColor.RED + " at "
                    + ChatColor.WHITE + formatShutdownAt()
                    + ChatColor.RED + ".");
            shutdownServer();
            return true;
        }

        try {
            startCountdown(parseTime(sub), sender);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
            sendUsage(sender, label);
        }
        return true;
    }

    private void cancelCountdown(CommandSender sender) {
        if (countdownTask == null) {
            sender.sendMessage(ChatColor.YELLOW + "There is no active shutdown countdown.");
            return;
        }

        countdownTask.cancel();
        countdownTask = null;
        secondsLeft = 0;
        broadcast(ChatColor.GREEN + "Server shutdown scheduled for "
                + ChatColor.WHITE + formatShutdownAt()
                + ChatColor.GREEN + " by "
                + ChatColor.WHITE + shutdownBy
                + ChatColor.GREEN + " has been cancelled by "
                + ChatColor.WHITE + sender.getName()
                + ChatColor.GREEN + ".");
        shutdownAtMillis = 0L;
        shutdownBy = null;
    }

    private void sendStatus(CommandSender sender) {
        if (countdownTask == null) {
            sender.sendMessage(ChatColor.YELLOW + "There is no active shutdown countdown.");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Server shutting down in "
                + ChatColor.WHITE + formatTime(secondsLeft)
                + ChatColor.YELLOW + " at "
                + ChatColor.WHITE + formatShutdownAt()
                + ChatColor.YELLOW + ". Requested by "
                + ChatColor.WHITE + shutdownBy
                + ChatColor.YELLOW + ".");
    }

    private void startCountdown(int seconds, CommandSender sender) {
        if (countdownTask != null) {
            countdownTask.cancel();
            broadcast(ChatColor.YELLOW + "Shutdown countdown reset by " + sender.getName() + ".");
        }

        secondsLeft = seconds;
        shutdownBy = sender.getName();
        shutdownAtMillis = System.currentTimeMillis() + seconds * 1000L;
        broadcast(ChatColor.RED + "Server shutdown countdown started by "
                + ChatColor.WHITE + shutdownBy
                + ChatColor.RED + ". Server will close at "
                + ChatColor.WHITE + formatShutdownAt()
                + ChatColor.RED + ".");
        broadcastCountdown(secondsLeft);

        countdownTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> {
                    secondsLeft--;

                    if (secondsLeft <= 0) {
                        broadcast(ChatColor.RED + "Server is closing now. Requested by "
                                + ChatColor.WHITE + shutdownBy
                                + ChatColor.RED + ".");
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
        broadcast(ChatColor.RED + "Server is shutting down in "
                + ChatColor.WHITE + formatTime(seconds)
                + ChatColor.RED + " at "
                + ChatColor.WHITE + formatShutdownAt()
                + ChatColor.RED + ". Requested by "
                + ChatColor.WHITE + shutdownBy
                + ChatColor.RED + ".");
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
        plugin.getServer().broadcastMessage(message);
    }

    private void shutdownServer() {
        plugin.getServer().shutdown();
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.RED + "  /" + label + " <time>   " + ChatColor.GRAY + "Start countdown. Examples: 30s, 5m, 1h, 1h30m");
        sender.sendMessage(ChatColor.RED + "  /" + label + " now      " + ChatColor.GRAY + "Shutdown immediately");
        sender.sendMessage(ChatColor.RED + "  /" + label + " cancel   " + ChatColor.GRAY + "Cancel active countdown");
        sender.sendMessage(ChatColor.RED + "  /" + label + " time     " + ChatColor.GRAY + "Show time remaining");
    }
}
