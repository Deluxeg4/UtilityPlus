package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
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
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {
            return handleReply(from, args);
        }
        if (label.equalsIgnoreCase("l") || label.equalsIgnoreCase("last")) {
            return handleLast(from, args);
        }

        if (args.length < 2) {
            from.sendMessage("§cUsage: /" + label + " <player> <message>");
            return true;
        }

        Player to = from.getServer().getPlayer(args[0]);
        if (to == null || !to.isOnline()) {
            from.sendMessage("§cPlayer §e" + args[0] + " §cis not online!");
            return true;
        }
        if (to.equals(from)) {
            from.sendMessage("§cYou cannot message yourself!");
            return true;
        }

        sendPM(from, to, buildMessage(args, 1));
        return true;
    }

    private boolean handleReply(Player from, String[] args) {
        if (args.length < 1) {
            from.sendMessage("§cUsage: /r <message>");
            return true;
        }

        UUID lastSenderUUID = chatManager.getLastPmSender(from.getUniqueId());
        if (lastSenderUUID == null) {
            from.sendMessage("§cYou have no one to reply to!");
            return true;
        }

        Player to = from.getServer().getPlayer(lastSenderUUID);
        if (to == null || !to.isOnline()) {
            from.sendMessage("§cThat player is no longer online.");
            return true;
        }

        sendPM(from, to, buildMessage(args, 0));
        return true;
    }

    private boolean handleLast(Player from, String[] args) {
        if (args.length < 1) {
            from.sendMessage("§cUsage: /last <message>");
            return true;
        }

        UUID lastTargetUUID = chatManager.getLastPmTarget(from.getUniqueId());
        if (lastTargetUUID == null) {
            from.sendMessage("§cYou have no last messaged player!");
            return true;
        }

        Player to = from.getServer().getPlayer(lastTargetUUID);
        if (to == null || !to.isOnline()) {
            from.sendMessage("§cThat player is no longer online.");
            return true;
        }

        sendPM(from, to, buildMessage(args, 0));
        return true;
    }

    private void sendPM(Player from, Player to, String message) {
        if (chatManager.isPmMuted(to.getUniqueId())) {
            from.sendMessage("§e" + to.getName() + " §7is not accepting private messages.");
            return;
        }

        if (chatManager.isIgnoring(to.getUniqueId(), from.getName())) {
            from.sendMessage("§e" + to.getName() + " §7is ignoring you.");
            return;
        }

        String toSender = "§7[§fYou §8-> §f" + to.getName() + "§7] §f" + message;
        String toTarget = "§7[§f" + from.getName() + " §8-> §fYou§7] §f" + message;
        UtilityPlus plugin = JavaPlugin.getPlugin(UtilityPlus.class);

        from.sendMessage(toSender);
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
