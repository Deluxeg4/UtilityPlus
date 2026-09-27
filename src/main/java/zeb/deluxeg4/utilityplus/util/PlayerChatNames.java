package zeb.deluxeg4.utilityplus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Adds message shortcuts to online player names in server chat components. */
public final class PlayerChatNames implements Listener {

    private static final Set<String> ONLINE_NAMES = ConcurrentHashMap.newKeySet();

    public PlayerChatNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ONLINE_NAMES.add(player.getName());
        }
    }

    public static Component decorate(Component message) {
        Component decorated = message;
        List<String> names = new ArrayList<>(ONLINE_NAMES);
        names.sort(Comparator.comparingInt(String::length).reversed());

        for (String name : names) {
            Pattern pattern = Pattern.compile("(?i)(?<![\\p{L}\\p{N}_])" + Pattern.quote(name) + "(?![\\p{L}\\p{N}_])");
            Component replacement = Component.text(name)
                    .hoverEvent(HoverEvent.showText(Component.text("Message ", NamedTextColor.GOLD)
                            .append(Component.text(name, NamedTextColor.DARK_AQUA))))
                    .clickEvent(ClickEvent.suggestCommand("/w " + name));
            decorated = decorated.replaceText(TextReplacementConfig.builder()
                    .match(pattern)
                    .replacement(replacement)
                    .build());
        }
        return decorated;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ONLINE_NAMES.add(event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        ONLINE_NAMES.remove(event.getPlayer().getName());
    }
}
