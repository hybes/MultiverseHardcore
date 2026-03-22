package dev.hybes.multiversehardcore.commands;

import dev.hybes.multiversehardcore.exceptions.InvalidMainSubcommandException;
import dev.hybes.multiversehardcore.commands.mainsubcommands.*;
import org.jetbrains.annotations.NotNull;

public class MainSubcommandFactory {
    public MainSubcommand getMainSubcommand(@NotNull String subcommandName) throws InvalidMainSubcommandException {
        switch (subcommandName.toLowerCase()) {
            case "create":
            case "createworld":
                return new CreateHardcoreWorldSubcommand();
            case "makehc":
            case "makeworldhc":
            case "makeworldhardcore":
                return new MakeWorldHardcoreSubcommand();
            case "player":
            case "playerinfo":
            case "seeplayer":
                return new GetPlayerParticipationInfoSubcommand();
            case "world":
            case "worldinfo":
            case "seeworld":
                return new GetWorldInfoSubcommand();
            case "list":
            case "worlds":
                return new GetWorldsListSubcommand();
            case "unban":
                return new UnbanPlayerSubcommand();
            case "version":
                return new GetPluginVersionSubcommand();
            case "setup":
                return new SetupSubcommand();
            default:
                throw new InvalidMainSubcommandException("Invalid subcommand <" + subcommandName + ">!");
        }
    }
}
