package zeb.deluxeg4.utilityplus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Messages {

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static JavaPlugin plugin;

    private Messages() {
    }

    public static void initialize(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        copyMessageDefaults();
    }

    public static void copyMessageDefaults() {
        if (plugin == null) return;

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

    public static Component legacy(String message) {
        return PlayerChatNames.decorate(LEGACY_AMPERSAND.deserialize(message));
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(legacy(message));
    }
}
