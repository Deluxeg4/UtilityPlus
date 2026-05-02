package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.managers.SpawnManager;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hybrid Spawn Listener - works on both Folia and Paper/Bukkit
 *
 * Folia: Uses async pool-based RTP with deferred teleport
 * Paper/Bukkit: Uses synchronous RTP generation in PlayerRespawnEvent
 */
public class SpawnListener implements Listener {

    private final SpawnManager spawnManager;
    private final boolean isFolia;

    // Folia only: pending RTP tracking for deferred teleport
    private final Set<UUID> pendingRtp = ConcurrentHashMap.newKeySet();

    public SpawnListener(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
        this.isFolia = spawnManager.isFolia();
    }

    // ---------------------------------------------------------------
    // Resolve spawn world if it wasn't loaded during onEnable
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (spawnManager.hasPendingWorld()) {
            spawnManager.tryResolvePendingWorld(event.getWorld().getName());
        }
    }

    // ---------------------------------------------------------------
    // First-join teleport
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!spawnManager.isTpOnFirstJoin()) return;
        if (spawnManager.isFirstJoin(player.getUniqueId())) {
            spawnManager.markKnown(player.getUniqueId());
            if (spawnManager.hasSpawn()) {
                if (isFolia) {
                    PaperFoliaTasks.teleport(player, spawnManager.getSpawn(), spawnManager.getPlugin(), null);
                } else {
                    player.teleport(spawnManager.getSpawn());
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Player Respawn - Different logic for Folia vs Paper
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        boolean hasBedOrAnchor = event.isBedSpawn() || event.isAnchorSpawn();
        if (hasBedOrAnchor) {
            if (isFolia) {
                pendingRtp.remove(player.getUniqueId());
            }
            return;
        }

        if (spawnManager.isRtpEnabled()) {
            if (isFolia) {
                handleFoliaRespawn(event, player);
            } else {
                handlePaperRespawn(event, player);
            }
            return;
        }

        // RTP disabled - fallback to spawn
        if (spawnManager.isTpNoRespawnPoint() || spawnManager.isTpOnDeath()) {
            if (spawnManager.hasSpawn()) {
                event.setRespawnLocation(spawnManager.getSpawn());
            }
        }
    }

    // ==================== FOLIA: Async Pool-based ====================

    private void handleFoliaRespawn(PlayerRespawnEvent event, Player player) {
        pendingRtp.remove(player.getUniqueId());

        Location loc = spawnManager.pollFromPool();
        if (loc != null) {
            event.setRespawnLocation(loc);
        } else {
            if (spawnManager.hasSpawn()) {
                event.setRespawnLocation(spawnManager.getSpawn());
            }
            scheduleFoliaRtpRetry(player, 0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isFolia) return;
        if (!spawnManager.isRtpEnabled()) return;

        Player player = event.getEntity();
        if (player.getBedSpawnLocation() != null || hasValidRespawnAnchor(player)) return;

        pendingRtp.add(player.getUniqueId());
        scheduleFoliaAliveCheck(player, 0);
    }

    private void scheduleFoliaAliveCheck(Player player, int elapsed) {
        if (elapsed > 1200) {
            pendingRtp.remove(player.getUniqueId());
            return;
        }

        PaperFoliaTasks.runForPlayerDelayed(spawnManager.getPlugin(), player, task -> {
            if (!player.isOnline()) {
                pendingRtp.remove(player.getUniqueId());
                return;
            }

            if (!player.isDead()) {
                if (pendingRtp.remove(player.getUniqueId())) {
                    doFoliaRtpTeleport(player);
                }
            } else {
                if (pendingRtp.contains(player.getUniqueId())) {
                    scheduleFoliaAliveCheck(player, elapsed + 5);
                }
            }
        }, 5L);
    }

    private void doFoliaRtpTeleport(Player player) {
        Location loc = spawnManager.pollFromPool();
        if (loc != null) {
            player.teleportAsync(loc);
        } else {
            scheduleFoliaRtpRetry(player, 0);
        }
    }

    private void scheduleFoliaRtpRetry(Player player, int attempt) {
        if (attempt >= 10) return;
        PaperFoliaTasks.runForPlayerDelayed(spawnManager.getPlugin(), player, task -> {
            if (!player.isOnline()) return;
            Location loc = spawnManager.pollFromPool();
            if (loc != null) {
                player.teleportAsync(loc);
            } else {
                scheduleFoliaRtpRetry(player, attempt + 1);
            }
        }, 20L);
    }

    // ==================== PAPER/BUKKIT: Sync ====================

    private void handlePaperRespawn(PlayerRespawnEvent event, Player player) {
        Location loc = spawnManager.getRandomLocation();
        if (loc != null) {
            event.setRespawnLocation(loc);
        } else if (spawnManager.hasSpawn()) {
            event.setRespawnLocation(spawnManager.getSpawn());
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------
    private boolean hasValidRespawnAnchor(Player player) {
        try {
            return player.getRespawnLocation() != null
                    && player.getRespawnLocation().getBlock().getType()
                    == org.bukkit.Material.RESPAWN_ANCHOR;
        } catch (Exception e) {
            return false;
        }
    }
}