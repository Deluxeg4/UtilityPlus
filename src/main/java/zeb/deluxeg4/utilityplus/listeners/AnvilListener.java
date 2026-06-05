package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.util.Messages;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AnvilListener implements Listener {

    private static final int ANVIL_RESULT_SLOT = 2;
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    /** Applies formatted anvil rename previews. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareAnvil(final PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof final Player player)
                || !player.hasPermission("utilityplus.anvil.color")) {
            return;
        }

        final ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta()) {
            return;
        }

        final ItemMeta meta = result.getItemMeta();
        if (!meta.hasDisplayName() || meta.displayName() == null) {
            return;
        }

        final String rawName = PLAIN_TEXT.serialize(meta.displayName());
        final String translatedName = translate(rawName);
        if (translatedName.equals(rawName)) {
            return;
        }

        meta.displayName(Messages.legacy(translatedName));
        result.setItemMeta(meta);
        event.setResult(result);
    }

    /** Applies formatted anvil rename results. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)
                || !(event.getInventory() instanceof AnvilInventory)
                || !player.hasPermission("utilityplus.anvil.color")
                || event.getRawSlot() != ANVIL_RESULT_SLOT) {
            return;
        }

        final ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) {
            return;
        }

        final ItemMeta meta = result.getItemMeta();
        if (!meta.hasDisplayName() || meta.displayName() == null) {
            return;
        }

        meta.displayName(Messages.legacy(translate(PLAIN_TEXT.serialize(meta.displayName()))));
        result.setItemMeta(meta);
    }

    private String translate(final String input) {
        return input
                .replace("{heart}", "\u2764")
                .replace("{star}", "\u2605")
                .replace("{arrow}", "\u27A4")
                .replace("{skull}", "\u2620")
                .replace("{music}", "\u266A")
                .replace("{check}", "\u2714")
                .replace("{cross}", "\u2718")
                .replace("{dot}", "\u2022")
                .replace("{diamond}", "\u25C6")
                .replace("{sword}", "\u2694")
                .replace("{shield}", "\uD83D\uDEE1")
                .replace("{fire}", "\uD83D\uDD25")
                .replace("{crown}", "\u265B")
                .replace("{lightning}", "\u26A1")
                .replace("{infinity}", "\u221E")
                .replace("{flower}", "\u273F")
                .replace("{moon}", "\u263D")
                .replace("{sun}", "\u2600");
    }
}
