package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();
    private final File dataFile;
    private FileConfiguration dataConfig;

    public VanishCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "vanished.yml");
        loadData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("utilityplus.vanish")) {
            Messages.send(player, "&cYou don't have permission to use this command.");
            return true;
        }

        boolean vanished = !isVanished(player.getUniqueId());
        setVanished(player, vanished);
        Messages.send(player, vanished ? "&aYou are now &7vanished&a!" : "&aYou are now &evisible&a!");
        return true;
    }

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public void setVanished(Player player, boolean vanished) {
        if (vanished) {
            vanishedPlayers.add(player.getUniqueId());
            applyVanishEffects(player);
            hideFromOthers(player);
        } else {
            vanishedPlayers.remove(player.getUniqueId());
            clearVanishEffects(player);
            showToEveryone(player);
        }
        saveData();
    }

    public void applyPersistentVanish(Player player) {
        if (!isVanished(player.getUniqueId())) {
            return;
        }
        applyVanishEffects(player);
        hideFromOthers(player);
    }

    public void hideVanishedPlayersFrom(Player viewer) {
        if (viewer.hasPermission("utilityplus.vanish.see")) {
            return;
        }

        for (Player vanished : Bukkit.getOnlinePlayers()) {
            if (!vanished.equals(viewer) && isVanished(vanished.getUniqueId())) {
                PaperFoliaTasks.runForPlayer(plugin, viewer, () -> viewer.hidePlayer(plugin, vanished));
            }
        }
    }

    private void applyVanishEffects(Player player) {
        player.setInvisible(true);
        player.setAllowFlight(true);
        player.setInvulnerable(true);
        player.setSleepingIgnored(true);
        player.setCollidable(false);
        player.setSilent(true);
        player.setCanPickupItems(false);
    }

    private void clearVanishEffects(Player player) {
        player.setInvisible(false);
        player.setAllowFlight(player.hasPermission("utilityplus.fly"));
        player.setInvulnerable(false);
        player.setSleepingIgnored(false);
        player.setCollidable(true);
        player.setSilent(false);
        player.setCanPickupItems(true);
    }

    private void hideFromOthers(Player vanishedPlayer) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(vanishedPlayer) && !viewer.hasPermission("utilityplus.vanish.see")) {
                PaperFoliaTasks.runForPlayer(plugin, viewer, () -> viewer.hidePlayer(plugin, vanishedPlayer));
            }
        }
    }

    private void showToEveryone(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer(plugin, viewer, () -> viewer.showPlayer(plugin, player));
        }
    }

    private void loadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        List<String> uuids = dataConfig.getStringList("vanished");
        for (String uuid : uuids) {
            try {
                vanishedPlayers.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveData() {
        plugin.getDataFolder().mkdirs();
        dataConfig.set("vanished", vanishedPlayers.stream().map(UUID::toString).toList());
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Vanish] Failed to save vanished.yml: " + e.getMessage());
        }
    }
}
