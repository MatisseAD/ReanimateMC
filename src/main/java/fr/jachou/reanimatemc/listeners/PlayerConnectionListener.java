package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class PlayerConnectionListener implements Listener {
    private final KOManager koManager;

    public PlayerConnectionListener(KOManager manager) {
        this.koManager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        koManager.handleLogout(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long remaining = koManager.pullOfflineKO(player.getUniqueId());
        if (remaining > 0) {
            Bukkit.getScheduler().runTaskLater(ReanimateMC.getInstance(), () -> koManager.setKO(player, (int) remaining), 1L);
        } else if (remaining == 0) {
            player.setHealth(0);
        }
    }
}
