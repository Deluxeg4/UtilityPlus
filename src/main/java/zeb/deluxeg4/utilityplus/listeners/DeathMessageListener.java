package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.commands.KillCommand;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.managers.DeathMessageManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class DeathMessageListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final ChatManager chatManager;
    private final DeathMessageManager deathMessageManager;
    private final JavaPlugin plugin;

    public DeathMessageListener(ChatManager chatManager, DeathMessageManager deathMessageManager, JavaPlugin plugin) {
        this.chatManager = chatManager;
        this.deathMessageManager = deathMessageManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = deathMessageManager.resolveKiller(event, victim);
        colorDeathMessage(event, victim, killer);
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
            message = colorWeaponName(message, killer);
        }

        if (!selfKillCommand) {
            Component itemComponent = createSourceComponent(event, victim, killer);
            if (itemComponent != null) {
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
        Bukkit.getConsoleSender().sendMessage(Messages.legacy(message));
    }

    private void broadcastDeathMessage(PlayerDeathEvent event, Player victim, String message, Component itemComponent) {
        event.setDeathMessage(null);

        String connector = ThreadLocalRandom.current().nextBoolean() ? " using " : " with ";
        Component component = Messages
                .legacy(message + color(plugin.getConfig().getString("death-message.message-color", "&c")) + connector)
                .append(itemComponent);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (chatManager.isDeathMessagesMuted(player.getUniqueId())
                    || chatManager.isIgnoringDeathMessage(player.getUniqueId(), victim.getName())) {
                continue;
            }
            PaperFoliaTasks.runForPlayer(plugin, player, () -> player.sendMessage(component));
        }
        Bukkit.getConsoleSender().sendMessage(Messages.legacy(message + connector).append(itemComponent));
    }

    private Component createSourceComponent(PlayerDeathEvent event, Player victim, Player killer) {
        if (killer == null) {
            return null;
        }

        ItemStack item = deathMessageManager.getKillerItem(killer);
        if (item != null) {
            return createItemComponent(item);
        }

        String causeName = deathMessageManager.getSimpleCauseName(event, victim, killer);
        if (causeName == null || causeName.isBlank()) {
            return null;
        }

        return Component.text(causeName, NamedTextColor.GOLD);
    }

    private Component createItemComponent(ItemStack weapon) {
        ItemMeta meta = weapon.getItemMeta();
        String displayName = meta != null && meta.hasDisplayName() && meta.displayName() != null
                ? PLAIN_TEXT.serialize(meta.displayName())
                : friendlyItemName(weapon.getType());

        return Component.text(displayName, NamedTextColor.GOLD)
                .hoverEvent(weapon.asHoverEvent());
    }

    private String friendlyItemName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(part);
        }
        return result.toString();
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
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR || !weapon.hasItemMeta()) {
            return message;
        }

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return message;
        }

        String weaponNameRaw = meta.displayName() != null ? PLAIN_TEXT.serialize(meta.displayName()) : meta.getDisplayName();
        String weaponNameStripped = weaponNameRaw;
        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&c"));

        return message.replace(weaponNameRaw, "&6" + weaponNameStripped + messageColor);
    }

    private String color(String value) {
        return value;
    }
}
