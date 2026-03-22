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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public final class MakeWorldHardcoreSubcommand extends MainSubcommand {

    private String worldName;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        try {
            initProperties(sender, args);
            checkValidInput();

            // If only world name given and sender is a player, launch setup wizard
            if (args.length == 2 && sender instanceof Player) {
                SetupManager.getInstance().startSession((Player) sender, worldName, SetupSession.Mode.MAKEHC);
                return true;
            }

            makeWorldHardcore();
            sendSuccessMessage();
        } catch (HardcoreWorldCreationException | InvalidCommandInputException e) {
            MessageSender.sendError(sender, e.getMessage());
        }
        return true;
    }

    @Override
    protected String getRequiredPermission() {
        return "multiversehardcore.makehc";
    }

    protected void initProperties(@NotNull CommandSender sender, @NotNull String[] args) {
        super.initProperties(sender, args);
        worldName = args.length > 1 ? args[1] : "";
    }

    private void checkValidInput() throws InvalidCommandInputException {
        checkSenderHasPermission();
        checkCommandContainsWorldName();
    }

    private void checkCommandContainsWorldName() throws InvalidCommandInputException {
        if (args.length < 2) {
            throw new InvalidCommandInputException(getWrongUsageMessage(HelpCommand.MAKE_COMMAND));
        }
    }

    private void makeWorldHardcore() throws HardcoreWorldCreationException {
        HardcoreWorldConfiguration configuration = getConfigurationFromArgs(args);
        HardcoreWorld.createHardcoreWorld(configuration);
    }

    private void sendSuccessMessage() {
        MessageSender.sendSuccess(sender, "World " + ChatColor.DARK_GREEN + worldName + ChatColor.GREEN + " is now Hardcore!");
    }

    private HardcoreWorldConfiguration getConfigurationFromArgs(@NotNull String[] args) {
        return new HardcoreWorldConfiguration(
                plugin.getServer().getWorld(args[1]), // world
                args.length > 7 ? plugin.getServer().getWorld(args[7]) : null, // spawnWorld
                new Date(), // startDate
                args.length > 3 ? Boolean.parseBoolean(args[3]) : true, // banForever
                args.length > 4 ? Long.parseLong(args[4]) : 30, // banLength
                args.length > 2 ? Boolean.parseBoolean(args[2]) : true, // spectatorMode
                args.length > 5 ? Boolean.parseBoolean(args[5]) : true, // includeNether
                args.length > 6 ? Boolean.parseBoolean(args[6]) : true); // includeEnd
    }
}
