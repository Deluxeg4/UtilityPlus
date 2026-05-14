package zeb.deluxeg4.utilityplus.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

public class AnnouncementManager {

    private final UtilityPlus plugin;
    private ScheduledTask task;
    private boolean showing = false;
    private long lastToggleTime;

    public AnnouncementManager(UtilityPlus plugin) {
        this.plugin = plugin;
        start();
    }

    public void start() {
        stop();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("announcement.action-bar");
        if (config == null || !config.getBoolean("enabled", false)) {
            return;
        }

        String text = ChatColor.translateAlternateColorCodes('&', config.getString("text", ""));
        long showDurationTicks = config.getLong("show-duration", 15) * 20L;
        long hideDurationTicks = config.getLong("hide-duration", 300) * 20L;

        lastToggleTime = System.currentTimeMillis();
        showing = true;

        task = PaperFoliaTasks.runGlobalTimer(plugin, (t) -> {
            long now = System.currentTimeMillis();
            long elapsedTicks = (now - lastToggleTime) / 50;

            if (showing) {
                if (elapsedTicks >= showDurationTicks) {
                    showing = false;
                    lastToggleTime = now;
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(text);
                    }
                }
            } else {
                if (elapsedTicks >= hideDurationTicks) {
                    showing = true;
                    lastToggleTime = now;
                }
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        start();
    }
}
