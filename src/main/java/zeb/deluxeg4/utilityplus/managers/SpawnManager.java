package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class SpawnManager {

    private final UtilityPlus plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private Location spawnLocation;

    // If the world wasn't loaded yet during onEnable, we store its name here
    // and resolve it lazily on first use or via WorldLoadEvent
    private String pendingWorldName = null;

    // Config flags
    private boolean tpOnFirstJoin;
    private boolean tpOnDeath;
    private boolean tpNoRespawnPoint;
    private int cooldownSeconds;
    private int warmupSeconds;
    private boolean randomFirstJoin;
    private boolean randomNoRespawnPoint;
    private String randomWorldName;
    private boolean randomUseWorldBorder;
    private int randomCenterX;
    private int randomCenterZ;
    private int randomMinRadius;
    private int randomMaxRadius;
    private int randomMaxAttempts;

    // Cooldown tracker: UUID -> last /spawn use timestamp (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Warmup task tracker: UUID -> active warmup task
    private final Map<UUID, ScheduledTask> warmupTasks = new HashMap<>();
    private ScheduledTask pendingSaveTask;

    // Tracks players who have joined before (persisted in spawn.yml)
    // We store them as a list so first-join detection survives restarts
    private final java.util.Set<UUID> knownPlayers = new java.util.HashSet<>();

    public SpawnManager(UtilityPlus plugin) {
        this.plugin = plugin;
        readConfig();
        loadData();
    }

    private void readConfig() {
        FileConfiguration cfg = plugin.getConfig();
        this.tpOnFirstJoin    = cfg.getBoolean("spawn.tp-spawn-first-join",      true);
        this.tpOnDeath        = cfg.getBoolean("spawn.tp-spawn-on-death",         true);
        this.tpNoRespawnPoint = cfg.getBoolean("spawn.tp-spawn-no-respawn-point", true);
        this.cooldownSeconds  = cfg.getInt    ("spawn.tp-spawn-cooldown",         30);
        this.warmupSeconds    = cfg.getInt    ("spawn.tp-spawn-warmup",           5);
        this.randomFirstJoin = cfg.getBoolean("spawn.random-spawn.first-join", false);
        this.randomNoRespawnPoint = cfg.getBoolean("spawn.random-spawn.no-respawn-point", false);
        this.randomWorldName = cfg.getString("spawn.random-spawn.world", "world");
        this.randomUseWorldBorder = cfg.getBoolean("spawn.random-spawn.use-world-border", true);
        this.randomCenterX = cfg.getInt("spawn.random-spawn.center-x", 0);
        this.randomCenterZ = cfg.getInt("spawn.random-spawn.center-z", 0);
        this.randomMinRadius = Math.max(0, cfg.getInt("spawn.random-spawn.min-radius", 100));
        this.randomMaxRadius = Math.max(randomMinRadius + 1, cfg.getInt("spawn.random-spawn.max-radius", 5000));
        this.randomMaxAttempts = Math.max(1, cfg.getInt("spawn.random-spawn.max-attempts", 32));
    }

    // ---------------------------------------------------------------
    // Data persistence
    // ---------------------------------------------------------------

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "spawn.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[SpawnManager] Could not create spawn.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load spawn location
        if (dataConfig.contains("spawn.world")) {
            String worldName = dataConfig.getString("spawn.world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                spawnLocation = buildLocation(world);
                plugin.getLogger().info("[SpawnManager] Spawn loaded at " + worldName);
            } else {
                // World not loaded yet — save name and resolve later
                pendingWorldName = worldName;
                plugin.getLogger().warning("[SpawnManager] World '" + worldName
                        + "' not loaded yet — spawn will be resolved when the world loads.");
            }
        }

        // Load known players (for first-join detection)
        if (dataConfig.contains("known-players")) {
            for (String uuidStr : dataConfig.getStringList("known-players")) {
                try { knownPlayers.add(UUID.fromString(uuidStr)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    /**
     * Called by SpawnListener on WorldLoadEvent.
     * Resolves the spawn location if the world was not ready during onEnable.
     */
    public boolean tryResolvePendingWorld(String loadedWorldName) {
        if (pendingWorldName == null) return false;
        if (!pendingWorldName.equalsIgnoreCase(loadedWorldName)) return false;

        World world = Bukkit.getWorld(loadedWorldName);
        if (world == null) return false;

        spawnLocation = buildLocation(world);
        pendingWorldName = null;
        plugin.getLogger().info("[SpawnManager] Spawn location resolved after world load: " + loadedWorldName);
        return true;
    }

    public boolean hasPendingWorld() {
        return pendingWorldName != null;
    }

    private Location buildLocation(World world) {
        return new Location(
                world,
                dataConfig.getDouble("spawn.x"),
                dataConfig.getDouble("spawn.y"),
                dataConfig.getDouble("spawn.z"),
                (float) dataConfig.getDouble("spawn.yaw"),
                (float) dataConfig.getDouble("spawn.pitch")
        );
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
        if (spawnLocation != null) {
            dataConfig.set("spawn.world", spawnLocation.getWorld().getName());
            dataConfig.set("spawn.x",     spawnLocation.getX());
            dataConfig.set("spawn.y",     spawnLocation.getY());
            dataConfig.set("spawn.z",     spawnLocation.getZ());
            dataConfig.set("spawn.yaw",   (double) spawnLocation.getYaw());
            dataConfig.set("spawn.pitch", (double) spawnLocation.getPitch());
        }

        // Persist known-players list
        java.util.List<String> uuidList = new java.util.ArrayList<>();
        for (UUID uuid : knownPlayers) uuidList.add(uuid.toString());
        dataConfig.set("known-players", uuidList);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[SpawnManager] Could not save spawn.yml!");
        }
    }

    private synchronized void cancelPendingSave() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            pendingSaveTask.cancel();
        }
        pendingSaveTask = null;
    }

    // ---------------------------------------------------------------
    // Spawn location
    // ---------------------------------------------------------------

    public void setSpawn(Location location) {
        this.spawnLocation = location;
        saveData();
    }

    public Location getSpawn() {
        return spawnLocation;
    }

    public boolean hasSpawn() {
        return spawnLocation != null;
    }

    // ---------------------------------------------------------------
    // Cooldown
    // ---------------------------------------------------------------

    public boolean isOnCooldown(UUID uuid) {
        return getCooldownSecondsLeft(uuid) > 0;
    }

    public long getCooldownSecondsLeft(UUID uuid) {
        Long last = cooldowns.get(uuid);
        if (last == null || cooldownSeconds <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0L, cooldownSeconds - elapsed);
    }

    public void applyCooldown(UUID uuid) {
        if (cooldownSeconds > 0) {
            cooldowns.put(uuid, System.currentTimeMillis());
        }
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getWarmupSeconds() {
        return warmupSeconds;
    }

    // ---------------------------------------------------------------
    // Warmup task management
    // ---------------------------------------------------------------

    public void startWarmup(UUID uuid, ScheduledTask task) {
        cancelWarmup(uuid);
        warmupTasks.put(uuid, task);
    }

    public void cancelWarmup(UUID uuid) {
        ScheduledTask t = warmupTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public boolean hasWarmup(UUID uuid) {
        return warmupTasks.containsKey(uuid);
    }

    public UtilityPlus getPlugin() {
        return plugin;
    }

    /** Called by ReloadCommand — re-reads spawn.yml and config values. */
    public void reload() {
        spawnLocation   = null;
        pendingWorldName = null;
        knownPlayers.clear();
        readConfig();
        loadData();
        plugin.getLogger().info("[SpawnManager] Reloaded.");
    }

    // ---------------------------------------------------------------
    // Config flag getters
    // ---------------------------------------------------------------

    public boolean isTpOnFirstJoin()    { return tpOnFirstJoin; }
    public boolean isTpOnDeath()        { return tpOnDeath; }
    public boolean isTpNoRespawnPoint() { return tpNoRespawnPoint; }
    public boolean isRandomFirstJoin() { return randomFirstJoin; }
    public boolean isRandomNoRespawnPoint() { return randomNoRespawnPoint; }

    public Location findRandomSpawn(Location fallback) {
        World world = getRandomSpawnWorld(fallback);
        if (world == null) return null;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < randomMaxAttempts; attempt++) {
            int x = randomCoordinate(random, true, world);
            int z = randomCoordinate(random, false, world);

            if (!isInsideAllowedBorder(world, x, z)) continue;

            Location safe = findSafeSurface(world, x, z);
            if (safe != null) return safe;
        }
        return null;
    }

    public void findRandomSpawnAsync(Location fallback, Consumer<Location> callback) {
        World world = getRandomSpawnWorld(fallback);
        if (world == null) {
            callback.accept(null);
            return;
        }

        findRandomSpawnAsync(world, callback, 0);
    }

    private void findRandomSpawnAsync(World world, Consumer<Location> callback, int attempt) {
        if (attempt >= randomMaxAttempts) {
            callback.accept(null);
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = randomCoordinate(random, true, world);
        int z = randomCoordinate(random, false, world);

        if (!isInsideAllowedBorder(world, x, z)) {
            findRandomSpawnAsync(world, callback, attempt + 1);
            return;
        }

        PaperFoliaTasks.runAtLocation(plugin, world, x, z, () -> {
            Location safe = findSafeSurface(world, x, z);
            if (safe != null) {
                callback.accept(safe);
                return;
            }

            findRandomSpawnAsync(world, callback, attempt + 1);
        });
    }

    private World getRandomSpawnWorld(Location fallback) {
        World configured = Bukkit.getWorld(randomWorldName);
        if (configured != null) return configured;
        return fallback != null ? fallback.getWorld() : null;
    }

    private int randomCoordinate(ThreadLocalRandom random, boolean xAxis, World world) {
        if (randomUseWorldBorder) {
            WorldBorder border = world.getWorldBorder();
            Location center = border.getCenter();
            int halfSize = Math.max(1, (int) Math.floor(border.getSize() / 2.0D) - 1);
            int range = Math.min(randomMaxRadius, halfSize);
            int min = -range;
            int max = range;
            int offset = randomNonZeroOffset(random, min, max);
            return (int) Math.floor((xAxis ? center.getX() : center.getZ()) + offset);
        }

        int offset = randomNonZeroOffset(random, -randomMaxRadius, randomMaxRadius);
        return (xAxis ? randomCenterX : randomCenterZ) + offset;
    }

    private int randomNonZeroOffset(ThreadLocalRandom random, int min, int max) {
        int effectiveMinRadius = Math.min(randomMinRadius, Math.max(Math.abs(min), Math.abs(max)));
        if (effectiveMinRadius <= 0) return random.nextInt(min, max + 1);

        int offset;
        do {
            offset = random.nextInt(min, max + 1);
        } while (Math.abs(offset) < effectiveMinRadius);
        return offset;
    }

    private boolean isInsideAllowedBorder(World world, int x, int z) {
        if (!randomUseWorldBorder) return true;
        WorldBorder border = world.getWorldBorder();
        double halfSize = border.getSize() / 2.0D;
        Location center = border.getCenter();
        return x >= center.getX() - halfSize
                && x <= center.getX() + halfSize
                && z >= center.getZ() - halfSize
                && z <= center.getZ() + halfSize;
    }

    private Location findSafeSurface(World world, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        if (highestY <= world.getMinHeight() || highestY + 2 >= world.getMaxHeight()) return null;

        for (int feetY = highestY; feetY <= highestY + 1; feetY++) {
            Block ground = world.getBlockAt(x, feetY - 1, z);
            Block feet = world.getBlockAt(x, feetY, z);
            Block head = world.getBlockAt(x, feetY + 1, z);

            if (!isSafeGround(ground.getType())) continue;
            if (!feet.isPassable() || !head.isPassable()) continue;

            return new Location(world, x + 0.5D, feetY, z + 0.5D, 0.0F, 0.0F);
        }
        return null;
    }

    private boolean isSafeGround(Material material) {
        if (!material.isSolid()) return false;
        return switch (material) {
            case BEDROCK, CACTUS, CAMPFIRE, SOUL_CAMPFIRE, FIRE, SOUL_FIRE, LAVA, MAGMA_BLOCK, POWDER_SNOW -> false;
            default -> true;
        };
    }

    // ---------------------------------------------------------------
    // First-join tracking
    // ---------------------------------------------------------------

    /** Returns true if this is the player's first time joining the server. */
    public boolean isFirstJoin(UUID uuid) {
        return !knownPlayers.contains(uuid);
    }

    /** Mark a player as having joined before. */
    public synchronized void markKnown(UUID uuid) {
        if (knownPlayers.add(uuid)) {
            saveLater();
        }
    }
}
