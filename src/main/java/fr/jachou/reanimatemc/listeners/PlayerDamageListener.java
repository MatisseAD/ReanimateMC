package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import fr.jachou.reanimatemc.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {
    private final KOManager koManager;

    public PlayerDamageListener(KOManager koManager) {
        this.koManager = koManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        if (!ReanimateMC.getInstance().getConfig().getBoolean("knockout.enabled"))
            return;

        if (Utils.isNPC(player)) return;

        double currentHealth = player.getHealth();
        double finalDamage = event.getFinalDamage();

        if (finalDamage >= currentHealth) {
            // If the player holds a Totem of Undying in either hand, let the
            // vanilla mechanic handle it (no KO state applied)
            if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                    player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                return;
            }

            event.setCancelled(true);
            if (!koManager.isKO(player)) {
                koManager.setKO(player);
                player.setHealth(1.0);
                notifyPlayerIsKO(player);

                // Particules (ex. particules rouges) si activées
                if (ReanimateMC.getInstance().getConfig().getBoolean("knockout.use_particles", true)) {
                    player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 10, 0.5, 0.5, 0.5,
                            new Particle.DustOptions(Color.RED, 1));
                }
                // Son de battement de cœur si activé
                if (ReanimateMC.getInstance().getConfig().getBoolean("knockout.heartbeat_sound", true)) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                }
            }
        }
    }

    private void notifyPlayerIsKO(Player player) {
        var x = player.getLocation().getX();
        var y = player.getLocation().getY();
        var z = player.getLocation().getZ();
        var world = player.getWorld().getName();
        var name = player.getName();
        player.sendMessage(ChatColor.GREEN +
            ReanimateMC.lang.get("player_ko",
                "player", name,
                "world", world,
                "x", String.valueOf((int) x),
                "y", String.valueOf((int) y),
                "z", String.valueOf((int) z)
            )
        );
    }
    
}
