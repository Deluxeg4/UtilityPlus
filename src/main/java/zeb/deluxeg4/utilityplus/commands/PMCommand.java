package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class PMCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public PMCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player from)) {
            Messages.send(sender, "&cThis command can only be used by players!");
            return true;
        }

        if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {
            return handleReply(from, args);
        }
        if (label.equalsIgnoreCase("l") || label.equalsIgnoreCase("last")) {
            return handleLast(from, args);
        }

        if (args.length < 2) {
            Messages.send(from, "&cUsage: /" + label + " <player> <message>");
            return true;
        }

        Player to = from.getServer().getPlayer(args[0]);
        if (to == null || !to.isOnline()) {
            Messages.send(from, "&cPlayer &e" + args[0] + " &cis not online!");
            return true;
        }
        if (to.equals(from)) {
            Messages.send(from, "&cYou cannot message yourself!");
            return true;
        }

        sendPM(from, to, buildMessage(args, 1));
        return true;
    }

    private boolean handleReply(Player from, String[] args) {
        if (args.length < 1) {
            Messages.send(from, "&cUsage: /r <message>");
            return true;
        }

        UUID lastSenderUUID = chatManager.getLastPmSender(from.getUniqueId());
        if (lastSenderUUID == null) {
            Messages.send(from, "&cYou have no one to reply to!");
            return true;
        }

        Player to = from.getServer().getPlayer(lastSenderUUID);
        if (to == null || !to.isOnline()) {
            Messages.send(from, "&cThat player is no longer online.");
            return true;
        }

        sendPM(from, to, buildMessage(args, 0));
        return true;
    }

    private boolean handleLast(Player from, String[] args) {
        if (args.length < 1) {
            Messages.send(from, "&cUsage: /last <message>");
            return true;
        }

        UUID lastTargetUUID = chatManager.getLastPmTarget(from.getUniqueId());
        if (lastTargetUUID == null) {
            Messages.send(from, "&cYou have no last messaged player!");
            return true;
        }

        Player to = from.getServer().getPlayer(lastTargetUUID);
        if (to == null || !to.isOnline()) {
            Messages.send(from, "&cThat player is no longer online.");
            return true;
        }

        sendPM(from, to, buildMessage(args, 0));
        return true;
    }

    private void sendPM(Player from, Player to, String message) {
        if (chatManager.isPmMuted(to.getUniqueId())) {
            Messages.send(from, "&e" + to.getName() + " &7is not accepting private messages.");
            return;
        }

        if (chatManager.isIgnoring(to.getUniqueId(), from.getName())) {
            Messages.send(from, "&e" + to.getName() + " &7is ignoring you.");
            return;
        }

        String toSender = "&dto " + to.getName() + ": " + message;
        String toTarget = "&d" + from.getName() + " whispers: " + message;
        UtilityPlus plugin = JavaPlugin.getPlugin(UtilityPlus.class);

        Messages.send(from, toSender);
        PaperFoliaTasks.send(plugin, to, toTarget);

        chatManager.setLastPmSender(to.getUniqueId(), from.getUniqueId());
        chatManager.setLastPmSender(from.getUniqueId(), to.getUniqueId());
        chatManager.setLastPmTarget(from.getUniqueId(), to.getUniqueId());
        chatManager.setLastPmTarget(to.getUniqueId(), from.getUniqueId());
    }

    private String buildMessage(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
