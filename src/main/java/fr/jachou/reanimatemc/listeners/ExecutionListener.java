package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ExecutionListener implements Listener {
    private KOManager koManager;

    public ExecutionListener(KOManager koManager) {
        this.koManager = koManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player))
            return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (!koManager.isKO(victim))
            return;
        if (!ReanimateMC.getInstance().getConfig().getBoolean("execution.enabled"))
            return;

        // If this hit just caused the KO, don't instantly execute
        if (victim.getHealth() - event.getFinalDamage() <= 0)
            return;

        event.setCancelled(true);

        damager.sendMessage(ChatColor.RED + ReanimateMC.lang.get("execution_in_progress"));
        int holdDuration = ReanimateMC.getInstance().getConfig().getInt("execution.hold_duration_ticks");

        // Après le délai, si le joueur est toujours K.O., l'exécution s’effectue
        ReanimateMC.getInstance().getServer().getScheduler().runTaskLater(ReanimateMC.getInstance(), () -> {
            if (koManager.isKO(victim)) {
                koManager.execute(victim);
            }
        }, holdDuration);
    }
}
