package zeb.deluxeg4.utilityplus.invsee;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import zeb.deluxeg4.utilityplus.UtilityPlus;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class PendingInventoryOrderManager {
    private static final int INVENTORY_SIZE = 45;
    private static final int ENDER_CHEST_SIZE = 27;
    private static final int STORAGE_START = 0;
    private static final int BOOTS_SLOT = 36;
    private static final int LEGGINGS_SLOT = 37;
    private static final int CHESTPLATE_SLOT = 38;
    private static final int HELMET_SLOT = 39;
    private static final int OFFHAND_SLOT = 40;

    private final UtilityPlus plugin;
    private final File folder;

    public PendingInventoryOrderManager(UtilityPlus plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "offline-invsee-orders");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("[InvSee] Could not create pending order folder.");
        }
    }

    public ItemStack[] load(UUID uuid, InventorySeeMode mode) {
        File file = file(uuid);
        if (!file.isFile()) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = path(mode);
        if (!config.isConfigurationSection(path)) {
            return null;
        }

        int size = mode == InventorySeeMode.INVENTORY ? INVENTORY_SIZE : ENDER_CHEST_SIZE;
        int savedSize = config.getInt(path + ".size", size);
        if (mode == InventorySeeMode.INVENTORY && savedSize == 54) {
            return migrateOldInventoryLayout(config, path);
        }

        ItemStack[] contents = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            contents[i] = config.getItemStack(path + ".slots." + i);
        }
        return contents;
    }

    public void save(UUID uuid, InventorySeeMode mode, ItemStack[] contents) throws IOException {
        File file = file(uuid);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = path(mode);
        config.set(path, null);
        config.set(path + ".size", contents.length);
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            config.set(path + ".slots." + i, item == null || item.getType() == Material.AIR ? null : item);
        }
        config.save(file);
    }

    public boolean apply(Player player) {
        UUID uuid = player.getUniqueId();
        File file = file(uuid);
        if (!file.isFile()) {
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean applied = false;
        if (config.isConfigurationSection(path(InventorySeeMode.INVENTORY))) {
            applyPlayerInventory(player.getInventory(), load(uuid, InventorySeeMode.INVENTORY));
            config.set(path(InventorySeeMode.INVENTORY), null);
            applied = true;
        }
        if (config.isConfigurationSection(path(InventorySeeMode.ENDER_CHEST))) {
            player.getEnderChest().setContents(trim(load(uuid, InventorySeeMode.ENDER_CHEST), ENDER_CHEST_SIZE));
            config.set(path(InventorySeeMode.ENDER_CHEST), null);
            applied = true;
        }

        if (!applied) {
            return false;
        }

        try {
            if (config.getKeys(false).isEmpty()) {
                if (!file.delete()) {
                    plugin.getLogger().warning("[InvSee] Could not delete applied pending order: " + file.getName());
                }
            } else {
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[InvSee] Could not update pending order after apply: " + e.getMessage());
        }
        return true;
    }

    private void applyPlayerInventory(PlayerInventory playerInventory, ItemStack[] contents) {
        if (contents == null) {
            return;
        }
        ItemStack[] storage = new ItemStack[36];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = copy(slot(contents, STORAGE_START + i));
        }
        playerInventory.setStorageContents(storage);
        playerInventory.setHelmet(copy(slot(contents, HELMET_SLOT)));
        playerInventory.setChestplate(copy(slot(contents, CHESTPLATE_SLOT)));
        playerInventory.setLeggings(copy(slot(contents, LEGGINGS_SLOT)));
        playerInventory.setBoots(copy(slot(contents, BOOTS_SLOT)));
        playerInventory.setItemInOffHand(emptyToAir(slot(contents, OFFHAND_SLOT)));
    }

    private ItemStack slot(ItemStack[] contents, int slot) {
        if (contents == null || slot < 0 || slot >= contents.length) {
            return null;
        }
        return contents[slot];
    }

    private ItemStack[] migrateOldInventoryLayout(YamlConfiguration config, String path) {
        ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < 36; i++) {
            contents[i] = config.getItemStack(path + ".slots." + (9 + i));
        }
        contents[BOOTS_SLOT] = config.getItemStack(path + ".slots.3");
        contents[LEGGINGS_SLOT] = config.getItemStack(path + ".slots.2");
        contents[CHESTPLATE_SLOT] = config.getItemStack(path + ".slots.1");
        contents[HELMET_SLOT] = config.getItemStack(path + ".slots.0");
        contents[OFFHAND_SLOT] = config.getItemStack(path + ".slots.4");
        return contents;
    }

    private ItemStack[] trim(ItemStack[] source, int size) {
        ItemStack[] result = new ItemStack[size];
        if (source == null) {
            return result;
        }
        for (int i = 0; i < result.length && i < source.length; i++) {
            result[i] = copy(source[i]);
        }
        return result;
    }

    private ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private ItemStack emptyToAir(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    private String path(InventorySeeMode mode) {
        return mode == InventorySeeMode.INVENTORY ? "inventory" : "ender-chest";
    }
}
