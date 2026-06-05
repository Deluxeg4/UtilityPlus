package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleDeathMessagesCommand implements CommandExecutor {

    private final ChatManager chatManager;
    private final boolean hard;

    public ToggleDeathMessagesCommand(ChatManager chatManager, boolean hard) {
        this.chatManager = chatManager;
        this.hard = hard;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "&cThis command can only be used by players.");
            return true;
        }

        boolean disabled = hard
                ? chatManager.toggleHardDeathMessages(player.getUniqueId())
                : chatManager.toggleDeathMessages(player.getUniqueId());
        Messages.send(player, disabled ? "&6Death messages hidden." : "&6Death messages unhidden.");
        return true;
    }
}
