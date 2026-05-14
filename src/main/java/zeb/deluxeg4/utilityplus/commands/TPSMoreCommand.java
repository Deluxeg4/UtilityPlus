package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.managers.CpuMonitor;
import zeb.deluxeg4.utilityplus.managers.TickMonitor;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TPSMoreCommand implements CommandExecutor {

    // Named constants แทนการใช้ magic numbers
    private static final int TICKS_10S = 200;   // 200 ticks = 10 วินาที
    private static final int TICKS_1M  = 1200;  // 1200 ticks = 1 นาที
    private static final double TPS_MIN = 0.0;
    private static final double TPS_MAX = 20.0;

    private final boolean isFolia;
    private final UtilityPlus plugin;
    private final TickMonitor tickMonitor;
    private final CpuMonitor cpuMonitor;

    public TPSMoreCommand(TickMonitor tickMonitor, CpuMonitor cpuMonitor) {
        this.plugin      = JavaPlugin.getPlugin(UtilityPlus.class);
        this.tickMonitor = tickMonitor;
        this.cpuMonitor  = cpuMonitor;
        this.isFolia     = PaperFoliaTasks.isFolia();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.tpsmore")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // ---- ดึงข้อมูลที่ต้องใช้ main/region thread ก่อน ----
        // snapshot ทุก world ทันทีบน calling thread (main thread)
        List<WorldSnapshot> worldSnapshots = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            Chunk[] chunks   = world.getLoadedChunks();   // cache ไว้ ไม่ต้องเรียกซ้ำ
            int entities     = world.getEntityCount();
            int playerCount  = world.getPlayers().size();
            worldSnapshots.add(new WorldSnapshot(world, chunks, entities, playerCount));
        }

        int totalPlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers   = Bukkit.getMaxPlayers();

        // ---- ต่อจากนี้ทำงานบน global/async thread ----
        PaperFoliaTasks.runGlobal(plugin, () -> {
            Consumer<String> out = message ->
                    PaperFoliaTasks.runForSender(plugin, sender, () -> sender.sendMessage(message));

            out.accept("§8§l--- §b§lTPS More Info §8§l---");

            // แสดง Server Implementation จากค่าจริง ไม่ hardcode
            String implName = Bukkit.getServer().getName();
            String implVersion = Bukkit.getServer().getVersion();
            if (isFolia) {
                out.accept("§7Server Implementation: §eFolia (Regionalized) §7- " + implVersion);
            } else {
                out.accept("§7Server Implementation: §e" + implName + " §7- " + implVersion);
                double[] tps = Bukkit.getTPS();
                out.accept("§7Global TPS (1m, 5m, 15m): "
                        + formatTPS(tps[0]) + "§7, "
                        + formatTPS(tps[1]) + "§7, "
                        + formatTPS(tps[2]));
            }

            // ---- Per-World Breakdown ----
            out.accept("§6Worlds Info:");
            int totalEntities = 0;
            int totalChunks   = 0;

            for (WorldSnapshot snap : worldSnapshots) {
                out.accept(" §b§n" + snap.world.getName());

                if (isFolia) {
                    // ส่ง chunks ที่ cache แล้วเข้าไป ไม่ต้องเรียก getLoadedChunks() ซ้ำ
                    List<Double> worldTPS = getRegionTPSForWorld(snap.world, snap.chunks);
                    if (!worldTPS.isEmpty()) {
                        Collections.sort(worldTPS);
                        double lowest  = worldTPS.get(0);
                        double median  = worldTPS.get(worldTPS.size() / 2);
                        double highest = worldTPS.get(worldTPS.size() - 1);
                        out.accept("  §7Regional TPS (low/med/high): "
                                + formatTPS(lowest) + " §7/ "
                                + formatTPS(median) + " §7/ "
                                + formatTPS(highest));
                        out.accept("  §7Active Regions: §e" + worldTPS.size());
                    } else {
                        out.accept("  §7Regional TPS: §cNo active regions");
                    }
                }

                out.accept("  §7Entities: §e" + snap.entityCount
                        + " §7| Chunks: §e" + snap.chunks.length
                        + " §7| Players: §e" + snap.playerCount);

                totalEntities += snap.entityCount;
                totalChunks   += snap.chunks.length;
            }

            // ---- Tick Durations (Spark Style) ----
            TickMonitor.Stats stats10s = tickMonitor.getStats(TICKS_10S);
            TickMonitor.Stats stats1m  = tickMonitor.getStats(TICKS_1M);

            out.accept("§6Tick Durations (min/med/95%ile/max ms)");
            out.accept(" §7last 10s: " + formatStatsSpark(stats10s));
            out.accept(" §7last 1m:  " + formatStatsSpark(stats1m));

            // ---- System Metrics ----
            out.accept("§6System Info:");
            out.accept(" §7Uptime: §e" + formatUptime());

            CpuMonitor.CpuStats cpu10s  = cpuMonitor.getStats(10);
            CpuMonitor.CpuStats cpu1m   = cpuMonitor.getStats(60);
            CpuMonitor.CpuStats cpu15m  = cpuMonitor.getStats(900);

            out.accept(" §7CPU Usage (10s, 1m, 15m):");
            out.accept("  §7System:  §e" + formatCpu(cpu10s.system())
                    + "%§7, §e" + formatCpu(cpu1m.system())
                    + "%§7, §e" + formatCpu(cpu15m.system()) + "%");
            out.accept("  §7Process: §e" + formatCpu(cpu10s.process())
                    + "%§7, §e" + formatCpu(cpu1m.process())
                    + "%§7, §e" + formatCpu(cpu15m.process()) + "%");

            out.accept(" §7Disk Usage: " + getDiskUsage());

            MemoryMXBean memoryBean  = ManagementFactory.getMemoryMXBean();
            MemoryUsage  heapUsage   = memoryBean.getHeapMemoryUsage();
            long usedMB = heapUsage.getUsed()  / 1024 / 1024;
            long maxMB  = heapUsage.getMax()   / 1024 / 1024;
            out.accept(" §7Memory: §e" + usedMB + " MB §7/ §e" + maxMB + " MB");

            // ---- Summary ----
            out.accept("§6Summary:");
            out.accept(" §7Total Players: §e" + totalPlayers + "§7 / §e" + maxPlayers);
            out.accept(" §7Total Resources: §e" + totalEntities
                    + " §7Entities | §e" + totalChunks + " §7Chunks");

            out.accept("§8§l-------------------------");
        });

        return true;
    }

    // ---- Helper: WorldSnapshot ----
    // เก็บข้อมูลที่ต้องดึงจาก main thread ไว้ใน record เดียว
    private static final class WorldSnapshot {
        final World   world;
        final Chunk[] chunks;
        final int     entityCount;
        final int     playerCount;

        WorldSnapshot(World world, Chunk[] chunks, int entityCount, int playerCount) {
            this.world       = world;
            this.chunks      = chunks;
            this.entityCount = entityCount;
            this.playerCount = playerCount;
        }
    }

    // ---- Folia Region TPS ----
    private List<Double> getRegionTPSForWorld(World world, Chunk[] cachedChunks) {
        List<Double> tpsList = new ArrayList<>();

        // ลองใช้ API สาธารณะก่อน
        if (addRegionTPSFromApi(world, cachedChunks, tpsList)) {
            return tpsList;
        }

        // fallback: reflection
        try {
            Method getHandle      = world.getClass().getMethod("getHandle");
            Object worldServer    = getHandle.invoke(world);
            Field  regioniserField = worldServer.getClass().getField("regioniser");
            Object regioniser     = regioniserField.get(worldServer);
            Method computeMethod  = regioniser.getClass()
                    .getDeclaredMethod("computeForAllRegionsUnsynchronised", Consumer.class);
            computeMethod.invoke(regioniser, (Consumer<Object>) region -> {
                try {
                    Method getTickData = region.getClass().getMethod("getTickData");
                    Object tickData    = getTickData.invoke(region);
                    Method getTPS      = tickData.getClass().getMethod("getTPS");
                    double[] tpsArr    = (double[]) getTPS.invoke(tickData);
                    if (tpsArr != null && tpsArr.length > 0) {
                        tpsList.add(tpsArr[0]);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.FINE, "Failed to read region tick data via reflection", e);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Failed to access regioniser via reflection", e);
        }

        return tpsList;
    }

    // รับ cachedChunks เพื่อไม่ต้องเรียก getLoadedChunks() อีกครั้ง
    // ใช้ Set เพื่อ deduplicate region — หลายก้อน chunk อาจอยู่ใน region เดียวกัน
    private boolean addRegionTPSFromApi(World world, Chunk[] cachedChunks, List<Double> tpsList) {
        if (cachedChunks.length == 0) {
            return true;
        }

        try {
            Method getRegionTPS = Bukkit.getServer().getClass()
                    .getMethod("getRegionTPS", World.class, int.class, int.class);

            // ใช้ Set<String> เพื่อกัน region เดิมซ้ำ (key = "regionX,regionZ")
            Set<String> seenRegions = new HashSet<>();
            int step = Math.max(1, cachedChunks.length / 512);

            for (int i = 0; i < cachedChunks.length; i += step) {
                Chunk  chunk     = cachedChunks[i];
                // Folia region granularity คือ 8x8 chunks
                String regionKey = (chunk.getX() >> 3) + "," + (chunk.getZ() >> 3);
                if (!seenRegions.add(regionKey)) {
                    continue; // region นี้เคยนับแล้ว
                }

                double[] tps = (double[]) getRegionTPS.invoke(
                        Bukkit.getServer(), world, chunk.getX(), chunk.getZ());
                if (tps != null && tps.length > 0) {
                    tpsList.add(tps[0]);
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "getRegionTPS API not available, falling back to reflection", e);
            return false;
        }
    }

    // ---- Formatters ----

    private String formatStatsSpark(TickMonitor.Stats s) {
        return String.format("%s §7/ %s §7/ %s §7/ %s",
                formatMSPT(s.min()), formatMSPT(s.median()), formatMSPT(s.p95()), formatMSPT(s.max()));
    }

    private String formatCpu(double cpu) {
        if (cpu < 0) return "§cN/A";
        return String.format("%.1f", cpu);
    }

    private String getDiskUsage() {
        File root  = new File(".").getAbsoluteFile();
        long total = root.getTotalSpace();
        long free  = root.getFreeSpace();
        if (total == 0) return "§cN/A";
        long   used    = total - free;
        double percent = (used * 100.0) / total;
        return String.format("§e%d GB §7/ §e%d GB §7(%.1f%%)",
                used  / 1024 / 1024 / 1024,
                total / 1024 / 1024 / 1024,
                percent);
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
//    private long getFolderSize(File folder) {
//        long size = 0;
//        File[] files = folder.listFiles();
//        if (files == null) return 0;
//        for (File file : files) {
//            if (file.isFile()) {
//                size += file.length();
//            } else if (file.isDirectory()) {
//                size += getFolderSize(file);
//            }
//        }
//        return size;
//    }
    private String formatTPS(double tps) {
        // clamp ทั้ง 2 ด้าน: ไม่ให้ติดลบ และไม่เกิน 20
        double clamped = Math.max(TPS_MIN, Math.min(TPS_MAX, tps));
        String color   = (clamped >= 18.0) ? "§a" : (clamped >= 15.0) ? "§e" : "§c";
        return color + String.format("%.2f", clamped);
    }

    private String formatMSPT(double mspt) {
        String color = (mspt <= 40.0) ? "§a" : (mspt <= 50.0) ? "§e" : "§c";
        return color + String.format("%.1f", mspt);
    }
}
