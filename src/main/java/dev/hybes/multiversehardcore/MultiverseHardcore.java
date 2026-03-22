package dev.hybes.multiversehardcore;

import dev.hybes.multiversehardcore.commands.HelpCommand;
import dev.hybes.multiversehardcore.commands.MainCommand;
import dev.hybes.multiversehardcore.events.PlayerChangeOfWorld;
import dev.hybes.multiversehardcore.events.PlayerDeath;
import dev.hybes.multiversehardcore.events.PlayerJoin;
import dev.hybes.multiversehardcore.files.HardcoreWorldsList;
import dev.hybes.multiversehardcore.utils.MessageSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;

import java.io.File;

public class MultiverseHardcore extends JavaPlugin {

    private static MultiverseHardcore instance;
    private MultiverseCoreApi multiverseCoreApi;
    private final boolean testing;

    public MultiverseHardcore() {
        testing = false;
    }

    // Constructor needed for tests.
    protected MultiverseHardcore(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        multiverseCoreApi = null;
        testing = true;
    }

    public static MultiverseHardcore getInstance() {
        return instance;
    }

    public MultiverseCoreApi getMultiverseCoreApi() {
        return multiverseCoreApi;
    }

    public WorldManager getWorldManager() {
        return multiverseCoreApi != null ? multiverseCoreApi.getWorldManager() : null;
    }

    public void setMultiverseCoreApi(MultiverseCoreApi multiverseCoreApi) {
        this.multiverseCoreApi = multiverseCoreApi;
    }

    @Override
    public void onEnable() {
        instance = this;
        if (!testing) {
            loadMultiverseCore();
        }
        saveDefaultConfig();
        loadMessagesPrefix();
        loadEventListeners();
        loadCommands();
        scheduleWorldCleanUp();
    }

    private void loadMultiverseCore() {
        multiverseCoreApi = MultiverseCoreApi.get();
        if (multiverseCoreApi == null) {
            throw new RuntimeException("Multiverse-Core not found!");
        }
    }

    private void loadMessagesPrefix() {
        String prefix = getConfig().getString("prefix");
        if (prefix != null) {
            MessageSender.setPrefix(prefix);
        }
    }

    private void loadEventListeners() {
        Listener[] listeners = {new PlayerDeath(), new PlayerChangeOfWorld(), new PlayerJoin()};
        for (Listener listener : listeners) {
            loadEventListener(listener);
        }
    }

    private void loadEventListener(Listener eventListener) {
        getServer().getPluginManager().registerEvents(eventListener, this);
    }

    private void loadCommands() {
        PluginCommand mainCommand = getCommand("mvhc");
        PluginCommand helpCommand = getCommand("mvhchelp");
        if (mainCommand != null && helpCommand != null) {
            mainCommand.setExecutor(new MainCommand());
            helpCommand.setExecutor(new HelpCommand());
        } else {
            throw new RuntimeException("Multiverse-Hardcore Command not found!");
        }
    }

    private void scheduleWorldCleanUp() {
        int cleanWorldsTicks = getConfig().getInt("clean_worlds_ticks");
        getServer().getScheduler().runTaskLater(this, HardcoreWorldsList.instance::cleanWorlds, cleanWorldsTicks);
    }
}
