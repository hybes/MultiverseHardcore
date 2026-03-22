package dev.hybes.multiversehardcore.commands.mainsubcommands;

import dev.hybes.multiversehardcore.commands.MainSubcommand;
import dev.hybes.multiversehardcore.exceptions.InvalidCommandInputException;
import dev.hybes.multiversehardcore.models.HardcoreWorld;
import dev.hybes.multiversehardcore.utils.MessageSender;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GetWorldsListSubcommand extends MainSubcommand {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        try {
            initProperties(sender, args);
            checkSenderHasPermission();
            MessageSender.sendInfo(sender, "Worlds list: ");
            List<HardcoreWorld> hcWorlds = HardcoreWorld.getHardcoreWorlds();
            StringBuilder worldsListMessage = new StringBuilder();
            for (HardcoreWorld hcWorld : hcWorlds) {
                appendWorldInfo(worldsListMessage, hcWorld);
            }
            sender.sendMessage(worldsListMessage.toString());
        } catch (InvalidCommandInputException e) {
            MessageSender.sendError(sender, e.getMessage());
        }
        return true;
    }

    @Override
    protected String getRequiredPermission() {
        return "multiversehardcore.list";
    }

    private void appendWorldInfo(@NotNull StringBuilder worldsListMessage, @NotNull HardcoreWorld hcWorld) {
        String worldName = hcWorld.getConfiguration().getWorld().getName();
        worldsListMessage.append(worldName);
        worldsListMessage.append("\n");
        if (hcWorld.getConfiguration().hasNether()) {
            worldsListMessage.append("\t").append(worldName).append("_nether\n");
        }
        if (hcWorld.getConfiguration().hasTheEnd()) {
            worldsListMessage.append("\t").append(worldName).append("_the_end\n");
        }
    }
}
