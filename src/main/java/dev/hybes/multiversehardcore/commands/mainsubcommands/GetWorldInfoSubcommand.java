package dev.hybes.multiversehardcore.commands.mainsubcommands;

import dev.hybes.multiversehardcore.commands.HelpCommand;
import dev.hybes.multiversehardcore.commands.MainSubcommand;
import dev.hybes.multiversehardcore.exceptions.InvalidCommandInputException;
import dev.hybes.multiversehardcore.exceptions.WorldIsNotHardcoreException;
import dev.hybes.multiversehardcore.models.HardcoreWorld;
import dev.hybes.multiversehardcore.utils.MessageSender;
import dev.hybes.multiversehardcore.utils.WorldUtils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GetWorldInfoSubcommand extends MainSubcommand {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        try {
            initProperties(sender, args);
            checkSenderHasPermission();
            checkConsoleHasSpecifiedArgs();
            World world = getCommandWorld();
            HardcoreWorld hcWorld = new HardcoreWorld(world.getName());
            sender.sendMessage(hcWorld.toString());
        } catch (InvalidCommandInputException | WorldIsNotHardcoreException e) {
            MessageSender.sendError(sender, e.getMessage());
        }
        return true;
    }

    @Override
    protected String getRequiredPermission() {
        return "multiversehardcore.world";
    }

    private void checkConsoleHasSpecifiedArgs() throws InvalidCommandInputException {
        if (args.length < 2 && !(sender instanceof Player)) {
            throw new InvalidCommandInputException(getWrongUsageMessage(HelpCommand.WORLD_COMMAND_OP));
        }
    }

    protected World getCommandWorld() throws InvalidCommandInputException {
        World world = args.length > 1 ? plugin.getServer().getWorld(args[1]) : ((Player) sender).getWorld();
        checkWorldExists(world);
        world = WorldUtils.getNormalWorld(world);
        return world;
    }
}
