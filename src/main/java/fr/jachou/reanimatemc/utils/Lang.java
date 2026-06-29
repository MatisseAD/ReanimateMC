package fr.jachou.reanimatemc.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Lang {

    private final Map<String, String> messages = new HashMap<>();
    private final JavaPlugin plugin;
    private String language;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        language = plugin.getConfig().getString("language", "en");
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        File langFile = new File(langFolder, language + ".yml");

        // Extract from jar if missing on disk
        if (!langFile.exists()) {
            String resource = "lang/" + language + ".yml";
            if (plugin.getResource(resource) != null) {
                plugin.saveResource(resource, false);
            } else {
                // Requested lang not bundled — fall back to English
                plugin.getLogger().warning("[ReanimateMC] Lang file not found for '" + language + "', falling back to 'en'.");
                language = "en";
                langFile = new File(langFolder, "en.yml");
                if (!langFile.exists()) plugin.saveResource("lang/en.yml", false);
            }
        }

        YamlConfiguration disk = YamlConfiguration.loadConfiguration(langFile);

        // Merge any new keys from the bundled lang file without overwriting customizations
        InputStream bundled = plugin.getResource("lang/" + language + ".yml");
        if (bundled != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8));
            disk.setDefaults(defaults);
            disk.options().copyDefaults(true);
            try { disk.save(langFile); } catch (Exception e) {
                plugin.getLogger().warning("[ReanimateMC] Could not save lang file: " + e.getMessage());
            }
        }

        messages.clear();
        for (String key : disk.getKeys(false)) {
            messages.put(key, disk.getString(key));
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, key);
    }

    public String get(String key, String... args) {
        String msg = get(key);
        for (int i = 0; i + 1 < args.length; i += 2) {
            msg = msg.replace("%" + args[i] + "%", args[i + 1]);
        }
        return msg;
    }
}
