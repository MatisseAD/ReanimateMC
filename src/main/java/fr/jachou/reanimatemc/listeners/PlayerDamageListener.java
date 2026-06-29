package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import fr.jachou.reanimatemc.managers.NPCSummonManager;
import fr.jachou.reanimatemc.utils.Utils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {

    private final KOManager koManager;
    private final NPCSummonManager npcSummonManager;

    public PlayerDamageListener(KOManager koManager, NPCSummonManager npcSummonManager) {
        this.koManager        = koManager;
        this.npcSummonManager = npcSummonManager;
    }

    /**
     * PROTECTOR intercept — runs at HIGHEST so damage is reduced before MONITOR reads it.
     * Only fires on EntityDamageByEntity so we have access to the attacker.
     * Skips if the attacker is the owner's own Protector golem (friendly fire).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (Utils.isNPC(victim)) return;
        if (koManager.isKO(victim)) return; // already KO, let KO logic handle it

        Entity attacker = event.getDamager();

        // Don't intercept hits coming from the player's own Protector
        if (attacker instanceof IronGolem) return;

        double ratio = npcSummonManager.getInterceptRatio(victim, attacker);
        if (ratio <= 0.0) return;

        double originalDamage = event.getDamage();
        double absorbed = originalDamage * ratio;
        event.setDamage(originalDamage - absorbed);

        // Transfer absorbed portion as real HP loss to the Protector entity
        npcSummonManager.applyTransferredDamage(victim, absorbed);
    }

    /**
     * KO trigger — runs at MONITOR to see the final resolved damage value after
     * all other plugins (and the intercept handler above) have processed it.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!ReanimateMC.getInstance().getConfig().getBoolean("knockout.enabled")) return;
        if (Utils.isNPC(player)) return;

        // Track last-damage time for self-revive combat check and cancel-on-damage
        koManager.trackLastDamage(player);

        // Cancel self-revive channel if the player takes damage while channeling
        if (koManager.isChannelingSelfRevive(player)
                && ReanimateMC.getInstance().getConfig()
                        .getBoolean("self_revive.cancel_on_damage", true)) {
            koManager.cancelSelfRevive(player, true);
        }

        double currentHealth = player.getHealth();
        double finalDamage   = event.getFinalDamage();

        if (finalDamage >= currentHealth) {
            if (player.getInventory().getItemInMainHand().getType()  == Material.TOTEM_OF_UNDYING ||
                player.getInventory().getItemInOffHand().getType()   == Material.TOTEM_OF_UNDYING) {
                return;
            }

            event.setCancelled(true);
            if (!koManager.isKO(player)) {
                koManager.setKO(player);
                player.setHealth(1.0);

                if (ReanimateMC.getInstance().getConfig().getBoolean("knockout.use_particles", true)) {
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 10, 0.5, 0.5, 0.5,
                            new Particle.DustOptions(Color.RED, 1));
                }
                if (ReanimateMC.getInstance().getConfig().getBoolean("knockout.heartbeat_sound", true)) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                }
            }
        }
    }
}
