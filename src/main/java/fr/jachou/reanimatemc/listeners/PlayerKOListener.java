package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.List;

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

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (koManager.isKO(event.getPlayer())) {
            event.setCancelled(true);
            koManager.sendDistress(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        boolean commandRestrictionsEnabled = ReanimateMC.getInstance().getConfig().getBoolean("knockout.enable_commands_allowed");
        if (!koManager.isKO(player) || player.isOp() || !commandRestrictionsEnabled) return;

        List<String> allowed = ReanimateMC.getInstance().getConfig().getStringList("knockout.allowed_commands");

        String msg = event.getMessage();
        String cmd = msg.startsWith("/") ? msg.substring(1) : msg;
        String label = cmd.split(" ")[0].toLowerCase();

        String base = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;

        boolean isAllowed = allowed.stream().map(String::toLowerCase).anyMatch(a ->
                a.equals(label) || a.equals(base)
        );

        if (!isAllowed) {
            event.setCancelled(true);
            player.sendMessage(ReanimateMC.lang.get("no_permission_ko"));
        }
    }

}
