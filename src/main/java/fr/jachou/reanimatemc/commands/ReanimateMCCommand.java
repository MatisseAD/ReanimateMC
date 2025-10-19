package fr.jachou.reanimatemc.commands;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import fr.jachou.reanimatemc.gui.ConfigGUI;
import fr.jachou.reanimatemc.managers.KOManager;
import fr.jachou.reanimatemc.managers.NPCSummonManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReanimateMCCommand implements CommandExecutor, TabCompleter {
    private final KOManager koManager;
    private final ConfigGUI configGui;
    private final NPCSummonManager npcSummonManager;

    public ReanimateMCCommand(KOManager koManager, ConfigGUI configGui, NPCSummonManager npcSummonManager) {
        this.koManager = koManager;
        this.configGui = configGui;
        this.npcSummonManager = npcSummonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Bukkit.getScheduler().runTaskAsynchronously(ReanimateMC.getInstance(), () -> ReanimateMC.getInstance().getNotifier().notifySenderIfOutdated(sender));
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
            if (!sender.hasPermission("reanimatemc.revive")) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
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
            Player player = (Player) sender;
            koManager.revive(target, player);
            sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("revived_confirmation", "player", target.getName()));
        } else if (subCommand.equalsIgnoreCase("knockout")) {
            if (!sender.hasPermission("reanimatemc.knockout")) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
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
            if (!sender.hasPermission("reanimatemc.status")) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
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
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be executed by a player.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("reanimatemc.crawl")) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
            if (!koManager.isKO(player)) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_in_ko"));
                return true;
            }
            if (!ReanimateMC.getInstance().getConfig().getBoolean("prone.allow_crawl", false)) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("crawl_not_allowed"));
                return true;
            }
            koManager.toggleCrawl(player);
        } else if (subCommand.equalsIgnoreCase("removeGlowingEffect")) {
            if (!sender.hasPermission("reanimatemc.removeglow")) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_remove_glowing_usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found"));
                return true;
            }
            target.setGlowing(false);
            sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("glowing_effect_removed"));

        } else if (subCommand.equalsIgnoreCase("gui") || subCommand.equalsIgnoreCase("config")) {
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
            if (!ReanimateMC.getInstance().getConfig().getBoolean("setup_completed", false)) {
                ReanimateMC.getInstance().getConfig().set("setup_completed", true);
                ReanimateMC.getInstance().saveConfig();
                player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("setup_complete"));
            }
        } else if (subCommand.equalsIgnoreCase("summon")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("reanimate.summon")) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_summon_usage"));
                return true;
            }
            
            // Parse NPC type
            ReanimatorNPC.ReanimatorType type;
            try {
                type = ReanimatorNPC.ReanimatorType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_invalid_type", "type", args[1]));
                return true;
            }
            
            // Optional target player
            Player targetPlayer = null;
            if (args.length >= 3) {
                targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found"));
                    return true;
                }
            }
            
            npcSummonManager.summon(player, type, targetPlayer);
            
        } else if (subCommand.equalsIgnoreCase("dismiss")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("reanimate.summon")) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
            
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_dismiss_usage"));
                return true;
            }
            
            if (args[1].equalsIgnoreCase("all")) {
                npcSummonManager.dismissAll(player);
            } else {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_dismiss_usage"));
            }
            
        } else if (subCommand.equalsIgnoreCase("npcs") || subCommand.equalsIgnoreCase("npcstatus")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("reanimate.summon")) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                return true;
            }
            
            List<ReanimatorNPC> npcs = npcSummonManager.getPlayerSummons(player);
            if (npcs.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("npc_none_active"));
            } else {
                player.sendMessage(ChatColor.GOLD + ReanimateMC.lang.get("npc_status_header"));
                for (ReanimatorNPC npc : npcs) {
                    long age = (System.currentTimeMillis() - npc.getSummonTime()) / 1000;
                    player.sendMessage(ChatColor.AQUA + "- " + npc.getType().getDisplayName() + 
                        ChatColor.GRAY + " (" + age + "s)");
                }
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_unknown"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        // Tout les tab completions
        if (strings.length == 1) {
            return List.of("reload", "revive", "knockout", "status", "crawl", "removeGlowingEffect", "config", "summon", "dismiss", "npcs");
        } else if (strings.length == 2) {
            // Si le premier argument est "revive", "knockout" ou "status", on affiche les joueurs en ligne
            if (strings[0].equalsIgnoreCase("revive") || strings[0].equalsIgnoreCase("knockout") || strings[0].equalsIgnoreCase("status") || strings[0].equalsIgnoreCase("removeGlowingEffect")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            } else if (strings[0].equalsIgnoreCase("summon")) {
                // Show NPC types
                List<String> types = new ArrayList<>();
                for (ReanimatorNPC.ReanimatorType type : ReanimatorNPC.ReanimatorType.values()) {
                    types.add(type.name().toLowerCase());
                }
                return types;
            } else if (strings[0].equalsIgnoreCase("dismiss")) {
                return List.of("all");
            }
        } else if (strings.length == 3) {
            // For summon command, show player names as optional target
            if (strings[0].equalsIgnoreCase("summon")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
        }

        return List.of();
    }
}


