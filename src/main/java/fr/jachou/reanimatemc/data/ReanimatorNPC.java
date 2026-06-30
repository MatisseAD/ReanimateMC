/* LICENSE BEGIN
    * This file is part of ReanimateMC.
    * ReanimateMC is under the proprietary of Frouzie.
    * You are not allowed to redistribute it and/or modify it.
LICENSE END
 */

package fr.jachou.reanimatemc.data;

import org.bukkit.entity.Entity;

import fr.jachou.reanimatemc.ReanimateMC;
import java.util.UUID;

/**
 * Represents a summoned NPC/Golem reanimator.
 *
 * <p>Each type has distinct behavior driven by NPCSummonManager:
 * <ul>
 *   <li>GOLEM      – standard: follows owner, revives KO'd players.</li>
 *   <li>HEALER     – support: auto-scans for KO'd allies, regeneration aura when
 *                    owner is low HP, periodic passive heal to owner, bonus HP on revive.</li>
 *   <li>PROTECTOR  – tank: 200 HP, attacks hostile mobs, transfers a portion of
 *                    incoming owner damage to itself, knockback burst on owner revive.</li>
 * </ul>
 *
 * <p>All summons have a configurable lifetime. Use {@link #getRemainingSeconds()} to
 * query remaining time and {@link #extendTime(long)} to add more seconds.
 */
public class ReanimatorNPC {

    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private final Entity entity;
    private final ReanimatorType type;
    private final long summonTime;

    /** Absolute epoch-ms when this NPC expires. -1 = no expiry. */
    private long expiresAt;

    private UUID targetPlayerId;

    /** Tick counter used to throttle per-type periodic actions. */
    private int behaviorTick = 0;

    /** Counter incremented once per onIdleTick call; used by HealerBehavior for heal throttle. */
    private int idleTick = 0;

    /** Whether the HEALER regeneration aura is currently active. */
    private boolean healerAuraActive  = false;
    /** Whether the PROTECTOR already sent the "can't revive" warning this KO session. */
    private boolean protectorWarned   = false;

    // ── Stuck detection ───────────────────────────────────────────────────────
    /** Last recorded location, sampled every behavior tick to detect stuck state. */
    private org.bukkit.Location lastPosition = null;

    /** Consecutive ticks the NPC hasn't moved while it should be following. */
    private int stuckTicks = 0;

    public enum ReanimatorType {
        GOLEM("npc_name_golem", "Iron Golem Reanimator"),
        HEALER("npc_name_healer", "Healing Golem"),
        PROTECTOR("npc_name_protector", "Protective Golem");

        private final String langKey;
        private final String fallback;

        ReanimatorType(String langKey, String fallback) {
            this.langKey  = langKey;
            this.fallback = fallback;
        }

        /** Returns the localized display name, falling back to the English default. */
        public String getDisplayName() {
            try {
                String v = ReanimateMC.lang.get(langKey);
                return (v == null || v.isEmpty() || v.equals(langKey)) ? fallback : v;
            } catch (Exception e) {
                return fallback;
            }
        }

        public String getLangKey() { return langKey; }
    }

    public ReanimatorNPC(UUID ownerId, String ownerName, Entity entity, ReanimatorType type, long lifetimeSeconds) {
        this.id          = UUID.randomUUID();
        this.ownerId     = ownerId;
        this.ownerName   = ownerName;
        this.entity      = entity;
        this.type        = type;
        this.summonTime  = System.currentTimeMillis();
        this.expiresAt   = lifetimeSeconds > 0
                ? this.summonTime + lifetimeSeconds * 1000L
                : -1L;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId()           { return id; }
    public UUID getOwnerId()      { return ownerId; }
    public String getOwnerName()  { return ownerName; }
    public Entity getEntity()     { return entity; }
    public ReanimatorType getType() { return type; }
    public long getSummonTime()   { return summonTime; }
    public long getExpiresAt()    { return expiresAt; }

    public UUID getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(UUID id) { this.targetPlayerId = id; }

    public int getBehaviorTick() { return behaviorTick; }
    public void incrementBehaviorTick() { this.behaviorTick++; }

    public int getIdleTick() { return idleTick; }
    public void incrementIdleTick() { this.idleTick++; }

    public boolean isHealerAuraActive()              { return healerAuraActive; }
    public void setHealerAuraActive(boolean active)  { this.healerAuraActive = active; }
    public boolean isProtectorWarned()               { return protectorWarned; }
    public void setProtectorWarned(boolean warned)   { this.protectorWarned = warned; }

    public org.bukkit.Location getLastPosition()                        { return lastPosition; }
    public void setLastPosition(org.bukkit.Location loc)                { this.lastPosition = loc; }
    public int getStuckTicks()                                          { return stuckTicks; }
    public void incrementStuckTicks()                                   { this.stuckTicks++; }
    public void resetStuckTicks()                                       { this.stuckTicks = 0; }

    // ── Lifetime helpers ──────────────────────────────────────────────────────

    /** Returns true if this NPC has a finite lifetime and it has expired. */
    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
    }

    /** Remaining seconds until expiry, or {@link Long#MAX_VALUE} if no expiry set. */
    public long getRemainingSeconds() {
        if (expiresAt <= 0) return Long.MAX_VALUE;
        long rem = (expiresAt - System.currentTimeMillis()) / 1000L;
        return Math.max(0, rem);
    }

    /** Adds {@code seconds} to the expiry time. Creates an expiry if none was set. */
    public void extendTime(long seconds) {
        if (expiresAt <= 0) {
            expiresAt = System.currentTimeMillis() + seconds * 1000L;
        } else {
            expiresAt += seconds * 1000L;
        }
    }

    // ── Entity validity ───────────────────────────────────────────────────────

    public boolean isValid() {
        return entity != null && entity.isValid();
    }

    public void remove() {
        if (entity != null && entity.isValid()) entity.remove();
    }
}
