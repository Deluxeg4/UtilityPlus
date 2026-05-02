package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {

    private final UtilityPlus plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private ScheduledTask pendingSaveTask;

    private final Map<UUID, PlayerStats> statsMap = new HashMap<>();

    public StatsManager(UtilityPlus plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[StatsManager] Could not create stats.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("stats")) {
            for (String uuidStr : dataConfig.getConfigurationSection("stats").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    int kills = dataConfig.getInt("stats." + uuidStr + ".kills");
                    int deaths = dataConfig.getInt("stats." + uuidStr + ".deaths");
                    String lastKnownName = dataConfig.getString("stats." + uuidStr + ".name", "Unknown");
                    statsMap.put(uuid, new PlayerStats(uuid, lastKnownName, kills, deaths));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveData() {
        cancelPendingSave();
        saveNow();
    }

    public synchronized void saveLater() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            return;
        }

        pendingSaveTask = PaperFoliaTasks.runGlobalDelayed(plugin, task -> {
            pendingSaveTask = null;
            saveNow();
        }, 30L * 20L);
    }

    private synchronized void saveNow() {
        for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
            String path = "stats." + entry.getKey().toString();
            dataConfig.set(path + ".kills", entry.getValue().getKills());
            dataConfig.set(path + ".deaths", entry.getValue().getDeaths());
            dataConfig.set(path + ".name", entry.getValue().getName());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[StatsManager] Could not save stats.yml!");
        }
    }

    private synchronized void cancelPendingSave() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            pendingSaveTask.cancel();
        }
        pendingSaveTask = null;
    }

    public PlayerStats getStats(UUID uuid) {
        PlayerStats ps = statsMap.get(uuid);
        if (ps == null) {
            // Return a temporary zero-stats object (not added to map)
            return new PlayerStats(uuid, "Unknown", 0, 0);
        }
        return ps;
    }

    public PlayerStats getOrCreateStats(UUID uuid, String name) {
        return statsMap.computeIfAbsent(uuid, k -> new PlayerStats(k, name != null ? name : "Unknown", 0, 0));
    }

    public synchronized void addKill(UUID uuid, String name) {
        PlayerStats ps = getOrCreateStats(uuid, name);
        ps.setName(name);
        ps.setKills(ps.getKills() + 1);
    }

    public synchronized void addDeath(UUID uuid, String name) {
        PlayerStats ps = getOrCreateStats(uuid, name);
        ps.setName(name);
        ps.setDeaths(ps.getDeaths() + 1);
    }

    public List<PlayerStats> getTopKills(int limit) {
        return statsMap.values().stream()
                .filter(ps -> ps.getKills() > 0)
                .sorted(Comparator.comparingInt(PlayerStats::getKills).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<PlayerStats> getTopDeaths(int limit) {
        return statsMap.values().stream()
                .filter(ps -> ps.getDeaths() > 0)
                .sorted(Comparator.comparingInt(PlayerStats::getDeaths).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void reload() {
        statsMap.clear();
        loadData();
        plugin.getLogger().info("[StatsManager] Reloaded.");
    }

    public static class PlayerStats {
        private final UUID uuid;
        private String name;
        private int kills;
        private int deaths;

        public PlayerStats(UUID uuid, String name, int kills, int deaths) {
            this.uuid = uuid;
            this.name = name;
            this.kills = kills;
            this.deaths = deaths;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getKills() { return kills; }
        public void setKills(int kills) { this.kills = kills; }
        public int getDeaths() { return deaths; }
        public void setDeaths(int deaths) { this.deaths = deaths; }
    }
}
