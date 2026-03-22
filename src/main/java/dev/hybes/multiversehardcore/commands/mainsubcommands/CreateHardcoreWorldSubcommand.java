package dev.hybes.multiversehardcore.commands.mainsubcommands;

import dev.hybes.multiversehardcore.commands.HelpCommand;
import dev.hybes.multiversehardcore.commands.MainSubcommand;
import dev.hybes.multiversehardcore.exceptions.HardcoreWorldCreationException;
import dev.hybes.multiversehardcore.exceptions.InvalidCommandInputException;
import dev.hybes.multiversehardcore.models.HardcoreWorld;
import dev.hybes.multiversehardcore.models.HardcoreWorldConfiguration;
import dev.hybes.multiversehardcore.setup.SetupManager;
import dev.hybes.multiversehardcore.setup.SetupSession;
import dev.hybes.multiversehardcore.utils.MessageSender;
import dev.hybes.multiversehardcore.utils.WorldUtils;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;

import java.util.Date;

public final class CreateHardcoreWorldSubcommand extends MainSubcommand {

    private boolean createNether, createEnd;
    private String worldName;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        try {
            initProperties(sender, args);
            checkValidInput();

            // If only world name given and sender is a player, launch setup wizard
            if (args.length == 2 && sender instanceof Player) {
                SetupManager.getInstance().startSession((Player) sender, worldName, SetupSession.Mode.CREATE);
                return true;
            }

            attemptWorldCreation();
            makeWorldHardcore();
            sendSuccessMessage();
        } catch (HardcoreWorldCreationException e) {
            handleHardcoreWorldCreationException(e);
        } catch (InvalidCommandInputException e) {
            MessageSender.sendError(sender, e.getMessage());
        }
        return true;
    }

    @Override
    protected String getRequiredPermission() {
        return "multiversehardcore.create";
    }

    protected void initProperties(@NotNull CommandSender sender, @NotNull String[] args) {
        super.initProperties(sender, args);
        createNether = args.length > 3 ? Boolean.parseBoolean(args[3]) : true;
        createEnd = args.length > 4 ? Boolean.parseBoolean(args[4]) : true;
        worldName = args.length > 1 ? args[1] : "";
    }

    private void checkValidInput() throws InvalidCommandInputException {
        checkSenderHasPermission();
        checkCommandContainsWorldName();
        checkWorldDoesNotExist();
    }

    private void checkWorldDoesNotExist() throws InvalidCommandInputException {
        if (WorldUtils.worldExists(worldName)) {
            throw new InvalidCommandInputException("World " + worldName + " already exists");
        }
    }

    private void checkCommandContainsWorldName() throws InvalidCommandInputException {
        if (args.length < 2) {
            throw new InvalidCommandInputException(getWrongUsageMessage(HelpCommand.CREATE_COMMAND));
        }
    }

    private void attemptWorldCreation() throws InvalidCommandInputException {
        MessageSender.sendInfo(sender, "Starting creation of world(s)...");
        if (!createWorlds()) {
            throw new InvalidCommandInputException("World(s) could not be created!");
        }
    }

    private void makeWorldHardcore() throws HardcoreWorldCreationException {
        HardcoreWorldConfiguration configuration = getConfigurationFromArgs();
        HardcoreWorld.createHardcoreWorld(configuration);
    }

    private void sendSuccessMessage() {
        MessageSender.sendSuccess(sender, "World " + ChatColor.DARK_GREEN + worldName + ChatColor.GREEN + " created!");
    }

    private void handleHardcoreWorldCreationException(@NotNull HardcoreWorldCreationException exception) {
        WorldManager worldManager = plugin.getWorldManager();
        MessageSender.sendError(sender, exception.getMessage());
        deleteWorldIfExists(worldManager, worldName);
        if (createNether) deleteWorldIfExists(worldManager, worldName + "_nether");
        if (createEnd) deleteWorldIfExists(worldManager, worldName + "_the_end");
    }

    private void deleteWorldIfExists(@NotNull WorldManager worldManager, @NotNull String name) {
        worldManager.getWorld(name).peek(w ->
                worldManager.deleteWorld(DeleteWorldOptions.world(w)));
    }

    private HardcoreWorldConfiguration getConfigurationFromArgs() {
        return new HardcoreWorldConfiguration(
                plugin.getServer().getWorld(args[1]),
                args.length > 9 ? plugin.getServer().getWorld(args[9]) : null,
                new Date(),
                args.length > 5 ? Boolean.parseBoolean(args[5]) : true,
                args.length > 6 ? Long.parseLong(args[6]) : 30,
                args.length > 2 ? Boolean.parseBoolean(args[2]) : true,
                args.length > 7 ? Boolean.parseBoolean(args[7]) : true,
                args.length > 8 ? Boolean.parseBoolean(args[8]) : true);
    }

    private boolean createWorlds() {
        return createOverworld() && createNetherIfNecessary() && createEndIfNecessary();
    }

    private boolean createOverworld() {
        return createHardcoreWorld(worldName, World.Environment.NORMAL);
    }

    private boolean createNetherIfNecessary() {
        WorldManager worldManager = plugin.getWorldManager();
        if (createNether && !createHardcoreWorld(worldName + "_nether", World.Environment.NETHER)) {
            deleteWorldIfExists(worldManager, worldName);
            return false;
        }
        return true;
    }

    private boolean createEndIfNecessary() {
        WorldManager worldManager = plugin.getWorldManager();
        if (createEnd && !createHardcoreWorld(worldName + "_the_end", World.Environment.THE_END)) {
            deleteWorldIfExists(worldManager, worldName);
            if (createNether) deleteWorldIfExists(worldManager, worldName + "_nether");
            return false;
        }
        return true;
    }

    private boolean createHardcoreWorld(@NotNull String worldName, @NotNull World.Environment environment) {
        try {
            attemptWorldCreation(worldName, environment);
            makeWorldAttributesHardcore(worldName);
            return true;
        } catch (HardcoreWorldCreationException e) {
            return false;
        }
    }

    private void attemptWorldCreation(@NotNull String worldName, @NotNull World.Environment environment)
            throws HardcoreWorldCreationException {
        WorldManager worldManager = plugin.getWorldManager();
        HardcoreWorldCreationException worldCreationException =
                new HardcoreWorldCreationException("World " + worldName + " could not be created");
        try {
            var result = worldManager.createWorld(
                    CreateWorldOptions.worldName(worldName)
                            .environment(environment)
            );
            if (result.isFailure()) {
                throw worldCreationException;
            }
        } catch (HardcoreWorldCreationException e) {
            throw e;
        } catch (Exception e) {
            throw worldCreationException;
        }
    }

    private void makeWorldAttributesHardcore(@NotNull String worldName) {
        WorldManager worldManager = plugin.getWorldManager();
        worldManager.getLoadedWorld(worldName).peek(world -> {
            world.setDifficulty(Difficulty.HARD);
        });
    }
}
