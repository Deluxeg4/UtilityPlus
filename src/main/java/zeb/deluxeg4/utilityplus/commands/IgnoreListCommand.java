package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class IgnoreListCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public IgnoreListCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "&cThis command can only be used by players.");
            return true;
        }

        Set<String> ignored = chatManager.getHardIgnoredPlayers(player.getUniqueId());
        if (ignored.isEmpty()) {
            Messages.send(player, "&eYou have no permanently ignored players.");
            return true;
        }

        Messages.send(player, "&6Permanently ignored players:");
        for (String name : ignored.stream().sorted().toList()) {
            Messages.send(player, "&7- &f" + name);
        }
        return true;
    }
}
