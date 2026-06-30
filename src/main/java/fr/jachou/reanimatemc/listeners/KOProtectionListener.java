package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.util.Vector;

public class KOProtectionListener implements Listener {

    private final KOManager koManager;

    public KOProtectionListener(KOManager koManager) {
        this.koManager = koManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityKnockback(EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!koManager.isKO(player)) return;
        if (!ReanimateMC.getInstance().getConfig().getBoolean("knockout.knockback_disabled", true)) return;
        event.setKnockback(new Vector(0, 0, 0));
    }

    /** Prevents new mobs from choosing a KO'd player as a target. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (!koManager.isKO(player)) return;
        if (ReanimateMC.getInstance().getConfig().getBoolean("knockout.mobs_attack_ko", false)) return;
        if (!(event.getEntity() instanceof Mob)) return;
        event.setCancelled(true);
    }

    /**
     * Cancels damage to KO'd players from mobs.
     * Covers both direct melee attacks and projectiles (arrows, fireballs, etc.)
     * shot by a mob. The mob's target is also cleared so it stops pursuing.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDamageKOPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!koManager.isKO(player)) return;
        if (ReanimateMC.getInstance().getConfig().getBoolean("knockout.mobs_attack_ko", false)) return;

        org.bukkit.entity.Entity damager = event.getDamager();

        // Direct melee attack from a mob
        if (damager instanceof Mob mob) {
            event.setCancelled(true);
            mob.setTarget(null);
            return;
        }

        // Projectile shot by a mob (arrow, fireball, snowball, etc.)
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Mob mob) {
            event.setCancelled(true);
            mob.setTarget(null);
            projectile.remove();
        }
    }
}
