package dev.hybes.multiversehardcore.commands.mainsubcommands;

import dev.hybes.multiversehardcore.commands.MainSubcommand;
import dev.hybes.multiversehardcore.exceptions.InvalidCommandInputException;
import dev.hybes.multiversehardcore.utils.MessageSender;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class GetPluginVersionSubcommand extends MainSubcommand {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        try {
            initProperties(sender, args);
            checkSenderHasPermission();
            String pluginVersion = plugin.getDescription().getVersion();
            MessageSender.sendInfo(sender, "Version: " + pluginVersion);
        } catch (InvalidCommandInputException e) {
            MessageSender.sendError(sender, e.getMessage());
        }
        return true;
    }

    @Override
    protected String getRequiredPermission() {
        return "multiversehardcore.version";
    }
}
