package zeb.deluxeg4.utilityplus.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class JoinMessageListener implements Listener {

    private final JavaPlugin plugin;

    public JoinMessageListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("join-message.hide-vanilla", true)) {
            event.setJoinMessage(null);
        }

        sendBedrockWarning(player);
        enableBedrockCoordinateHud(player);

        if (!plugin.getConfig().getBoolean("join-message.enabled", true)) {
            return;
        }

        String message = plugin.getConfig().getString("join-message.message", "&3{player} joined the game");

        if (plugin.getConfig().getBoolean("join-message.broadcast", true)) {
            event.joinMessage(Messages.legacy(formatMessage(message, player)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean serverStopping = isServerStopping();

        if (serverStopping) {
            String message = plugin.getConfig().getString("leave-message.message", "&3{player} left the game");
            event.quitMessage(Messages.legacy(formatMessage(message, player)));
            return;
        }

        if (plugin.getConfig().getBoolean("leave-message.hide-vanilla", true)) {
            event.setQuitMessage(null);
        }

        if (!plugin.getConfig().getBoolean("leave-message.enabled", true)) {
            return;
        }

        String message = plugin.getConfig().getString("leave-message.message", "&e{player} left the game");

        if (plugin.getConfig().getBoolean("leave-message.broadcast", true)) {
            event.quitMessage(Messages.legacy(formatMessage(message, player)));
        }
    }

    private String formatMessage(String message, Player player) {
        message = message
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{world}", player.getWorld().getName())
                .replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(plugin.getServer().getMaxPlayers()));

        return message;
    }

    private boolean isServerStopping() {
        try {
            Method method = plugin.getServer().getClass().getMethod("isStopping");
            Object result = method.invoke(plugin.getServer());
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    /**
     * Floodgate is optional, so its API is accessed reflectively. This keeps the
     * plugin loadable on servers that do not use Geyser/Floodgate.
     */
    private void sendBedrockWarning(Player player) {
        if (!plugin.getConfig().getBoolean("bedrock-warning.enabled", true) || !isFloodgatePlayer(player.getUniqueId())) {
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("bedrock-warning.message");
        if (messages.isEmpty()) {
            messages = List.of(
                    "&62b2t-th is best played on Java Edition. The Bedrock Edition",
                    "&6experience may not be optimal - 2b2t-th.org/bedrock"
            );
        }

        for (String message : messages) {
            Messages.send(player, formatMessage(message, player));
        }
    }

    private boolean isFloodgatePlayer(UUID playerId) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("floodgate")) {
            return false;
        }

        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, playerId);
            return result instanceof Boolean isFloodgatePlayer && isFloodgatePlayer;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    /**
     * Enables Bedrock's native coordinate HUD (the upper-left display) for this
     * Floodgate player. Geyser is optional and is accessed reflectively so that
     * UtilityPlus still works normally when it is installed on a proxy instead.
     */
    private void enableBedrockCoordinateHud(Player player) {
        if (!plugin.getConfig().getBoolean("bedrock-coordinates.enabled", true) || !isFloodgatePlayer(player.getUniqueId())) {
            return;
        }

        // Let Geyser finish its initial client setup before overriding this rule.
        PaperFoliaTasks.runForPlayerDelayed(plugin, player, task -> {
            if (!player.isOnline() || !isFloodgatePlayer(player.getUniqueId())) {
                return;
            }

            try {
                Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                Object geyserApi = geyserApiClass.getMethod("api").invoke(null);
                Object connection = geyserApiClass
                        .getMethod("connectionByUuid", UUID.class)
                        .invoke(geyserApi, player.getUniqueId());

                if (connection != null) {
                    connection.getClass()
                            .getMethod("sendGameRule", String.class, Object.class)
                            .invoke(connection, "showcoordinates", true);
                }
            } catch (ReflectiveOperationException ignored) {
                // Geyser is not installed locally, or uses an incompatible API.
            }
        }, 20L);
    }
}
