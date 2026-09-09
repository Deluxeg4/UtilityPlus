package zeb.deluxeg4.utilityplus.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

import java.util.ArrayList;
import java.util.List;

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

        List<String> configuredTexts = config.getStringList("text");
        if (configuredTexts.isEmpty()) {
            String text = config.getString("text", "");
            if (!text.isEmpty()) {
                configuredTexts = List.of(text);
            }
        }
        List<Component> texts = new ArrayList<>();
        for (String configuredText : configuredTexts) {
            texts.add(Messages.legacy(configuredText));
        }
        if (texts.isEmpty()) {
            return;
        }

        long showDurationTicks = config.getLong("show-duration", 15) * 20L;
        long hideDurationTicks = config.getLong("hide-duration", 300) * 20L;

        lastToggleTime = System.currentTimeMillis();
        showing = true;
        final int[] textIndex = {0};

        task = PaperFoliaTasks.runGlobalTimer(plugin, (t) -> {
            long now = System.currentTimeMillis();
            long elapsedTicks = (now - lastToggleTime) / 50;

            if (showing) {
                if (elapsedTicks >= showDurationTicks) {
                    showing = false;
                    lastToggleTime = now;
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(texts.get(textIndex[0]));
                    }
                }
            } else {
                if (elapsedTicks >= hideDurationTicks) {
                    showing = true;
                    textIndex[0] = (textIndex[0] + 1) % texts.size();
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
