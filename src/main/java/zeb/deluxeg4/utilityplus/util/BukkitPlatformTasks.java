package zeb.deluxeg4.utilityplus.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class BukkitPlatformTasks implements PlatformTasks {
    @Override
    public ScheduledTask runAsync(Plugin plugin, Consumer<ScheduledTask> task) {
        return Bukkit.getAsyncScheduler().runNow(plugin, task);
    }

    @Override
    public ScheduledTask runAsyncTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                task,
                ticksToMillis(initialDelayTicks),
                ticksToMillis(periodTicks),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public ScheduledTask runGlobalTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> task.accept(null),
                initialDelayTicks,
                periodTicks
        );
        return new BukkitScheduledTask(plugin, bukkitTask, true);
    }

    @Override
    public ScheduledTask runGlobalDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> task.accept(null),
                delayTicks
        );
        return new BukkitScheduledTask(plugin, bukkitTask, false);
    }

    @Override
    public void runGlobal(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public boolean runForPlayer(Plugin plugin, Player player, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
        return true;
    }

    @Override
    public ScheduledTask runForPlayerDelayed(Plugin plugin, Player player, Consumer<ScheduledTask> task, long delayTicks) {
        return runGlobalDelayed(plugin, task, delayTicks);
    }

    @Override
    public ScheduledTask runForPlayerTimer(Plugin plugin, Player player, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return runGlobalTimer(plugin, task, initialDelayTicks, periodTicks);
    }

    @Override
    public void runAtLocation(Plugin plugin, World world, int blockX, int blockZ, Runnable task) {
        runGlobal(plugin, task);
    }

    @Override
    public void teleport(Plugin plugin, Player player, Location destination, Consumer<Boolean> after) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = player.teleport(destination);
            if (after != null) {
                after.accept(success);
            }
        });
    }

    private long ticksToMillis(long ticks) {
        return Math.max(1L, ticks) * 50L;
    }

    private static final class BukkitScheduledTask implements ScheduledTask {
        private final Plugin plugin;
        private final BukkitTask task;
        private final boolean repeating;

        private BukkitScheduledTask(Plugin plugin, BukkitTask task, boolean repeating) {
            this.plugin = plugin;
            this.task = task;
            this.repeating = repeating;
        }

        @Override
        public Plugin getOwningPlugin() {
            return plugin;
        }

        @Override
        public boolean isRepeatingTask() {
            return repeating;
        }

        @Override
        public CancelledState cancel() {
            if (task.isCancelled()) {
                return repeating ? CancelledState.NEXT_RUNS_CANCELLED_ALREADY : CancelledState.CANCELLED_ALREADY;
            }
            task.cancel();
            return repeating ? CancelledState.NEXT_RUNS_CANCELLED : CancelledState.CANCELLED_BY_CALLER;
        }

        @Override
        public ExecutionState getExecutionState() {
            return task.isCancelled() ? ExecutionState.CANCELLED : ExecutionState.IDLE;
        }
    }
}
