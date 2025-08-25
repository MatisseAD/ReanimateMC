package fr.jachou.reanimatemc.utils.updater;

import fr.jachou.reanimatemc.ReanimateMC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class UpdateNotifier {
    private final UpdateChecker checker;

    public UpdateNotifier(UpdateChecker checker) {
        this.checker = checker;
    }

    public void notifyIfOutdated() {
        String game = Bukkit.getBukkitVersion(); // ex: 1.21.1-R0.1-SNAPSHOT
        Optional<UpdateInfo> info = checker.check(game);
        info.ifPresent(this::broadcast);
    }

    private void broadcast(UpdateInfo info) {
        String msg = ChatColor.GOLD + "[ReanimateMC] " + ChatColor.YELLOW
                + ReanimateMC.lang.get("update_available") + " " + ChatColor.AQUA + info.latestVersion + ChatColor.YELLOW
                + " — " + ChatColor.UNDERLINE + info.versionPageUrl;

        String logMsg = ReanimateMC.lang.get("update_log",
                "version", info.latestVersion,
                "url", info.versionPageUrl);
        Bukkit.getLogger().info("[ReanimateMC] " + logMsg);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("reanimatemc.admin")) {
                p.sendMessage(msg);
            }
        }
    }

    public void notifySenderIfOutdated(CommandSender sender) {
        String game = Bukkit.getBukkitVersion();
        checker.check(game).ifPresentOrElse(
                info -> {
                    String updateMsg = ReanimateMC.lang.get("update_command_available",
                            "version", info.latestVersion,
                            "url", info.versionPageUrl);
                    sender.sendMessage(ChatColor.GREEN + updateMsg);
                },
                () -> {
                    String upToDateMsg = ReanimateMC.lang.get("update_up_to_date");
                    sender.sendMessage(ChatColor.GRAY + upToDateMsg);
                }
        );
    }
}