package zeb.deluxeg4.utilityplus.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PerformanceBarManager implements Listener {
    public enum RamUnit {
        GB,
        MB
    }

    private enum BarType {
        TPS,
        RAM
    }

    private static final int MSPT_SAMPLE_TICKS = 20;

    private final UtilityPlus plugin;
    private final TickMonitor tickMonitor;
    private final Map<BarKey, BarSession> sessions = new ConcurrentHashMap<>();
    private ScheduledTask updateTask;

    public PerformanceBarManager(UtilityPlus plugin, TickMonitor tickMonitor) {
        this.plugin = plugin;
        this.tickMonitor = tickMonitor;
    }

    public boolean toggleTpsBar(Player player) {
        BarKey key = new BarKey(player.getUniqueId(), BarType.TPS);
        BarSession active = sessions.remove(key);
        if (active != null) {
            active.bar.removeAll();
            stopTaskIfIdle();
            return false;
        }

        BossBar bar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(player);
        BarSession session = new BarSession(player.getUniqueId(), BarType.TPS, RamUnit.GB, bar);
        sessions.put(key, session);
        ensureTask();
        updatePlayer(session);
        return true;
    }

    public boolean toggleRamBar(Player player, RamUnit unit) {
        BarKey key = new BarKey(player.getUniqueId(), BarType.RAM);
        BarSession active = sessions.remove(key);
        if (active != null) {
            active.bar.removeAll();
            stopTaskIfIdle();
            return false;
        }

        BossBar bar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(player);
        BarSession session = new BarSession(player.getUniqueId(), BarType.RAM, unit, bar);
        sessions.put(key, session);
        ensureTask();
        updatePlayer(session);
        return true;
    }

    public void close(UUID uuid) {
        for (BarType type : BarType.values()) {
            BarSession session = sessions.remove(new BarKey(uuid, type));
            if (session != null) {
                session.bar.removeAll();
            }
        }
        stopTaskIfIdle();
    }

    public void closeAll() {
        sessions.values().forEach(session -> session.bar.removeAll());
        sessions.clear();
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        close(event.getPlayer().getUniqueId());
    }

    private void ensureTask() {
        if (updateTask != null) {
            return;
        }
        updateTask = PaperFoliaTasks.runGlobalTimer(plugin, task -> updateAll(), 20L, 20L);
    }

    private void stopTaskIfIdle() {
        if (!sessions.isEmpty() || updateTask == null) {
            return;
        }
        updateTask.cancel();
        updateTask = null;
    }

    private void updateAll() {
        for (Map.Entry<BarKey, BarSession> entry : sessions.entrySet()) {
            BarSession session = entry.getValue();
            Player player = Bukkit.getPlayer(session.playerId);
            if (player == null || !player.isOnline()) {
                sessions.remove(entry.getKey());
                session.bar.removeAll();
                continue;
            }
            PaperFoliaTasks.runForPlayer(plugin, player, () -> updatePlayer(session));
        }
        stopTaskIfIdle();
    }

    private void updatePlayer(BarSession session) {
        if (session.type == BarType.TPS) {
            updateTpsBar(session.bar);
            return;
        }
        updateRamBar(session.bar, session.ramUnit);
    }

    private void updateTpsBar(BossBar bar) {
        double tps = currentTps();
        double mspt = tickMonitor.getStats(MSPT_SAMPLE_TICKS).median();

        bar.setTitle(ChatColor.GREEN + "TPS " + formatTwo(tps)
                + ChatColor.GRAY + " MSPT " + formatTwo(mspt));
        bar.setProgress(clamp(tps / 20.0D));
        bar.setColor(tps >= 18.0D ? BarColor.GREEN : tps >= 15.0D ? BarColor.YELLOW : BarColor.RED);
    }

    private void updateRamBar(BossBar bar, RamUnit unit) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        long used = heap.getUsed();
        long max = heap.getMax() > 0 ? heap.getMax() : Runtime.getRuntime().maxMemory();
        double percent = max <= 0L ? 0.0D : (used * 100.0D) / max;

        bar.setTitle(ChatColor.AQUA + "RAM "
                + formatMemory(used, unit) + "/" + formatMemory(max, unit) + " " + unit.name()
                + ChatColor.GRAY + " " + formatOne(percent) + "%");
        bar.setProgress(clamp(percent / 100.0D));
        bar.setColor(percent < 70.0D ? BarColor.GREEN : percent < 90.0D ? BarColor.YELLOW : BarColor.RED);
    }

    private double currentTps() {
        try {
            double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0) {
                return Math.min(20.0D, Math.max(0.0D, tps[0]));
            }
        } catch (RuntimeException ignored) {
        }
        return 20.0D;
    }

    private String formatMemory(long bytes, RamUnit unit) {
        if (unit == RamUnit.MB) {
            return String.valueOf(bytes / 1024L / 1024L);
        }
        return formatTwo(bytes / 1024.0D / 1024.0D / 1024.0D);
    }

    private String formatOne(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private String formatTwo(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private record BarKey(UUID playerId, BarType type) {
    }

    private static final class BarSession {
        private final UUID playerId;
        private final BarType type;
        private final RamUnit ramUnit;
        private final BossBar bar;

        private BarSession(UUID playerId, BarType type, RamUnit ramUnit, BossBar bar) {
            this.playerId = playerId;
            this.type = type;
            this.ramUnit = ramUnit;
            this.bar = bar;
        }
    }
}
