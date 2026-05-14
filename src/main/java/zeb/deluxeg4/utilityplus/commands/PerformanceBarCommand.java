package zeb.deluxeg4.utilityplus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import zeb.deluxeg4.utilityplus.managers.PerformanceBarManager;

public final class PerformanceBarCommand implements CommandExecutor {
    private final PerformanceBarManager manager;

    public PerformanceBarCommand(PerformanceBarManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("utilityplus.performancebar")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (label.equalsIgnoreCase("tpsbar")) {
            boolean enabled = manager.toggleTpsBar(player);
            player.sendMessage(enabled ? "§aTPS bar enabled." : "§cTPS bar disabled.");
            return true;
        }

        PerformanceBarManager.RamUnit unit = parseUnit(args);
        boolean enabled = manager.toggleRamBar(player, unit);
        player.sendMessage(enabled ? "§aRAM bar enabled." : "§cRAM bar disabled.");
        return true;
    }

    private PerformanceBarManager.RamUnit parseUnit(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("mb")) {
            return PerformanceBarManager.RamUnit.MB;
        }
        return PerformanceBarManager.RamUnit.GB;
    }
}
