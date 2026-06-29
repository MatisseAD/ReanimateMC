package fr.jachou.reanimatemc.behavior;

import fr.jachou.reanimatemc.data.ReanimatorNPC;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/**
 * Strategy interface defining all type-specific behavior for a reanimator NPC.
 *
 * <p>One implementation exists per {@link ReanimatorNPC.ReanimatorType}.
 * {@code NPCSummonManager} calls these methods at the appropriate points in the
 * lifecycle; implementations must not manage scheduling themselves.
 */
public interface NPCBehavior {

    /**
     * Called every 3 seconds (3 behavior ticks) when the NPC is idle — i.e. no
     * KO target is being chased. Handles type-specific scanning, auras, combat, etc.
     *
     * @param npc   the NPC data record
     * @param mob   the underlying Bukkit Mob entity
     * @param owner the online owner of this NPC
     */
    void onIdleTick(ReanimatorNPC npc, Mob mob, Player owner);

    /**
     * Called once immediately after the golem entity is spawned and configured.
     * Use for any one-time setup beyond what {@code spawnEntity} already does.
     */
    void onSpawn(ReanimatorNPC npc, IronGolem golem);

    /**
     * Called after a successful revive of {@code target} by this NPC.
     * Apply type-specific post-revive effects here (bonus HP, knockback, etc.).
     *
     * @param npc     the NPC that performed the revive
     * @param target  the player who was revived
     * @param reviver the owner of the NPC (credited for the revive)
     */
    void onRevive(ReanimatorNPC npc, Player target, Player reviver);

    /**
     * Returns true if this type is capable of reviving KO'd players.
     * GOLEM and HEALER return true; PROTECTOR returns false for non-owner targets.
     */
    boolean canRevive();

    /**
     * Returns true if this type revives its own owner when the owner falls KO.
     * GOLEM and HEALER return true; PROTECTOR returns false.
     */
    default boolean canReviveOwner() { return true; }

    /**
     * Returns true if this type actively scans for and revives KO'd allies nearby
     * without needing an explicit target set via command.
     * Only HEALER returns true.
     */
    default boolean canReviveAllies() { return false; }

    /**
     * Builds the nameplate string shown above the golem.
     *
     * @param npc    the NPC record (for type and lifetime)
     * @param hp     current HP
     * @param maxHp  maximum HP
     * @param remSec remaining lifetime seconds; {@link Long#MAX_VALUE} = unlimited
     */
    String buildNameplate(ReanimatorNPC npc, double hp, double maxHp, long remSec);

    /** Particle used in the spiral spawn animation. */
    Particle getSpawnParticle();

    /** Sound played when the NPC is summoned. */
    Sound getSpawnSound();

    /** Maximum HP for this type's entity. */
    double getMaxHp();
}
