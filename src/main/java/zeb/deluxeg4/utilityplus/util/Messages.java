package zeb.deluxeg4.utilityplus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Messages {

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private Messages() {
    }

    public static Component legacy(String message) {
        return LEGACY_AMPERSAND.deserialize(message);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(legacy(message));
    }
}
