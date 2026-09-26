package zeb.deluxeg4.utilityplus.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Location;
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
    private boolean coordinatesEnabled;
    private String coordinateFormat;

    public AnnouncementManager(UtilityPlus plugin) {
        this.plugin = plugin;
        start();
    }

    public void start() {
        stop();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("announcement.action-bar");
        List<Component> texts = new ArrayList<>();
        if (config != null && config.getBoolean("enabled", false)) {
            List<String> configuredTexts = config.getStringList("text");
            if (configuredTexts.isEmpty()) {
                String text = config.getString("text", "");
                if (!text.isEmpty()) {
                    configuredTexts = List.of(text);
                }
            }
            for (String configuredText : configuredTexts) {
                texts.add(Messages.legacy(configuredText));
            }
        }

        coordinatesEnabled = plugin.getConfig().getBoolean("bedrock-coordinates.enabled", true);
        coordinateFormat = plugin.getConfig().getString(
                "bedrock-coordinates.format",
                "&6Pos: ({x}, {y}, {z})"
        );
        if (texts.isEmpty() && !coordinatesEnabled) {
            return;
        }

        long showDurationTicks = config == null ? 15 * 20L : config.getLong("show-duration", 15) * 20L;
        long hideDurationTicks = config == null ? 300 * 20L : config.getLong("hide-duration", 300) * 20L;

        lastToggleTime = System.currentTimeMillis();
        showing = !texts.isEmpty();
        final int[] textIndex = {0};

        task = PaperFoliaTasks.runGlobalTimer(plugin, (t) -> {
            long now = System.currentTimeMillis();
            long elapsedTicks = (now - lastToggleTime) / 50;

            if (!texts.isEmpty() && showing) {
                if (elapsedTicks >= showDurationTicks) {
                    showing = false;
                    lastToggleTime = now;
                }
            } else if (!texts.isEmpty()) {
                if (elapsedTicks >= hideDurationTicks) {
                    showing = true;
                    textIndex[0] = (textIndex[0] + 1) % texts.size();
                    lastToggleTime = now;
                }
            }

            Component announcement = showing && !texts.isEmpty() ? texts.get(textIndex[0]) : null;
            for (Player player : Bukkit.getOnlinePlayers()) {
                PaperFoliaTasks.runForPlayer(plugin, player, () -> sendActionBar(player, announcement));
            }
        }, 2L, 2L);
    }

    private void sendActionBar(Player player, Component announcement) {
        if (!player.isOnline()) {
            return;
        }

        Component coordinates = null;
        if (coordinatesEnabled && player.getName().startsWith(".")) {
            Location location = player.getLocation();
            coordinates = Messages.legacy(coordinateFormat
                    .replace("{x}", String.valueOf(location.getBlockX()))
                    .replace("{y}", String.valueOf(location.getBlockY()))
                    .replace("{z}", String.valueOf(location.getBlockZ())));
        }

        if (announcement != null && coordinates != null) {
            player.sendActionBar(announcement.append(Component.text("  ")).append(coordinates));
        } else if (coordinates != null) {
            player.sendActionBar(coordinates);
        } else if (announcement != null) {
            player.sendActionBar(announcement);
        }
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
