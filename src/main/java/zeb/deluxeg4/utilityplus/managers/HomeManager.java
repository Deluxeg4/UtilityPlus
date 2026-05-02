package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.util.*;

/**
 * Stores home data as an encoded binary file: homes.dat
 *
 * Binary format (DataOutputStream):
 *   [int: total entries]
 *   per entry:
 *     [long: UUID MSB] [long: UUID LSB]
 *     [UTF: slotName]  [UTF: worldName]
 *     [double: x] [double: y] [double: z]
 *     [float: yaw] [float: pitch]
 *
 * Config keys:
 *   home.warmup    — seconds warmup before TP  (default 3, 0 = instant)
 *   home.max-homes — max slots per player       (default 4, max 4)
 */
public class HomeManager {

    public static final List<String> ALL_SLOTS = Arrays.asList("1", "2", "3", "4");

    private final UtilityPlus plugin;
    private File dataFile;

    // Map<PlayerUUID, Map<SlotName, Location>>  — isolated per UUID, no cross-contamination
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    // Active warmup tasks
    private final Map<UUID, ScheduledTask> warmupTasks = new HashMap<>();
    private ScheduledTask pendingSaveTask;

    // Config values (refreshed on reload)
    private int warmupSeconds;
    private int maxHomes;

    public HomeManager(UtilityPlus plugin) {
        this.plugin = plugin;
        readConfig();
        loadData();
    }

    // ── Config ───────────────────────────────────────────────────────

    private void readConfig() {
        warmupSeconds = plugin.getConfig().getInt("home.warmup",    3);
        maxHomes      = plugin.getConfig().getInt("home.max-homes", 4);
        if (maxHomes < 1) maxHomes = 1;
        if (maxHomes > ALL_SLOTS.size()) maxHomes = ALL_SLOTS.size();
    }

    /** Re-reads config + re-loads data from disk. Called by /upreload. */
    public void reload() {
        homes.clear();
        readConfig();
        loadData();
        plugin.getLogger().info("[HomeManager] Reloaded — warmup=" + warmupSeconds + "s, max-homes=" + maxHomes);
    }

    // ── Binary persistence ───────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "homes.dat");
        if (!dataFile.exists()) return;

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(dataFile)))) {

            int count = in.readInt();
            int loaded = 0;
            for (int i = 0; i < count; i++) {
                UUID   uuid      = new UUID(in.readLong(), in.readLong());
                String slot      = in.readUTF();
                String worldName = in.readUTF();
                double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
                float  yaw = in.readFloat(), pitch = in.readFloat();

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("[HomeManager] Skipped home '" + slot
                        + "' for " + uuid + " — world '" + worldName + "' not loaded.");
                    continue;
                }
                homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                     .put(slot, new Location(world, x, y, z, yaw, pitch));
                loaded++;
            }
            plugin.getLogger().info("[HomeManager] Loaded " + loaded + "/" + count + " home entries.");

        } catch (EOFException ignored) {
        } catch (IOException e) {
            plugin.getLogger().severe("[HomeManager] Failed to load homes.dat: " + e.getMessage());
        }
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
        if (dataFile == null) dataFile = new File(plugin.getDataFolder(), "homes.dat");
        plugin.getDataFolder().mkdirs();

        // count total entries first
        int count = 0;
        for (Map<String, Location> m : homes.values()) count += m.size();

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(dataFile)))) {

            out.writeInt(count);
            for (Map.Entry<UUID, Map<String, Location>> pe : homes.entrySet()) {
                UUID uuid = pe.getKey();
                for (Map.Entry<String, Location> he : pe.getValue().entrySet()) {
                    Location loc = he.getValue();
                    out.writeLong(uuid.getMostSignificantBits());
                    out.writeLong(uuid.getLeastSignificantBits());
                    out.writeUTF(he.getKey());
                    out.writeUTF(loc.getWorld().getName());
                    out.writeDouble(loc.getX());
                    out.writeDouble(loc.getY());
                    out.writeDouble(loc.getZ());
                    out.writeFloat(loc.getYaw());
                    out.writeFloat(loc.getPitch());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[HomeManager] Failed to save homes.dat: " + e.getMessage());
        }
    }

    private synchronized void cancelPendingSave() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            pendingSaveTask.cancel();
        }
        pendingSaveTask = null;
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    /** Slots currently available based on max-homes config. */
    public List<String> getActiveSlots() {
        return ALL_SLOTS.subList(0, maxHomes);
    }

    public boolean isValidSlot(String name) { return getActiveSlots().contains(name); }

    public synchronized boolean setHome(UUID uuid, String name, Location loc) {
        if (!isValidSlot(name)) return false;
        homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>()).put(name, loc);
        saveLater();
        return true;
    }

    public Location getHome(UUID uuid, String name) {
        Map<String, Location> m = homes.get(uuid);
        return m == null ? null : m.get(name);
    }

    public boolean hasHome(UUID uuid, String name) { return getHome(uuid, name) != null; }

    public synchronized boolean deleteHome(UUID uuid, String name) {
        Map<String, Location> m = homes.get(uuid);
        if (m == null || !m.containsKey(name)) return false;
        m.remove(name);
        saveLater();
        return true;
    }

    public Set<String> getHomes(UUID uuid) {
        Map<String, Location> m = homes.get(uuid);
        return m == null ? Collections.emptySet() : Collections.unmodifiableSet(m.keySet());
    }

    public int getHomeCount(UUID uuid) {
        Map<String, Location> m = homes.get(uuid);
        return m == null ? 0 : m.size();
    }

    // ── Getters ──────────────────────────────────────────────────────

    public int getMaxHomes()       { return maxHomes; }
    public int getWarmupSeconds()  { return warmupSeconds; }
    public UtilityPlus getPlugin() { return plugin; }

    // ── Warmup ───────────────────────────────────────────────────────

    public void startWarmup(UUID uuid, ScheduledTask task) {
        cancelWarmup(uuid);
        warmupTasks.put(uuid, task);
    }

    public void cancelWarmup(UUID uuid) {
        ScheduledTask t = warmupTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public boolean hasWarmup(UUID uuid) { return warmupTasks.containsKey(uuid); }
}
