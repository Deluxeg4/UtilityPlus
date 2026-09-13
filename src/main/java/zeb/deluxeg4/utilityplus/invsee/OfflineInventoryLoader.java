package zeb.deluxeg4.utilityplus.invsee;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class OfflineInventoryLoader {
    private OfflineInventoryLoader() {
    }

    static ItemStack[] load(UUID uuid, InventorySeeMode mode) throws IOException {
        File file = playerDataFile(uuid);
        if (file == null || !file.isFile()) {
            return null;
        }

        Object root;
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            root = readNamedTag(input);
        }

        if (!(root instanceof Map<?, ?> rootMap)) {
            return null;
        }

        return mode == InventorySeeMode.INVENTORY
                ? readInventory(rootMap)
                : readEnderChest(rootMap);
    }

    static void save(UUID uuid, InventorySeeMode mode, ItemStack[] contents) throws IOException {
        File file = playerDataFile(uuid);
        if (file == null || !file.isFile()) {
            throw new IOException("playerdata file not found");
        }

        Object root;
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            root = readNamedTag(input);
        }

        if (!(root instanceof Map<?, ?> rawRoot)) {
            throw new IOException("invalid playerdata root");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rootMap = (Map<String, Object>) rawRoot;
        if (mode == InventorySeeMode.INVENTORY) {
            rootMap.put("Inventory", writeInventory(contents));
        } else {
            rootMap.put("EnderItems", writeEnderChest(contents));
        }

        File tempFile = new File(file.getParentFile(), file.getName() + ".utilityplus.tmp");
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile)))) {
            writeNamedRoot(output, rootMap);
        }

        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static File playerDataFile(UUID uuid) {
        World world = Bukkit.getWorlds().stream()
                .filter(candidate -> candidate.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElseGet(() -> Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0));
        if (world == null) {
            return null;
        }
        return new File(new File(world.getWorldFolder(), "playerdata"), uuid + ".dat");
    }

    private static ItemStack[] readInventory(Map<?, ?> root) {
        ItemStack[] contents = new ItemStack[45];
        Object inventory = root.get("Inventory");
        if (!(inventory instanceof List<?> items)) {
            return contents;
        }

        for (Object itemObject : items) {
            if (!(itemObject instanceof Map<?, ?> itemTag)) {
                continue;
            }

            int slot = number(itemTag.get("Slot"), -999);
            ItemStack item = itemStack(itemTag);
            if (item == null) {
                continue;
            }

            if (slot >= 0 && slot <= 35) {
                contents[slot] = item;
            } else if (slot == 103) {
                contents[39] = item;
            } else if (slot == 102) {
                contents[38] = item;
            } else if (slot == 101) {
                contents[37] = item;
            } else if (slot == 100) {
                contents[36] = item;
            } else if (slot == -106 || slot == 150) {
                contents[40] = item;
            } else if (slot == 39) {
                contents[39] = item;
            } else if (slot == 38) {
                contents[38] = item;
            } else if (slot == 37) {
                contents[37] = item;
            } else if (slot == 36) {
                contents[36] = item;
            } else if (slot == 40) {
                contents[40] = item;
            }
        }

        readArmorItems(root, contents);
        readHandItems(root, contents);
        readEquipment(root, contents);
        return contents;
    }

    private static void readArmorItems(Map<?, ?> root, ItemStack[] contents) {
        Object armorItems = firstPresent(root, "ArmorItems", "armor_items", "armorItems");
        if (!(armorItems instanceof List<?> items)) {
            return;
        }
        setIfEmpty(contents, 36, itemAt(items, 0)); // boots
        setIfEmpty(contents, 37, itemAt(items, 1)); // leggings
        setIfEmpty(contents, 38, itemAt(items, 2)); // chestplate
        setIfEmpty(contents, 39, itemAt(items, 3)); // helmet
    }

    private static void readHandItems(Map<?, ?> root, ItemStack[] contents) {
        Object handItems = firstPresent(root, "HandItems", "hand_items", "handItems");
        if (!(handItems instanceof List<?> items)) {
            return;
        }
        setIfEmpty(contents, 40, itemAt(items, 1));
    }

    private static void readEquipment(Map<?, ?> root, ItemStack[] contents) {
        Object equipment = firstPresent(root, "equipment", "Equipment");
        if (!(equipment instanceof Map<?, ?> equipmentTag)) {
            return;
        }
        setIfEmpty(contents, 39, itemStackFromObject(firstPresent(equipmentTag, "head", "helmet", "HEAD", "HELMET")));
        setIfEmpty(contents, 38, itemStackFromObject(firstPresent(equipmentTag, "chest", "chestplate", "CHEST", "CHESTPLATE")));
        setIfEmpty(contents, 37, itemStackFromObject(firstPresent(equipmentTag, "legs", "leggings", "LEGS", "LEGGINGS")));
        setIfEmpty(contents, 36, itemStackFromObject(firstPresent(equipmentTag, "feet", "boots", "FEET", "BOOTS")));
        setIfEmpty(contents, 40, itemStackFromObject(firstPresent(equipmentTag, "offhand", "off_hand", "OFFHAND", "OFF_HAND")));
    }

    private static ItemStack itemAt(List<?> items, int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }
        return itemStackFromObject(items.get(index));
    }

    private static ItemStack itemStackFromObject(Object value) {
        return value instanceof Map<?, ?> itemTag ? itemStack(itemTag) : null;
    }

    private static void setIfEmpty(ItemStack[] contents, int slot, ItemStack item) {
        if (item == null) {
            return;
        }
        if (contents[slot] == null || contents[slot].getType() == Material.AIR || contents[slot].isEmpty()) {
            contents[slot] = item;
        }
    }

    private static Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static ItemStack[] readEnderChest(Map<?, ?> root) {
        ItemStack[] contents = new ItemStack[27];
        Object enderItems = root.get("EnderItems");
        if (!(enderItems instanceof List<?> items)) {
            return contents;
        }

        for (Object itemObject : items) {
            if (!(itemObject instanceof Map<?, ?> itemTag)) {
                continue;
            }
            int slot = number(itemTag.get("Slot"), -1);
            if (slot < 0 || slot >= contents.length) {
                continue;
            }
            contents[slot] = itemStack(itemTag);
        }

        return contents;
    }

    private static ItemStack itemStack(Map<?, ?> itemTag) {
        Object idValue = itemTag.get("id");
        if (!(idValue instanceof String id)) {
            return null;
        }

        String materialName = id.replace("minecraft:", "")
                .toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
        Material material = Material.matchMaterial(materialName);
        if (material == null || material == Material.AIR) {
            return null;
        }

        int count = number(itemTag.get("count"), number(itemTag.get("Count"), 1));
        count = Math.max(1, Math.min(material.getMaxStackSize(), count));
        return new ItemStack(material, count);
    }

    private static List<Object> writeInventory(ItemStack[] contents) {
        List<Object> items = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            addItem(items, contents, slot, (byte) slot);
        }
        addItem(items, contents, 39, (byte) 103);
        addItem(items, contents, 38, (byte) 102);
        addItem(items, contents, 37, (byte) 101);
        addItem(items, contents, 36, (byte) 100);
        addItem(items, contents, 40, (byte) -106);
        return items;
    }

    private static List<Object> writeEnderChest(ItemStack[] contents) {
        List<Object> items = new ArrayList<>();
        for (int slot = 0; slot < Math.min(27, contents.length); slot++) {
            addItem(items, contents, slot, (byte) slot);
        }
        return items;
    }

    private static void addItem(List<Object> items, ItemStack[] contents, int inventorySlot, byte dataSlot) {
        if (inventorySlot < 0 || inventorySlot >= contents.length) {
            return;
        }
        ItemStack item = contents[inventorySlot];
        if (item == null || item.getType() == Material.AIR || item.isEmpty()) {
            return;
        }

        Map<String, Object> tag = new HashMap<>();
        tag.put("Slot", dataSlot);
        tag.put("id", item.getType().getKey().asString());
        tag.put("count", Math.max(1, Math.min(99, item.getAmount())));
        items.add(tag);
    }

    private static int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static Object readNamedTag(DataInputStream input) throws IOException {
        int type = input.readUnsignedByte();
        if (type == 0) {
            return null;
        }
        input.readUTF();
        return readPayload(input, type);
    }

    private static void writeNamedRoot(DataOutputStream output, Map<String, Object> root) throws IOException {
        output.writeByte(10);
        writeString(output, "");
        writeCompoundPayload(output, root);
    }

    private static Object readPayload(DataInputStream input, int type) throws IOException {
        return switch (type) {
            case 1 -> input.readByte();
            case 2 -> input.readShort();
            case 3 -> input.readInt();
            case 4 -> input.readLong();
            case 5 -> input.readFloat();
            case 6 -> input.readDouble();
            case 7 -> readByteArray(input);
            case 8 -> readString(input);
            case 9 -> readList(input);
            case 10 -> readCompound(input);
            case 11 -> readIntArray(input);
            case 12 -> readLongArray(input);
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        };
    }

    private static byte[] readByteArray(DataInputStream input) throws IOException {
        int length = input.readInt();
        byte[] bytes = new byte[Math.max(0, length)];
        input.readFully(bytes);
        return bytes;
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static List<Object> readList(DataInputStream input) throws IOException {
        int childType = input.readUnsignedByte();
        int length = input.readInt();
        List<Object> list = new ArrayList<>(Math.max(0, length));
        for (int i = 0; i < length; i++) {
            list.add(readPayload(input, childType));
        }
        return list;
    }

    private static Map<String, Object> readCompound(DataInputStream input) throws IOException {
        Map<String, Object> compound = new HashMap<>();
        while (true) {
            int type;
            try {
                type = input.readUnsignedByte();
            } catch (EOFException e) {
                break;
            }
            if (type == 0) {
                break;
            }
            String name = readString(input);
            compound.put(name, readPayload(input, type));
        }
        return compound;
    }

    private static int[] readIntArray(DataInputStream input) throws IOException {
        int length = input.readInt();
        int[] values = new int[Math.max(0, length)];
        for (int i = 0; i < values.length; i++) {
            values[i] = input.readInt();
        }
        return values;
    }

    private static long[] readLongArray(DataInputStream input) throws IOException {
        int length = input.readInt();
        long[] values = new long[Math.max(0, length)];
        for (int i = 0; i < values.length; i++) {
            values[i] = input.readLong();
        }
        return values;
    }

    private static void writePayload(DataOutputStream output, int type, Object value) throws IOException {
        switch (type) {
            case 1 -> output.writeByte(((Number) value).byteValue());
            case 2 -> output.writeShort(((Number) value).shortValue());
            case 3 -> output.writeInt(((Number) value).intValue());
            case 4 -> output.writeLong(((Number) value).longValue());
            case 5 -> output.writeFloat(((Number) value).floatValue());
            case 6 -> output.writeDouble(((Number) value).doubleValue());
            case 7 -> writeByteArray(output, (byte[]) value);
            case 8 -> writeString(output, (String) value);
            case 9 -> writeListPayload(output, (List<?>) value);
            case 10 -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> compound = (Map<String, Object>) value;
                writeCompoundPayload(output, compound);
            }
            case 11 -> writeIntArray(output, (int[]) value);
            case 12 -> writeLongArray(output, (long[]) value);
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        }
    }

    private static void writeCompoundPayload(DataOutputStream output, Map<String, Object> compound) throws IOException {
        for (Map.Entry<String, Object> entry : compound.entrySet()) {
            Object value = entry.getValue();
            int type = typeOf(value);
            if (type == 0) {
                continue;
            }
            output.writeByte(type);
            writeString(output, entry.getKey());
            writePayload(output, type, value);
        }
        output.writeByte(0);
    }

    private static void writeListPayload(DataOutputStream output, List<?> list) throws IOException {
        int childType = list.isEmpty() ? 0 : typeOf(list.get(0));
        output.writeByte(childType);
        output.writeInt(list.size());
        for (Object element : list) {
            writePayload(output, childType, element);
        }
    }

    private static void writeByteArray(DataOutputStream output, byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static void writeIntArray(DataOutputStream output, int[] values) throws IOException {
        output.writeInt(values.length);
        for (int value : values) {
            output.writeInt(value);
        }
    }

    private static void writeLongArray(DataOutputStream output, long[] values) throws IOException {
        output.writeInt(values.length);
        for (long value : values) {
            output.writeLong(value);
        }
    }

    private static int typeOf(Object value) {
        if (value instanceof Byte) return 1;
        if (value instanceof Short) return 2;
        if (value instanceof Integer) return 3;
        if (value instanceof Long) return 4;
        if (value instanceof Float) return 5;
        if (value instanceof Double) return 6;
        if (value instanceof byte[]) return 7;
        if (value instanceof String) return 8;
        if (value instanceof List<?>) return 9;
        if (value instanceof Map<?, ?>) return 10;
        if (value instanceof int[]) return 11;
        if (value instanceof long[]) return 12;
        return 0;
    }
}
