package zeb.deluxeg4.utilityplus.invsee;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventorySeeSessionManager {
    private static final int INVENTORY_SIZE = 45;
    private static final int ENDER_CHEST_SIZE = 27;
    private static final int STORAGE_START = 0;
    private static final int BOOTS_SLOT = 36;
    private static final int LEGGINGS_SLOT = 37;
    private static final int CHESTPLATE_SLOT = 38;
    private static final int HELMET_SLOT = 39;
    private static final int OFFHAND_SLOT = 40;
    private static final int[] UNUSED_INVENTORY_SLOTS = {41, 42, 43, 44};

    private final UtilityPlus plugin;
    private final InventorySeeMode mode;
    private final PendingInventoryOrderManager pendingOrders;
    private final NamespacedKey placeholderKey;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public InventorySeeSessionManager(UtilityPlus plugin, InventorySeeMode mode, PendingInventoryOrderManager pendingOrders) {
        this.plugin = plugin;
        this.mode = mode;
        this.pendingOrders = pendingOrders;
        this.placeholderKey = new NamespacedKey(plugin, "invsee_placeholder");
    }

    public void open(OfflinePlayer target, Player viewer) {
        if (!(target instanceof Player targetPlayer) || !targetPlayer.isOnline()) {
            openOffline(target, viewer);
            return;
        }

        Session previous = sessions.remove(viewer.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }

        Inventory inventory = Bukkit.createInventory(
                null,
                mode == InventorySeeMode.INVENTORY ? INVENTORY_SIZE : ENDER_CHEST_SIZE,
                mode == InventorySeeMode.INVENTORY
                        ? ChatColor.DARK_GRAY + "Inventory: " + ChatColor.YELLOW + targetPlayer.getName()
                        : ChatColor.DARK_GRAY + "Ender Chest: " + ChatColor.YELLOW + targetPlayer.getName()
        );
        Session session = new Session(viewer.getUniqueId(), targetPlayer.getUniqueId(), inventory, false);
        sessions.put(viewer.getUniqueId(), session);

        refresh(session, () -> PaperFoliaTasks.runForPlayer(plugin, viewer, () -> {
            if (!viewer.isOnline()) {
                close(viewer.getUniqueId());
                return;
            }
            viewer.openInventory(inventory);
            session.refreshTask = PaperFoliaTasks.runGlobalTimer(plugin, task -> refresh(session, null), 5L, 5L);
        }));
    }

    public boolean isSessionInventory(Player viewer, Inventory inventory) {
        Session session = sessions.get(viewer.getUniqueId());
        return session != null && session.inventory.equals(inventory);
    }

    public boolean isReadOnlySession(Player viewer, Inventory inventory) {
        Session session = sessions.get(viewer.getUniqueId());
        return session != null && session.inventory.equals(inventory) && session.readOnly;
    }

    public boolean isOfflineSession(Player viewer, Inventory inventory) {
        Session session = sessions.get(viewer.getUniqueId());
        return session != null && session.inventory.equals(inventory) && session.offline;
    }

    public void markDirtyIfOffline(Player viewer, Inventory inventory) {
        Session session = sessions.get(viewer.getUniqueId());
        if (session != null && session.inventory.equals(inventory) && session.offline) {
            session.dirty = true;
        }
    }

    public boolean isPlaceholderItem(ItemStack item) {
        return isPlaceholder(item);
    }

    public boolean isLockedSlot(Player viewer, int rawSlot) {
        Session session = sessions.get(viewer.getUniqueId());
        if (session == null || mode != InventorySeeMode.INVENTORY || rawSlot < 0 || rawSlot >= INVENTORY_SIZE) {
            return false;
        }
        for (int slot : UNUSED_INVENTORY_SLOTS) {
            if (rawSlot == slot) {
                return true;
            }
        }
        return false;
    }

    public boolean canPlaceInSlot(Player viewer, int rawSlot, ItemStack item) {
        Session session = sessions.get(viewer.getUniqueId());
        if (session == null || mode != InventorySeeMode.INVENTORY || rawSlot < 0 || rawSlot >= INVENTORY_SIZE) {
            return true;
        }
        if (item == null || item.getType() == Material.AIR || item.isEmpty()) {
            return true;
        }
        if (isPlaceholder(item)) {
            return false;
        }
        if (rawSlot >= STORAGE_START && rawSlot < STORAGE_START + 36) {
            return true;
        }
        return switch (rawSlot) {
            case BOOTS_SLOT -> isBoots(item.getType());
            case LEGGINGS_SLOT -> isLeggings(item.getType());
            case CHESTPLATE_SLOT -> isChestplate(item.getType());
            case HELMET_SLOT -> isHelmet(item.getType());
            case OFFHAND_SLOT -> true;
            default -> false;
        };
    }

    public void applyViewerChanges(Player viewer) {
        Session session = sessions.get(viewer.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.offline) {
            session.dirty = true;
            saveOfflineChanges(viewer, session);
            return;
        }
        if (session.readOnly) {
            return;
        }

        Player target = Bukkit.getPlayer(session.targetId);
        if (target == null || !target.isOnline()) {
            viewer.sendMessage(ChatColor.RED + "Target player is no longer online.");
            close(viewer.getUniqueId());
            return;
        }

        ItemStack[] contents = session.inventory.getContents();
        session.applyingChanges = true;
        PaperFoliaTasks.runForPlayer(plugin, target, () -> {
            if (mode == InventorySeeMode.INVENTORY) {
                applyPlayerInventory(target.getInventory(), contents);
            } else {
                target.getEnderChest().setContents(trim(contents, ENDER_CHEST_SIZE));
            }
            PaperFoliaTasks.runForPlayerDelayed(plugin, viewer, task -> session.applyingChanges = false, 1L);
        });
    }

    public void close(UUID viewerId) {
        Session session = sessions.remove(viewerId);
        if (session != null) {
            session.cancel();
        }
    }

    public void closeAll() {
        sessions.values().forEach(Session::cancel);
        sessions.clear();
    }

    private void refresh(Session session, Runnable after) {
        if (session.applyingChanges) {
            if (after != null) {
                after.run();
            }
            return;
        }

        Player target = Bukkit.getPlayer(session.targetId);
        Player viewer = Bukkit.getPlayer(session.viewerId);
        if (target == null || viewer == null || !target.isOnline() || !viewer.isOnline()) {
            close(session.viewerId);
            return;
        }

        PaperFoliaTasks.runForPlayer(plugin, target, () -> {
            ItemStack[] snapshot = mode == InventorySeeMode.INVENTORY
                    ? snapshotPlayerInventory(target.getInventory())
                    : trim(target.getEnderChest().getContents(), ENDER_CHEST_SIZE);

            PaperFoliaTasks.runForPlayer(plugin, viewer, () -> {
                if (!viewer.isOnline()) {
                    close(session.viewerId);
                    return;
                }
                session.inventory.setContents(snapshot);
                if (after != null) {
                    after.run();
                }
            });
        });
    }

    private ItemStack[] snapshotPlayerInventory(PlayerInventory playerInventory) {
        ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        ItemStack[] storage = playerInventory.getStorageContents();
        for (int i = 0; i < storage.length && STORAGE_START + i < contents.length; i++) {
            contents[STORAGE_START + i] = copy(storage[i]);
        }
        contents[HELMET_SLOT] = copy(playerInventory.getHelmet());
        contents[CHESTPLATE_SLOT] = copy(playerInventory.getChestplate());
        contents[LEGGINGS_SLOT] = copy(playerInventory.getLeggings());
        contents[BOOTS_SLOT] = copy(playerInventory.getBoots());
        contents[OFFHAND_SLOT] = copy(playerInventory.getItemInOffHand());
        setSlotPlaceholderIfEmpty(contents, HELMET_SLOT, "Helmet Slot");
        setSlotPlaceholderIfEmpty(contents, CHESTPLATE_SLOT, "Chestplate Slot");
        setSlotPlaceholderIfEmpty(contents, LEGGINGS_SLOT, "Leggings Slot");
        setSlotPlaceholderIfEmpty(contents, BOOTS_SLOT, "Boots Slot");
        setOffhandPlaceholderIfEmpty(contents);
        setUnusedInventorySlots(contents);
        return contents;
    }

    private void openOffline(OfflinePlayer target, Player viewer) {
        if (!target.hasPlayedBefore()) {
            viewer.sendMessage(ChatColor.RED + "That player has no playerdata.");
            return;
        }

        Session previous = sessions.remove(viewer.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }

        ItemStack[] snapshot = pendingOrders.load(target.getUniqueId(), mode);
        boolean pending = snapshot != null;
        try {
            if (snapshot == null) {
                snapshot = OfflineInventoryLoader.load(target.getUniqueId(), mode);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[InvSee] Could not read offline playerdata for " + target.getName() + ": " + e.getMessage());
            viewer.sendMessage(ChatColor.RED + "Could not read that player's world data.");
            return;
        }

        if (snapshot == null) {
            viewer.sendMessage(ChatColor.RED + "That player has no playerdata.");
            return;
        }

        if (mode == InventorySeeMode.INVENTORY) {
            setSlotPlaceholderIfEmpty(snapshot, HELMET_SLOT, "Helmet Slot");
            setSlotPlaceholderIfEmpty(snapshot, CHESTPLATE_SLOT, "Chestplate Slot");
            setSlotPlaceholderIfEmpty(snapshot, LEGGINGS_SLOT, "Leggings Slot");
            setSlotPlaceholderIfEmpty(snapshot, BOOTS_SLOT, "Boots Slot");
            setOffhandPlaceholderIfEmpty(snapshot);
            setUnusedInventorySlots(snapshot);
        }

        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        Inventory inventory = Bukkit.createInventory(
                null,
                mode == InventorySeeMode.INVENTORY ? INVENTORY_SIZE : ENDER_CHEST_SIZE,
                mode == InventorySeeMode.INVENTORY
                        ? ChatColor.DARK_GRAY + "Offline Inventory: " + ChatColor.YELLOW + name
                        : ChatColor.DARK_GRAY + "Offline Ender Chest: " + ChatColor.YELLOW + name
        );
        inventory.setContents(snapshot);

        Session session = new Session(viewer.getUniqueId(), target.getUniqueId(), inventory, false, true);
        sessions.put(viewer.getUniqueId(), session);
        PaperFoliaTasks.runForPlayer(plugin, viewer, () -> {
            if (viewer.isOnline()) {
                viewer.openInventory(inventory);
                viewer.sendMessage(ChatColor.YELLOW + (pending
                        ? "Loaded pending offline order. Changes will apply when the player joins."
                        : "Offline edits will be saved as an order and applied when the player joins."));
            }
        });
    }

    private void saveOfflineChanges(Player viewer, Session session) {
        if (!session.dirty) {
            return;
        }
        session.applyingChanges = true;
        try {
            ItemStack[] contents = session.inventory.getContents();
            if (mode == InventorySeeMode.INVENTORY) {
                contents = contents.clone();
                contents[HELMET_SLOT] = copyNonPlaceholder(contents[HELMET_SLOT]);
                contents[CHESTPLATE_SLOT] = copyNonPlaceholder(contents[CHESTPLATE_SLOT]);
                contents[LEGGINGS_SLOT] = copyNonPlaceholder(contents[LEGGINGS_SLOT]);
                contents[BOOTS_SLOT] = copyNonPlaceholder(contents[BOOTS_SLOT]);
                contents[OFFHAND_SLOT] = copyNonPlaceholder(contents[OFFHAND_SLOT]);
                for (int slot : UNUSED_INVENTORY_SLOTS) {
                    contents[slot] = null;
                }
            }
            pendingOrders.save(session.targetId, mode, contents);
            session.dirty = false;
            if (!session.saveNotified) {
                viewer.sendMessage(ChatColor.GREEN + "Saved offline order. It will apply when the player joins.");
                session.saveNotified = true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[InvSee] Could not save offline playerdata: " + e.getMessage());
            viewer.sendMessage(ChatColor.RED + "Could not save offline playerdata.");
        } finally {
            session.applyingChanges = false;
        }
    }

    private void applyPlayerInventory(PlayerInventory playerInventory, ItemStack[] contents) {
        ItemStack[] storage = new ItemStack[36];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = copyNonPlaceholder(contents[STORAGE_START + i]);
        }
        playerInventory.setStorageContents(storage);
        playerInventory.setHelmet(copyNonPlaceholder(contents[HELMET_SLOT]));
        playerInventory.setChestplate(copyNonPlaceholder(contents[CHESTPLATE_SLOT]));
        playerInventory.setLeggings(copyNonPlaceholder(contents[LEGGINGS_SLOT]));
        playerInventory.setBoots(copyNonPlaceholder(contents[BOOTS_SLOT]));
        playerInventory.setItemInOffHand(emptyToAir(copyNonPlaceholder(contents[OFFHAND_SLOT])));
    }

    private ItemStack[] trim(ItemStack[] source, int size) {
        ItemStack[] result = new ItemStack[size];
        for (int i = 0; i < result.length && i < source.length; i++) {
            result[i] = copy(source[i]);
        }
        return result;
    }

    private ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private ItemStack copyNonPlaceholder(ItemStack item) {
        if (item == null || isPlaceholder(item)) {
            return null;
        }
        return item.clone();
    }

    private ItemStack emptyToAir(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    private void setSlotPlaceholderIfEmpty(ItemStack[] contents, int slot, String label) {
        if (contents[slot] == null || contents[slot].getType() == Material.AIR) {
            contents[slot] = placeholder(Material.PURPLE_STAINED_GLASS_PANE, label);
        }
    }

    private void setOffhandPlaceholderIfEmpty(ItemStack[] contents) {
        if (contents[OFFHAND_SLOT] == null || contents[OFFHAND_SLOT].getType() == Material.AIR) {
            contents[OFFHAND_SLOT] = placeholder(Material.BARRIER, "Offhand Slot");
        }
    }

    private void setUnusedInventorySlots(ItemStack[] contents) {
        for (int slot : UNUSED_INVENTORY_SLOTS) {
            contents[slot] = placeholder(Material.GRAY_STAINED_GLASS_PANE, "Unused Slot");
        }
    }

    private ItemStack placeholder(Material material, String label) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + label);
            meta.getPersistentDataContainer().set(placeholderKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(placeholderKey, PersistentDataType.BYTE);
    }

    private boolean isHelmet(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_SKULL")
                || name.endsWith("_HEAD")
                || material == Material.CARVED_PUMPKIN;
    }

    private boolean isChestplate(Material material) {
        return material.name().endsWith("_CHESTPLATE") || material == Material.ELYTRA;
    }

    private boolean isLeggings(Material material) {
        return material.name().endsWith("_LEGGINGS");
    }

    private boolean isBoots(Material material) {
        return material.name().endsWith("_BOOTS");
    }

    private static final class Session {
        private final UUID viewerId;
        private final UUID targetId;
        private final Inventory inventory;
        private final boolean readOnly;
        private final boolean offline;
        private ScheduledTask refreshTask;
        private volatile boolean applyingChanges;
        private boolean dirty;
        private boolean saveNotified;

        private Session(UUID viewerId, UUID targetId, Inventory inventory, boolean readOnly) {
            this(viewerId, targetId, inventory, readOnly, false);
        }

        private Session(UUID viewerId, UUID targetId, Inventory inventory, boolean readOnly, boolean offline) {
            this.viewerId = viewerId;
            this.targetId = targetId;
            this.inventory = inventory;
            this.readOnly = readOnly;
            this.offline = offline;
        }

        private void cancel() {
            if (refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
        }
    }
}
