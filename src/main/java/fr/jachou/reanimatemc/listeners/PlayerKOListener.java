package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.Bukkit;
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
            var data = koManager.getKOData(e.getPlayer());
            if (e.isSneaking()) {
                if (data.getSuicideTaskId() == -1) {
                    long seconds = ReanimateMC.getInstance().getConfig().getLong("knockout.suicide_hold_seconds", 3);
                    long delay = seconds * 20L;
                    int task = Bukkit.getScheduler().scheduleSyncDelayedTask(ReanimateMC.getInstance(), () -> {
                        if (koManager.isKO(e.getPlayer())) {
                            koManager.suicide(e.getPlayer());
                        }
                    }, delay);
                    data.setSuicideTaskId(task);
                    e.getPlayer().sendMessage(ReanimateMC.lang.get("suicide_start", "time", String.valueOf(seconds)));
                }
            } else {
                if (data.getSuicideTaskId() != -1) {
                    Bukkit.getScheduler().cancelTask(data.getSuicideTaskId());
                    data.setSuicideTaskId(-1);
                    e.getPlayer().sendMessage(ReanimateMC.lang.get("ko_shift_click_cancelled"));
                }
            }
        }
    }

}
