package dev.hybes.multiversehardcore.setup;

import java.util.UUID;

/**
 * Holds the in-progress setup state for a player configuring a new hardcore world.
 */
public class SetupSession {

    public enum Mode {
        CREATE,  // creating a brand-new world via Multiverse
        MAKEHC   // converting an existing world to hardcore
    }

    public enum Step {
        INCLUDE_NETHER,
        INCLUDE_END,
        SPECTATOR_MODE,
        BAN_FOREVER,
        BAN_LENGTH,
        RESPAWN_WORLD,
        CONFIRM
    }

    private final UUID playerId;
    private final String worldName;
    private final Mode mode;

    // Defaults — nether & end included by default
    private boolean includeNether = true;
    private boolean includeEnd = true;
    private boolean spectatorMode = true;
    private boolean banForever = true;
    private long banLength = 30; // seconds
    private String respawnWorld = null;

    private Step currentStep;

    public SetupSession(UUID playerId, String worldName, Mode mode) {
        this.playerId = playerId;
        this.worldName = worldName;
        this.mode = mode;
        this.currentStep = Step.INCLUDE_NETHER;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getWorldName() {
        return worldName;
    }

    public Mode getMode() {
        return mode;
    }

    public Step getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Step step) {
        this.currentStep = step;
    }

    public boolean isIncludeNether() {
        return includeNether;
    }

    public void setIncludeNether(boolean includeNether) {
        this.includeNether = includeNether;
    }

    public boolean isIncludeEnd() {
        return includeEnd;
    }

    public void setIncludeEnd(boolean includeEnd) {
        this.includeEnd = includeEnd;
    }

    public boolean isSpectatorMode() {
        return spectatorMode;
    }

    public void setSpectatorMode(boolean spectatorMode) {
        this.spectatorMode = spectatorMode;
    }

    public boolean isBanForever() {
        return banForever;
    }

    public void setBanForever(boolean banForever) {
        this.banForever = banForever;
    }

    public long getBanLength() {
        return banLength;
    }

    public void setBanLength(long banLength) {
        this.banLength = banLength;
    }

    public String getRespawnWorld() {
        return respawnWorld;
    }

    public void setRespawnWorld(String respawnWorld) {
        this.respawnWorld = respawnWorld;
    }
}
