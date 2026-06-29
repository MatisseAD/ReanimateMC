package fr.jachou.reanimatemc.behavior;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

/**
 * PROTECTOR — tank reanimator.
 *
 * <p>Idle-tick extras:
 * <ul>
 *   <li>Targets the nearest hostile mob within combat_radius that is also within
 *       15 blocks of the owner.</li>
 * </ul>
 * Post-revive (owner only): knockback burst to nearby entities.
 * Damage intercept and transfer are handled by NPCSummonManager + PlayerDamageListener.
 */
public class ProtectorBehavior implements NPCBehavior {

    @Override
    public void onIdleTick(ReanimatorNPC npc, Mob mob, Player owner) {
        double radius = cfg("protector.combat_radius", 12.0);

        // Priority: whoever last damaged the owner, any entity type
        Entity lastDamager = owner.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent edbe
                ? edbe.getDamager() : null;
        if (lastDamager instanceof LivingEntity le
                && !le.getUniqueId().equals(mob.getUniqueId())
                && mob.getLocation().distance(le.getLocation()) <= radius) {
            mob.setTarget(le);
            return;
        }

        // Fallback: nearest LivingEntity threatening the owner (any type — players, mobs, etc.)
        owner.getWorld()
                .getNearbyEntities(mob.getLocation(), radius, radius, radius)
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> !(e instanceof IronGolem))   // exclude allied golems
                .filter(e -> !e.getUniqueId().equals(owner.getUniqueId()))
                .filter(e -> !e.getUniqueId().equals(mob.getUniqueId()))
                .filter(e -> e.getLocation().distance(owner.getLocation()) < radius)
                .map(e -> (LivingEntity) e)
                .min(java.util.Comparator.comparingDouble(e -> e.getLocation().distance(mob.getLocation())))
                .ifPresent(mob::setTarget);
    }

    @Override
    public void onSpawn(ReanimatorNPC npc, IronGolem golem) {
        double maxHp = cfg("protector.max_hp", 200.0);
        AttributeInstance attr = golem.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(maxHp);
            golem.setHealth(maxHp);
        }
    }

    @Override
    public void onRevive(ReanimatorNPC npc, Player target, Player reviver) {
        // Knockback burst only when reviving its own owner
        if (!target.getUniqueId().equals(npc.getOwnerId())) return;

        Location center = npc.getEntity().getLocation();
        center.getWorld().getNearbyEntities(center, 5, 5, 5).forEach(e -> {
            if (e instanceof Player && e.getUniqueId().equals(target.getUniqueId())) return;
            if (!(e instanceof LivingEntity)) return;
            Vector dir = e.getLocation().toVector().subtract(center.toVector());
            // Avoid NaN: skip entities at the exact same XZ position as the golem
            if (dir.getX() == 0 && dir.getZ() == 0) return;
            dir.setY(0).normalize().multiply(1.5).setY(0.4);
            e.setVelocity(dir);
        });
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 0.5, 0.2, 0.5);
        center.getWorld().playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.7f);
    }

    /**
     * Returns true only when:
     * 1. {@code npc_summon.protector.revive_if_no_healer} is true (default true), AND
     * 2. The owner has no active HEALER NPC.
     *
     * This is evaluated lazily in {@link fr.jachou.reanimatemc.managers.NPCSummonManager}
     * via the {@code canReviveOwner()} check which is called once per behavior tick.
     * The actual healer check is handled in NPCSummonManager.updateBehavior() before
     * calling this method — see overrideCanReviveOwner(npc, owner).
     */
    @Override
    public boolean canRevive() {
        return false;
    }

    @Override
    public boolean canReviveOwner() {
        // Base value from config — actual healer check done in NPCSummonManager
        return ReanimateMC.getInstance().getConfig()
                .getBoolean("npc_summon.protector.revive_if_no_healer", true);
    }

    @Override
    public String buildNameplate(ReanimatorNPC npc, double hp, double maxHp, long remSec) {
        ChatColor hpColor = (hp / maxHp) > 0.5 ? ChatColor.GREEN
                : (hp / maxHp) > 0.25 ? ChatColor.YELLOW : ChatColor.RED;
        return ChatColor.RED + "⚔ " + npc.getType().getDisplayName()
                + ChatColor.DARK_GRAY + " [" + ChatColor.WHITE + npc.getOwnerName() + ChatColor.DARK_GRAY + "]"
                + ChatColor.GRAY + " | " + hpColor + "❤ " + (int) hp + "/" + (int) maxHp
                + timeSuffix(remSec);
    }

    @Override public Particle getSpawnParticle() { return Particle.FLAME; }
    @Override public Sound    getSpawnSound()    { return Sound.ENTITY_IRON_GOLEM_REPAIR; }
    @Override public double   getMaxHp()         {
        return ReanimateMC.getInstance().getConfig().getDouble("npc_summon.protector.max_hp", 200.0);
    }

    private String timeSuffix(long remSec) {
        if (remSec == Long.MAX_VALUE || remSec < 0) return "";
        return ChatColor.GRAY + " | " + ChatColor.YELLOW + "⏱ " + formatTime(remSec);
    }

    private String formatTime(long s) {
        return s >= 60 ? (s / 60) + "m" + (s % 60) + "s" : s + "s";
    }

    private double cfg(String key, double def) {
        return ReanimateMC.getInstance().getConfig().getDouble("npc_summon." + key, def);
    }
}
