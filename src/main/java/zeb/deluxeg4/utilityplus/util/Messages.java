package zeb.deluxeg4.utilityplus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern LEGACY_COLOR_CODE = Pattern.compile("&[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static JavaPlugin plugin;

    private Messages() {
    }

    public static void initialize(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        copyMessageDefaults();
    }

    public static void copyMessageDefaults() {
        if (plugin == null) return;

        migrateLegacyConfig();

        InputStream resource = plugin.getResource("config.yml");
        if (resource == null) return;

        boolean changed = false;
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "config.yml"));
        try (InputStream input = resource;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            ConfigurationSection defaults = YamlConfiguration.loadConfiguration(reader).getConfigurationSection("messages");
            if (defaults == null) return;

            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key)) continue;
                String path = "messages." + key;
                if (!diskConfig.isSet(path)) {
                    plugin.getConfig().set(path, defaults.get(key));
                    changed = true;
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not load default message settings: " + exception.getMessage());
        }

        if (changed) plugin.saveConfig();
    }

    public static String config(String path, String fallback) {
        return plugin == null ? fallback : plugin.getConfig().getString("messages." + path, fallback);
    }

    public static Component parse(String message) {
        return PlayerChatNames.decorate(component(message));
    }

    public static Component component(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static void sendUndecorated(CommandSender sender, String message) {
        sender.sendMessage(MINI_MESSAGE.deserialize(message));
    }

    private static void migrateLegacyConfig() {
        boolean changed = false;
        for (String key : plugin.getConfig().getKeys(true)) {
            if (plugin.getConfig().isConfigurationSection(key)) continue;
            String value = plugin.getConfig().getString(key);
            if (value == null || !LEGACY_COLOR_CODE.matcher(value).find()) continue;

            Component component = LEGACY_AMPERSAND.deserialize(value);
            plugin.getConfig().set(key, MINI_MESSAGE.serialize(component));
            changed = true;
        }
        if (changed) plugin.saveConfig();
    }
}
