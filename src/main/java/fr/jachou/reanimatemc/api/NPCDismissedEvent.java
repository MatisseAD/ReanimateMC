package fr.jachou.reanimatemc.api;

import fr.jachou.reanimatemc.data.ReanimatorNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a reanimator NPC is removed for any reason:
 * manual dismiss, lifetime expiry, owner offline timeout, or plugin disable.
 */
public class NPCDismissedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public enum Reason { MANUAL, EXPIRED, OFFLINE_TIMEOUT, PLUGIN_DISABLE }

    private final Player owner;
    private final ReanimatorNPC npc;
    private final Reason reason;

    public NPCDismissedEvent(Player owner, ReanimatorNPC npc, Reason reason) {
        this.owner  = owner;
        this.npc    = npc;
        this.reason = reason;
    }

    /** May be null if the owner is offline at the time of removal. */
    public Player getOwner()      { return owner; }
    public ReanimatorNPC getNpc() { return npc; }
    public Reason getReason()     { return reason; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
