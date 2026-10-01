package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

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
            Messages.send(sender, "<red>This command can only be used by players.");
            return true;
        }

        if (args.length < 1) {
            Messages.send(player, Messages.config("bad-command", "<dark_red>Bad command. Type /help for all commands."));
            return true;
        }

        Player target = Bukkit.getOnlinePlayers().stream()
                .filter(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(args[0]))
                .findFirst()
                .orElse(null);
        Set<String> existingIgnores = deathMessages
                ? Set.of()
                : hard
                ? chatManager.getHardIgnoredPlayers(player.getUniqueId())
                : chatManager.getIgnoredPlayers(player.getUniqueId());
        String targetName;
        if (target != null) {
            targetName = target.getName();
        } else if (!existingIgnores.isEmpty()) {
            targetName = existingIgnores.stream()
                    .filter(name -> name.equalsIgnoreCase(args[0]))
                    .findFirst()
                    .orElse(null);
            if (targetName == null) {
                Messages.send(player, Messages.config("player-not-online", "<gold>This player is not online."));
                return true;
            }
        } else {
            Messages.send(player, Messages.config("player-not-online", "<gold>This player is not online."));
            return true;
        }
        if (targetName.equalsIgnoreCase(player.getName())) {
            Messages.send(player, Messages.config("ignore.self", "<gold>You can not ignore yourself."));
            return true;
        }

        boolean enabled = deathMessages
                ? chatManager.toggleDeathMessageIgnore(player.getUniqueId(), targetName)
                : hard
                ? chatManager.toggleHardIgnore(player.getUniqueId(), targetName)
                : chatManager.toggleIgnore(player.getUniqueId(), targetName);

        if (deathMessages) {
            sendConfigured(player, enabled ? "ignore.death-enabled" : "ignore.death-disabled",
                    enabled ? "<gold>You will no longer see this players death messages."
                            : "<gold>You will now see this players death messages.", targetName);
        } else if (hard) {
            sendConfigured(player, enabled ? "ignore.hard-enabled" : "ignore.hard-disabled",
                    enabled ? "<gold>Permanently ignoring <dark_aqua>{player}.<gold> This is saved in <dark_gray>/ignorelist."
                            : "<gold>No longer permanently ignoring <dark_aqua>{player}.", targetName, true);
        } else {
            sendConfigured(player, enabled ? "ignore.normal-enabled" : "ignore.normal-disabled",
                    enabled ? "<gold>Now ignoring <dark_aqua>{player}" : "<gold>No longer ignoring <dark_aqua>{player}.", targetName, true);
        }
        return true;
    }

    private void sendConfigured(Player player, String path, String fallback, String targetName) {
        sendConfigured(player, path, fallback, targetName, false);
    }

    private void sendConfigured(Player player, String path, String fallback, String targetName,
                                boolean withoutPlayerNameHover) {
        String message = Messages.config(path, fallback).replace("{player}", targetName);
        if (withoutPlayerNameHover) {
            Messages.sendUndecorated(player, message);
            return;
        }
        Messages.send(player, message);
    }
}
