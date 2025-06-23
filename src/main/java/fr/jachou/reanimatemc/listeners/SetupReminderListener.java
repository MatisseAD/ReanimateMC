package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Reminds admins to run /reanimatemc config until the plugin has been configured.
 */
public class SetupReminderListener implements Listener {
    private final ReanimateMC plugin;

    public SetupReminderListener(ReanimateMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("setup_completed", false)) {
            Player p = event.getPlayer();
            if (p.hasPermission("reanimatemc.admin") || p.hasPermission("*")) {
                p.sendMessage(ChatColor.GOLD + ReanimateMC.lang.get("first_run_message"));
            }
        }
    }
}
