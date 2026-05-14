package zeb.deluxeg4.utilityplus.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaPlatformTasks implements PlatformTasks {
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
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }

    @Override
    public ScheduledTask runGlobalDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task, delayTicks);
    }

    @Override
    public void runGlobal(Plugin plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public boolean runForPlayer(Plugin plugin, Player player, Runnable task) {
        return player.getScheduler().execute(plugin, task, null, 1L);
    }

    @Override
    public ScheduledTask runForPlayerDelayed(Plugin plugin, Player player, Consumer<ScheduledTask> task, long delayTicks) {
        return player.getScheduler().runDelayed(plugin, task, null, delayTicks);
    }

    @Override
    public ScheduledTask runForPlayerTimer(Plugin plugin, Player player, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return player.getScheduler().runAtFixedRate(plugin, task, null, initialDelayTicks, periodTicks);
    }

    @Override
    public void runAtLocation(Plugin plugin, World world, int blockX, int blockZ, Runnable task) {
        Bukkit.getRegionScheduler().execute(plugin, world, blockX >> 4, blockZ >> 4, task);
    }

    @Override
    public void teleport(Plugin plugin, Player player, Location destination, Consumer<Boolean> after) {
        player.teleportAsync(destination).thenAccept(success -> {
            if (after != null) {
                runForPlayer(plugin, player, () -> after.accept(success));
            }
        });
    }

    private long ticksToMillis(long ticks) {
        return Math.max(1L, ticks) * 50L;
    }
}
