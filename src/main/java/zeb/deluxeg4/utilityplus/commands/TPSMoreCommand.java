package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.managers.CpuMonitor;
import zeb.deluxeg4.utilityplus.managers.TickMonitor;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TPSMoreCommand implements CommandExecutor {

    private static final int TEN_SECOND_TICKS = 200;
    private static final int ONE_MINUTE_TICKS = 1_200;
    private static final int MAX_REGION_SAMPLE_SIZE = 512;
    private static final double MIN_TPS = 0.0D;
    private static final double MAX_TPS = 20.0D;

    private final boolean folia;
    private final UtilityPlus plugin;
    private final TickMonitor tickMonitor;
    private final CpuMonitor cpuMonitor;

    public TPSMoreCommand(final TickMonitor tickMonitor, final CpuMonitor cpuMonitor) {
        this.plugin = JavaPlugin.getPlugin(UtilityPlus.class);
        this.tickMonitor = tickMonitor;
        this.cpuMonitor = cpuMonitor;
        this.folia = PaperFoliaTasks.isFolia();
    }

    /** Shows detailed server performance information. */
    @Override
    public boolean onCommand(
            final CommandSender sender,
            final Command command,
            final String label,
            final String[] args
    ) {
        if (!sender.hasPermission("utilityplus.tpsmore")) {
            Messages.send(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        final List<WorldSnapshot> worldSnapshots = snapshotWorlds();
        final int totalPlayers = Bukkit.getOnlinePlayers().size();
        final int maxPlayers = Bukkit.getMaxPlayers();

        PaperFoliaTasks.runGlobal(plugin, () -> sendReport(sender, worldSnapshots, totalPlayers, maxPlayers));
        return true;
    }

    private List<WorldSnapshot> snapshotWorlds() {
        final List<WorldSnapshot> snapshots = new ArrayList<>();
        for (final World world : Bukkit.getWorlds()) {
            final Chunk[] chunks = world.getLoadedChunks();
            snapshots.add(new WorldSnapshot(world, chunks, world.getEntityCount(), world.getPlayers().size()));
        }
        return snapshots;
    }

    private void sendReport(
            final CommandSender sender,
            final List<WorldSnapshot> worldSnapshots,
            final int totalPlayers,
            final int maxPlayers
    ) {
        final Consumer<String> output = message ->
                PaperFoliaTasks.runForSender(plugin, sender, () -> Messages.send(sender, message));

        output.accept("&8&l--- &b&lTPS More Info &8&l---");
        sendServerImplementation(output);
        sendWorldBreakdown(output, worldSnapshots);
        sendTickDurations(output);
        sendSystemInfo(output);
        output.accept("&6Summary:");
        output.accept(" &7Total Players: &e" + totalPlayers + "&7 / &e" + maxPlayers);
        output.accept(" &7Total Resources: &e" + totalEntities(worldSnapshots)
                + " &7Entities | &e" + totalChunks(worldSnapshots) + " &7Chunks");
        output.accept("&8&l-------------------------");
    }

    private void sendServerImplementation(final Consumer<String> output) {
        final String implementationName = Bukkit.getServer().getName();
        final String implementationVersion = Bukkit.getServer().getVersion();

        if (folia) {
            output.accept("&7Server Implementation: &eFolia (Regionalized) &7- " + implementationVersion);
            return;
        }

        final double[] tps = Bukkit.getTPS();
        output.accept("&7Server Implementation: &e" + implementationName + " &7- " + implementationVersion);
        output.accept("&7Global TPS (1m, 5m, 15m): "
                + formatTps(tps[0]) + "&7, "
                + formatTps(tps[1]) + "&7, "
                + formatTps(tps[2]));
    }

    private void sendWorldBreakdown(final Consumer<String> output, final List<WorldSnapshot> worldSnapshots) {
        output.accept("&6Worlds Info:");
        for (final WorldSnapshot snapshot : worldSnapshots) {
            output.accept(" &b&n" + snapshot.world.getName());
            if (folia) {
                sendFoliaRegionStats(output, snapshot);
            }
            output.accept("  &7Entities: &e" + snapshot.entityCount
                    + " &7| Chunks: &e" + snapshot.chunks.length
                    + " &7| Players: &e" + snapshot.playerCount);
        }
    }

    private void sendFoliaRegionStats(final Consumer<String> output, final WorldSnapshot snapshot) {
        final List<Double> worldTps = getRegionTpsForWorld(snapshot.world, snapshot.chunks);
        if (worldTps.isEmpty()) {
            output.accept("  &7Regional TPS: &cNo active regions");
            return;
        }

        Collections.sort(worldTps);
        final double lowest = worldTps.get(0);
        final double median = worldTps.get(worldTps.size() / 2);
        final double highest = worldTps.get(worldTps.size() - 1);

        output.accept("  &7Regional TPS (low/med/high): "
                + formatTps(lowest) + " &7/ "
                + formatTps(median) + " &7/ "
                + formatTps(highest));
        output.accept("  &7Active Regions: &e" + worldTps.size());
    }

    private void sendTickDurations(final Consumer<String> output) {
        final TickMonitor.Stats tenSecondStats = tickMonitor.getStats(TEN_SECOND_TICKS);
        final TickMonitor.Stats oneMinuteStats = tickMonitor.getStats(ONE_MINUTE_TICKS);

        output.accept("&6Tick Durations (min/med/95%ile/max ms)");
        output.accept(" &7last 10s: " + formatStats(tenSecondStats));
        output.accept(" &7last 1m:  " + formatStats(oneMinuteStats));
    }

    private void sendSystemInfo(final Consumer<String> output) {
        final CpuMonitor.CpuStats tenSecondCpu = cpuMonitor.getStats(10);
        final CpuMonitor.CpuStats oneMinuteCpu = cpuMonitor.getStats(60);
        final CpuMonitor.CpuStats fifteenMinuteCpu = cpuMonitor.getStats(900);
        final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        final long usedMemoryMb = heapUsage.getUsed() / 1024L / 1024L;
        final long maxMemoryMb = heapUsage.getMax() / 1024L / 1024L;

        output.accept("&6System Info:");
        output.accept(" &7Uptime: &e" + formatUptime());
        output.accept(" &7CPU Usage (10s, 1m, 15m):");
        output.accept("  &7System:  &e" + formatCpu(tenSecondCpu.system())
                + "%&7, &e" + formatCpu(oneMinuteCpu.system())
                + "%&7, &e" + formatCpu(fifteenMinuteCpu.system()) + "%");
        output.accept("  &7Process: &e" + formatCpu(tenSecondCpu.process())
                + "%&7, &e" + formatCpu(oneMinuteCpu.process())
                + "%&7, &e" + formatCpu(fifteenMinuteCpu.process()) + "%");
        output.accept(" &7Disk Usage: " + getDiskUsage());
        output.accept(" &7Memory: &e" + usedMemoryMb + " MB &7/ &e" + maxMemoryMb + " MB");
    }

    private List<Double> getRegionTpsForWorld(final World world, final Chunk[] chunks) {
        final List<Double> tpsValues = new ArrayList<>();
        if (addRegionTpsFromApi(world, chunks, tpsValues)) {
            return tpsValues;
        }

        try {
            final Method getHandle = world.getClass().getMethod("getHandle");
            final Object worldServer = getHandle.invoke(world);
            final Field regioniserField = worldServer.getClass().getField("regioniser");
            final Object regioniser = regioniserField.get(worldServer);
            final Method computeMethod = regioniser.getClass()
                    .getDeclaredMethod("computeForAllRegionsUnsynchronised", Consumer.class);

            computeMethod.invoke(regioniser, (Consumer<Object>) region -> addRegionTpsViaReflection(tpsValues, region));
        } catch (final Exception exception) {
            plugin.getLogger().log(Level.FINE, "Failed to access regioniser via reflection", exception);
        }

        return tpsValues;
    }

    private void addRegionTpsViaReflection(final List<Double> tpsValues, final Object region) {
        try {
            final Method getTickData = region.getClass().getMethod("getTickData");
            final Object tickData = getTickData.invoke(region);
            final Method getTps = tickData.getClass().getMethod("getTPS");
            final double[] tpsArray = (double[]) getTps.invoke(tickData);
            if (tpsArray != null && tpsArray.length > 0) {
                tpsValues.add(tpsArray[0]);
            }
        } catch (final Exception exception) {
            plugin.getLogger().log(Level.FINE, "Failed to read region tick data via reflection", exception);
        }
    }

    private boolean addRegionTpsFromApi(
            final World world,
            final Chunk[] chunks,
            final List<Double> tpsValues
    ) {
        if (chunks.length == 0) {
            return true;
        }

        try {
            final Method getRegionTps = Bukkit.getServer().getClass()
                    .getMethod("getRegionTPS", World.class, int.class, int.class);
            final Set<String> seenRegions = new HashSet<>();
            final int step = Math.max(1, chunks.length / MAX_REGION_SAMPLE_SIZE);

            for (int index = 0; index < chunks.length; index += step) {
                final Chunk chunk = chunks[index];
                final String regionKey = (chunk.getX() >> 3) + "," + (chunk.getZ() >> 3);
                if (!seenRegions.add(regionKey)) {
                    continue;
                }

                final double[] tps = (double[]) getRegionTps.invoke(
                        Bukkit.getServer(),
                        world,
                        chunk.getX(),
                        chunk.getZ()
                );
                if (tps != null && tps.length > 0) {
                    tpsValues.add(tps[0]);
                }
            }
            return true;
        } catch (final Exception exception) {
            plugin.getLogger().log(Level.FINE, "getRegionTPS API not available, falling back to reflection", exception);
            return false;
        }
    }

    private int totalEntities(final List<WorldSnapshot> snapshots) {
        int total = 0;
        for (final WorldSnapshot snapshot : snapshots) {
            total += snapshot.entityCount;
        }
        return total;
    }

    private int totalChunks(final List<WorldSnapshot> snapshots) {
        int total = 0;
        for (final WorldSnapshot snapshot : snapshots) {
            total += snapshot.chunks.length;
        }
        return total;
    }

    private String formatStats(final TickMonitor.Stats stats) {
        return String.format(
                Locale.US,
                "%s &7/ %s &7/ %s &7/ %s",
                formatMspt(stats.min()),
                formatMspt(stats.median()),
                formatMspt(stats.p95()),
                formatMspt(stats.max())
        );
    }

    private String formatCpu(final double cpu) {
        if (cpu < 0.0D) {
            return "&cN/A";
        }
        return String.format(Locale.US, "%.1f", cpu);
    }

    private String getDiskUsage() {
        final File root = new File(".").getAbsoluteFile();
        final long total = root.getTotalSpace();
        final long free = root.getFreeSpace();
        if (total == 0L) {
            return "&cN/A";
        }

        final long used = total - free;
        final double percent = (used * 100.0D) / total;
        return String.format(
                Locale.US,
                "&e%d GB &7/ &e%d GB &7(%.1f%%)",
                used / 1024L / 1024L / 1024L,
                total / 1024L / 1024L / 1024L,
                percent
        );
    }

    private String formatUptime() {
        final long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        final long days = totalSeconds / 86_400L;
        final long hours = (totalSeconds % 86_400L) / 3_600L;
        final long minutes = (totalSeconds % 3_600L) / 60L;
        final long seconds = totalSeconds % 60L;
        final StringBuilder uptime = new StringBuilder();

        if (days > 0L) {
            uptime.append(days).append("d ");
        }
        if (hours > 0L) {
            uptime.append(hours).append("h ");
        }
        if (minutes > 0L) {
            uptime.append(minutes).append("m ");
        }
        uptime.append(seconds).append("s");
        return uptime.toString();
    }

    private String formatTps(final double tps) {
        final double clamped = Math.max(MIN_TPS, Math.min(MAX_TPS, tps));
        final String color = clamped >= 18.0D ? "&a" : clamped >= 15.0D ? "&e" : "&c";
        return color + String.format(Locale.US, "%.2f", clamped);
    }

    private String formatMspt(final double mspt) {
        final String color = mspt <= 40.0D ? "&a" : mspt <= 50.0D ? "&e" : "&c";
        return color + String.format(Locale.US, "%.1f", mspt);
    }

    private record WorldSnapshot(World world, Chunk[] chunks, int entityCount, int playerCount) {
    }
}
