package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerKOListener implements Listener {

    private final KOManager koManager;
    private final long suicideDelayTicks;

    public PlayerKOListener(KOManager koManager) {
        this.koManager = koManager;
        long seconds = ReanimateMC.getInstance().getConfig().getLong("knockout.suicide_hold_seconds", 3);
        this.suicideDelayTicks = seconds * 20L;
    }

    @EventHandler
    public void onPlayerShiftClick(PlayerToggleSneakEvent e) {
        if (koManager.isKO(e.getPlayer())) {
            e.setCancelled(true);
            if (e.isSneaking()) {
                var data = koManager.getKOData(e.getPlayer());
                if (data.getSuicideTaskId() == -1) {
                    int task = Bukkit.getScheduler().scheduleSyncDelayedTask(ReanimateMC.getInstance(), () -> {
                        if (koManager.isKO(e.getPlayer())) {
                            koManager.suicide(e.getPlayer());
                        }
                    }, suicideDelayTicks);
                    data.setSuicideTaskId(task);
                    e.getPlayer().sendMessage(ReanimateMC.lang.get("suicide_start", "time", String.valueOf(suicideDelayTicks / 20)));
                }
            }
        }
    }

}
