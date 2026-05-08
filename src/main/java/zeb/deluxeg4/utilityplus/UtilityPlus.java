package zeb.deluxeg4.utilityplus;

import zeb.deluxeg4.utilityplus.commands.*;
import zeb.deluxeg4.utilityplus.invsee.InventorySeeMode;
import zeb.deluxeg4.utilityplus.invsee.InventorySeeSessionManager;
import zeb.deluxeg4.utilityplus.listeners.*;
import zeb.deluxeg4.utilityplus.managers.*;
import zeb.deluxeg4.utilityplus.tabcomplete.TabCompleterManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class UtilityPlus extends JavaPlugin {

    private SpawnManager spawnManager;
    private HomeManager  homeManager;
    private TPAManager   tpaManager;
    private ChatManager  chatManager;
    private TeamManager  teamManager;
    private StatsManager statsManager;
    private DeathMessageManager deathMessageManager;
    private TabListManager tabListManager;
    private AnnouncementManager announcementManager;
    private VanishCommand vanishCommand;
    private TickMonitor tickMonitor;
    private CpuMonitor  cpuMonitor;
    private InventorySeeSessionManager inventorySeeSessionManager;
    private InventorySeeSessionManager enderChestSeeSessionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ── Managers ─────────────────────────────────────────────────
        spawnManager = new SpawnManager(this);
        homeManager  = new HomeManager(this);
        tpaManager   = new TPAManager(this);
        chatManager  = new ChatManager(this);
        teamManager  = new TeamManager(this);
        statsManager = new StatsManager(this);
        deathMessageManager = new DeathMessageManager(this);
        tabListManager = new TabListManager(this);
        announcementManager = new AnnouncementManager(this);
        tickMonitor  = new TickMonitor(this);
        cpuMonitor   = new CpuMonitor(this);
        inventorySeeSessionManager = new InventorySeeSessionManager(this, InventorySeeMode.INVENTORY);
        enderChestSeeSessionManager = new InventorySeeSessionManager(this, InventorySeeMode.ENDER_CHEST);

        // ── Executors ─────────────────────────────────────────────────
        // Spawn
        SpawnCommand spawnCmd = new SpawnCommand(spawnManager);
        registerCommand("setspawn", spawnCmd);

        TwoBTwoTCommand twoBTwoTCommand = new TwoBTwoTCommand(this, chatManager);
        registerCommands(twoBTwoTCommand,
                "ignore", "ignorehard", "ignorelist", "ignoredeathmsgs",
                "togglechat", "toggleprivatemsgs", "toggledeathmsgs", "toggledeathmsgshard");

        PMCommand pmCmd = new PMCommand(chatManager);
        registerCommands(pmCmd, "msg", "w", "whisper", "pm", "r", "reply", "l", "last");

        // Team
        registerCommand("team", new TeamCommand(teamManager, chatManager));

        // Reload
        registerCommand("upreload", new ReloadCommand(this));

        // Vanish
        vanishCommand = new VanishCommand(this);
        registerCommand("v", vanishCommand);

        // Broadcast
        BroadcastCommand bcCmd = new BroadcastCommand(this);
        registerCommands(bcCmd, "bc", "broadcast");

        // Gamemode
        GamemodeCommand gmCmd = new GamemodeCommand();
        registerCommands(gmCmd, "gmc", "gms", "gmsp", "gma");

        // Kill
        registerCommand("kill", new KillCommand(this));

        // Shutdown countdown
        registerCommand("stopnow", new StopNowCommand(this));

        // Overclock
        OverclockCommand overclockCommand = new OverclockCommand(this);
        registerCommand("overclock", overclockCommand);

        // Inventory GUIs
        InventorySeeCommand inventorySeeCommand = new InventorySeeCommand(this);
        registerCommands(inventorySeeCommand, "invsee", "enderchestsee");

        // Summon
        registerCommand("s", new STapwarp());

        // Help
        registerCommand("help", new HelpCommand(this));

        // TPS More
        TPSMoreCommand tpsMoreCmd = new TPSMoreCommand(tickMonitor, cpuMonitor);
        registerCommands(tpsMoreCmd, "tpsmore", "tps");

        // ── Tab Completers ────────────────────────────────────────────
        TabCompleterManager tab = new TabCompleterManager(homeManager, teamManager);
        List<String> allCmds = List.of(
            "setspawn",
            "ignore","ignorehard","ignorelist","ignoredeathmsgs",
            "togglechat","toggleprivatemsgs","toggledeathmsgs","toggledeathmsgshard",
            "msg","w","whisper","pm","r","reply","l","last",
            "team","upreload","stopnow",
            "v","bc","broadcast","gmc","gms","gmsp","gma","kill","s","help",
            "tpsmore", "tps", "invsee", "enderchestsee"
        );
        registerTabCompleters(tab, allCmds);
        command("overclock").setTabCompleter(overclockCommand);

        // ── Listeners ─────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new SpawnListener(spawnManager), this);
        getServer().getPluginManager().registerEvents(new TPAListener(tpaManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(chatManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        getServer().getPluginManager().registerEvents(new TeamListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new JoinMessageListener(this), this);
        getServer().getPluginManager().registerEvents(new StatsListener(statsManager, chatManager, deathMessageManager, this), this);
        getServer().getPluginManager().registerEvents(new StatsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new TabListListener(this, tabListManager), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this, vanishCommand), this);
        getServer().getPluginManager().registerEvents(new InventorySeeListener(this), this);

        getLogger().info("UtilityPlus enabled!");
    }

    @Override
    public void onDisable() {
        if (spawnManager != null) spawnManager.saveData();
        if (homeManager  != null) homeManager.saveData();
        if (chatManager != null) chatManager.saveData();
        if (teamManager  != null) teamManager.saveData();
        if (statsManager != null) statsManager.saveData();
        if (tabListManager != null) tabListManager.stop();
        if (announcementManager != null) announcementManager.stop();
        if (vanishCommand != null) vanishCommand.saveData();
        if (inventorySeeSessionManager != null) inventorySeeSessionManager.closeAll();
        if (enderChestSeeSessionManager != null) enderChestSeeSessionManager.closeAll();
        getLogger().info("UtilityPlus disabled!");
    }

    public SpawnManager getSpawnManager() { return spawnManager; }
    public HomeManager  getHomeManager()  { return homeManager; }
    public TPAManager   getTpaManager()   { return tpaManager; }
    public ChatManager  getChatManager()  { return chatManager; }
    public TeamManager  getTeamManager()  { return teamManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public DeathMessageManager getDeathMessageManager() { return deathMessageManager; }
    public TabListManager getTabListManager() { return tabListManager; }
    public AnnouncementManager getAnnouncementManager() { return announcementManager; }
    public InventorySeeSessionManager getInventorySeeSessionManager() { return inventorySeeSessionManager; }
    public InventorySeeSessionManager getEnderChestSeeSessionManager() { return enderChestSeeSessionManager; }

    private void registerCommands(CommandExecutor executor, String... names) {
        for (String name : names) {
            registerCommand(name, executor);
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        command(name).setExecutor(executor);
    }

    private void registerTabCompleters(TabCompleter completer, List<String> names) {
        for (String name : names) {
            command(name).setTabCompleter(completer);
        }
    }

    private PluginCommand command(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '/" + name + "' is missing from plugin.yml");
        }
        return command;
    }
}
