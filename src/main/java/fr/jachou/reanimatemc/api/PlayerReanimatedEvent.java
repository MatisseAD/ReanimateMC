package fr.jachou.reanimatemc.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerReanimatedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Player reanimator;
    private final long timestamp;
    private final boolean successful;

    public PlayerReanimatedEvent(Player player, Player reanimator, boolean successful) {
        this.player = player;
        this.reanimator = reanimator;
        this.timestamp = System.currentTimeMillis();
        this.successful = successful;
    }

    public Player getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return player.getName();
    }

    public Player getReanimator() {
        return reanimator;
    }

    public String getReanimatorName() {
        return reanimator != null ? reanimator.getName() : "Unknown";
    }


    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}