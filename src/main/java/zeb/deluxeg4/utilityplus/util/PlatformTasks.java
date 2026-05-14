package zeb.deluxeg4.utilityplus.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

interface PlatformTasks {
    ScheduledTask runAsync(Plugin plugin, Consumer<ScheduledTask> task);

    ScheduledTask runAsyncTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks);

    ScheduledTask runGlobalTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks);

    ScheduledTask runGlobalDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks);

    void runGlobal(Plugin plugin, Runnable task);

    boolean runForPlayer(Plugin plugin, Player player, Runnable task);

    ScheduledTask runForPlayerDelayed(Plugin plugin, Player player, Consumer<ScheduledTask> task, long delayTicks);

    ScheduledTask runForPlayerTimer(Plugin plugin, Player player, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks);

    void runAtLocation(Plugin plugin, World world, int blockX, int blockZ, Runnable task);

    void teleport(Plugin plugin, Player player, Location destination, Consumer<Boolean> after);
}
