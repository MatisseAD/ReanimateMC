package fr.jachou.reanimatemc.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerKOEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final int duration;
    private boolean cancelled = false;

    public PlayerKOEvent(Player player, int duration) {
        this.player = player;
        this.duration = duration;
    }

    public Player getPlayer() {
        return player;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}