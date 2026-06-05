package zeb.deluxeg4.utilityplus.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import zeb.deluxeg4.utilityplus.util.Messages;

import java.util.Iterator;

public class ChatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final ChatManager chatManager;

    public ChatListener(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        event.message(highlightMessage(event.message()));

        if (chatManager.isGlobalMuted(sender.getUniqueId())) {
            event.setCancelled(true);
            Messages.send(sender, "&cGlobal chat is disabled. Use &e/chat on&c to re-enable.");
            return;
        }

        Iterator<Audience> recipients = event.viewers().iterator();
        while (recipients.hasNext()) {
            Audience audience = recipients.next();
            if (audience instanceof Player recipient
                    && (chatManager.isGlobalMuted(recipient.getUniqueId())
                    || chatManager.isIgnoring(recipient.getUniqueId(), sender.getName()))) {
                recipients.remove();
            }
        }
    }

    private Component highlightMessage(Component message) {
        if (message == null) {
            return Component.empty();
        }
        String plain = PLAIN_TEXT.serialize(message);
        if (!plain.startsWith(">")) {
            return message;
        }

        return Component.text(plain, NamedTextColor.GREEN);
    }
}
