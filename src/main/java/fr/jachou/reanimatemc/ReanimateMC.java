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
import fr.jachou.reanimatemc.managers.KOManager;
import fr.jachou.reanimatemc.utils.Lang;
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
    public static Lang lang;
    private ConfigGUI configGui;

    public static ReanimateMC getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Création (si nécessaire) du fichier config.yml

        // Langues
        lang = new Lang(this);

        // Inclure les Metrics
        int pluginId = 20034;
        Metrics metrics = new Metrics(this, pluginId);

        // Initialisation du gestionnaire des états K.O.
        koManager = new KOManager(this);

        // Instantiate and register GUI listener
        configGui = new ConfigGUI(this);
        getServer().getPluginManager().registerEvents(configGui, this);

        // Enregistrement des écouteurs d’événements
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(koManager), this);
        getServer().getPluginManager().registerEvents(new ReanimationListener(koManager), this);
        getServer().getPluginManager().registerEvents(new ExecutionListener(koManager), this);
        getServer().getPluginManager().registerEvents(new LootListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerKOListener(koManager), this);

        // Enregistrement de la commande principale
        getCommand("reanimatemc").setExecutor(new ReanimateMCCommand(koManager, configGui));
        getCommand("reanimatemc").setTabCompleter(new ReanimateMCCommand(koManager, configGui));

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
        }.runTaskTimer(this, 0L, 20L); // Exécute toutes les 20 ticks (1 seconde)
    }

    @Override
    public void onDisable() {
        // Annulation de toutes les tâches programmées relatives aux joueurs en K.O.
        koManager.cancelAllTasks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (koManager.isKO(player)) {
                koManager.revive(player);
                player.sendMessage(ChatColor.RED + lang.get("plugin_disabled"));
            }
        }
    }

    public KOManager getKoManager() {
        return koManager;
    }
}
