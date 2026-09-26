package zeb.deluxeg4.utilityplus.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import zeb.deluxeg4.utilityplus.util.Messages;

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
        if (!plugin.getConfig().getBoolean("bedrock-warning.enabled", true) || !isBedrockPlayer(player)) {
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

    private boolean isBedrockPlayer(Player player) {
        // Floodgate prefixes Bedrock usernames with a dot on this network. The
        // prefix is forwarded to backend servers even when Floodgate is only
        // installed on the proxy, so this check does not need the backend API.
        if (player.getName().startsWith(".")) {
            return true;
        }

        if (!plugin.getServer().getPluginManager().isPluginEnabled("floodgate")) {
            return false;
        }

        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, player.getUniqueId());
            return result instanceof Boolean isFloodgatePlayer && isFloodgatePlayer;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

}
