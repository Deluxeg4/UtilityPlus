package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OverclockCommand implements TabExecutor {

    private static final Set<String> CURSE = Set.of("vanishing_curse", "binding_curse");
    private static final Set<String> BINARY_ENCHANTS = Set.of(
            "mending", "silk_touch", "infinity", "channeling", "multishot", "aqua_affinity"
    );

    private static final Map<String, String> VANILLA_NAME = Map.ofEntries(
            Map.entry("sharpness", "Sharpness"),
            Map.entry("smite", "Smite"),
            Map.entry("bane_of_arthropods", "Bane of Arthropods"),
            Map.entry("knockback", "Knockback"),
            Map.entry("fire_aspect", "Fire Aspect"),
            Map.entry("looting", "Looting"),
            Map.entry("sweeping_edge", "Sweeping Edge"),
            Map.entry("power", "Power"),
            Map.entry("punch", "Punch"),
            Map.entry("flame", "Flame"),
            Map.entry("infinity", "Infinity"),
            Map.entry("multishot", "Multishot"),
            Map.entry("quick_charge", "Quick Charge"),
            Map.entry("piercing", "Piercing"),
            Map.entry("impaling", "Impaling"),
            Map.entry("riptide", "Riptide"),
            Map.entry("loyalty", "Loyalty"),
            Map.entry("channeling", "Channeling"),
            Map.entry("protection", "Protection"),
            Map.entry("fire_protection", "Fire Protection"),
            Map.entry("blast_protection", "Blast Protection"),
            Map.entry("projectile_protection", "Projectile Protection"),
            Map.entry("thorns", "Thorns"),
            Map.entry("respiration", "Respiration"),
            Map.entry("depth_strider", "Depth Strider"),
            Map.entry("aqua_affinity", "Aqua Affinity"),
            Map.entry("feather_falling", "Feather Falling"),
            Map.entry("soul_speed", "Soul Speed"),
            Map.entry("swift_sneak", "Swift Sneak"),
            Map.entry("efficiency", "Efficiency"),
            Map.entry("silk_touch", "Silk Touch"),
            Map.entry("fortune", "Fortune"),
            Map.entry("luck_of_the_sea", "Luck of the Sea"),
            Map.entry("lure", "Lure"),
            Map.entry("density", "Density"),
            Map.entry("breach", "Breach"),
            Map.entry("wind_burst", "Wind Burst"),
            Map.entry("unbreaking", "Unbreaking"),
            Map.entry("mending", "Mending"),
            Map.entry("vanishing_curse", "Curse of Vanishing"),
            Map.entry("binding_curse", "Curse of Binding")
    );

    private final UtilityPlus plugin;
    private final NamespacedKey dataKey;
    private final NamespacedKey orderKey;

    public OverclockCommand(UtilityPlus plugin) {
        this.plugin = plugin;
        this.dataKey = new NamespacedKey(plugin, "real_enchants");
        this.orderKey = new NamespacedKey(plugin, "enchant_order");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("utilityplus.overclock")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cHold an item in your main hand first.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("32k")) {
            apply32kToItem(item);
            player.sendMessage("§aApplied 32k overclock to your item.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("name")) {
            String name = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                item.setItemMeta(meta);
                player.sendMessage("§aRenamed item to: §r" + name);
            }
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /overclock <enchant> <level> | /overclock 32k | /overclock name <name>");
            return true;
        }

        String enchName = args[0].toLowerCase();
        String levelInput = args[1];
        boolean relativeLevel = levelInput.startsWith("+") || levelInput.startsWith("-");
        int level;
        try {
            level = Integer.parseInt(levelInput);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid level.");
            return true;
        }

        if (!relativeLevel && level < 1) {
            player.sendMessage("§cLevel must be 1 or higher.");
            return true;
        }
        if (relativeLevel && level == 0) {
            player.sendMessage("§cRelative level cannot be 0.");
            return true;
        }

        Enchantment enchant = getEnchant(enchName);
        if (enchant == null) {
            player.sendMessage("§cUnknown enchantment: §e" + enchName);
            return true;
        }

        Map<String, Integer> real = getReal(item);
        List<String> order = getOrder(item);

        int newLevel = relativeLevel ? real.getOrDefault(enchName, 0) + level : level;
        if (newLevel <= 0) {
            real.remove(enchName);
            order.remove(enchName);
            item.removeEnchantment(enchant);
        } else {
            real.put(enchName, newLevel);
            if (!order.contains(enchName)) {
                order.add(enchName);
            }
            item.addUnsafeEnchantment(enchant, Math.min(newLevel, 255));
        }

        saveReal(item, real);
        saveOrder(item, order);
        updateLore(item, real, order);

        if (newLevel <= 0) {
            player.sendMessage("§aRemoved " + enchName + " from your item.");
        } else {
            player.sendMessage("§a" + enchName + " level is now: §e" + newLevel);
        }
        return true;
    }

    private void apply32kToItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName("§b§oAlpha's Stacked 32k's");
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);

        double baseDamage = item.getType().name().contains("NETHERITE") ? 8.0 : 7.0;
        AttributeModifier modifier = new AttributeModifier(
                new NamespacedKey(plugin, "overclock_32k_damage"),
                16391.0 - baseDamage,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier);

        List<String> forcedOrder = Arrays.asList(
                "sharpness", "knockback", "fire_aspect", "looting",
                "sweeping_edge", "unbreaking", "mending", "vanishing_curse"
        );
        meta.getPersistentDataContainer().set(orderKey, PersistentDataType.STRING, String.join(";", forcedOrder));
        meta.getPersistentDataContainer().set(
                dataKey,
                PersistentDataType.STRING,
                "sharpness:32767;knockback:10;fire_aspect:32767;looting:10;sweeping_edge:3;unbreaking:32767;mending:1;vanishing_curse:1;"
        );

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(List.of(
                "§7Sharpness enchantment.level.32767",
                "§7Knockback X",
                "§7Fire Aspect enchantment.level.32767",
                "§7Looting X",
                "§7Sweeping Edge III",
                "§7Unbreaking enchantment.level.32767",
                "§7Mending",
                "§cCurse of Vanishing"
        ));
        item.setItemMeta(meta);

        item.addUnsafeEnchantment(Objects.requireNonNull(getEnchant("sharpness")), 255);
        item.addUnsafeEnchantment(Objects.requireNonNull(getEnchant("knockback")), 10);
        item.addUnsafeEnchantment(Objects.requireNonNull(getEnchant("fire_aspect")), 255);
        item.addUnsafeEnchantment(Objects.requireNonNull(getEnchant("looting")), 10);
        Enchantment sweepingEdge = getEnchant("sweeping_edge");
        if (sweepingEdge != null) {
            item.addUnsafeEnchantment(sweepingEdge, 3);
        }
        item.addUnsafeEnchantment(Objects.requireNonNull(getEnchant("unbreaking")), 255);
        item.addUnsafeEnchantment(Objects.requireNonNull(getEnchant("mending")), 1);
        item.addUnsafeEnchantment(Objects.requireNonNull(getEnchant("vanishing_curse")), 1);
    }

    private void updateLore(ItemStack item, Map<String, Integer> real, List<String> order) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> newLore = new ArrayList<>();
        if (meta.hasLore()) {
            for (String line : Objects.requireNonNull(meta.getLore())) {
                String clean = ChatColor.stripColor(line);
                boolean isEnchantLine = false;
                if (clean.contains("enchantment.level.")) {
                    isEnchantLine = true;
                } else if (clean.matches(".*\\s[IVXLCDM]+$")) {
                    String base = clean.replaceAll("\\s[IVXLCDM]+$", "").trim();
                    if (VANILLA_NAME.containsValue(base)) isEnchantLine = true;
                } else if (VANILLA_NAME.containsValue(clean)) {
                    isEnchantLine = true;
                }
                if (!isEnchantLine) {
                    newLore.add(line);
                }
            }
        }

        for (String key : order) {
            if (!real.containsKey(key)) continue;
            int lvl = real.get(key);
            String name = VANILLA_NAME.getOrDefault(key, key);
            String color = CURSE.contains(key) ? "§c" : "§7";
            boolean hideLevelOne = BINARY_ENCHANTS.contains(key) || CURSE.contains(key);
            if (lvl == 1 && hideLevelOne) {
                newLore.add(color + name);
            } else if (lvl <= 10) {
                newLore.add(color + name + " " + toRoman(lvl));
            } else {
                newLore.add(color + name + " enchantment.level." + lvl);
            }
        }

        meta.setLore(newLore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private Map<String, Integer> getReal(ItemStack item) {
        Map<String, Integer> map = new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return map;

        String raw = meta.getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
        if (raw != null) {
            for (String part : raw.split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    try {
                        map.put(kv[0], Integer.parseInt(kv[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return map;
    }

    private void saveReal(ItemStack item, Map<String, Integer> map) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, sb.toString());
        item.setItemMeta(meta);
    }

    private List<String> getOrder(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new ArrayList<>();

        String raw = meta.getPersistentDataContainer().get(orderKey, PersistentDataType.STRING);
        return raw == null || raw.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(raw.split(";")));
    }

    private void saveOrder(ItemStack item, List<String> list) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(orderKey, PersistentDataType.STRING, String.join(";", new LinkedHashSet<>(list)));
        item.setItemMeta(meta);
    }

    private Enchantment getEnchant(String name) {
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.getKey().getKey().equalsIgnoreCase(name)) {
                return enchantment;
            }
        }
        return null;
    }

    private String toRoman(int number) {
        if (number <= 0) return "";
        String[] romans = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                result.append(romans[i]);
            }
        }
        return result.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if ("32k".startsWith(args[0].toLowerCase())) list.add("32k");
            if ("name".startsWith(args[0].toLowerCase())) list.add("name");
            for (Enchantment enchantment : Enchantment.values()) {
                String name = enchantment.getKey().getKey();
                if (name.startsWith(args[0].toLowerCase())) {
                    list.add(name);
                }
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("name") && !args[0].equalsIgnoreCase("32k")) {
            list.addAll(Arrays.asList("1", "10", "100", "32767"));
        }
        return list;
    }
}
