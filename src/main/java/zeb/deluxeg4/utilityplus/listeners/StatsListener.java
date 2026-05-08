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

import java.util.concurrent.ThreadLocalRandom;

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

        // Auto-save every few deaths or use periodic save
        // For simplicity, we save on every death/kill here, but in a busy server
        // you might want to save periodically.
        statsManager.saveData();
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

        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&c"));
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

        // Color weapon name in gold if present in message
        if (killer != null) {
            message = colorWeaponName(message, killer);
        }

        if (killer != null && !selfKillCommand) {
            Component itemComponent = createNamedWeaponComponent(killer, messageColor);
            if (itemComponent != null) {
                // Trim trailing spaces to prevent double spacing before "using"
                message = message.replaceAll("\\s+$", "");
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

        String connector = ThreadLocalRandom.current().nextBoolean() ? " using " : " with ";
        Component component = LegacyComponentSerializer.legacySection()
                .deserialize(message + color(plugin.getConfig().getString("death-message.message-color", "&c")) + connector)
                .append(itemComponent);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (chatManager.isDeathMessagesMuted(player.getUniqueId())
                    || chatManager.isIgnoringDeathMessage(player.getUniqueId(), victim.getName())) {
                continue;
            }
            PaperFoliaTasks.runForPlayer(plugin, player, () -> player.sendMessage(component));
        }
        Bukkit.getConsoleSender().sendMessage(message + connector + LegacyComponentSerializer.legacySection().serialize(itemComponent));
    }

    private Component createNamedWeaponComponent(Player killer, String messageColor) {
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR || !weapon.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }

        String displayName = meta.getDisplayName();
        // Always make weapon name gold (&6)
        displayName = ChatColor.GOLD + ChatColor.stripColor(displayName);

        return LegacyComponentSerializer.legacySection()
                .deserialize(ChatColor.RESET + displayName)
                .hoverEvent(weapon.asHoverEvent());
    }

    private String colorName(String message, String name) {
        if (name == null || name.isEmpty()) {
            return message;
        }

        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&c"));
        String nameColor = color(plugin.getConfig().getString("death-message.name-color", "&b"));

        return message.replace(name, nameColor + name + messageColor);
    }

    private String colorWeaponName(String message, Player killer) {
        if (killer == null) return message;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR || !weapon.hasItemMeta()) {
            return message;
        }

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return message;
        }

        String weaponNameRaw = meta.getDisplayName();
        // Strip all existing color codes and apply gold
        String weaponNameStripped = ChatColor.stripColor(weaponNameRaw);
        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&c"));

        // Replace the raw weapon name (with any colors) with stripped + gold version
        return message.replace(weaponNameRaw, ChatColor.GOLD + weaponNameStripped + messageColor);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}