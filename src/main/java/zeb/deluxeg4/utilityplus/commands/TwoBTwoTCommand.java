package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TwoBTwoTCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public TwoBTwoTCommand(UtilityPlus plugin, ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        switch (label.toLowerCase()) {
            case "ignore":
                return toggleName(player, args, false, false);
            case "ignorehard":
                return toggleName(player, args, true, false);
            case "ignoredeathmsgs":
                return toggleName(player, args, true, true);
            case "ignorelist":
                return sendIgnoreList(player);
            case "togglechat":
                return toggleChat(player);
            case "toggleprivatemsgs":
                return togglePrivateMessages(player);
            case "toggledeathmsgs":
                return toggleDeathMessages(player, false);
            case "toggledeathmsgshard":
                return toggleDeathMessages(player, true);
            default:
                return true;
        }
    }

    private boolean toggleName(Player player, String[] args, boolean hard, boolean deathMessages) {
        if (args.length < 1) {
            player.sendMessage("§cUsage: /" + (deathMessages ? "ignoredeathmsgs" : hard ? "ignorehard" : "ignore") + " <player>");
            return true;
        }

        String targetName = args[0];
        boolean enabled = deathMessages
                ? chatManager.toggleDeathMessageIgnore(player.getUniqueId(), targetName)
                : hard
                ? chatManager.toggleHardIgnore(player.getUniqueId(), targetName)
                : chatManager.toggleIgnore(player.getUniqueId(), targetName);

        String type = deathMessages ? "death messages from" : "messages from";
        player.sendMessage((enabled ? "§aIgnoring " : "§eUnignored ") + type + " §f" + targetName + "§r.");
        return true;
    }

    private boolean sendIgnoreList(Player player) {
        Set<String> ignored = chatManager.getHardIgnoredPlayers(player.getUniqueId());
        if (ignored.isEmpty()) {
            player.sendMessage("§eYou have no permanently ignored players.");
            return true;
        }

        player.sendMessage("§6Permanently ignored players:");
        for (String name : ignored.stream().sorted().toList()) {
            player.sendMessage("§7- §f" + name);
        }
        return true;
    }

    private boolean toggleChat(Player player) {
        boolean disabled = chatManager.toggleGlobalMuted(player.getUniqueId());
        player.sendMessage(disabled ? "§cGlobal chat is now hidden." : "§aGlobal chat is now visible.");
        return true;
    }

    private boolean togglePrivateMessages(Player player) {
        boolean disabled = chatManager.togglePmMuted(player.getUniqueId());
        player.sendMessage(disabled ? "§cPrivate messages are now hidden." : "§aPrivate messages are now visible.");
        return true;
    }

    private boolean toggleDeathMessages(Player player, boolean hard) {
        boolean disabled = hard
                ? chatManager.toggleHardDeathMessages(player.getUniqueId())
                : chatManager.toggleDeathMessages(player.getUniqueId());
        player.sendMessage(disabled ? "§cDeath messages are now hidden." : "§aDeath messages are now visible.");
        return true;
    }

}
