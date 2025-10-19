/* LICENSE BEGIN
    * This file is part of ReanimateMC.
    * ReanimateMC is under the proprietary of Frouzie.
    * You are not allowed to redistribute it and/or modify it.
LICENSE END
 */

package fr.jachou.reanimatemc;


import fr.jachou.reanimatemc.commands.ReanimateMCCommand;
import fr.jachou.reanimatemc.externals.Metrics;
import fr.jachou.reanimatemc.gui.ConfigGUI;
import fr.jachou.reanimatemc.listeners.*;
import fr.jachou.reanimatemc.listeners.SetupReminderListener;
import fr.jachou.reanimatemc.managers.KOManager;
import fr.jachou.reanimatemc.managers.NPCSummonManager;
import fr.jachou.reanimatemc.managers.StatsManager;
import fr.jachou.reanimatemc.utils.Lang;
import fr.jachou.reanimatemc.utils.updater.UpdateChecker;
import fr.jachou.reanimatemc.utils.updater.UpdateNotifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public final class ReanimateMC extends JavaPlugin {

    private static ReanimateMC instance;
    private KOManager koManager;
    private StatsManager statsManager;
    private NPCSummonManager npcSummonManager;
    public static Lang lang;
    private ConfigGUI configGui;
    private UpdateNotifier notifier;

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public static ReanimateMC getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Création (si nécessaire) du fichier config.yml

        // Langues
        lang = new Lang(this);

        // Statistics manager
        statsManager = new StatsManager(this);

        // Inclure les Metrics
        int pluginId = 20034;
        Metrics metrics = new Metrics(this, pluginId);

        // Initialisation du gestionnaire des états K.O.
        koManager = new KOManager(this);

        // Initialisation du gestionnaire de NPCs
        npcSummonManager = new NPCSummonManager(this, koManager);

        // Instantiate and register GUI listener
        configGui = new ConfigGUI(this);
        getServer().getPluginManager().registerEvents(configGui, this);

        // Enregistrement des écouteurs d’événements
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(koManager), this);
        getServer().getPluginManager().registerEvents(new ReanimationListener(koManager), this);
        getServer().getPluginManager().registerEvents(new ExecutionListener(koManager), this);
        getServer().getPluginManager().registerEvents(new LootListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerKOListener(koManager), this);
        getServer().getPluginManager().registerEvents(new TotemListener(koManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(koManager), this);

        // Enregistrement de la commande principale
        getCommand("reanimatemc").setExecutor(new ReanimateMCCommand(koManager, configGui, npcSummonManager));
        getCommand("reanimatemc").setTabCompleter(new ReanimateMCCommand(koManager, configGui, npcSummonManager));

        // Tâche pour vérfier si les joueurs ont l'effet glowing
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.hasPotionEffect(PotionEffectType.GLOWING) && !koManager.isKO(player)) {
                        player.setGlowing(false);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        Bukkit.getConsoleSender().sendMessage("ReanimateMC running on version " + getDescription().getVersion() + "!");

        notifier = new UpdateNotifier(new UpdateChecker(Bukkit.getServer().getVersion()));

        new BukkitRunnable() {
            @Override public void run() { notifier.notifyIfOutdated(); }
        }.runTaskAsynchronously(this);

        long periodTicks = 12L * 60L * 60L * 20L;
        new BukkitRunnable() {
            @Override public void run() { notifier.notifyIfOutdated(); }
        }.runTaskTimerAsynchronously(this, periodTicks, periodTicks);

        if (!getConfig().getBoolean("setup_completed", false)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("reanimatemc.admin") || p.hasPermission("*")) {
                        p.sendMessage(ChatColor.GOLD + lang.get("first_run_message"));
                    }
                }
            }, 40L);
        }

        getServer().getPluginManager().registerEvents(new SetupReminderListener(this), this);
    }

    @Override
    public void onDisable() {
        // Annulation de toutes les tâches programmées relatives aux joueurs en K.O.
        koManager.cancelAllTasks();
        
        // Cleanup NPC summons
        if (npcSummonManager != null) {
            npcSummonManager.cleanup();
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (koManager.isKO(player)) {
                player.setHealth(0);
                player.sendMessage(ChatColor.RED + lang.get("plugin_disabled"));
            }
        }

        Bukkit.getConsoleSender().sendMessage("ReanimateMC has been disabled.");
    }

    public KOManager getKoManager() {
        return koManager;
    }

    public UpdateNotifier getNotifier() {
        return notifier;
    }

    public NPCSummonManager getNpcSummonManager() {
        return npcSummonManager;
    }
}
