package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import fr.jachou.reanimatemc.managers.NPCSummonManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles damage and death events for system-owned Iron Golems.
 *
 * <p>Self-damage prevention: Iron Golems can damage themselves when trapped
 * against a wall (stuck pathfinding). Any damage event where the damager is
 * the golem itself, or another registered allied golem, is cancelled.
 *
 * <p>Invulnerability mode ({@code npc_summon.invulnerable}): cancels all
 * incoming damage to registered golems.
 *
 * <p>Low-HP warning: when HP drops below {@code npc_summon.low_hp_threshold}
 * (default 30%), an action-bar alert is sent to the owner.
 *
 * <p>Death cleanup: drops and XP cleared; owner notified; NPC record removed.
 */
public class NPCDamageListener implements Listener {

    private final NPCSummonManager npcSummonManager;

    public NPCDamageListener(NPCSummonManager npcSummonManager) {
        this.npcSummonManager = npcSummonManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;
        IronGolem golem = (IronGolem) event.getEntity();
        ReanimatorNPC npc = npcSummonManager.getNPCByEntity(golem);
        if (npc == null) return;

        // Self-damage and allied-golem friendly-fire prevention
        if (event instanceof EntityDamageByEntityEvent edbe) {
            Entity damager = edbe.getDamager();
            if (damager.getUniqueId().equals(golem.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            if (damager instanceof IronGolem
                    && npcSummonManager.getNPCByEntity(damager) != null) {
                event.setCancelled(true);
                return;
            }
        }

        // Invulnerability mode
        if (ReanimateMC.getInstance().getConfig().getBoolean("npc_summon.invulnerable", false)) {
            event.setCancelled(true);
            return;
        }

        // Low-HP warning
        double damageAfter = golem.getHealth() - event.getFinalDamage();
        double maxHp = golem.getAttribute(Attribute.MAX_HEALTH).getValue();
        double threshold = ReanimateMC.getInstance().getConfig()
                .getDouble("npc_summon.low_hp_threshold", 0.30);
        if (damageAfter > 0 && (damageAfter / maxHp) <= threshold) {
            Player owner = Bukkit.getPlayer(npc.getOwnerId());
            if (owner != null && owner.isOnline()) {
                String icon = switch (npc.getType()) {
                    case HEALER    -> "✚";
                    case PROTECTOR -> "⚔";
                    default        -> "✦";
                };
                owner.sendActionBar(ChatColor.RED + icon + " "
                        + ReanimateMC.lang.get("npc_low_hp",
                                "type", npc.getType().getDisplayName(),
                                "hp", String.valueOf((int) damageAfter)));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;
        IronGolem golem = (IronGolem) event.getEntity();
        ReanimatorNPC npc = npcSummonManager.getNPCByEntity(golem);
        if (npc == null) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        Player owner = Bukkit.getPlayer(npc.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_died",
                    "type", npc.getType().getDisplayName()));
        }

        Bukkit.getScheduler().runTask(ReanimateMC.getInstance(),
                () -> npcSummonManager.onNPCDied(npc));
    }
}
