package fr.jachou.reanimatemc.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class StatsManager {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration config;

    private int knockoutCount;
    private int reviveCount;

    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        this.knockoutCount = config.getInt("knockouts", 0);
        this.reviveCount = config.getInt("revives", 0);
    }

    public void addKnockout() {
        knockoutCount++;
        save();
    }

    public void addRevive() {
        reviveCount++;
        save();
    }

    public int getKnockoutCount() {
        return knockoutCount;
    }

    public int getReviveCount() {
        return reviveCount;
    }

    private void save() {
        config.set("knockouts", knockoutCount);
        config.set("revives", reviveCount);
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }
}
