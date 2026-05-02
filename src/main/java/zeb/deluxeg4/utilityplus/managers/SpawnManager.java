package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// Folia imports (optional - only used if Folia is detected)
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class SpawnManager {

    private final UtilityPlus plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private Location spawnLocation;

    // If the world wasn't loaded yet during onEnable, we store its name here
    private String pendingWorldName = null;

    // Config flags
    private boolean tpOnFirstJoin;
    private boolean tpOnDeath;
    private boolean tpNoRespawnPoint;
    private int cooldownSeconds;
    private int warmupSeconds;

    // Random Spawn Settings
    private boolean rtpEnabled;
    private int rtpMinRadius;
    private int rtpMaxRadius;
    private int rtpAttempts;
    private String rtpWorldName;

    // ---------------------------------------------------------------
    // RTP Location Pool (Folia-safe pre-generation)
    // ---------------------------------------------------------------
    private final ConcurrentLinkedQueue<Location> rtpLocationPool = new ConcurrentLinkedQueue<>();

    // จำนวน location ที่ต้องการเก็บไว้ใน pool
    private static final int POOL_TARGET_SIZE = 20;

    // ป้องกัน refill ซ้อนกัน
    private final AtomicInteger pendingRefillCount = new AtomicInteger(0);

    // ---------------------------------------------------------------
    // Unsafe ground types for RTP
    // ---------------------------------------------------------------
    private static final Set<Material> UNSAFE_GROUND = EnumSet.of(
            Material.CACTUS,
            Material.CAMPFIRE,
            Material.FIRE,
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.POWDER_SNOW,
            Material.SOUL_CAMPFIRE,
            Material.SOUL_FIRE,
            Material.SWEET_BERRY_BUSH,
            Material.WATER
    );

    // Cooldown tracker: UUID -> last /spawn use timestamp (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Warmup task tracker: UUID -> active warmup task (Folia)
    private final Map<UUID, ScheduledTask> warmupTasks = new HashMap<>();
    // Warmup task tracker for Paper/Bukkit
    private final Map<UUID, BukkitTask> warmupTasksBukkit = new HashMap<>();

    // Tracks players who have joined before (persisted in spawn.yml)
    private final java.util.Set<UUID> knownPlayers = new java.util.HashSet<>();

    // Platform detection
    private final boolean isFolia;

    public SpawnManager(UtilityPlus plugin) {
        this.plugin = plugin;
        this.isFolia = detectFolia();
        readConfig();
        loadData();

        // Start RTP pool only on Folia
        if (isFolia) {
            schedulePoolRefill(POOL_TARGET_SIZE);
        }
    }

    private boolean detectFolia() {
        try {
            // Check for Folia's async scheduler method
            Bukkit.class.getMethod("getAsyncScheduler");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    private void readConfig() {
        FileConfiguration cfg = plugin.getConfig();
        this.tpOnFirstJoin    = cfg.getBoolean("spawn.tp-spawn-first-join",      true);
        this.tpOnDeath        = cfg.getBoolean("spawn.tp-spawn-on-death",         true);
        this.tpNoRespawnPoint = cfg.getBoolean("spawn.tp-spawn-no-respawn-point", true);
        this.cooldownSeconds  = cfg.getInt    ("spawn.tp-spawn-cooldown",         30);
        this.warmupSeconds    = cfg.getInt    ("spawn.tp-spawn-warmup",           5);

        this.rtpEnabled   = cfg.getBoolean("random-respawn.enabled",      true);
        this.rtpMinRadius = Math.max(0,             cfg.getInt("random-respawn.min-radius", 50));
        this.rtpMaxRadius = Math.max(rtpMinRadius,  cfg.getInt("random-respawn.max-radius", 1000));
        this.rtpAttempts  = Math.max(1,             cfg.getInt("random-respawn.attempts",   48));
        this.rtpWorldName = cfg.getString("random-respawn.world", "world");
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
                pendingWorldName = worldName;
                plugin.getLogger().warning("[SpawnManager] World '" + worldName
                        + "' not loaded yet — spawn will be resolved when the world loads.");
            }
        }

        // Load known players
        if (dataConfig.contains("known-players")) {
            for (String uuidStr : dataConfig.getStringList("known-players")) {
                try { knownPlayers.add(UUID.fromString(uuidStr)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public boolean tryResolvePendingWorld(String loadedWorldName) {
        if (pendingWorldName == null) return false;
        if (!pendingWorldName.equalsIgnoreCase(loadedWorldName)) return false;

        World world = Bukkit.getWorld(loadedWorldName);
        if (world == null) return false;

        spawnLocation    = buildLocation(world);
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
        if (spawnLocation != null) {
            dataConfig.set("spawn.world", spawnLocation.getWorld().getName());
            dataConfig.set("spawn.x",     spawnLocation.getX());
            dataConfig.set("spawn.y",     spawnLocation.getY());
            dataConfig.set("spawn.z",     spawnLocation.getZ());
            dataConfig.set("spawn.yaw",   (double) spawnLocation.getYaw());
            dataConfig.set("spawn.pitch", (double) spawnLocation.getPitch());
        }

        java.util.List<String> uuidList = new java.util.ArrayList<>();
        for (UUID uuid : knownPlayers) uuidList.add(uuid.toString());
        dataConfig.set("known-players", uuidList);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[SpawnManager] Could not save spawn.yml!");
        }
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

    public int getCooldownSeconds()  { return cooldownSeconds; }
    public int getWarmupSeconds()    { return warmupSeconds; }

    // ---------------------------------------------------------------
    // Warmup task management (Hybrid Folia/Paper)
    // ---------------------------------------------------------------

    public void startWarmup(UUID uuid, Object task) {
        cancelWarmup(uuid);
        if (isFolia && task instanceof ScheduledTask) {
            warmupTasks.put(uuid, (ScheduledTask) task);
        } else if (!isFolia && task instanceof BukkitTask) {
            warmupTasksBukkit.put(uuid, (BukkitTask) task);
        }
    }

    public void cancelWarmup(UUID uuid) {
        if (isFolia) {
            ScheduledTask t = warmupTasks.remove(uuid);
            if (t != null) t.cancel();
        } else {
            BukkitTask t = warmupTasksBukkit.remove(uuid);
            if (t != null) t.cancel();
        }
    }

    public boolean hasWarmup(UUID uuid) {
        if (isFolia) {
            return warmupTasks.containsKey(uuid);
        } else {
            return warmupTasksBukkit.containsKey(uuid);
        }
    }

    public UtilityPlus getPlugin() { return plugin; }

    /** Called by ReloadCommand — re-reads spawn.yml and config values. */
    public void reload() {
        spawnLocation    = null;
        pendingWorldName = null;
        knownPlayers.clear();
        rtpLocationPool.clear();
        pendingRefillCount.set(0);
        readConfig();
        loadData();

        // Re-generate pool หลัง reload (Folia only)
        if (isFolia) {
            schedulePoolRefill(POOL_TARGET_SIZE);
        }
    }

    // ---------------------------------------------------------------
    // Config flag getters
    // ---------------------------------------------------------------

    public boolean isTpOnFirstJoin()    { return tpOnFirstJoin; }
    public boolean isTpOnDeath()        { return tpOnDeath; }
    public boolean isTpNoRespawnPoint() { return tpNoRespawnPoint; }
    public boolean isRtpEnabled()       { return rtpEnabled; }

    // ---------------------------------------------------------------
    // Random Respawn Location — Hybrid Folia (async pool) / Paper (sync)
    // ---------------------------------------------------------------

    /**
     * Get random respawn location — uses pool on Folia, sync generation on Paper/Bukkit
     */
    public Location getRandomLocation() {
        if (isFolia) {
            return getRandomLocationFolia();
        } else {
            return getRandomLocationPaper();
        }
    }

    // ==================== FOLIA: Async Pool-based ====================

    private Location getRandomLocationFolia() {
        Location loc = rtpLocationPool.poll();
        int poolSize = rtpLocationPool.size();

        int need = POOL_TARGET_SIZE - poolSize - pendingRefillCount.get();
        if (need > 0) {
            schedulePoolRefill(need);
        }

        if (loc != null) {
            return loc;
        }

        return null;
    }

    public Location pollFromPool() {
        if (!isFolia) return null;
        Location loc = rtpLocationPool.poll();
        int need = POOL_TARGET_SIZE - rtpLocationPool.size() - pendingRefillCount.get();
        if (need > 0) schedulePoolRefill(need);
        return loc;
    }

    private void schedulePoolRefill(int count) {
        if (!rtpEnabled || !isFolia) return;

        for (int i = 0; i < count; i++) {
            pendingRefillCount.incrementAndGet();
            plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
                World world = Bukkit.getWorld(rtpWorldName);
                if (world == null) world = Bukkit.getWorlds().get(0);
                if (world == null) {
                    pendingRefillCount.decrementAndGet();
                    return;
                }
                generateAndAddToPool(world, 0);
            });
        }
    }

    private void generateAndAddToPool(World world, int attempt) {
        if (attempt >= rtpAttempts) {
            pendingRefillCount.decrementAndGet();
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double minR2 = (double) rtpMinRadius * rtpMinRadius;
        double maxR2 = (double) rtpMaxRadius * rtpMaxRadius;
        double distance = Math.sqrt(minR2 + random.nextDouble() * (maxR2 - minR2));

        Location center = world.getSpawnLocation();
        int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);

        if (!isInsideWorldBorder(world, x, z)) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, t ->
                    generateAndAddToPool(world, attempt + 1));
            return;
        }

        final World finalWorld = world;
        final int finalX = x;
        final int finalZ = z;

        world.getChunkAtAsync(x >> 4, z >> 4).whenComplete((chunk, ex) -> {
            if (ex != null || chunk == null) {
                plugin.getServer().getAsyncScheduler().runNow(plugin, t ->
                        generateAndAddToPool(finalWorld, attempt + 1));
                return;
            }

            plugin.getServer().getRegionScheduler().execute(plugin,
                    finalWorld, finalX >> 4, finalZ >> 4, () -> {
                        try {
                            Location loc = finalWorld
                                    .getHighestBlockAt(finalX, finalZ, HeightMap.MOTION_BLOCKING_NO_LEAVES)
                                    .getLocation()
                                    .add(0.5, 1, 0.5);

                            if (isSafeLocation(loc)) {
                                loc.setYaw(ThreadLocalRandom.current().nextFloat() * 360.0F);
                                loc.setPitch(0.0F);
                                rtpLocationPool.offer(loc);
                            }
                            pendingRefillCount.decrementAndGet();
                        } catch (Exception regionEx) {
                            plugin.getServer().getAsyncScheduler().runNow(plugin, t ->
                                    generateAndAddToPool(finalWorld, attempt + 1));
                        }
                    });
        });
    }

    // ==================== PAPER/BUKKIT: Sync Generation ====================

    private Location getRandomLocationPaper() {
        World world = Bukkit.getWorld(rtpWorldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        Location center = getRandomRespawnCenter(world);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < rtpAttempts; attempt++) {
            double theta = random.nextDouble(0, Math.PI * 2);
            double r = randomRadius(random);

            int x = center.getBlockX() + (int) Math.round(r * Math.cos(theta));
            int z = center.getBlockZ() + (int) Math.round(r * Math.sin(theta));

            if (!isInsideWorldBorder(world, x, z)) {
                continue;
            }

            Location loc = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES)
                    .getLocation()
                    .add(0.5, 1, 0.5);

            if (isSafeLocation(loc)) {
                loc.setYaw(random.nextFloat() * 360.0F);
                loc.setPitch(0.0F);
                return loc;
            }
        }

        return spawnLocation != null ? spawnLocation : world.getSpawnLocation();
    }

    private Location getRandomRespawnCenter(World world) {
        if (spawnLocation != null && spawnLocation.getWorld() != null
                && spawnLocation.getWorld().getName().equals(world.getName())) {
            return spawnLocation;
        }
        return world.getSpawnLocation();
    }

    private double randomRadius(ThreadLocalRandom random) {
        if (rtpMaxRadius <= rtpMinRadius) {
            return rtpMinRadius;
        }
        double minSquared = (double) rtpMinRadius * rtpMinRadius;
        double maxSquared = (double) rtpMaxRadius * rtpMaxRadius;
        return Math.sqrt(random.nextDouble(minSquared, maxSquared));
    }


    private boolean isInsideWorldBorder(World world, int x, int z) {
        WorldBorder border = world.getWorldBorder();
        Location center   = border.getCenter();
        double radius     = border.getSize() / 2.0D;

        return (x + 0.5D) >= center.getX() - radius
                && (x + 0.5D) <= center.getX() + radius
                && (z + 0.5D) >= center.getZ() - radius
                && (z + 0.5D) <= center.getZ() + radius;
    }

    private boolean isSafeLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        int y = loc.getBlockY();
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) return false;

        Block feet   = loc.getBlock();
        Block head   = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().add(0, -1, 0).getBlock();
        Material groundType = ground.getType();

        return feet.isPassable()
                && head.isPassable()
                && groundType.isSolid()
                && !ground.isLiquid()
                && !UNSAFE_GROUND.contains(groundType);
    }

    // ---------------------------------------------------------------
    // First-join tracking
    // ---------------------------------------------------------------

    public boolean isFirstJoin(UUID uuid) {
        return !knownPlayers.contains(uuid);
    }

    public void markKnown(UUID uuid) {
        if (knownPlayers.add(uuid)) {
            saveData();
        }
    }
}