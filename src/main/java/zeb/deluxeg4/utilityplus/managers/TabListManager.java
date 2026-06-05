package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TabListManager {

    private static final double DEFAULT_TPS = 20.0D;
    private static final int MIN_UPDATE_INTERVAL_SECONDS = 1;

    private final UtilityPlus plugin;
    private final Map<UUID, String[]> lastSent = new ConcurrentHashMap<>();

    private ScheduledTask updateTask;
    private boolean enabled;
    private long updateIntervalTicks;
    private List<String> headerLines;
    private List<String> footerLines;
    private double lastKnownTps = DEFAULT_TPS;
    private Method worldTpsMethod;
    private boolean worldTpsMethodChecked;

    public TabListManager(final UtilityPlus plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Reloads tab-list settings and restarts the update task. */
    public void reload() {
        enabled = plugin.getConfig().getBoolean("tab-list.enabled", true);
        final int updateIntervalSeconds = Math.max(
                MIN_UPDATE_INTERVAL_SECONDS,
                plugin.getConfig().getInt("tab-list.update-interval", 5)
        );

        updateIntervalTicks = updateIntervalSeconds * 20L;
        headerLines = plugin.getConfig().getStringList("tab-list.header");
        footerLines = plugin.getConfig().getStringList("tab-list.footer");
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

    /** Stops the tab-list update task. */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    /** Updates one player's tab-list header and footer. */
    public void update(final Player player) {
        if (!player.isOnline()) {
            onPlayerQuit(player);
            return;
        }

        if (!enabled) {
            clear(player);
            return;
        }

        updatePlayerTabList(player, getAverageWorldTps(), formatUptime());
    }

    /** Clears cached tab-list state for a player who left. */
    public void onPlayerQuit(final Player player) {
        lastSent.remove(player.getUniqueId());
    }

    private void start() {
        updateTask = PaperFoliaTasks.runGlobalTimer(
                plugin,
                task -> updateAll(),
                updateIntervalTicks,
                updateIntervalTicks
        );
    }

    private void updateAll() {
        final double tps = getAverageWorldTps();
        final String uptime = formatUptime();
        final String onlineCount = String.valueOf(Bukkit.getOnlinePlayers().size());

        for (final Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer(plugin, player, () -> updateIfOnline(player, tps, uptime, onlineCount));
        }
    }

    private void updateIfOnline(
            final Player player,
            final double tps,
            final String uptime,
            final String onlineCount
    ) {
        if (player.isOnline()) {
            updatePlayerTabList(player, tps, uptime, onlineCount);
            return;
        }
        onPlayerQuit(player);
    }

    private void updatePlayerTabList(final Player player, final double tps, final String uptime) {
        updatePlayerTabList(player, tps, uptime, String.valueOf(Bukkit.getOnlinePlayers().size()));
    }

    private void updatePlayerTabList(
            final Player player,
            final double tps,
            final String uptime,
            final String onlineCount
    ) {
        final String header = formatLines(headerLines, player, tps, uptime, onlineCount);
        final String footer = formatLines(footerLines, player, tps, uptime, onlineCount);
        final String[] previous = lastSent.get(player.getUniqueId());

        if (previous != null && previous[0].equals(header) && previous[1].equals(footer)) {
            return;
        }

        player.sendPlayerListHeaderAndFooter(Messages.legacy(header), Messages.legacy(footer));
        lastSent.put(player.getUniqueId(), new String[] {header, footer});
    }

    private void clearAll() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer(plugin, player, () -> clear(player));
        }
    }

    private void clear(final Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        lastSent.remove(player.getUniqueId());
    }

    private String formatLines(
            final List<String> lines,
            final Player player,
            final double tps,
            final String uptime,
            final String onlineCount
    ) {
        return String.join("\n", lines)
                .replace("%server_tps_1_colored%", formatTps(tps))
                .replace("%server_online%", onlineCount)
                .replace("%player_ping%", String.valueOf(player.getPing()))
                .replace("%server_uptime%", uptime);
    }

    private double getAverageWorldTps() {
        double total = 0.0D;
        int count = 0;

        for (final World world : Bukkit.getWorlds()) {
            final Double tps = getWorldTps(world);
            if (tps != null) {
                total += tps;
                count++;
            }
        }

        if (count > 0) {
            lastKnownTps = total / count;
            return lastKnownTps;
        }
        return getServerTpsOrFallback();
    }

    private Double getWorldTps(final World world) {
        if (!worldTpsMethodChecked) {
            worldTpsMethodChecked = true;
            try {
                worldTpsMethod = Bukkit.getServer().getClass().getMethod("getTPS", Location.class);
            } catch (final NoSuchMethodException ignored) {
                worldTpsMethod = null;
            }
        }

        if (worldTpsMethod == null) {
            return null;
        }

        try {
            final Location spawnLocation = world.getSpawnLocation();
            final double[] tps = (double[]) worldTpsMethod.invoke(Bukkit.getServer(), spawnLocation);
            if (tps != null && tps.length > 0) {
                return tps[0];
            }
        } catch (final ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private double getServerTpsOrFallback() {
        try {
            final double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0) {
                lastKnownTps = tps[0];
            }
        } catch (final UnsupportedOperationException ignored) {
        } catch (final RuntimeException ignored) {
        }
        return lastKnownTps;
    }

    private String formatTps(final double tps) {
        final String color = tps > 18.0D ? "&a" : tps > 16.0D ? "&e" : "&c";
        return color + String.format(Locale.US, "%.2f", Math.min(tps, DEFAULT_TPS));
    }

    private String formatUptime() {
        final long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        final long days = totalSeconds / 86_400L;
        final long hours = (totalSeconds % 86_400L) / 3_600L;
        final long minutes = (totalSeconds % 3_600L) / 60L;
        final long seconds = totalSeconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }
}
