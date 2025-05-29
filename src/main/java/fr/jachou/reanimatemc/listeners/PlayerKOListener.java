package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerKOListener implements Listener {

    private final KOManager koManager;

    public PlayerKOListener(KOManager koManager) {
        this.koManager = koManager;
    }

    @EventHandler
    public void onPlayerShiftClick(PlayerToggleSneakEvent e) {
        if (koManager.isKO(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ReanimateMC.lang.get("ko_shift_click_cancelled"));
        }
    }

}
