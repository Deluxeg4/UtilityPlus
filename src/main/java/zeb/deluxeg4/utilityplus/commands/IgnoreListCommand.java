package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IgnoreListCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 9;
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

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

        Set<String> hardIgnoredPlayers = chatManager.getHardIgnoredPlayers(player.getUniqueId());
        Set<String> softIgnoredPlayers = chatManager.getIgnoredPlayers(player.getUniqueId());
        Set<String> ignored = new HashSet<>(hardIgnoredPlayers);
        ignored.addAll(softIgnoredPlayers);
        if (ignored.isEmpty()) {
            Messages.send(player, Messages.config("ignore-list.empty", "&6No players ignored."));
            return true;
        }

        List<String> names = ignored.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        int totalPages = (names.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = parsePage(args, totalPages);
        if (page < 1) {
            String path = totalPages == 1
                    ? "ignore-list.invalid-page-singular"
                    : "ignore-list.invalid-page-plural";
            String fallback = totalPages == 1
                    ? "&cInvalid page argument, there are only {pages} page."
                    : "&cInvalid page argument, there are only {pages} pages.";
            Messages.send(player, Messages.config(path, fallback).replace("{pages}", String.valueOf(totalPages)));
            return true;
        }
        sendHeader(player, page, totalPages);

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, names.size());
        for (String name : names.subList(start, end)) {
            String displayName = Bukkit.getOnlinePlayers().stream()
                    .filter(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(name))
                    .map(Player::getName)
                    .findFirst()
                    .orElse(name);
            String rowPrefix = Messages.config("ignore-list.player-row-prefix", "&3{player} &7[")
                    .replace("{player}", displayName);
            Component row = AMPERSAND.deserialize(rowPrefix);
            boolean hardIgnored = containsIgnoreCase(hardIgnoredPlayers, name);
            boolean softIgnored = containsIgnoreCase(softIgnoredPlayers, name);
            if (hardIgnored) {
                row = row.append(configured("ignore-list.hard-label", "&6hard")
                        .hoverEvent(HoverEvent.showText(AMPERSAND.deserialize(Messages.config(
                                "ignore-list.hard-hover", "&6Click to remove the permanent ignore"))))
                        .clickEvent(ClickEvent.runCommand("/ignorehard " + displayName)));
            }
            if (hardIgnored && softIgnored) {
                row = row.append(AMPERSAND.deserialize("&7, "));
            }
            if (softIgnored) {
                row = row.append(configured("ignore-list.soft-label", "&6soft")
                        .hoverEvent(HoverEvent.showText(AMPERSAND.deserialize(Messages.config(
                                "ignore-list.soft-hover", "&6Click to remove the soft ignore"))))
                        .clickEvent(ClickEvent.runCommand("/ignore " + displayName)));
            }
            row = row.append(configured("ignore-list.player-row-suffix", "&7]"));
            player.sendMessage(row);
        }
        return true;
    }

    private boolean containsIgnoreCase(Set<String> names, String target) {
        return names.stream().anyMatch(name -> name.equalsIgnoreCase(target));
    }

    private int parsePage(String[] args, int totalPages) {
        if (args.length == 0) return 1;
        if (args.length != 1) return -1;
        try {
            int page = Integer.parseInt(args[0]);
            return page >= 1 && page <= totalPages ? page : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void sendHeader(Player player, int page, int totalPages) {
        Component previous = page > 1
                ? pageButton("ignore-list.previous-active", "&b[<]", "ignore-list.previous-hover",
                        "&6Click to go to the previous page", page - 1)
                : configured("ignore-list.previous-inactive", "&7[<]");
        Component next = page < totalPages
                ? pageButton("ignore-list.next-active", "&b[>]", "ignore-list.next-hover",
                        "&6Click to go to the next page", page + 1)
                : configured("ignore-list.next-inactive", "&7[>]");

        String pageCounter = Messages.config("ignore-list.page-counter", " &7{page}/{pages} ")
                .replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(totalPages));
        player.sendMessage(configured("ignore-list.title", "&6Ignored players &7")
                .append(previous)
                .append(AMPERSAND.deserialize(pageCounter))
                .append(next)
                .append(configured("ignore-list.suffix", "&7]")));
    }

    private Component pageButton(String labelPath, String labelFallback, String hoverPath,
                                 String hoverFallback, int page) {
        return configured(labelPath, labelFallback)
                .hoverEvent(HoverEvent.showText(configured(hoverPath, hoverFallback)))
                .clickEvent(ClickEvent.runCommand("/ignorelist " + page));
    }

    private Component configured(String path, String fallback) {
        return AMPERSAND.deserialize(Messages.config(path, fallback));
    }
}
