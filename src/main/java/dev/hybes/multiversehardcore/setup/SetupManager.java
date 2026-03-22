package dev.hybes.multiversehardcore.setup;

import dev.hybes.multiversehardcore.MultiverseHardcore;
import dev.hybes.multiversehardcore.exceptions.HardcoreWorldCreationException;
import dev.hybes.multiversehardcore.models.HardcoreWorld;
import dev.hybes.multiversehardcore.models.HardcoreWorldConfiguration;
import dev.hybes.multiversehardcore.utils.MessageSender;
import dev.hybes.multiversehardcore.utils.WorldUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;

import org.mvplugins.multiverse.core.world.MultiverseWorld;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages interactive setup sessions for creating/converting hardcore worlds.
 * Renders clickable chat UI with [Yes]/[No] buttons per step.
 */
public class SetupManager {

    private static final SetupManager INSTANCE = new SetupManager();
    private final Map<UUID, SetupSession> sessions = new HashMap<>();

    private SetupManager() {
    }

    public static SetupManager getInstance() {
        return INSTANCE;
    }

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public SetupSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    /**
     * Start a new setup wizard for a player.
     */
    public void startSession(Player player, String worldName, SetupSession.Mode mode) {
        // For makehc, validate the world exists before starting
        if (mode == SetupSession.Mode.MAKEHC && !WorldUtils.worldExists(worldName)) {
            MessageSender.sendError(player, "World '" + worldName + "' does not exist! Create it first with Multiverse or use /mvhc create " + worldName);
            return;
        }
        // For create, validate the world doesn't already exist
        if (mode == SetupSession.Mode.CREATE && WorldUtils.worldExists(worldName)) {
            MessageSender.sendError(player, "World '" + worldName + "' already exists! Use /mvhc makehc " + worldName + " to convert it.");
            return;
        }

        SetupSession session = new SetupSession(player.getUniqueId(), worldName, mode);
        sessions.put(player.getUniqueId(), session);
        sendHeader(player, worldName, mode);
        promptCurrentStep(player, session);
    }

    /**
     * Cancel an active session.
     */
    public void cancelSession(UUID playerId) {
        sessions.remove(playerId);
    }

    /**
     * Handle a response from a clickable button or typed input.
     */
    public void handleResponse(Player player, String value) {
        SetupSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        SetupSession.Step step = session.getCurrentStep();

        switch (step) {
            case INCLUDE_NETHER:
                session.setIncludeNether(value.equalsIgnoreCase("yes"));
                session.setCurrentStep(SetupSession.Step.INCLUDE_END);
                break;
            case INCLUDE_END:
                session.setIncludeEnd(value.equalsIgnoreCase("yes"));
                session.setCurrentStep(SetupSession.Step.SPECTATOR_MODE);
                break;
            case SPECTATOR_MODE:
                session.setSpectatorMode(value.equalsIgnoreCase("yes"));
                if (session.isSpectatorMode()) {
                    // Skip respawn world question — not needed in spectator mode
                    session.setCurrentStep(SetupSession.Step.BAN_FOREVER);
                } else {
                    session.setCurrentStep(SetupSession.Step.RESPAWN_WORLD);
                }
                break;
            case RESPAWN_WORLD:
                session.setRespawnWorld(value);
                session.setCurrentStep(SetupSession.Step.BAN_FOREVER);
                break;
            case BAN_FOREVER:
                session.setBanForever(value.equalsIgnoreCase("yes"));
                if (session.isBanForever()) {
                    // Skip ban length — not needed for permanent bans
                    session.setCurrentStep(SetupSession.Step.CONFIRM);
                } else {
                    session.setCurrentStep(SetupSession.Step.BAN_LENGTH);
                }
                break;
            case BAN_LENGTH:
                try {
                    long length = Long.parseLong(value);
                    if (length <= 0) {
                        MessageSender.sendError(player, "Ban length must be greater than 0. Try again:");
                        return;
                    }
                    session.setBanLength(length);
                    session.setCurrentStep(SetupSession.Step.CONFIRM);
                } catch (NumberFormatException e) {
                    MessageSender.sendError(player, "That's not a valid number. Type the ban length in seconds:");
                    return;
                }
                break;
            case CONFIRM:
                if (value.equalsIgnoreCase("yes")) {
                    executeSetup(player, session);
                } else {
                    cancelSession(player.getUniqueId());
                    MessageSender.sendInfo(player, "Setup cancelled.");
                }
                return;
        }

        promptCurrentStep(player, session);
    }

    // ── UI Rendering ──────────────────────────────────────────

    private void sendHeader(Player player, String worldName, SetupSession.Mode mode) {
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        String action = mode == SetupSession.Mode.CREATE ? "Create" : "Convert";
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + " Hardcore World Setup");
        player.sendMessage(ChatColor.GRAY + " " + action + ": " + ChatColor.WHITE + worldName);
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
    }

    private void promptCurrentStep(Player player, SetupSession session) {
        switch (session.getCurrentStep()) {
            case INCLUDE_NETHER:
                sendYesNoQuestion(player, "Include Nether?",
                        "Death in the nether dimension counts towards this hardcore world.",
                        session.isIncludeNether());
                break;
            case INCLUDE_END:
                sendYesNoQuestion(player, "Include The End?",
                        "Death in the end dimension counts towards this hardcore world.",
                        session.isIncludeEnd());
                break;
            case SPECTATOR_MODE:
                sendYesNoQuestion(player, "Spectator mode on death?",
                        "Players become spectators instead of being teleported away.",
                        session.isSpectatorMode());
                break;
            case RESPAWN_WORLD:
                sendWorldSelector(player, session);
                break;
            case BAN_FOREVER:
                sendYesNoQuestion(player, "Ban permanently on death?",
                        "Players can never rejoin after dying. If no, they get a timed ban.",
                        session.isBanForever());
                break;
            case BAN_LENGTH:
                sendTextPrompt(player, "Ban length (in seconds)?",
                        "How long players are banned after death. e.g. 3600 = 1 hour");
                break;
            case CONFIRM:
                sendConfirmation(player, session);
                break;
        }
    }

    private void sendYesNoQuestion(Player player, String question, String description, boolean defaultYes) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " " + question);
        player.sendMessage(ChatColor.GRAY + " " + description);

        TextComponent line = new TextComponent("  ");

        // [Yes] button
        TextComponent yes = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "[Yes]");
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mvhc setup yes"));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to select Yes")));

        TextComponent spacer = new TextComponent("  ");

        // [No] button
        TextComponent no = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "[No]");
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mvhc setup no"));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to select No")));

        TextComponent defaultLabel = new TextComponent(
                ChatColor.DARK_GRAY + "  (default: " + (defaultYes ? "yes" : "no") + ")");

        line.addExtra(yes);
        line.addExtra(spacer);
        line.addExtra(no);
        line.addExtra(defaultLabel);
        player.spigot().sendMessage(line);
        player.sendMessage("");
    }

    private void sendTextPrompt(Player player, String question, String description) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " " + question);
        player.sendMessage(ChatColor.GRAY + " " + description);
        player.sendMessage(ChatColor.YELLOW + " Type: " + ChatColor.WHITE + "/mvhc setup <value>");

        // Also add a cancel button
        TextComponent cancel = new TextComponent(ChatColor.RED + "  [Cancel Setup]");
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mvhc setup cancel"));
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to cancel setup")));
        player.spigot().sendMessage(cancel);
        player.sendMessage("");
    }

    private void sendWorldSelector(Player player, SetupSession session) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " Select respawn world:");
        player.sendMessage(ChatColor.GRAY + " Where players teleport after dying. Click a world below:");
        player.sendMessage("");

        // Get all loaded Multiverse worlds, excluding the hardcore world being set up
        MultiverseHardcore plugin = MultiverseHardcore.getInstance();
        WorldManager worldManager = plugin.getWorldManager();
        String setupWorld = session.getWorldName();

        List<String> worldNames;
        if (worldManager != null) {
            worldNames = worldManager.getLoadedWorlds().stream()
                    .map(w -> w.getName())
                    .filter(name -> !name.equals(setupWorld)
                            && !name.equals(setupWorld + "_nether")
                            && !name.equals(setupWorld + "_the_end"))
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            // Fallback to Bukkit worlds
            worldNames = plugin.getServer().getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> !name.equals(setupWorld)
                            && !name.equals(setupWorld + "_nether")
                            && !name.equals(setupWorld + "_the_end"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (worldNames.isEmpty()) {
            player.sendMessage(ChatColor.RED + "  No other worlds available!");
            player.sendMessage(ChatColor.YELLOW + "  Type: " + ChatColor.WHITE + "/mvhc setup <world_name>");
        } else {
            for (String name : worldNames) {
                TextComponent worldBtn = new TextComponent("  ");

                TextComponent button = new TextComponent(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + name + "]");
                button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mvhc setup " + name));
                button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text("Click to select " + name + " as respawn world")));

                worldBtn.addExtra(button);
                player.spigot().sendMessage(worldBtn);
            }
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_GRAY + "  Or type: /mvhc setup <world_name>");
        }

        // Cancel button
        TextComponent cancel = new TextComponent(ChatColor.RED + "  [Cancel Setup]");
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mvhc setup cancel"));
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to cancel setup")));
        player.spigot().sendMessage(cancel);
        player.sendMessage("");
    }

    private void sendConfirmation(Player player, SetupSession session) {
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + " Review your settings:");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  World: " + ChatColor.WHITE + session.getWorldName());
        player.sendMessage(ChatColor.GRAY + "  Include Nether: " + formatBool(session.isIncludeNether()));
        player.sendMessage(ChatColor.GRAY + "  Include End: " + formatBool(session.isIncludeEnd()));
        player.sendMessage(ChatColor.GRAY + "  Spectator Mode: " + formatBool(session.isSpectatorMode()));
        if (!session.isSpectatorMode()) {
            player.sendMessage(ChatColor.GRAY + "  Respawn World: " + ChatColor.WHITE + session.getRespawnWorld());
        }
        player.sendMessage(ChatColor.GRAY + "  Ban Forever: " + formatBool(session.isBanForever()));
        if (!session.isBanForever()) {
            player.sendMessage(ChatColor.GRAY + "  Ban Length: " + ChatColor.WHITE + session.getBanLength() + "s");
        }
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        TextComponent line = new TextComponent("  ");

        TextComponent confirm = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "[Confirm]");
        confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mvhc setup yes"));
        confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to create the hardcore world")));

        TextComponent spacer = new TextComponent("    ");

        TextComponent cancel = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "[Cancel]");
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mvhc setup no"));
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to cancel setup")));

        line.addExtra(confirm);
        line.addExtra(spacer);
        line.addExtra(cancel);
        player.spigot().sendMessage(line);
        player.sendMessage("");
    }

    private String formatBool(boolean value) {
        return value ? (ChatColor.GREEN + "Yes") : (ChatColor.RED + "No");
    }

    // ── Execution ─────────────────────────────────────────────

    private void executeSetup(Player player, SetupSession session) {
        sessions.remove(player.getUniqueId());
        MultiverseHardcore plugin = MultiverseHardcore.getInstance();

        if (session.getMode() == SetupSession.Mode.CREATE) {
            executeCreate(player, session, plugin);
        } else {
            executeMakeHC(player, session, plugin);
        }
    }

    private void executeCreate(Player player, SetupSession session, MultiverseHardcore plugin) {
        String worldName = session.getWorldName();
        WorldManager worldManager = plugin.getWorldManager();

        if (WorldUtils.worldExists(worldName)) {
            MessageSender.sendError(player, "World " + worldName + " already exists!");
            return;
        }

        MessageSender.sendInfo(player, "Creating world(s)...");

        // Create overworld
        if (!createWorld(worldManager, worldName, World.Environment.NORMAL)) {
            MessageSender.sendError(player, "Failed to create world " + worldName);
            return;
        }
        setWorldHardcore(worldManager, worldName);

        // Create nether if requested
        if (session.isIncludeNether()) {
            if (!createWorld(worldManager, worldName + "_nether", World.Environment.NETHER)) {
                MessageSender.sendError(player, "Failed to create nether world. Rolling back...");
                deleteWorld(worldManager, worldName);
                return;
            }
            setWorldHardcore(worldManager, worldName + "_nether");
        }

        // Create end if requested
        if (session.isIncludeEnd()) {
            if (!createWorld(worldManager, worldName + "_the_end", World.Environment.THE_END)) {
                MessageSender.sendError(player, "Failed to create end world. Rolling back...");
                deleteWorld(worldManager, worldName);
                if (session.isIncludeNether()) deleteWorld(worldManager, worldName + "_nether");
                return;
            }
            setWorldHardcore(worldManager, worldName + "_the_end");
        }

        // Register as hardcore
        if (!registerHardcoreWorld(player, session, plugin)) {
            // Rollback
            deleteWorld(worldManager, worldName);
            if (session.isIncludeNether()) deleteWorld(worldManager, worldName + "_nether");
            if (session.isIncludeEnd()) deleteWorld(worldManager, worldName + "_the_end");
            return;
        }

        MessageSender.sendSuccess(player, "Hardcore world " + ChatColor.DARK_GREEN + worldName
                + ChatColor.GREEN + " created successfully!");
    }

    private void executeMakeHC(Player player, SetupSession session, MultiverseHardcore plugin) {
        if (!registerHardcoreWorld(player, session, plugin)) return;
        MessageSender.sendSuccess(player, "World " + ChatColor.DARK_GREEN + session.getWorldName()
                + ChatColor.GREEN + " is now Hardcore!");
    }

    private boolean registerHardcoreWorld(Player player, SetupSession session, MultiverseHardcore plugin) {
        World world = plugin.getServer().getWorld(session.getWorldName());
        World spawnWorld = session.getRespawnWorld() != null
                ? plugin.getServer().getWorld(session.getRespawnWorld()) : null;

        HardcoreWorldConfiguration config = new HardcoreWorldConfiguration(
                world, spawnWorld, new Date(),
                session.isBanForever(), session.getBanLength(),
                session.isSpectatorMode(),
                session.isIncludeNether(), session.isIncludeEnd());

        try {
            HardcoreWorld.createHardcoreWorld(config);
            return true;
        } catch (HardcoreWorldCreationException e) {
            MessageSender.sendError(player, e.getMessage());
            return false;
        }
    }

    private boolean createWorld(WorldManager worldManager, String name, World.Environment env) {
        try {
            var result = worldManager.createWorld(
                    CreateWorldOptions.worldName(name).environment(env));
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    private void setWorldHardcore(WorldManager worldManager, String name) {
        worldManager.getLoadedWorld(name).peek(w -> w.setDifficulty(Difficulty.HARD));
    }

    private void deleteWorld(WorldManager worldManager, String name) {
        worldManager.getWorld(name).peek(w ->
                worldManager.deleteWorld(DeleteWorldOptions.world(w)));
    }
}
