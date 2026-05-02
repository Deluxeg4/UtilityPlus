package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.commands.KillCommand;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.managers.DeathMessageManager;
import zeb.deluxeg4.utilityplus.managers.StatsManager;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsListener implements Listener {

    private final StatsManager statsManager;
    private final ChatManager chatManager;
    private final DeathMessageManager deathMessageManager;
    private final JavaPlugin plugin;

    public StatsListener(StatsManager statsManager, ChatManager chatManager, DeathMessageManager deathMessageManager, JavaPlugin plugin) {
        this.statsManager = statsManager;
        this.chatManager = chatManager;
        this.deathMessageManager = deathMessageManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Count death for victim
        statsManager.addDeath(victim.getUniqueId(), victim.getName());

        // Count kill for killer
        if (killer != null) {
            statsManager.addKill(killer.getUniqueId(), killer.getName());
        }

        colorDeathMessage(event, victim, killer);
        
        statsManager.saveLater();
    }

    private void colorDeathMessage(PlayerDeathEvent event, Player victim, Player killer) {
        boolean selfKillCommand = victim.hasMetadata(KillCommand.SELF_KILL_METADATA);
        if (selfKillCommand) {
            victim.removeMetadata(KillCommand.SELF_KILL_METADATA, plugin);
        }

        if (!plugin.getConfig().getBoolean("death-message.enabled", true)) {
            if (selfKillCommand) {
                broadcastDeathMessage(event, victim, victim.getName() + " ended their life");
            }
            return;
        }

        String originalMessage = deathMessageManager.getMessage(event, victim, killer, selfKillCommand);

        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&4"));
        String template = color(plugin.getConfig().getString("death-message.message", "{message}"));
        String killerName = killer != null ? killer.getName() : "";

        String message = messageColor + template
                .replace("{message}", originalMessage)
                .replace("{player}", victim.getName())
                .replace("{killer}", killerName);

        message = colorName(message, victim.getName());

        if (killer != null) {
            message = colorName(message, killer.getName());
        }

        if (killer != null && !selfKillCommand) {
            Component itemComponent = createNamedWeaponComponent(killer);
            if (itemComponent != null) {
                broadcastDeathMessage(event, victim, message, itemComponent);
                return;
            }
        }

        broadcastDeathMessage(event, victim, message);
    }

    private void broadcastDeathMessage(PlayerDeathEvent event, Player victim, String message) {
        event.setDeathMessage(null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (chatManager.isDeathMessagesMuted(player.getUniqueId())
                    || chatManager.isIgnoringDeathMessage(player.getUniqueId(), victim.getName())) {
                continue;
            }
            PaperFoliaTasks.send(plugin, player, message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void broadcastDeathMessage(PlayerDeathEvent event, Player victim, String message, Component itemComponent) {
        event.setDeathMessage(null);

        Component component = LegacyComponentSerializer.legacySection()
                .deserialize(message + color(plugin.getConfig().getString("death-message.message-color", "&4")) + " using ")
                .append(itemComponent);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (chatManager.isDeathMessagesMuted(player.getUniqueId())
                    || chatManager.isIgnoringDeathMessage(player.getUniqueId(), victim.getName())) {
                continue;
            }
            PaperFoliaTasks.runForPlayer(plugin, player, () -> player.sendMessage(component));
        }
        Bukkit.getConsoleSender().sendMessage(message + " using " + LegacyComponentSerializer.legacySection().serialize(itemComponent));
    }

    private Component createNamedWeaponComponent(Player killer) {
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR || !weapon.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }

        return LegacyComponentSerializer.legacySection()
                .deserialize(ChatColor.GOLD + displayName)
                .hoverEvent(weapon.asHoverEvent());
    }

    private String colorName(String message, String name) {
        if (name == null || name.isEmpty()) {
            return message;
        }

        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&4"));
        String nameColor = color(plugin.getConfig().getString("death-message.name-color", "&3"));

        return message.replace(name, nameColor + name + messageColor);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
