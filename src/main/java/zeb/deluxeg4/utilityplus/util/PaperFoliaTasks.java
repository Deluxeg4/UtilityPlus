package zeb.deluxeg4.utilityplus.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class PaperFoliaTasks {

    private static final boolean FOLIA = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
    private static final PlatformTasks PLATFORM = createPlatform();

    private PaperFoliaTasks() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static ScheduledTask runAsync(Plugin plugin, Consumer<ScheduledTask> task) {
        return PLATFORM.runAsync(plugin, task);
    }

    public static ScheduledTask runAsyncTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return PLATFORM.runAsyncTimer(plugin, task, initialDelayTicks, periodTicks);
    }

    public static ScheduledTask runGlobalTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return PLATFORM.runGlobalTimer(plugin, task, initialDelayTicks, periodTicks);
    }

    public static ScheduledTask runGlobalDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks) {
        return PLATFORM.runGlobalDelayed(plugin, task, delayTicks);
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        PLATFORM.runGlobal(plugin, task);
    }

    public static void runForSender(Plugin plugin, CommandSender sender, Runnable task) {
        if (sender instanceof Player player) {
            runForPlayer(plugin, player, task);
            return;
        }
        runGlobal(plugin, task);
    }

    public static boolean runForPlayer(Plugin plugin, Player player, Runnable task) {
        return PLATFORM.runForPlayer(plugin, player, task);
    }

    public static ScheduledTask runForPlayerDelayed(Plugin plugin, Player player, Consumer<ScheduledTask> task, long delayTicks) {
        return PLATFORM.runForPlayerDelayed(plugin, player, task, delayTicks);
    }

    public static ScheduledTask runForPlayerTimer(
            Plugin plugin,
            Player player,
            Consumer<ScheduledTask> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return PLATFORM.runForPlayerTimer(plugin, player, task, initialDelayTicks, periodTicks);
    }

    public static void runAtLocation(Plugin plugin, World world, int blockX, int blockZ, Runnable task) {
        PLATFORM.runAtLocation(plugin, world, blockX, blockZ, task);
    }

    public static void send(Plugin plugin, Player player, String message) {
        runForPlayer(plugin, player, () -> player.sendMessage(message));
    }

    public static void broadcast(Plugin plugin, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(plugin, player, message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static void teleport(Player player, Location destination, Plugin plugin, Consumer<Boolean> after) {
        PLATFORM.teleport(plugin, player, destination, after);
    }

    private static PlatformTasks createPlatform() {
        String className = FOLIA
                ? "zeb.deluxeg4.utilityplus.util.FoliaPlatformTasks"
                : "zeb.deluxeg4.utilityplus.util.BukkitPlatformTasks";
        try {
            return (PlatformTasks) Class.forName(className).getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Missing platform implementation: " + className, e);
        }
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
