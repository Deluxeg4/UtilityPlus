package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.Bukkit;
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
            Messages.send(player, "&4Bad command. Type /help for all commands.");
            return true;
        }

        String targetName = args[0];
        boolean online = Bukkit.getOnlinePlayers().stream()
                .anyMatch(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(targetName));
        if (!online) {
            Messages.send(player, "&6This player is not online.");
            return true;
        }
        if (targetName.equalsIgnoreCase(player.getName())) {
            Messages.send(player, "&6You can not ignore yourself.");
            return true;
        }

        boolean enabled = deathMessages
                ? chatManager.toggleDeathMessageIgnore(player.getUniqueId(), targetName)
                : hard
                ? chatManager.toggleHardIgnore(player.getUniqueId(), targetName)
                : chatManager.toggleIgnore(player.getUniqueId(), targetName);

        if (deathMessages) {
            Messages.send(player, enabled
                    ? "&6You will no longer see this players death messages."
                    : "&6You will now see this players death messages.");
        } else if (hard) {
            Messages.send(player, enabled
                    ? "&6Permanently ignoring " + "&3" + targetName + "." + "&6 This is saved in &8/ignorelist."
                    : "&6No longer permanently ignoring " + targetName + ".");
        } else {
            Messages.send(player, enabled
                    ? "&6Now ignoring &3" + targetName
                    : "&6No longer ignoring &3" + targetName + ".");
        }
        return true;
    }
}
