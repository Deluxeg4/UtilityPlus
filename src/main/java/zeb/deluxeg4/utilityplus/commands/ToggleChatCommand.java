package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleChatCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public ToggleChatCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "<red>This command can only be used by players.");
            return true;
        }

        boolean disabled = chatManager.toggleGlobalMuted(player.getUniqueId());
        Messages.send(player, disabled ? "<gold>Chat messages hidden." : "<gold>Chat messages unhidden.");
        return true;
    }
}
