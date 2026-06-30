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

    private final KOManager       koManager;
    private final ConfigGUI       configGui;
    private final NPCSummonManager npcSummonManager;

    public ReanimateMCCommand(KOManager koManager, ConfigGUI configGui, NPCSummonManager npcSummonManager) {
        this.koManager        = koManager;
        this.configGui        = configGui;
        this.npcSummonManager = npcSummonManager;
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    /** Op-level: always true for OPs even without a permission plugin. */
    private boolean hasAdminPerm(CommandSender sender, String node) {
        if (sender.isOp()) return true;
        return sender.hasPermission(node);
    }

    /** Player-level: opLevel=true → isOp() fallback. */
    private boolean hasPerm(CommandSender sender, String node, boolean opLevel) {
        if (opLevel && sender.isOp()) return true;
        return sender.hasPermission(node);
    }

    // ── Main dispatcher ───────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Standalone commands /selfrevive, /sr, /cancelselfrevive, /cancelsr
        String cmdName = command.getName().toLowerCase();
        if (cmdName.equals("selfrevive") || cmdName.equals("sr")) {
            if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
            if (!hasPerm(player, "reanimatemc.selfrevive", false)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
            if (!ReanimateMC.getInstance().getConfig().getBoolean("self_revive.enabled", true)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_disabled")); return true; }
            if (!koManager.isKO(player)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_in_ko")); return true; }
            koManager.startSelfRevive(player);
            return true;
        }
        if (cmdName.equals("cancelselfrevive") || cmdName.equals("cancelsr")) {
            if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
            if (!koManager.isChannelingSelfRevive(player)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_not_channeling")); return true; }
            koManager.cancelSelfRevive(player, true);
            return true;
        }

        if (args.length < 1) {
            sendPluginInfo(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── Admin ─────────────────────────────────────────────────────────

            case "reload" -> {
                if (!hasAdminPerm(sender, "reanimatemc.admin")) {
                    sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                    return true;
                }
                ReanimateMC.getInstance().reloadConfig();
                ReanimateMC.getInstance().getConfig().options().copyDefaults(true);
                ReanimateMC.getInstance().saveConfig();
                ReanimateMC.lang.loadLanguage();
                sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("config_reloaded"));
            }

            case "knockout" -> {
                if (!hasAdminPerm(sender, "reanimatemc.knockout")) {
                    sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_knockout_usage"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found")); return true; }
                if (koManager.isKO(target)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_already_ko")); return true; }
                koManager.setKO(target);
                sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("knockout_set", "player", target.getName()));
            }

            case "purge" -> {
                if (!hasAdminPerm(sender, "reanimatemc.admin")) {
                    sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                    return true;
                }
                int removed = koManager.purgeOrphanedHolograms();
                sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("purge_complete", "count", String.valueOf(removed)));
            }

            case "kolist" -> {
                if (!hasAdminPerm(sender, "reanimatemc.admin")) {
                    sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                    return true;
                }
                java.util.Set<java.util.UUID> uuids = koManager.getKOPlayerUUIDs();
                if (uuids.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("kolist_empty"));
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "━━━━━ " + ReanimateMC.lang.get("kolist_header") + " ━━━━━");
                for (java.util.UUID uuid : uuids) {
                    Player t = Bukkit.getPlayer(uuid);
                    if (t == null || !t.isOnline()) continue;
                    long rem = koManager.getRemainingKOSeconds(t);
                    ChatColor c = rem > 30 ? ChatColor.GREEN : rem > 10 ? ChatColor.YELLOW : ChatColor.RED;
                    sender.sendMessage(ChatColor.WHITE + t.getName() + ChatColor.GRAY + " - " + c + rem + "s");
                }
                sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━");
            }

            case "removeglowingreffect", "removeglow", "removeglowingeffect" -> {
                if (!hasAdminPerm(sender, "reanimatemc.removeglow")) {
                    sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
                    return true;
                }
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_remove_glowing_usage")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found")); return true; }
                target.setGlowing(false);
                sender.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("glowing_effect_removed"));
            }

            case "info" -> {
                sendPluginInfo(sender);
            }

            case "version", "ver" -> {
                sendVersion(sender);
            }

            // ── Player-level ──────────────────────────────────────────────────

            case "revive" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!hasPerm(player, "reanimatemc.revive", false)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                if (args.length < 2) { player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_revive_usage")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found")); return true; }
                if (!koManager.isKO(target)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_ko")); return true; }
                npcSummonManager.startCommandRevive(player, target);
            }

            case "status" -> {
                if (!hasPerm(sender, "reanimatemc.status", false)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_status_usage")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found")); return true; }
                String status = koManager.isKO(target) ? ReanimateMC.lang.get("status_ko") : ReanimateMC.lang.get("status_normal");
                sender.sendMessage(ChatColor.AQUA + target.getName() + " : " + status);
            }

            case "distress" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!koManager.isKO(player)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_in_ko")); return true; }
                koManager.sendDistress(player);
            }

            case "selfrevive", "sr" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!hasPerm(player, "reanimatemc.selfrevive", false)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                if (!ReanimateMC.getInstance().getConfig().getBoolean("self_revive.enabled", true)) {
                    player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_disabled")); return true;
                }
                if (!koManager.isKO(player)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_in_ko")); return true; }
                koManager.startSelfRevive(player);
            }

            case "cancelsr", "cancelselfrevive" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!koManager.isChannelingSelfRevive(player)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_not_channeling")); return true; }
                koManager.cancelSelfRevive(player, true);
            }

            case "crawl" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!hasPerm(player, "reanimatemc.crawl", false)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                if (!koManager.isKO(player)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_in_ko")); return true; }
                if (!ReanimateMC.getInstance().getConfig().getBoolean("prone.allow_crawl", false)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("crawl_not_allowed")); return true; }
                koManager.toggleCrawl(player);
            }

            case "gui", "config" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_gui_player_only")); return true; }
                if (!hasAdminPerm(player, "reanimatemc.config")) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                configGui.openGUI(player);
                if (!ReanimateMC.getInstance().getConfig().getBoolean("setup_completed", false)) {
                    ReanimateMC.getInstance().getConfig().set("setup_completed", true);
                    ReanimateMC.getInstance().saveConfig();
                    player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("setup_complete"));
                }
            }

            // ── NPC system ────────────────────────────────────────────────────

            case "summon" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!hasPerm(player, "reanimatemc.summon", true)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                if (args.length < 2) { player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_summon_usage")); return true; }

                // /rmc summon team <p1> <p2> ...
                if (args[1].equalsIgnoreCase("team")) {
                    if (args.length < 4) { player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_summon_team_usage")); return true; }
                    ReanimatorNPC.ReanimatorType teamType;
                    try { teamType = ReanimatorNPC.ReanimatorType.valueOf(args[2].toUpperCase()); }
                    catch (IllegalArgumentException e) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_invalid_type", "type", args[2])); return true; }
                    List<Player> team = new ArrayList<>();
                    for (int i = 3; i < args.length; i++) {
                        Player m = Bukkit.getPlayer(args[i]);
                        if (m == null) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found") + ": " + args[i]); return true; }
                        team.add(m);
                    }
                    npcSummonManager.summonTeam(player, teamType, team);
                    return true;
                }

                // /rmc summon <type> [targetPlayer]
                ReanimatorNPC.ReanimatorType type;
                try { type = ReanimatorNPC.ReanimatorType.valueOf(args[1].toUpperCase()); }
                catch (IllegalArgumentException e) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_invalid_type", "type", args[1])); return true; }
                Player targetPlayer = args.length >= 3 ? Bukkit.getPlayer(args[2]) : null;
                if (args.length >= 3 && targetPlayer == null) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_found")); return true; }
                npcSummonManager.summon(player, type, targetPlayer);
            }

            case "dismiss" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!hasPerm(player, "reanimatemc.summon", true)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                if (args.length < 2) { player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_dismiss_usage")); return true; }
                if (args[1].equalsIgnoreCase("all")) {
                    npcSummonManager.dismissAll(player);
                } else {
                    ReanimatorNPC.ReanimatorType tf = null;
                    try { tf = ReanimatorNPC.ReanimatorType.valueOf(args[1].toUpperCase()); } catch (IllegalArgumentException ignored) {}
                    if (tf != null) {
                        int c = npcSummonManager.dismissByType(player, tf);
                        if (c == 0) player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_none_active"));
                    } else {
                        player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_dismiss_usage"));
                    }
                }
            }

            case "extend" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!hasPerm(player, "reanimatemc.summon", true)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                if (args.length < 2) { player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_extend_usage")); return true; }
                long secs;
                try { secs = Long.parseLong(args[1]); if (secs <= 0) throw new NumberFormatException(); }
                catch (NumberFormatException e) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_extend_usage")); return true; }
                boolean free = hasPerm(player, "reanimatemc.summon.overridecost", true);
                npcSummonManager.extendNpcTime(player, secs, !free);
            }

            case "npcs", "npcstatus" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + ReanimateMC.lang.get("command_player_only")); return true; }
                if (!hasPerm(player, "reanimatemc.summon", true)) { player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission")); return true; }
                List<ReanimatorNPC> npcs = npcSummonManager.getPlayerSummons(player);
                if (npcs.isEmpty()) { player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("npc_none_active")); }
                else npcSummonManager.sendNpcStatus(player);
            }

            // ── Help ──────────────────────────────────────────────────────────

            case "help", "?" -> sendHelp(sender);

            default -> sender.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("command_unknown"));
        }
        return true;
    }

    // ── Help / Version / Info ─────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "━━━━━ ReanimateMC " + ReanimateMC.lang.get("cmd_help_title") + " ━━━━━");
        row(sender, "/rmc reload",                               "cmd_help_reload");
        row(sender, "/rmc revive <" + ReanimateMC.lang.get("cmd_arg_player") + ">",      "cmd_help_revive");
        row(sender, "/rmc selfrevive",                            "cmd_help_selfrevive");
        row(sender, "/rmc cancelselfrevive",                      "cmd_help_cancelselfrevive");
        row(sender, "/rmc knockout <" + ReanimateMC.lang.get("cmd_arg_player") + ">",    "cmd_help_knockout");
        row(sender, "/rmc status <" + ReanimateMC.lang.get("cmd_arg_player") + ">",      "cmd_help_status");
        row(sender, "/rmc distress",                              "cmd_help_distress");
        row(sender, "/rmc crawl",                                 "cmd_help_crawl");
        row(sender, "/rmc config",                                "cmd_help_config");
        row(sender, "/rmc summon <golem|healer|protector> [" + ReanimateMC.lang.get("cmd_arg_player") + "]", "cmd_help_summon");
        row(sender, "/rmc summon team <golem|healer|protector> <p1> <p2...>",             "cmd_help_summon_team");
        row(sender, "/rmc dismiss <all|golem|healer|protector>",  "cmd_help_dismiss");
        row(sender, "/rmc extend <" + ReanimateMC.lang.get("cmd_arg_seconds") + ">",     "cmd_help_extend");
        row(sender, "/rmc npcs",                                  "cmd_help_npcs");
        row(sender, "/rmc purge",                                 "cmd_help_purge");
        row(sender, "/rmc kolist",                                "cmd_help_kolist");
        row(sender, "/rmc info",                                  "cmd_help_info");
        row(sender, "/rmc version",                               "cmd_help_version");
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void row(CommandSender s, String cmd, String descKey) {
        s.sendMessage(ChatColor.YELLOW + cmd + ChatColor.GRAY + " - " + ReanimateMC.lang.get(descKey));
    }

    private void sendVersion(CommandSender sender) {
        String version = ReanimateMC.getInstance().getDescription().getVersion();
        String authors = String.join(", ", ReanimateMC.getInstance().getDescription().getAuthors());
        String mcVersion = Bukkit.getMinecraftVersion();
        sender.sendMessage(ChatColor.GOLD + "━━━━━ ReanimateMC ━━━━━");
        sender.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("cmd_version_plugin")  + " " + ChatColor.WHITE + "v" + version);
        sender.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("cmd_version_author")  + " " + ChatColor.WHITE + authors);
        sender.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("cmd_version_mc")      + " " + ChatColor.WHITE + mcVersion);
        sender.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("cmd_version_api")     + " " + ChatColor.WHITE + "Paper 1.21.4");
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendPluginInfo(CommandSender sender) {
        String version  = ReanimateMC.getInstance().getDescription().getVersion();
        String authors  = String.join(", ", ReanimateMC.getInstance().getDescription().getAuthors());
        String mcVersion = Bukkit.getMinecraftVersion();
        boolean vaultOk = ReanimateMC.getInstance().getVault() != null
                && ReanimateMC.getInstance().getVault().isEnabled();
        boolean papiOk  = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        boolean npcOk   = ReanimateMC.getInstance().getConfig().getBoolean("npc_summon.enabled", false);
        boolean koOk    = ReanimateMC.getInstance().getConfig().getBoolean("knockout.enabled", true);
        boolean exeOk   = ReanimateMC.getInstance().getConfig().getBoolean("execution.enabled", true);
        boolean selfRev = ReanimateMC.getInstance().getConfig().getBoolean("self_revive.enabled", true);
        boolean crawlOk = ReanimateMC.getInstance().getConfig().getBoolean("prone.allow_crawl", false);
        boolean distress= ReanimateMC.getInstance().getConfig().getBoolean("knockout.distress.enabled", true);
        int koCount     = koManager.getKOPlayerUUIDs().size();
        String lang     = ReanimateMC.getInstance().getConfig().getString("language", "en");

        String on  = ChatColor.GREEN  + "✔";
        String off = ChatColor.RED    + "✘";

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━ ReanimateMC ━━━━━━━━━");
        sender.sendMessage(ChatColor.GRAY + "  Version  " + ChatColor.WHITE + "v" + version
                + ChatColor.DARK_GRAY + "  |  " + ChatColor.GRAY + "API " + ChatColor.WHITE + mcVersion
                + ChatColor.DARK_GRAY + "  |  " + ChatColor.GRAY + "Author " + ChatColor.WHITE + authors);
        sender.sendMessage(ChatColor.GRAY + "  Language " + ChatColor.WHITE + lang);
        sender.sendMessage(ChatColor.GOLD + " ─── Systems ───────────────────");
        sender.sendMessage(ChatColor.GRAY + "  K.O. System          " + (koOk   ? on : off));
        sender.sendMessage(ChatColor.GRAY + "  Execution System      " + (exeOk  ? on : off));
        sender.sendMessage(ChatColor.GRAY + "  Crawl / Prone         " + (crawlOk? on : off));
        sender.sendMessage(ChatColor.GRAY + "  Distress Signal       " + (distress?on : off));
        sender.sendMessage(ChatColor.GRAY + "  Self-Revive           " + (selfRev? on : off));
        sender.sendMessage(ChatColor.GRAY + "  NPC Reanimators       " + (npcOk  ? on : off));
        sender.sendMessage(ChatColor.GOLD + " ─── Integrations ──────────────");
        sender.sendMessage(ChatColor.GRAY + "  Vault Economy         " + (vaultOk? on : off));
        sender.sendMessage(ChatColor.GRAY + "  PlaceholderAPI        " + (papiOk ? on : off));
        sender.sendMessage(ChatColor.GOLD + " ─── Live Status ───────────────");
        sender.sendMessage(ChatColor.GRAY + "  Players currently K.O.: " + ChatColor.WHITE + koCount);
        sender.sendMessage(ChatColor.YELLOW + "  Type /rmc help for all commands");
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── Tab complete ──────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "revive", "selfrevive", "sr", "cancelselfrevive",
                    "cancelsr", "knockout", "status", "crawl", "distress",
                    "config", "summon", "dismiss", "extend", "npcs", "purge", "help",
                    "version", "info", "kolist", "removeGlowingEffect"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "revive", "knockout", "status", "removeglowingeffect",
                        "removeglowingreffect", "removeglow" -> onlinePlayers(args[1]);
                case "summon" -> filter(typeNames("team"), args[1]);
                case "dismiss" -> filter(typeNames("all"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("summon")) {
                if (args[1].equalsIgnoreCase("team")) {
                    // /rmc summon team <type>
                    return filter(typeNames(), args[2]);
                }
                // /rmc summon <type> [player]
                return onlinePlayers(args[2]);
            }
        }
        // /rmc summon team <type> <p1> <p2>... — keep suggesting players
        if (args.length >= 4 && args[0].equalsIgnoreCase("summon") && args[1].equalsIgnoreCase("team")) {
            return onlinePlayers(args[args.length - 1]);
        }
        return List.of();
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> typeNames(String... extra) {
        List<String> list = new ArrayList<>();
        for (ReanimatorNPC.ReanimatorType t : ReanimatorNPC.ReanimatorType.values()) {
            list.add(t.name().toLowerCase());
        }
        for (String e : extra) list.add(e);
        return list;
    }

    private List<String> filter(List<String> source, String prefix) {
        return source.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
