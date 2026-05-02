package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TabListManager {

    private final UtilityPlus plugin;
    private ScheduledTask updateTask;
    private boolean enabled;
    private long updateIntervalTicks;
    private List<String> headerLines;
    private List<String> footerLines;

    // Cache reflection Method — สร้างครั้งเดียว
    private Method worldTpsMethod;
    private boolean worldTpsMethodChecked = false;

    // Cache last sent header/footer per player — skip packet ถ้าไม่มีอะไรเปลี่ยน
    private final Map<UUID, String[]> lastSent = new HashMap<>();

    public TabListManager(UtilityPlus plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("tab-list.enabled", true);
        int updateIntervalSeconds = Math.max(1, plugin.getConfig().getInt("tab-list.update-interval", 5));
        this.updateIntervalTicks = updateIntervalSeconds * 20L;
        this.headerLines = plugin.getConfig().getStringList("tab-list.header");
        this.footerLines = plugin.getConfig().getStringList("tab-list.footer");

        // Reset reflection cache เมื่อ reload
        worldTpsMethod = null;
        worldTpsMethodChecked = false;
        lastSent.clear();

        stop();
        if (enabled) {
            start();
            updateAll();
        } else {
            clearAll();
        }
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void update(Player player) {
        if (!enabled) {
            clear(player);
            return;
        }

        // คำนวณ shared values ก่อน แล้วส่ง player เข้าไป
        double tps = getAverageWorldTps();
        String uptime = formatUptime();
        updatePlayerTabList(player, tps, uptime);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void start() {
        updateTask = PaperFoliaTasks.runGlobalTimer(plugin, task -> updateAll(), updateIntervalTicks, updateIntervalTicks);
    }

    private void updateAll() {
        // คำนวณ shared values ครั้งเดียวต่อ tick — ไม่ใช่ต่อ player
        double tps = getAverageWorldTps();
        String uptime = formatUptime();
        String onlineCount = String.valueOf(Bukkit.getOnlinePlayers().size());

        for (Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer(plugin, player, () ->
                    updatePlayerTabList(player, tps, uptime, onlineCount));
        }
    }

    private void updatePlayerTabList(Player player, double tps, String uptime) {
        updatePlayerTabList(player, tps, uptime, String.valueOf(Bukkit.getOnlinePlayers().size()));
    }

    private void updatePlayerTabList(Player player, double tps, String uptime, String onlineCount) {
        String header = formatLines(headerLines, player, tps, uptime, onlineCount);
        String footer = formatLines(footerLines, player, tps, uptime, onlineCount);

        // Skip packet ถ้า header/footer เหมือนเดิมทุกประการ
        String[] prev = lastSent.get(player.getUniqueId());
        if (prev != null && prev[0].equals(header) && prev[1].equals(footer)) {
            return;
        }

        player.setPlayerListHeaderFooter(header, footer);
        lastSent.put(player.getUniqueId(), new String[]{header, footer});
    }

    private void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer(plugin, player, () -> clear(player));
        }
    }

    private void clear(Player player) {
        player.setPlayerListHeaderFooter("", "");
        lastSent.remove(player.getUniqueId());
    }

    /** เรียกเมื่อ player ออกจากเซิร์ฟเวอร์ เพื่อป้องกัน memory leak */
    public void onPlayerQuit(Player player) {
        lastSent.remove(player.getUniqueId());
    }

    // ─── Formatting ───────────────────────────────────────────────────────────

    private String formatLines(List<String> lines, Player player,
                               double tps, String uptime, String onlineCount) {
        return color(String.join("\n", lines)
                .replace("%server_tps_1_colored%", formatTps(tps))
                .replace("%server_online%", onlineCount)
                .replace("%player_ping%", String.valueOf(player.getPing()))
                .replace("%server_uptime%", uptime));
    }

    // ─── TPS ──────────────────────────────────────────────────────────────────

    private double getAverageWorldTps() {
        String[] worldNames = {"world", "world_nether", "world_the_end"};
        double total = 0.0D;
        int count = 0;

        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Double tps = getWorldTps(world);
            if (tps != null) {
                total += tps;
                count++;
            }
        }

        return count > 0 ? total / count : Bukkit.getTPS()[0];
    }

    private Double getWorldTps(World world) {
        // Lazy-init + cache Method — ไม่ใช้ reflection ซ้ำทุก call
        if (!worldTpsMethodChecked) {
            worldTpsMethodChecked = true;
            try {
                worldTpsMethod = Bukkit.getServer().getClass()
                        .getMethod("getTPS", Location.class);
            } catch (NoSuchMethodException ignored) {
                // Fork นี้ไม่รองรับ per-location TPS
            }
        }

        if (worldTpsMethod == null) return null;

        try {
            double[] tps = (double[]) worldTpsMethod.invoke(
                    Bukkit.getServer(), world.getSpawnLocation());
            if (tps != null && tps.length > 0) {
                return tps[0];
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {}

        return null;
    }

    private String formatTps(double tps) {
        String color = tps > 18.0D ? "&a" : tps > 16.0D ? "&e" : "&c";
        return color + String.format("%.2f", Math.min(tps, 20.0D));
    }

    // ─── Uptime ───────────────────────────────────────────────────────────────

    private String formatUptime() {
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(
                ManagementFactory.getRuntimeMXBean().getUptime());
        long days    = totalSeconds / 86400L;
        long hours   = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L)  / 60L;
        long seconds = totalSeconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    // ─── Color ────────────────────────────────────────────────────────────────

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}