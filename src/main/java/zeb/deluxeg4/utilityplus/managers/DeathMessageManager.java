package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DeathMessageManager {

    private final UtilityPlus plugin;
    private final Map<String, List<String>> messages = new HashMap<>();

    public DeathMessageManager(UtilityPlus plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        messages.clear();
        File file = new File(plugin.getDataFolder(), "message.json");
        if (!file.exists()) {
            plugin.saveResource("message.json", false);
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            load(JsonParser.parseReader(reader).getAsJsonObject());
            return;
        } catch (Exception e) {
            plugin.getLogger().warning("[DeathMessageManager] Could not load message.json from data folder, using bundled defaults.");
        }

        try (InputStream stream = plugin.getResource("message.json")) {
            if (stream == null) {
                plugin.getLogger().warning("[DeathMessageManager] Bundled message.json is missing.");
                return;
            }
            load(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException e) {
            plugin.getLogger().warning("[DeathMessageManager] Could not load bundled message.json.");
        }
    }

    public String getMessage(PlayerDeathEvent event, Player victim, Player killer, boolean selfKillCommand) {
        String category = resolveCategory(event, victim, killer, selfKillCommand);
        String template = randomTemplate(category);
        if (template == null && !"generic".equals(category)) {
            template = randomTemplate("generic");
        }
        if (template == null) {
            return fallbackMessage(victim, killer, selfKillCommand);
        }

        // Skip weapon in text if using custom weapon category - weapon will be shown as hover component instead
        boolean skipWeapon = "player_using_item".equals(category);
        return applyPlaceholders(template, victim, killer, skipWeapon);
    }

    private void load(JsonObject root) {
        Gson gson = new Gson();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            List<String> list = new ArrayList<>();
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    list.add(element.getAsString());
                }
            }
            if (!list.isEmpty()) {
                messages.put(entry.getKey().toLowerCase(Locale.ROOT), list);
            }
        }
    }

    private String resolveCategory(PlayerDeathEvent event, Player victim, Player killer, boolean selfKillCommand) {
        if (selfKillCommand) {
            return "command_kill";
        }

        if (killer != null) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (hasCustomName(weapon) && messages.containsKey("player_using_item")) {
                return "player_using_item";
            }
            return "player_kill";
        }

        EntityDamageEvent damageEvent = victim.getLastDamageCause();
        if (damageEvent == null) {
            return "unknown";
        }

        EntityType killerType = event.getDamageSource().getCausingEntity() != null
                ? event.getDamageSource().getCausingEntity().getType()
                : null;
        String entityCategory = categoryForEntity(killerType);
        if (entityCategory != null) {
            return entityCategory;
        }

        return categoryForCause(damageEvent.getCause());
    }

    private String categoryForEntity(EntityType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case ZOMBIE, HUSK, DROWNED, ZOMBIE_VILLAGER -> "zombie";
            case SKELETON, STRAY -> "skeleton";
            case CREEPER -> "creeper";
            case SPIDER, CAVE_SPIDER -> "spider";
            case ENDERMAN -> "enderman";
            case WARDEN -> "warden";
            case IRON_GOLEM -> "iron_golem";
            case PIGLIN, PIGLIN_BRUTE, ZOMBIFIED_PIGLIN -> "piglin";
            case HOGLIN -> "hoglin";
            case ZOGLIN -> "zoglin";
            case GUARDIAN, ELDER_GUARDIAN -> "guardian";
            case BEE -> "bee";
            case WOLF -> "wolf";
            case GOAT -> "goat";
            case BREEZE -> "breeze";
            case BOGGED -> "bogged";
            case ENDER_DRAGON -> "dragon";
            case WITHER -> "wither_boss";
            case ARROW, SPECTRAL_ARROW -> "arrow";
            case TRIDENT -> "trident";
            case FIREBALL, SMALL_FIREBALL, DRAGON_FIREBALL -> "fireball";
            case WIND_CHARGE, BREEZE_WIND_CHARGE -> "wind_charge";
            case ENDER_PEARL -> "ender_pearl";
            case TNT, TNT_MINECART -> "tnt";
            case END_CRYSTAL -> "crystal";
            case MINECART, CHEST_MINECART, COMMAND_BLOCK_MINECART, FURNACE_MINECART, HOPPER_MINECART, SPAWNER_MINECART -> "minecart";
            case BOAT, CHEST_BOAT -> "boat_crash";
            default -> null;
        };
    }

    private String categoryForCause(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case CONTACT -> "contact";
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> "mob_kill";
            case PROJECTILE -> "projectile";
            case SUFFOCATION -> "suffocation";
            case FALL -> "fall";
            case FIRE -> "fire";
            case FIRE_TICK -> "fire";
            case MELTING, FREEZE -> "freeze";
            case LAVA -> "lava";
            case DROWNING -> "drowning";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            case VOID -> "void";
            case LIGHTNING -> "lightning";
            case SUICIDE -> "suicide";
            case STARVATION -> "starvation";
            case POISON -> "poison";
            case MAGIC -> "magic";
            case WITHER -> "wither";
            case FALLING_BLOCK -> "falling_block";
            case THORNS -> "thorns";
            case DRAGON_BREATH -> "dragon";
            case CUSTOM -> "custom";
            case FLY_INTO_WALL -> "fly_into_wall";
            case HOT_FLOOR -> "hot_floor";
            case CRAMMING -> "cramming";
            case DRYOUT -> "drowning";
            case SONIC_BOOM -> "sonic_boom";
            case WORLD_BORDER -> "world_border";
            default -> "generic";
        };
    }

    private String randomTemplate(String category) {
        List<String> list = messages.get(category.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private String applyPlaceholders(String template, Player victim, Player killer, boolean skipWeapon) {
        String weapon = killer != null ? weaponName(killer.getInventory().getItemInMainHand()) : "";
        return template
                .replace("%victim%", victim.getName())
                .replace("%killer%", killer != null ? killer.getName() : "the server")
                .replace("%weapon%", skipWeapon ? "" : weapon)
                .replace("%world%", victim.getWorld().getName())
                .replace("%x%", String.valueOf(victim.getLocation().getBlockX()))
                .replace("%y%", String.valueOf(victim.getLocation().getBlockY()))
                .replace("%z%", String.valueOf(victim.getLocation().getBlockZ()));
    }

    private String fallbackMessage(Player victim, Player killer, boolean selfKillCommand) {
        if (selfKillCommand) {
            return victim.getName() + " ended their life";
        }
        if (killer != null) {
            return victim.getName() + " was slain by " + killer.getName();
        }
        return victim.getName() + " died";
    }

    private boolean hasCustomName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName();
    }

    private String weaponName(ItemStack item) {
        if (hasCustomName(item)) {
            return item.getItemMeta().getDisplayName();
        }
        if (item == null || item.getType() == Material.AIR) {
            return "fists";
        }
        return item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}