package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {

    private final UtilityPlus plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private ScheduledTask pendingSaveTask;

    private final Set<UUID> globalMuted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pmMuted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> deathMessagesMuted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> hardDeathMessagesMuted = ConcurrentHashMap.newKeySet();

    private final Map<UUID, UUID> lastPmSender = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastPmTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> ignoredPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> hardIgnoredPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> ignoredDeathMessages = new ConcurrentHashMap<>();

    public ChatManager(UtilityPlus plugin) {
        this.plugin = plugin;
        loadData();
    }

    public boolean isGlobalMuted(UUID uuid) {
        return globalMuted.contains(uuid);
    }

    public void muteGlobal(UUID uuid) {
        globalMuted.add(uuid);
    }

    public void unmuteGlobal(UUID uuid) {
        globalMuted.remove(uuid);
    }

    public boolean isPmMuted(UUID uuid) {
        return pmMuted.contains(uuid);
    }

    public void mutePm(UUID uuid) {
        pmMuted.add(uuid);
    }

    public void unmutePm(UUID uuid) {
        pmMuted.remove(uuid);
    }

    public UUID getLastPmSender(UUID uuid) {
        return lastPmSender.get(uuid);
    }

    public void setLastPmSender(UUID target, UUID sender) {
        lastPmSender.put(target, sender);
    }

    public UUID getLastPmTarget(UUID uuid) {
        return lastPmTarget.get(uuid);
    }

    public void setLastPmTarget(UUID sender, UUID target) {
        lastPmTarget.put(sender, target);
    }

    public boolean toggleGlobalMuted(UUID uuid) {
        if (globalMuted.contains(uuid)) {
            globalMuted.remove(uuid);
            return false;
        }
        globalMuted.add(uuid);
        return true;
    }

    public boolean togglePmMuted(UUID uuid) {
        if (pmMuted.contains(uuid)) {
            pmMuted.remove(uuid);
            return false;
        }
        pmMuted.add(uuid);
        return true;
    }

    public boolean toggleDeathMessages(UUID uuid) {
        if (deathMessagesMuted.contains(uuid)) {
            deathMessagesMuted.remove(uuid);
            return false;
        }
        deathMessagesMuted.add(uuid);
        return true;
    }

    public boolean toggleHardDeathMessages(UUID uuid) {
        if (hardDeathMessagesMuted.contains(uuid)) {
            hardDeathMessagesMuted.remove(uuid);
            saveLater();
            return false;
        }
        hardDeathMessagesMuted.add(uuid);
        saveLater();
        return true;
    }

    public boolean isDeathMessagesMuted(UUID uuid) {
        return deathMessagesMuted.contains(uuid) || hardDeathMessagesMuted.contains(uuid);
    }

    public boolean toggleIgnore(UUID viewer, String targetName) {
        return toggleName(ignoredPlayers.computeIfAbsent(viewer, ignored -> ConcurrentHashMap.newKeySet()), targetName);
    }

    public boolean toggleHardIgnore(UUID viewer, String targetName) {
        boolean ignored = toggleName(hardIgnoredPlayers.computeIfAbsent(viewer, uuid -> ConcurrentHashMap.newKeySet()), targetName);
        saveLater();
        return ignored;
    }

    public boolean toggleDeathMessageIgnore(UUID viewer, String targetName) {
        boolean ignored = toggleName(ignoredDeathMessages.computeIfAbsent(viewer, uuid -> ConcurrentHashMap.newKeySet()), targetName);
        saveLater();
        return ignored;
    }

    public boolean isIgnoring(UUID viewer, String targetName) {
        String key = normalize(targetName);
        return ignoredPlayers.getOrDefault(viewer, Set.of()).contains(key)
                || hardIgnoredPlayers.getOrDefault(viewer, Set.of()).contains(key);
    }

    public boolean isIgnoringDeathMessage(UUID viewer, String targetName) {
        return ignoredDeathMessages.getOrDefault(viewer, Set.of()).contains(normalize(targetName));
    }

    public Set<String> getHardIgnoredPlayers(UUID viewer) {
        return new HashSet<>(hardIgnoredPlayers.getOrDefault(viewer, Set.of()));
    }

    public void reload() {
        hardIgnoredPlayers.clear();
        ignoredDeathMessages.clear();
        hardDeathMessagesMuted.clear();
        loadData();
    }

    public void saveData() {
        cancelPendingSave();
        saveNow();
    }

    private synchronized void saveLater() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            return;
        }

        pendingSaveTask = PaperFoliaTasks.runGlobalDelayed(plugin, task -> {
            pendingSaveTask = null;
            saveNow();
        }, 30L * 20L);
    }

    private synchronized void saveNow() {
        if (dataConfig == null) {
            return;
        }

        dataConfig.set("hard-ignore", null);
        dataConfig.set("death-message-ignore", null);
        dataConfig.set("hard-death-messages-muted", hardDeathMessagesMuted.stream().map(UUID::toString).toList());

        for (Map.Entry<UUID, Set<String>> entry : hardIgnoredPlayers.entrySet()) {
            dataConfig.set("hard-ignore." + entry.getKey(), entry.getValue().stream().sorted().toList());
        }
        for (Map.Entry<UUID, Set<String>> entry : ignoredDeathMessages.entrySet()) {
            dataConfig.set("death-message-ignore." + entry.getKey(), entry.getValue().stream().sorted().toList());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[ChatManager] Could not save chat.yml!");
        }
    }

    private synchronized void cancelPendingSave() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            pendingSaveTask.cancel();
        }
        pendingSaveTask = null;
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "chat.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[ChatManager] Could not create chat.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String uuidString : dataConfig.getStringList("hard-death-messages-muted")) {
            parseUuid(uuidString, hardDeathMessagesMuted);
        }

        loadNameMap("hard-ignore", hardIgnoredPlayers);
        loadNameMap("death-message-ignore", ignoredDeathMessages);
    }

    private void loadNameMap(String path, Map<UUID, Set<String>> target) {
        if (!dataConfig.isConfigurationSection(path)) {
            return;
        }

        for (String uuidString : dataConfig.getConfigurationSection(path).getKeys(false)) {
            UUID uuid = parseUuid(uuidString, null);
            if (uuid == null) {
                continue;
            }
            Set<String> names = ConcurrentHashMap.newKeySet();
            for (String name : dataConfig.getStringList(path + "." + uuidString)) {
                names.add(normalize(name));
            }
            target.put(uuid, names);
        }
    }

    private UUID parseUuid(String value, Set<UUID> target) {
        try {
            UUID uuid = UUID.fromString(value);
            if (target != null) {
                target.add(uuid);
            }
            return uuid;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean toggleName(Set<String> names, String targetName) {
        String key = normalize(targetName);
        if (names.contains(key)) {
            names.remove(key);
            return false;
        }
        names.add(key);
        return true;
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
