/* LICENSE BEGIN
    * This file is part of ReanimateMC.
    * ReanimateMC is under the proprietary of Frouzie.
    * You are not allowed to redistribute it and/or modify it.
LICENSE END
 */

package fr.jachou.reanimatemc.data;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a summoned NPC/Golem reanimator
 */
public class ReanimatorNPC {
    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private final Entity entity;
    private final ReanimatorType type;
    private final long summonTime;
    private UUID targetPlayerId;
    
    public enum ReanimatorType {
        GOLEM("Iron Golem Reanimator"),
        HEALER("Healing Golem"),
        PROTECTOR("Protective Golem");
        
        private final String displayName;
        
        ReanimatorType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public ReanimatorNPC(UUID ownerId, String ownerName, Entity entity, ReanimatorType type) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.entity = entity;
        this.type = type;
        this.summonTime = System.currentTimeMillis();
        this.targetPlayerId = null;
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public ReanimatorType getType() {
        return type;
    }
    
    public long getSummonTime() {
        return summonTime;
    }
    
    public UUID getTargetPlayerId() {
        return targetPlayerId;
    }
    
    public void setTargetPlayerId(UUID targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
    
    public boolean isValid() {
        return entity != null && entity.isValid();
    }
    
    public void remove() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }
}
