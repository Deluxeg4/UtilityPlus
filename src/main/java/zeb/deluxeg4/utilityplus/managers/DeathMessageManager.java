package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
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
    private final Map<String, List<String>> simpleMessages = createSimpleMessages();

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

        return applyPlaceholders(template, victim, killer);
    }

    public Player resolveKiller(PlayerDeathEvent event, Player victim) {
        Player killer = victim.getKiller();
        if (killer != null) {
            return killer;
        }

        Entity causingEntity = event.getDamageSource().getCausingEntity();
        if (causingEntity instanceof Player player) {
            return player;
        }

        return null;
    }

    public ItemStack getKillerItem(Player killer) {
        if (killer == null) {
            return null;
        }

        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        return item;
    }

    public String getSimpleCauseName(PlayerDeathEvent event, Player victim, Player killer) {
        if (killer == null || getKillerItem(killer) != null) {
            return null;
        }

        Entity direct = event.getDamageSource().getDirectEntity();
        if (direct != null) {
            String entityName = simpleNameForEntity(direct.getType());
            if (entityName != null) {
                return entityName;
            }
        }

        return null;
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

        String damageTypeCategory = categoryForDamageType(event.getDamageSource().getDamageType());
        if (damageTypeCategory != null) {
            return damageTypeCategory;
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

    private String categoryForDamageType(DamageType type) {
        if (type == null) {
            return null;
        }

        NamespacedKey key = type.getKey();
        String name = key.getKey();
        return switch (name) {
            case "bad_respawn_point" -> "bad_respawn_point";
            case "lava" -> "lava";
            case "in_fire", "on_fire" -> "fire";
            case "campfire" -> "campfire";
            case "hot_floor" -> "hot_floor";
            case "explosion", "player_explosion" -> "explosion";
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
        List<String> simpleList = simpleMessages.get(category.toLowerCase(Locale.ROOT));
        if (simpleList != null && !simpleList.isEmpty()) {
            return simpleList.get(ThreadLocalRandom.current().nextInt(simpleList.size()));
        }

        List<String> list = messages.get(category.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private String applyPlaceholders(String template, Player victim, Player killer) {
        String weapon = killer != null ? weaponName(killer.getInventory().getItemInMainHand()) : "";
        return template
                .replace("%victim%", victim.getName())
                .replace("%killer%", killer != null ? killer.getName() : "the server")
                .replace("%weapon%", weapon)
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

    private String simpleNameForEntity(EntityType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case END_CRYSTAL -> "end crystal";
            case TNT, TNT_MINECART -> "TNT";
            case FIREBALL, SMALL_FIREBALL, DRAGON_FIREBALL -> "fireball";
            case ARROW, SPECTRAL_ARROW -> "arrow";
            case TRIDENT -> "trident";
            case WIND_CHARGE, BREEZE_WIND_CHARGE -> "wind charge";
            default -> null;
        };
    }

    private String simpleNameForDamageType(DamageType type) {
        if (type == null) {
            return null;
        }

        String name = type.getKey().getKey();
        return switch (name) {
            case "bad_respawn_point" -> "bed or respawn anchor";
            case "lava" -> "lava";
            case "in_fire", "on_fire" -> "fire";
            case "campfire" -> "campfire";
            case "hot_floor" -> "magma block";
            case "explosion", "player_explosion" -> "explosion";
            default -> null;
        };
    }

    private String simpleNameForCause(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case LAVA -> "lava";
            case FIRE, FIRE_TICK -> "fire";
            case HOT_FLOOR -> "magma block";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            case PROJECTILE -> "projectile";
            default -> null;
        };
    }

    private Map<String, List<String>> createSimpleMessages() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("generic", List.of(
                "%victim% died like a newfriend",
                "%victim% got sent back to spawn",
                "%victim% lost the survival test",
                "%victim% got removed from the map",
                "%victim% became loot on the floor",
                "%victim% got skill checked",
                "%victim% ran out of luck",
                "%victim% got clowned by the server",
                "%victim% got put in the dirt",
                "%victim% folded under pressure"
        ));
        map.put("unknown", List.of(
                "%victim% died to weird server stuff",
                "%victim% got killed by minecraft nonsense",
                "%victim% got deleted by something stupid",
                "%victim% died and nobody knows why"
        ));
        map.put("player_kill", List.of(
                "%victim% got dropped by %killer%",
                "%victim% got packed by %killer%",
                "%killer% sent %victim% back to spawn",
                "%victim% got folded by %killer%",
                "%killer% smoked %victim%",
                "%killer% ran through %victim%",
                "%victim% got farmed by %killer%",
                "%victim% got owned by %killer%",
                "%killer% turned %victim% into loot",
                "%victim% got rolled by %killer%",
                "%killer% made %victim% look free",
                "%victim% got put down by %killer%",
                "%killer% packed %victim% up",
                "%victim% lost the 1v1 to %killer%",
                "%killer% clipped %victim%",
                "%victim% got sent to spawn by %killer%",
                "%killer% ended %victim%'s run",
                "%victim% got cleaned by %killer%",
                "%killer% made %victim% drop"
        ));
        map.put("player_using_item", List.of(
                "%victim% got dropped by %killer%",
                "%victim% got packed by %killer%",
                "%killer% folded %victim%",
                "%killer% smoked %victim%",
                "%victim% got rolled by %killer%",
                "%killer% made %victim% drop",
                "%victim% got deleted by %killer%",
                "%killer% sent %victim% back to spawn",
                "%victim% got farmed by %killer%",
                "%killer% turned %victim% into loot",
                "%victim% got clipped by %killer%",
                "%killer% packed %victim% up"
        ));
        map.put("mob_kill", List.of(
                "%victim% lost to a mob",
                "%victim% got farmed by PvE",
                "%victim% got packed by server AI",
                "%victim% lost to something with no brain"
        ));
        map.put("zombie", List.of("%victim% got eaten by a zombie", "%victim% lost to a walking corpse", "%victim% got chewed up by a zombie"));
        map.put("skeleton", List.of("%victim% got sniped by a skeleton", "%victim% got bowed by bones", "%victim% lost to a dead archer"));
        map.put("creeper", List.of("%victim% got creepered", "%victim% got cratered by a creeper", "%victim% got blown out by a green rat"));
        map.put("spider", List.of("%victim% got jumped by a spider", "%victim% lost to too many legs", "%victim% got web checked"));
        map.put("enderman", List.of("%victim% looked at an enderman and lost", "%victim% got folded by tall purple anger", "%victim% failed the eye contact test"));
        map.put("warden", List.of("%victim% got deleted by the warden", "%victim% made noise and paid for it", "%victim% got erased by the deep dark"));
        map.put("iron_golem", List.of("%victim% got launched by an iron golem", "%victim% got uppercut by village security", "%victim% lost to iron hands"));
        map.put("piglin", List.of("%victim% got taxed by piglins", "%victim% forgot gold and got jumped", "%victim% got robbed in the nether"));
        map.put("hoglin", List.of("%victim% got shoved by a hoglin", "%victim% got run over by nether pork", "%victim% lost to angry bacon"));
        map.put("zoglin", List.of("%victim% got run over by a zoglin", "%victim% got deleted by rotten pork", "%victim% got slammed by a dead hog"));
        map.put("guardian", List.of("%victim% got lasered by a guardian", "%victim% got beamed by a wet cube", "%victim% lost to ocean security"));
        map.put("bee", List.of("%victim% got stung by a bee", "%victim% lost to a flying needle", "%victim% angered the wrong hive"));
        map.put("wolf", List.of("%victim% got chewed by a wolf", "%victim% got dogpiled", "%victim% lost to someone's pet"));
        map.put("goat", List.of("%victim% got rammed by a goat", "%victim% got headbutted off script", "%victim% lost to farm knockback"));
        map.put("breeze", List.of("%victim% got blown around by a breeze", "%victim% got air comboed", "%victim% got bullied by wind"));
        map.put("bogged", List.of("%victim% got swamp-sniped", "%victim% got poisoned by swamp bones", "%victim% got shot by a wet skeleton"));
        map.put("projectile", List.of("%victim% caught a shot to the face", "%victim% forgot to dodge", "%victim% ate a projectile"));
        map.put("arrow", List.of("%victim% got shot", "%victim% got arrowed", "%victim% got turned into target practice"));
        map.put("trident", List.of("%victim% got forked", "%victim% got skewered", "%victim% caught a trident"));
        map.put("fireball", List.of("%victim% got fireballed", "%victim% got roasted from range", "%victim% caught hot mail"));
        map.put("wind_charge", List.of("%victim% got wind-charged", "%victim% got blasted by air", "%victim% got knocked into shame"));
        map.put("explosion", List.of("%victim% got blown up", "%victim% got sent by boom", "%victim% became a crater stain"));
        map.put("tnt", List.of("%victim% got TNT'd", "%victim% got cannon checked", "%victim% became blast loot", "%victim% got redstone packed"));
        map.put("crystal", List.of(
                "%victim% got crystalled",
                "%victim% got popped by crystal",
                "%victim% got crystal checked",
                "%victim% got sent by shiny glass",
                "%victim% forgot the totem",
                "%victim% got 2b2t'd by a crystal",
                "%victim% became armor damage"
        ));
        map.put("bad_respawn_point", List.of("%victim% got bed-bombed", "%victim% got anchor-bombed", "%victim% clicked the wrong respawn block"));
        map.put("bed_explosion", List.of("%victim% got bed-bombed", "%victim% slept too hard", "%victim% got killed by a mattress"));
        map.put("respawn_anchor", List.of("%victim% got anchor-bombed", "%victim% got packed by purple boom", "%victim% clicked glowstone death"));
        map.put("fire", List.of("%victim% got cooked", "%victim% burned like a base leak", "%victim% lost to orange air"));
        map.put("lava", List.of("%victim% took a lava bath", "%victim% donated gear to lava", "%victim% got melted", "%victim% swam in hot soup"));
        map.put("campfire", List.of("%victim% got grilled", "%victim% stood on the BBQ", "%victim% became camp food"));
        map.put("hot_floor", List.of("%victim% danced on magma", "%victim% got foot cooked", "%victim% failed the crouch check"));
        map.put("lightning", List.of("%victim% got smited", "%victim% got fried by the sky", "%victim% got picked by lightning"));
        map.put("drowning", List.of("%victim% drowned", "%victim% forgot to breathe", "%victim% lost to water"));
        map.put("suffocation", List.of("%victim% suffocated", "%victim% got block trapped", "%victim% got hugged by stone"));
        map.put("starvation", List.of("%victim% forgot to eat", "%victim% lost to hunger", "%victim% got killed by no food"));
        map.put("freeze", List.of("%victim% froze like a laggy client", "%victim% got turned into ice loot", "%victim% lost to snow"));
        map.put("contact", List.of("%victim% touched the wrong block", "%victim% got poked by the map", "%victim% lost to a block"));
        map.put("cactus", List.of("%victim% hugged a cactus", "%victim% got cactus checked", "%victim% lost to a green spike"));
        map.put("berry_bush", List.of("%victim% lost to berries", "%victim% got bush trapped", "%victim% got killed by breakfast"));
        map.put("fall", List.of(
                "%victim% failed the water bucket",
                "%victim% forgot gravity exists",
                "%victim% hit the floor too hard",
                "%victim% missed the clutch",
                "%victim% took the fast way down",
                "%victim% got floor checked"
        ));
        map.put("void", List.of("%victim% fell into the void", "%victim% got eaten by nothing", "%victim% left the map"));
        map.put("world_border", List.of("%victim% got pushed by the border", "%victim% tried to leave 2b2t", "%victim% lost to the wall"));
        map.put("falling_block", List.of("%victim% got crushed by a block", "%victim% forgot to look up", "%victim% got flattened"));
        map.put("anvil", List.of("%victim% got anvil'd", "%victim% got bonked by iron", "%victim% got renamed to dead"));
        map.put("cramming", List.of("%victim% got squashed in a crowd", "%victim% died in the player pile", "%victim% got entity crammed"));
        map.put("thorns", List.of("%victim% hit thorns and died", "%victim% punched armor and lost", "%victim% killed themselves on thorns"));
        map.put("fly_into_wall", List.of("%victim% flew into a wall", "%victim% got elytra checked", "%victim% became wall paste"));
        map.put("ender_pearl", List.of("%victim% pearled into death", "%victim% threw their life away", "%victim% got pearl checked"));
        map.put("mace_smash", List.of("%victim% got mace-smacked", "%victim% got slammed by a mace", "%victim% got bonked from above"));
        map.put("dragon", List.of("%victim% got slapped by the dragon", "%victim% lost the end fight", "%victim% became dragon food"));
        map.put("wither_boss", List.of("%victim% got withered by the boss", "%victim% got farmed by the wither", "%victim% became beacon parts"));
        map.put("wither", List.of("%victim% withered away", "%victim% got black-hearted", "%victim% rotted out"));
        map.put("magic", List.of("%victim% got magic'd", "%victim% lost to particles", "%victim% got potion checked"));
        map.put("poison", List.of("%victim% got poisoned", "%victim% died slow", "%victim% watched the hearts leave"));
        map.put("sonic_boom", List.of("%victim% got sonic-boomed", "%victim% got bass dropped", "%victim% lost to loud damage"));
        map.put("suicide", List.of("%victim% killed themselves", "%victim% gave up", "%victim% packed themselves"));
        map.put("command_kill", List.of("%victim% used /kill like a coward", "%victim% pressed the easy way out", "%victim% self packed"));
        return map;
    }
}
