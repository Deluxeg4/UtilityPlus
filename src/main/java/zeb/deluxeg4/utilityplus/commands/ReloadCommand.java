package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final UtilityPlus plugin;

    public ReloadCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.reload")) {
            sender.sendMessage("§cYou don't have permission!"); return true;
        }

        sender.sendMessage("§eReloading UtilityPlus...");

        // 1. Save current in-memory data to disk before wiping
        plugin.getSpawnManager().saveData();
        plugin.getHomeManager().saveData();
        plugin.getTeamManager().saveData();
        plugin.getStatsManager().saveData();

        // 2. Reload config.yml from disk
        plugin.reloadConfig();

        // 3. Reinitialize each manager — re-reads config values + data files
        plugin.getSpawnManager().reload();
        plugin.getHomeManager().reload();
        plugin.getChatManager().reload();
        plugin.getTeamManager().reload();
        plugin.getStatsManager().reload();
        plugin.getDeathMessageManager().reload();
        plugin.getTabListManager().reload();
        plugin.getAnnouncementManager().reload();

        sender.sendMessage("§a§lUtilityPlus reloaded!");
        sender.sendMessage("§7config.yml §a✔  §7spawn §a✔  §7homes §a✔  §7teams §a✔  §7stats §a✔  §7announcement §a✔");
        return true;
    }
}
