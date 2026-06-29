package fr.jachou.reanimatemc.behavior;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;

/**
 * GOLEM — standard reanimator.
 *
 * <p>Capabilities:
 * <ul>
 *   <li>Follows owner.</li>
 *   <li>Revives owner when KO.</li>
 *   <li>Revives explicit targets set via {@code /rmc summon golem <player>}.</li>
 *   <li>Defends owner: attacks any LivingEntity (players, mobs, etc.) that last
 *       damaged the owner within {@code golem.combat_radius}.</li>
 * </ul>
 *
 * <p>Does NOT heal HP passively. Does NOT auto-scan for KO'd allies.
 */
public class GolemBehavior implements NPCBehavior {

    @Override
    public void onIdleTick(ReanimatorNPC npc, Mob mob, Player owner) {
        // Defend owner: target whoever last hurt them, any entity type
        Entity lastDamager = owner.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent edbe
                ? edbe.getDamager() : null;
        double radius = cfg("golem.combat_radius", 12.0);
        if (lastDamager instanceof LivingEntity le
                && !le.getUniqueId().equals(mob.getUniqueId())
                && mob.getLocation().distance(le.getLocation()) <= radius) {
            mob.setTarget(le);
            return;
        }
        // Fallback: nearest hostile mob
        owner.getWorld()
                .getNearbyEntities(mob.getLocation(), radius, radius, radius)
                .stream()
                .filter(e -> e instanceof Monster)
                .filter(e -> e.getLocation().distance(owner.getLocation()) < radius)
                .map(e -> (LivingEntity) e)
                .min(java.util.Comparator.comparingDouble(e -> e.getLocation().distance(mob.getLocation())))
                .ifPresent(mob::setTarget);
    }

    @Override
    public void onSpawn(ReanimatorNPC npc, IronGolem golem) { }

    @Override
    public void onRevive(ReanimatorNPC npc, Player target, Player reviver) {
        // No post-revive bonus for base Golem
    }

    @Override public boolean canRevive()       { return true; }
    @Override public boolean canReviveAllies() { return false; }

    @Override
    public String buildNameplate(ReanimatorNPC npc, double hp, double maxHp, long remSec) {
        return ChatColor.GREEN + "✦ " + npc.getType().getDisplayName()
                + ChatColor.DARK_GRAY + " [" + ChatColor.WHITE + npc.getOwnerName() + ChatColor.DARK_GRAY + "]"
                + ChatColor.GRAY + " | " + ChatColor.RED + "❤ " + (int) hp
                + timeSuffix(remSec);
    }

    @Override public Particle getSpawnParticle() { return Particle.SOUL_FIRE_FLAME; }
    @Override public Sound    getSpawnSound()    { return Sound.BLOCK_BEACON_ACTIVATE; }
    @Override public double   getMaxHp()         {
        return ReanimateMC.getInstance().getConfig().getDouble("npc_summon.golem.max_hp", 100.0);
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
