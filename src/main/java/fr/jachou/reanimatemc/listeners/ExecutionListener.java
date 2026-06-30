package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExecutionListener implements Listener {
    private final KOManager koManager;
    private final Map<UUID, UUID> pendingExecutions = new HashMap<>(); // victim -> executioner

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

        event.setCancelled(true);

        // If another player is already executing this victim, block the new attempt
        UUID currentExecutioner = pendingExecutions.get(victim.getUniqueId());
        if (currentExecutioner != null && !currentExecutioner.equals(damager.getUniqueId())) {
            damager.sendMessage(ChatColor.RED + ReanimateMC.lang.get("execution_in_progress"));
            return;
        }

        damager.sendMessage(ChatColor.RED + ReanimateMC.lang.get("execution_in_progress"));
        int holdDuration = ReanimateMC.getInstance().getConfig().getInt("execution.hold_duration_ticks", 40);

        pendingExecutions.put(victim.getUniqueId(), damager.getUniqueId());

        ReanimateMC.getInstance().getServer().getScheduler().runTaskLater(ReanimateMC.getInstance(), () -> {
            pendingExecutions.remove(victim.getUniqueId());
            if (koManager.isKO(victim)) {
                koManager.execute(victim);
            }
        }, holdDuration);
    }
}
