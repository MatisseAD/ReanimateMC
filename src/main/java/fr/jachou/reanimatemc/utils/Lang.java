package fr.jachou.reanimatemc.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Lang {
    private Map<String, String> messages = new HashMap<>();
    private JavaPlugin plugin;
    private String language;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        // Récupère la langue définie dans la config (par défaut "en")
        language = plugin.getConfig().getString("language", "en");
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        File langFile = new File(langFolder, language + ".yml");
        if (!langFile.exists()){
            // Sauvegarde le fichier de langue par défaut depuis les resources
            plugin.saveResource("lang/" + language + ".yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        messages.clear();
        for (String key: config.getKeys(false)) {
            messages.put(key, config.getString(key));
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, key);
    }

    // Remplace les marqueurs %key% par leur valeur correspondante
    public String get(String key, String... args) {
        String msg = get(key);
        for (int i = 0; i < args.length; i += 2) {
            msg = msg.replace("%" + args[i] + "%", args[i+1]);
        }
        return msg;
    }
}
