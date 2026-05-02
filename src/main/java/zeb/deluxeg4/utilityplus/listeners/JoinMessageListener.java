package zeb.deluxeg4.utilityplus.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

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

        if (!plugin.getConfig().getBoolean("join-message.enabled", true)) {
            return;
        }

        String message = plugin.getConfig().getString("join-message.message", "&3{player} joined the game");
        message = formatMessage(message, player);

        if (plugin.getConfig().getBoolean("join-message.broadcast", true)) {
            event.setJoinMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean serverStopping = isServerStopping();

        if (serverStopping) {
            String message = plugin.getConfig().getString("leave-message.message", "&3{player} left the game");
            event.setQuitMessage(formatMessage(message, player));
            return;
        }

        if (plugin.getConfig().getBoolean("leave-message.hide-vanilla", true)) {
            event.setQuitMessage(null);
        }

        if (!plugin.getConfig().getBoolean("leave-message.enabled", true)) {
            return;
        }

        String message = plugin.getConfig().getString("leave-message.message", "&e{player} left the game");
        message = formatMessage(message, player);

        if (plugin.getConfig().getBoolean("leave-message.broadcast", true)) {
            event.setQuitMessage(message);
        }
    }

    private String formatMessage(String message, Player player) {
        message = message
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{world}", player.getWorld().getName())
                .replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(plugin.getServer().getMaxPlayers()));

        return ChatColor.translateAlternateColorCodes('&', message);
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
}
