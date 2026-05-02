package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * TeamManager — Hypixel-style team system
 *
 * Rules:
 *   - No custom team name — team is identified by owner's name ("Steve's Team")
 *   - 1 player = 1 team max
 *   - Roles: OWNER > CO_LEADER > MEMBER
 *   - Invite system with timeout
 *   - Auto-kick members offline > N minutes (configurable)
 */
public class TeamManager {

    // ── Role ─────────────────────────────────────────────────────────
    public enum Role {
        OWNER("Owner"), CO_LEADER("Co-Leader"), MEMBER("Member");

        private final String display;
        Role(String d) { this.display = d; }
        public String getDisplay() { return display; }

        public boolean isHigherThan(Role other) { return this.ordinal() < other.ordinal(); }

        public Role promoted() {
            if (this == MEMBER) return CO_LEADER;
            if (this == CO_LEADER) return OWNER;
            return null;
        }
        public Role demoted() {
            if (this == OWNER) return CO_LEADER;
            if (this == CO_LEADER) return MEMBER;
            return null;
        }
    }

    // ── Team ─────────────────────────────────────────────────────────
    public static class Team {
        /** Internal key — owner's UUID string (stable even if name changes). */
        public final String id;
        /** Display name — "<ownerName>'s Team", updated on load. */
        public String displayName;

        private final Map<UUID, Role> members = new LinkedHashMap<>();

        public Team(String id, UUID ownerUUID, String ownerName) {
            this.id          = id;
            this.displayName = ownerName + "'s Team";
            members.put(ownerUUID, Role.OWNER);
        }

        public UUID getOwnerUUID() {
            return members.entrySet().stream()
                    .filter(e -> e.getValue() == Role.OWNER)
                    .map(Map.Entry::getKey).findFirst().orElse(null);
        }

        public Role getRole(UUID uuid)          { return members.get(uuid); }
        public boolean hasMember(UUID uuid)     { return members.containsKey(uuid); }
        public int size()                       { return members.size(); }
        public Map<UUID, Role> getMembers()     { return Collections.unmodifiableMap(members); }
        public void addMember(UUID uuid)        { members.put(uuid, Role.MEMBER); }
        public void removeMember(UUID uuid)     { members.remove(uuid); }
        public void setRole(UUID uuid, Role r)  { members.put(uuid, r); }
    }

    // ── Invite ───────────────────────────────────────────────────────
    public static class Invite {
        public final UUID   inviterUUID;
        public final String inviterName;
        public final String teamId;
        public final long   createdAt;

        public Invite(UUID inviterUUID, String inviterName, String teamId) {
            this.inviterUUID = inviterUUID;
            this.inviterName = inviterName;
            this.teamId      = teamId;
            this.createdAt   = System.currentTimeMillis();
        }

        public boolean isExpired(int timeoutSeconds) {
            return (System.currentTimeMillis() - createdAt) / 1000L >= timeoutSeconds;
        }
    }

    // ── State ─────────────────────────────────────────────────────────
    private final Map<String, Team>       teams      = new LinkedHashMap<>(); // teamId → Team
    private final Map<UUID,   String>     playerTeam = new HashMap<>();       // UUID → teamId
    private final Map<UUID,   Invite>     invites    = new HashMap<>();       // inviteeUUID → Invite
    private final Map<UUID,   Long>       offlineSince = new HashMap<>();     // UUID → ms went offline

    private final UtilityPlus plugin;
    private int maxMembers;
    private int inviteTimeoutSeconds;
    private int offlineKickMinutes;

    private File             dataFile;
    private FileConfiguration dataConfig;
    private ScheduledTask    offlineCheckTask;
    private ScheduledTask    pendingSaveTask;

    public TeamManager(UtilityPlus plugin) {
        this.plugin = plugin;
        readConfig();
        loadData();
        startOfflineChecker();
    }

    // ── Config ───────────────────────────────────────────────────────

    private void readConfig() {
        maxMembers           = plugin.getConfig().getInt("team.max-members",      10);
        inviteTimeoutSeconds = plugin.getConfig().getInt("team.invite-timeout",   60);
        offlineKickMinutes   = plugin.getConfig().getInt("team.offline-kick-minutes", 5);
    }

    public void reload() {
        if (offlineCheckTask != null && !offlineCheckTask.isCancelled()) offlineCheckTask.cancel();
        teams.clear();
        playerTeam.clear();
        invites.clear();
        offlineSince.clear();
        readConfig();
        loadData();
        startOfflineChecker();
        plugin.getLogger().info("[TeamManager] Reloaded.");
    }

    // ── Offline auto-kick checker ────────────────────────────────────

    private void startOfflineChecker() {
        // Check every 60 seconds
        offlineCheckTask = PaperFoliaTasks.runGlobalTimer(plugin, (task) -> {
            if (offlineKickMinutes <= 0) return;
            long now = System.currentTimeMillis();
            long threshold = offlineKickMinutes * 60_000L;

            // Check all players who are currently recorded as offline
            List<UUID> toKick = new ArrayList<>();
            for (Map.Entry<UUID, Long> entry : offlineSince.entrySet()) {
                if (now - entry.getValue() >= threshold) toKick.add(entry.getKey());
            }

            for (UUID uuid : toKick) {
                Team team = getPlayerTeam(uuid);
                if (team == null) { offlineSince.remove(uuid); continue; }

                // Never auto-kick the owner
                if (team.getRole(uuid) == Role.OWNER) { offlineSince.remove(uuid); continue; }

                offlineSince.remove(uuid);
                kickMember(team, uuid);

                // Notify online members
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                broadcastToTeam(team, "§e" + (name != null ? name : "A member")
                        + " §7was removed for being offline too long.", null);
                plugin.getLogger().info("[TeamManager] Auto-kicked " + uuid + " from " + team.displayName);
            }
        }, 60 * 20L, 60 * 20L);
    }

    // Call when player goes offline
    public void recordOffline(UUID uuid) {
        if (isInTeam(uuid)) offlineSince.put(uuid, System.currentTimeMillis());
    }

    // Call when player comes back online
    public void recordOnline(UUID uuid) {
        offlineSince.remove(uuid);
    }

    // ── Queries ──────────────────────────────────────────────────────

    public int  getMaxMembers()           { return maxMembers; }
    public int  getInviteTimeoutSeconds() { return inviteTimeoutSeconds; }
    public int  getOfflineKickMinutes()   { return offlineKickMinutes; }

    public Team getTeamById(String id)    { return teams.get(id); }

    public Team getPlayerTeam(UUID uuid) {
        String id = playerTeam.get(uuid);
        return id == null ? null : teams.get(id);
    }

    public boolean isInTeam(UUID uuid)   { return playerTeam.containsKey(uuid); }
    public Collection<Team> getAllTeams() { return teams.values(); }

    // ── Create ───────────────────────────────────────────────────────

    /** Returns null on success, error string on failure. */
    public synchronized String createTeam(UUID ownerUUID, String ownerName) {
        if (isInTeam(ownerUUID)) return "You are already in a team! Use /team leave first.";

        String id = ownerUUID.toString();
        Team team = new Team(id, ownerUUID, ownerName);
        teams.put(id, team);
        playerTeam.put(ownerUUID, id);
        saveLater();
        return null;
    }

    // ── Disband ──────────────────────────────────────────────────────

    public synchronized void disbandTeam(Team team) {
        new ArrayList<>(team.getMembers().keySet()).forEach(uuid -> {
            playerTeam.remove(uuid);
            offlineSince.remove(uuid);
        });
        teams.remove(team.id);
        saveLater();
    }

    // ── Invite system ────────────────────────────────────────────────

    public String sendInvite(UUID inviterUUID, String inviterName, UUID inviteeUUID) {
        Team team = getPlayerTeam(inviterUUID);
        if (team == null)             return "You are not in a team!";
        Role role = team.getRole(inviterUUID);
        if (role == Role.MEMBER)      return "Only Owner and Co-Leader can invite players.";
        if (team.size() >= maxMembers) return "Your team is full! (" + maxMembers + "/" + maxMembers + ")";
        if (isInTeam(inviteeUUID))    return "That player is already in a team.";
        if (invites.containsKey(inviteeUUID) && !invites.get(inviteeUUID).isExpired(inviteTimeoutSeconds))
            return "That player already has a pending invite.";

        invites.put(inviteeUUID, new Invite(inviterUUID, inviterName, team.id));
        return null;
    }

    public Invite getInvite(UUID inviteeUUID) {
        Invite inv = invites.get(inviteeUUID);
        if (inv == null) return null;
        if (inv.isExpired(inviteTimeoutSeconds)) { invites.remove(inviteeUUID); return null; }
        return inv;
    }

    public void removeInvite(UUID inviteeUUID) { invites.remove(inviteeUUID); }

    /** Accept invite — returns error or null on success. */
    public synchronized String acceptInvite(UUID inviteeUUID) {
        Invite inv = getInvite(inviteeUUID);
        if (inv == null) return "You have no pending invite (or it expired).";
        if (isInTeam(inviteeUUID)) { invites.remove(inviteeUUID); return "You are already in a team."; }

        Team team = getTeamById(inv.teamId);
        if (team == null) { invites.remove(inviteeUUID); return "That team no longer exists."; }
        if (team.size() >= maxMembers) { invites.remove(inviteeUUID); return "That team is now full."; }

        team.addMember(inviteeUUID);
        playerTeam.put(inviteeUUID, inv.teamId);
        offlineSince.remove(inviteeUUID);
        invites.remove(inviteeUUID);
        saveLater();
        return null;
    }

    // ── Leave ────────────────────────────────────────────────────────

    /** Returns true if the team was disbanded (owner left with no members). */
    public synchronized boolean leaveTeam(UUID uuid) {
        Team team = getPlayerTeam(uuid);
        if (team == null) return false;

        boolean wasOwner = team.getRole(uuid) == Role.OWNER;
        team.removeMember(uuid);
        playerTeam.remove(uuid);
        offlineSince.remove(uuid);

        if (team.size() == 0) {
            teams.remove(team.id);
            saveLater();
            return true;
        }

        if (wasOwner) {
            // Transfer: prefer Co-Leader, else first Member
            UUID next = team.getMembers().entrySet().stream()
                    .filter(e -> e.getValue() == Role.CO_LEADER)
                    .map(Map.Entry::getKey).findFirst()
                    .orElse(team.getMembers().keySet().iterator().next());
            team.setRole(next, Role.OWNER);
            // Update displayName to new owner
            Player newOwner = Bukkit.getPlayer(next);
            if (newOwner != null) team.displayName = newOwner.getName() + "'s Team";
        }

        saveLater();
        return false;
    }

    // ── Kick ─────────────────────────────────────────────────────────

    public synchronized boolean kickMember(Team team, UUID target) {
        if (!team.hasMember(target)) return false;
        team.removeMember(target);
        playerTeam.remove(target);
        offlineSince.remove(target);
        saveLater();
        return true;
    }

    // ── Promote / Demote ─────────────────────────────────────────────

    public synchronized String promote(Team team, UUID actor, UUID target) {
        Role actorRole  = team.getRole(actor);
        Role targetRole = team.getRole(target);
        if (actorRole == null || targetRole == null) return "Player not in team.";
        if (!actorRole.isHigherThan(targetRole))     return "You cannot promote someone of equal or higher rank.";
        if (targetRole == Role.CO_LEADER) {
            // Promote to Owner → demote actor to Co-Leader
            team.setRole(target, Role.OWNER);
            team.setRole(actor,  Role.CO_LEADER);
            // Update displayName
            Player newOwner = Bukkit.getPlayer(target);
            if (newOwner != null) team.displayName = newOwner.getName() + "'s Team";
            saveLater(); return null;
        }
        Role next = targetRole.promoted();
        if (next == null) return "Cannot promote further.";
        team.setRole(target, next);
        saveLater(); return null;
    }

    public synchronized String demote(Team team, UUID actor, UUID target) {
        Role actorRole  = team.getRole(actor);
        Role targetRole = team.getRole(target);
        if (actorRole == null || targetRole == null) return "Player not in team.";
        if (!actorRole.isHigherThan(targetRole))     return "You cannot demote someone of equal or higher rank.";
        Role next = targetRole.demoted();
        if (next == null || next == Role.OWNER)      return "Cannot demote further.";
        team.setRole(target, next);
        saveLater(); return null;
    }

    // ── Persistence ──────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!dataFile.exists()) return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.contains("teams")) return;

        for (String id : dataConfig.getConfigurationSection("teams").getKeys(false)) {
            String path = "teams." + id;
            String ownerStr = dataConfig.getString(path + ".owner");
            if (ownerStr == null) continue;
            UUID ownerUUID;
            try { ownerUUID = UUID.fromString(ownerStr); } catch (Exception e) { continue; }

            String ownerName = dataConfig.getString(path + ".owner-name", "Unknown");
            Team team = new Team(id, ownerUUID, ownerName);
            playerTeam.put(ownerUUID, id);

            if (dataConfig.contains(path + ".members")) {
                for (String uuidStr : dataConfig.getConfigurationSection(path + ".members").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Role role = Role.valueOf(dataConfig.getString(path + ".members." + uuidStr, "MEMBER"));
                        if (role != Role.OWNER) {
                            team.setRole(uuid, role);
                            playerTeam.put(uuid, id);
                        }
                    } catch (Exception ignored) {}
                }
            }
            teams.put(id, team);
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
        if (dataFile == null) dataFile = new File(plugin.getDataFolder(), "teams.yml");
        dataConfig = new YamlConfiguration();
        for (Team team : teams.values()) {
            String path = "teams." + team.id;
            UUID owner = team.getOwnerUUID();
            dataConfig.set(path + ".owner", owner != null ? owner.toString() : "");
            // Save owner name so we can restore displayName on reload
            Player ownerPlayer = owner != null ? Bukkit.getPlayer(owner) : null;
            String ownerName = ownerPlayer != null ? ownerPlayer.getName()
                    : Bukkit.getOfflinePlayer(owner != null ? owner : new UUID(0,0)).getName();
            dataConfig.set(path + ".owner-name", ownerName != null ? ownerName : "Unknown");

            for (Map.Entry<UUID, Role> entry : team.getMembers().entrySet()) {
                dataConfig.set(path + ".members." + entry.getKey(), entry.getValue().name());
            }
        }
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("[TeamManager] Cannot save teams.yml!"); }
    }

    private synchronized void cancelPendingSave() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            pendingSaveTask.cancel();
        }
        pendingSaveTask = null;
    }

    // ── Broadcast helper ─────────────────────────────────────────────
    public static void broadcastToTeam(Team team, String message, UUID exclude) {
        for (UUID uuid : team.getMembers().keySet()) {
            if (uuid.equals(exclude)) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                UtilityPlus plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(UtilityPlus.class);
                PaperFoliaTasks.runForSender(plugin, p, () -> p.sendMessage("§7[§6" + team.displayName + "§7] " + message));
            }
        }
    }
}
