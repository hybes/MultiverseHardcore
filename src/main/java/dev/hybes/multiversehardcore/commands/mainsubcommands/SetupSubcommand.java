package dev.hybes.multiversehardcore.commands.mainsubcommands;

import dev.hybes.multiversehardcore.commands.MainSubcommand;
import dev.hybes.multiversehardcore.setup.SetupManager;
import dev.hybes.multiversehardcore.utils.MessageSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles responses from the interactive setup wizard.
 * Usage: /mvhc setup <value>
 */
public final class SetupSubcommand extends MainSubcommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            MessageSender.sendError(sender, "Only players can use the setup wizard.");
            return true;
        }

        Player player = (Player) sender;
        SetupManager manager = SetupManager.getInstance();

        if (!manager.hasSession(player.getUniqueId())) {
            MessageSender.sendError(player, "You don't have an active setup session. Use /mvhc create <world> or /mvhc makehc <world> to start one.");
            return true;
        }

        if (args.length < 2) {
            MessageSender.sendError(player, "Usage: /mvhc setup <value>");
            return true;
        }

        String value = args[1];

        if (value.equalsIgnoreCase("cancel")) {
            manager.cancelSession(player.getUniqueId());
            MessageSender.sendInfo(player, "Setup cancelled.");
            return true;
        }

        manager.handleResponse(player, value);
        return true;
    }

    @Override
    protected String getRequiredPermission() {
        return "multiversehardcore.create";
    }
}
