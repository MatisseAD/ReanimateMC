package fr.jachou.reanimatemc.api;

import fr.jachou.reanimatemc.data.ReanimatorNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player successfully summons a reanimator NPC.
 * Cancelling this event prevents the NPC from being registered and spawned.
 */
public class NPCSummonedEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player summoner;
    private final ReanimatorNPC npc;
    private boolean cancelled = false;

    public NPCSummonedEvent(Player summoner, ReanimatorNPC npc) {
        this.summoner = summoner;
        this.npc      = npc;
    }

    public Player getSummoner()  { return summoner; }
    public ReanimatorNPC getNpc() { return npc; }

    @Override public boolean isCancelled()              { return cancelled; }
    @Override public void setCancelled(boolean cancel)  { this.cancelled = cancel; }
    @Override public HandlerList getHandlers()          { return handlers; }
    public static HandlerList getHandlerList()          { return handlers; }
}
