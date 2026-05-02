package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.managers.SpawnManager;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class    SpawnListener implements Listener {

    private final SpawnManager spawnManager;

    public SpawnListener(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
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
        if (!spawnManager.isTpOnFirstJoin()) return;

        Player player = event.getPlayer();

        if (spawnManager.isFirstJoin(player.getUniqueId())) {
            spawnManager.markKnown(player.getUniqueId());

            if (spawnManager.isRandomFirstJoin()) {
                spawnManager.findRandomSpawnAsync(player.getLocation(), randomSpawn -> {
                    if (!player.isOnline()) return;

                    Location target = randomSpawn != null ? randomSpawn : spawnManager.getSpawn();
                    if (target == null) return;

                    PaperFoliaTasks.runForPlayer(spawnManager.getPlugin(), player, () -> {
                        PaperFoliaTasks.teleport(player, target, spawnManager.getPlugin(), success -> {
                            if (success) {
                                player.sendMessage(randomSpawn != null
                                        ? "Â§aWelcome to the server! You have been sent to a random spawn."
                                        : "Â§aWelcome to the server! You have been teleported to spawn.");
                            }
                        });
                    });
                });
                return;
            }

            if (spawnManager.hasSpawn()) {
                PaperFoliaTasks.teleport(player, spawnManager.getSpawn(), spawnManager.getPlugin(), success -> {
                    if (success) {
                        player.sendMessage("§aWelcome to the server! You have been teleported to spawn.");
                    }
                });
            }
        }
    }

    // ---------------------------------------------------------------
    // On-death / no-respawn-point teleport
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        boolean hasBedSpawn = event.isBedSpawn() || event.isAnchorSpawn();

        // Priority: Bed/Anchor spawn first
        if (hasBedSpawn) {
            return;
        }

        // Fallback to normal spawn if configured
        if (spawnManager.isTpNoRespawnPoint() || spawnManager.isTpOnDeath()) {
            if (spawnManager.isRandomNoRespawnPoint() && PaperFoliaTasks.isFolia()) {
                Player player = event.getPlayer();
                Location fallback = event.getRespawnLocation();
                spawnManager.findRandomSpawnAsync(fallback, randomSpawn -> {
                    if (randomSpawn != null && player.isOnline()) {
                        PaperFoliaTasks.runForPlayer(spawnManager.getPlugin(), player, () -> {
                            PaperFoliaTasks.teleport(player, randomSpawn, spawnManager.getPlugin(), null);
                        });
                    }
                });
                if (spawnManager.hasSpawn()) {
                    event.setRespawnLocation(spawnManager.getSpawn());
                }
                return;
            }

            Location randomSpawn = spawnManager.isRandomNoRespawnPoint()
                    ? spawnManager.findRandomSpawn(event.getRespawnLocation())
                    : null;
            if (randomSpawn != null) {
                event.setRespawnLocation(randomSpawn);
                return;
            }

            if (spawnManager.hasSpawn()) {
                event.setRespawnLocation(spawnManager.getSpawn());
            }
        }
    }
}
