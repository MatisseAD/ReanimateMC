package fr.jachou.reanimatemc.commands;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.gui.ConfigGUI;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ReanimateMCCommand implements CommandExecutor, TabCompleter {
    private final KOManager koManager;
    private final ConfigGUI configGui;

    public ReanimateMCCommand(KOManager koManager, ConfigGUI configGui) {
        this.koManager = koManager;
        this.configGui = configGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_usage"));
            return true;
        }

        String subCommand = args[0];
        if (subCommand.equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("reanimatemc.admin")) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
            ReanimateMC.getInstance().reloadConfig();
            ReanimateMC.lang.loadLanguage();
            sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("config_reloaded"));
        } else if (subCommand.equalsIgnoreCase("revive")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_revive_usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found"));
                return true;
            }
            if (!koManager.isKO(target)) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_ko"));
                return true;
            }
            koManager.revive(target);
            sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("revived_confirmation", "player", target.getName()));
        } else if (subCommand.equalsIgnoreCase("knockout")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_knockout_usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found"));
                return true;
            }
            if (koManager.isKO(target)) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_already_ko"));
                return true;
            }
            koManager.setKO(target);
            sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("knockout_set", "player", target.getName()));
        } else if (subCommand.equalsIgnoreCase("status")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_status_usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found"));
                return true;
            }
            String status = koManager.isKO(target) ? ReanimateMC.lang.get("status_ko") : ReanimateMC.lang.get("status_normal");
            sender.sendMessage(ChatColor.AQUA + target.getName() + " : " + status);
        } else if (subCommand.equalsIgnoreCase("crawl")) {
            // Permet au joueur KO de basculer entre l'immobilisation totale et le mode crawl
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be executed by a player.");
                return true;
            }
            Player player = (Player) sender;
            if (!koManager.isKO(player)) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_in_ko"));
                return true;
            }
            // Vérifier que l'option "prone.allow_crawl" est activée
            if (!ReanimateMC.getInstance().getConfig().getBoolean("prone.allow_crawl", false)) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("crawl_not_allowed"));
                return true;
            }
            koManager.toggleCrawl(player);
        } else if (subCommand.equalsIgnoreCase("removeGlowingEffect")) {
            // Permet de retirer l'effet de glow d'un joueur KO
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_remove_glowing_usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found"));
                return true;
            }

            /*if (target.isGlowing()) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_glowing"));
                return true;
            }*/

            target.setGlowing(false);
            sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("glowing_effect_removed"));


        } else if (subCommand.equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_gui_player_only"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("reanimatemc.admin")) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
            configGui.openGUI(player);
        } else {
            sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_unknown"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        // Tout les tab completions
        if (strings.length == 1) {
            return List.of("reload", "revive", "knockout", "status", "crawl", "removeGlowingEffect", "gui");
        } else if (strings.length == 2) {
            // Si le premier argument est "revive", "knockout" ou "status", on affiche les joueurs en ligne
            if (strings[0].equalsIgnoreCase("revive") || strings[0].equalsIgnoreCase("knockout") || strings[0].equalsIgnoreCase("status") || strings[0].equalsIgnoreCase("removeGlowingEffect")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
        }

        return List.of();
    }
}


