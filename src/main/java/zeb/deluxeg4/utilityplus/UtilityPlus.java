package zeb.deluxeg4.utilityplus;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import zeb.deluxeg4.utilityplus.commands.BroadcastCommand;
import zeb.deluxeg4.utilityplus.commands.HelpCommand;
import zeb.deluxeg4.utilityplus.commands.IgnoreCommand;
import zeb.deluxeg4.utilityplus.commands.IgnoreListCommand;
import zeb.deluxeg4.utilityplus.commands.KillCommand;
import zeb.deluxeg4.utilityplus.commands.PMCommand;
import zeb.deluxeg4.utilityplus.commands.PingCommand;
import zeb.deluxeg4.utilityplus.commands.ReloadCommand;
import zeb.deluxeg4.utilityplus.commands.StopNowCommand;
import zeb.deluxeg4.utilityplus.commands.TPSMoreCommand;
import zeb.deluxeg4.utilityplus.commands.ToggleChatCommand;
import zeb.deluxeg4.utilityplus.commands.ToggleDeathMessagesCommand;
import zeb.deluxeg4.utilityplus.commands.TogglePrivateMessagesCommand;
import zeb.deluxeg4.utilityplus.commands.UptimeCommand;
import zeb.deluxeg4.utilityplus.commands.VanishCommand;
import zeb.deluxeg4.utilityplus.listeners.AnvilListener;
import zeb.deluxeg4.utilityplus.listeners.ChatListener;
import zeb.deluxeg4.utilityplus.listeners.DeathMessageListener;
import zeb.deluxeg4.utilityplus.listeners.JoinMessageListener;
import zeb.deluxeg4.utilityplus.listeners.SpawnListener;
import zeb.deluxeg4.utilityplus.listeners.TabListListener;
import zeb.deluxeg4.utilityplus.listeners.VanishListener;
import zeb.deluxeg4.utilityplus.managers.AnnouncementManager;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.managers.CpuMonitor;
import zeb.deluxeg4.utilityplus.managers.DeathMessageManager;
import zeb.deluxeg4.utilityplus.managers.SpawnManager;
import zeb.deluxeg4.utilityplus.managers.TabListManager;
import zeb.deluxeg4.utilityplus.managers.TickMonitor;
import zeb.deluxeg4.utilityplus.tabcomplete.TabCompleterManager;

import java.util.List;

public class UtilityPlus extends JavaPlugin {

    private SpawnManager spawnManager;
    private ChatManager chatManager;
    private DeathMessageManager deathMessageManager;
    private TabListManager tabListManager;
    private AnnouncementManager announcementManager;
    private VanishCommand vanishCommand;
    private TickMonitor tickMonitor;
    private CpuMonitor cpuMonitor;

    /** Enables UtilityPlus and registers commands, listeners, and managers. */
    @Override
    public void onEnable() {
        saveDefaultConfig();

        spawnManager = new SpawnManager(this);
        chatManager = new ChatManager(this);
        deathMessageManager = new DeathMessageManager(this);
        tabListManager = new TabListManager(this);
        announcementManager = new AnnouncementManager(this);
        tickMonitor = new TickMonitor(this);
        cpuMonitor = new CpuMonitor(this);

        registerCommand("ignore", new IgnoreCommand(chatManager, false, false));
        registerCommand("ignorehard", new IgnoreCommand(chatManager, true, false));
        registerCommand("ignoredeathmsgs", new IgnoreCommand(chatManager, true, true));
        registerCommand("ignorelist", new IgnoreListCommand(chatManager));
        registerCommand("togglechat", new ToggleChatCommand(chatManager));
        registerCommand("toggleprivatemsgs", new TogglePrivateMessagesCommand(chatManager));
        registerCommand("toggledeathmsgs", new ToggleDeathMessagesCommand(chatManager, false));
        registerCommand("toggledeathmsgshard", new ToggleDeathMessagesCommand(chatManager, true));

        final PMCommand privateMessageCommand = new PMCommand(chatManager);
        registerCommands(privateMessageCommand, "msg", "w", "whisper", "pm", "r", "reply", "l", "last");

        registerCommand("upreload", new ReloadCommand(this));

        vanishCommand = new VanishCommand(this);
        registerCommand("v", vanishCommand);

        final BroadcastCommand broadcastCommand = new BroadcastCommand(this);
        registerCommands(broadcastCommand, "bc", "broadcast");

        registerCommand("kill", new KillCommand(this));
        registerCommand("stopnow", new StopNowCommand(this));

        registerCommand("help", new HelpCommand(this));

        final TPSMoreCommand tpsMoreCommand = new TPSMoreCommand(tickMonitor, cpuMonitor);
        registerCommands(tpsMoreCommand, "tpsmore", "tps");

        final PingCommand pingCommand = new PingCommand();
        registerCommands(pingCommand, "ping", "pingall");
        registerCommand("uptime", new UptimeCommand());

        final TabCompleterManager tabCompleter = new TabCompleterManager();
        final List<String> commandNames = List.of(
                "ignore", "ignorehard", "ignorelist", "ignoredeathmsgs",
                "togglechat", "toggleprivatemsgs", "toggledeathmsgs", "toggledeathmsgshard",
                "msg", "w", "whisper", "pm", "r", "reply", "l", "last",
                "upreload", "stopnow",
                "v", "bc", "broadcast", "kill", "help",
                "tpsmore", "tps", "ping", "pingall",
                "uptime"
        );
        registerTabCompleters(tabCompleter, commandNames);

        getServer().getPluginManager().registerEvents(new SpawnListener(spawnManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(chatManager), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        getServer().getPluginManager().registerEvents(new JoinMessageListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathMessageListener(chatManager, deathMessageManager, this), this);
        getServer().getPluginManager().registerEvents(new TabListListener(this, tabListManager), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this, vanishCommand), this);

        getLogger().info("UtilityPlus enabled!");
    }

    /** Disables UtilityPlus and flushes persistent manager state. */
    @Override
    public void onDisable() {
        if (spawnManager != null) {
            spawnManager.saveData();
        }
        if (chatManager != null) {
            chatManager.saveData();
        }
        if (tabListManager != null) {
            tabListManager.stop();
        }
        if (announcementManager != null) {
            announcementManager.stop();
        }
        if (vanishCommand != null) {
            vanishCommand.saveData();
        }
        getLogger().info("UtilityPlus disabled!");
    }

    /** Returns the spawn manager. */
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    /** Returns the chat manager. */
    public ChatManager getChatManager() {
        return chatManager;
    }

    /** Returns the death-message manager. */
    public DeathMessageManager getDeathMessageManager() {
        return deathMessageManager;
    }

    /** Returns the tab-list manager. */
    public TabListManager getTabListManager() {
        return tabListManager;
    }

    /** Returns the announcement manager. */
    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    private void registerCommands(final CommandExecutor executor, final String... names) {
        for (final String name : names) {
            registerCommand(name, executor);
        }
    }

    private void registerCommand(final String name, final CommandExecutor executor) {
        command(name).setExecutor(executor);
    }

    private void registerTabCompleters(final TabCompleter completer, final List<String> names) {
        for (final String name : names) {
            command(name).setTabCompleter(completer);
        }
    }

    private PluginCommand command(final String name) {
        final PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '/" + name + "' is missing from plugin.yml");
        }
        return command;
    }
}
