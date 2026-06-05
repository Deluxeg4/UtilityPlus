package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class IgnoreCommand implements CommandExecutor {

    private final ChatManager chatManager;
    private final boolean hard;
    private final boolean deathMessages;

    public IgnoreCommand(ChatManager chatManager, boolean hard, boolean deathMessages) {
        this.chatManager = chatManager;
        this.hard = hard;
        this.deathMessages = deathMessages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (args.length < 1) {
            Messages.send(player, "&cUsage: /" + label.toLowerCase() + " <player>");
            return true;
        }

        String targetName = args[0];
        boolean enabled = deathMessages
                ? chatManager.toggleDeathMessageIgnore(player.getUniqueId(), targetName)
                : hard
                ? chatManager.toggleHardIgnore(player.getUniqueId(), targetName)
                : chatManager.toggleIgnore(player.getUniqueId(), targetName);

        String type = deathMessages ? "death messages from" : "messages from";
        Messages.send(player, (enabled ? "&aIgnoring " : "&eUnignored ") + type + " &f" + targetName + "&r.");
        return true;
    }
}
