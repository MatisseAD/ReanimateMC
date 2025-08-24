package fr.jachou.reanimatemc.gui;

import fr.jachou.reanimatemc.ReanimateMC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigGUI implements Listener {
    private final ReanimateMC plugin;
    private final FileConfiguration cfg;

    // Organiser les options par catégories
    private final static Map<String, GuiOption> REANIMATION_OPTIONS = new LinkedHashMap<>();
    private final static Map<String, GuiOption> KNOCKOUT_OPTIONS = new LinkedHashMap<>();
    private final static Map<String, GuiOption> GAMEPLAY_OPTIONS = new LinkedHashMap<>();
    private final static Map<String, GuiOption> INTERFACE_OPTIONS = new LinkedHashMap<>();

    static {
        // Réanimation (Ligne 1: slots 0-2)
        REANIMATION_OPTIONS.put("reanimation.require_special_item",
                new GuiOption("option_reanimation_require", 1));
        REANIMATION_OPTIONS.put("execution.enabled",
                new GuiOption("option_execution_enabled", 2));
        REANIMATION_OPTIONS.put("execution.message_broadcast",
                new GuiOption("option_execution_broadcast", 3));

        // Knockout (Ligne 2: slots 9-13) 
        KNOCKOUT_OPTIONS.put("knockout.enabled",
                new GuiOption("option_knockout_enabled", 10));
        KNOCKOUT_OPTIONS.put("knockout.movement_disabled",
                new GuiOption("option_knockout_move", 11));
        KNOCKOUT_OPTIONS.put("knockout.use_particles",
                new GuiOption("option_knockout_particles", 12));
        KNOCKOUT_OPTIONS.put("knockout.heartbeat_sound",
                new GuiOption("option_knockout_sound", 13));
        KNOCKOUT_OPTIONS.put("knockout.blindness",
                new GuiOption("option_knockout_blindness", 14));

        // Gameplay (Ligne 3: slots 18-20)
        GAMEPLAY_OPTIONS.put("prone.enabled",
                new GuiOption("option_prone_enabled", 19));
        GAMEPLAY_OPTIONS.put("prone.allow_crawl",
                new GuiOption("option_prone_crawl", 20));
        GAMEPLAY_OPTIONS.put("looting.enabled",
                new GuiOption("option_looting_enabled", 21));

        // Interface (Ligne 4: slots 27-28)
        INTERFACE_OPTIONS.put("tablist.enabled",
                new GuiOption("option_tablist_enabled", 28));
    }

    // Slots spéciaux
    private static final int CATEGORY_REANIMATION_SLOT = 0;
    private static final int CATEGORY_KNOCKOUT_SLOT = 9;
    private static final int CATEGORY_GAMEPLAY_SLOT = 18;
    private static final int CATEGORY_INTERFACE_SLOT = 27;

    private static final int STATS_SLOT = 4;
    private static final int INFO_SLOT = 22;
    private static final int LANG_SLOT = 31;
    private static final int RELOAD_SLOT = 32;
    private static final int SAVE_SLOT = 40;
    private static final int RESET_SLOT = 41;
    private static final int CLOSE_SLOT = 49;

    private static final java.util.List<String> LANGS = java.util.Arrays.asList("en", "fr", "es", "de", "pt", "it");

    private static class GuiOption {
        final String langKey;
        final int slot;

        GuiOption(String langKey, int slot) {
            this.langKey = langKey;
            this.slot = slot;
        }
    }

    public ConfigGUI(ReanimateMC plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void openGUI(Player player) {
        // Interface 6x9 pour plus d'espace
        Inventory inv = Bukkit.createInventory(null, 9 * 6,
                ChatColor.translateAlternateColorCodes('&',
                        ReanimateMC.lang.get("gui_title"))
        );

        // Remplir les bordures avec des panneaux de verre
        fillBorders(inv);

        // Créer les indicateurs de catégories
        createCategoryIndicators(inv);

        // Remplir toutes les options par catégorie
        populateOptions(inv, REANIMATION_OPTIONS);
        populateOptions(inv, KNOCKOUT_OPTIONS);
        populateOptions(inv, GAMEPLAY_OPTIONS);
        populateOptions(inv, INTERFACE_OPTIONS);

        // Créer les éléments utilitaires  
        createUtilityItems(inv);

        player.openInventory(inv);
    }

    private void fillBorders(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        // Bordures supérieure et inférieure
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
            if (inv.getItem(45 + i) == null) inv.setItem(45 + i, glass);
        }

        // Bordures latérales
        for (int row = 1; row < 5; row++) {
            if (inv.getItem(row * 9) == null) inv.setItem(row * 9, glass);
            if (inv.getItem(row * 9 + 8) == null) inv.setItem(row * 9 + 8, glass);
        }
    }

    private void createCategoryIndicators(Inventory inv) {
        // Indicateur Réanimation
        ItemStack reanimationCat = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta rMeta = reanimationCat.getItemMeta();
        rMeta.setDisplayName(ChatColor.GOLD + ReanimateMC.lang.get("category_reanimation"));
        rMeta.setLore(Arrays.asList(ChatColor.GRAY + ReanimateMC.lang.get("category_reanimation_desc")));
        rMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        reanimationCat.setItemMeta(rMeta);
        inv.setItem(CATEGORY_REANIMATION_SLOT, reanimationCat);

        // Indicateur Knockout
        ItemStack knockoutCat = new ItemStack(Material.IRON_SWORD);
        ItemMeta kMeta = knockoutCat.getItemMeta();
        kMeta.setDisplayName(ChatColor.RED + ReanimateMC.lang.get("category_knockout"));
        kMeta.setLore(Arrays.asList(ChatColor.GRAY + ReanimateMC.lang.get("category_knockout_desc")));
        kMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        knockoutCat.setItemMeta(kMeta);
        inv.setItem(CATEGORY_KNOCKOUT_SLOT, knockoutCat);

        // Indicateur Gameplay
        ItemStack gameplayCat = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta gMeta = gameplayCat.getItemMeta();
        gMeta.setDisplayName(ChatColor.BLUE + ReanimateMC.lang.get("category_gameplay"));
        gMeta.setLore(Arrays.asList(ChatColor.GRAY + ReanimateMC.lang.get("category_gameplay_desc")));
        gMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        gameplayCat.setItemMeta(gMeta);
        inv.setItem(CATEGORY_GAMEPLAY_SLOT, gameplayCat);

        // Indicateur Interface
        ItemStack interfaceCat = new ItemStack(Material.ITEM_FRAME);
        ItemMeta iMeta = interfaceCat.getItemMeta();
        iMeta.setDisplayName(ChatColor.AQUA + ReanimateMC.lang.get("category_interface"));
        iMeta.setLore(Arrays.asList(ChatColor.GRAY + ReanimateMC.lang.get("category_interface_desc")));
        interfaceCat.setItemMeta(iMeta);
        inv.setItem(CATEGORY_INTERFACE_SLOT, interfaceCat);
    }

    private void populateOptions(Inventory inv, Map<String, GuiOption> options) {
        for (Map.Entry<String, GuiOption> entry : options.entrySet()) {
            String path = entry.getKey();
            GuiOption opt = entry.getValue();

            boolean enabled = cfg.getBoolean(path, false);
            Material mat = enabled ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            String name = ReanimateMC.lang.get(opt.langKey);
            meta.setDisplayName(ChatColor.YELLOW + name);
            meta.setLore(Arrays.asList(
                    enabled
                            ? ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_on"))
                            : ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_off")),
                    "",
                    ChatColor.GRAY + ReanimateMC.lang.get("click_toggle"),
                    ChatColor.GRAY + ReanimateMC.lang.get("config_path", "path", path)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);

            inv.setItem(opt.slot, item);
        }
    }

    private void createUtilityItems(Inventory inv) {
        // Statistiques
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta sMeta = stats.getItemMeta();
        sMeta.setDisplayName(ChatColor.AQUA + ReanimateMC.lang.get("stats_title"));
        sMeta.setLore(Arrays.asList(
                ChatColor.WHITE + ReanimateMC.lang.get("stats_ko", "value", String.valueOf(plugin.getStatsManager().getKnockoutCount())),
                ChatColor.WHITE + ReanimateMC.lang.get("stats_revive", "value", String.valueOf(plugin.getStatsManager().getReviveCount())),
                "",
                ChatColor.GRAY + ReanimateMC.lang.get("click_stats")
        ));
        stats.setItemMeta(sMeta);
        inv.setItem(STATS_SLOT, stats);

        // Informations du plugin
        ItemStack info = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(ChatColor.GREEN + ReanimateMC.lang.get("plugin_info"));
        String authors = String.join(", ", plugin.getDescription().getAuthors());
        if (authors.isEmpty()) authors = "?";
        iMeta.setLore(Arrays.asList(
                ChatColor.WHITE + ReanimateMC.lang.get("info_version", "value", plugin.getDescription().getVersion()),
                ChatColor.WHITE + ReanimateMC.lang.get("info_authors", "value", authors),
                "",
                ChatColor.GRAY + ReanimateMC.lang.get("plugin_name")
        ));
        info.setItemMeta(iMeta);
        inv.setItem(INFO_SLOT, info);

        // Changement de langue
        ItemStack langItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta lMeta = langItem.getItemMeta();
        lMeta.setDisplayName(ChatColor.YELLOW + ReanimateMC.lang.get("change_language"));
        lMeta.setLore(Arrays.asList(
                ChatColor.WHITE + ReanimateMC.lang.get("current_lang", "lang", plugin.getConfig().getString("language")),
                "",
                ChatColor.GRAY + ReanimateMC.lang.get("click_change_lang")
        ));
        langItem.setItemMeta(lMeta);
        inv.setItem(LANG_SLOT, langItem);

        // Recharger la configuration
        ItemStack reload = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta rMeta = reload.getItemMeta();
        rMeta.setDisplayName(ChatColor.LIGHT_PURPLE + ReanimateMC.lang.get("reload_config"));
        rMeta.setLore(Arrays.asList(
                ChatColor.GRAY + ReanimateMC.lang.get("reload_desc1"),
                ChatColor.GRAY + ReanimateMC.lang.get("reload_desc2")
        ));
        reload.setItemMeta(rMeta);
        inv.setItem(RELOAD_SLOT, reload);

        // Sauvegarder
        ItemStack save = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN + ReanimateMC.lang.get("save"));
        saveMeta.setLore(Arrays.asList(
                ChatColor.GRAY + ReanimateMC.lang.get("save_desc1"),
                ChatColor.GRAY + ReanimateMC.lang.get("save_desc2")
        ));
        save.setItemMeta(saveMeta);
        inv.setItem(SAVE_SLOT, save);

        // Reset
        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = reset.getItemMeta();
        resetMeta.setDisplayName(ChatColor.RED + ReanimateMC.lang.get("reset"));
        resetMeta.setLore(Arrays.asList(
                ChatColor.GRAY + ReanimateMC.lang.get("reset_desc1"),
                ChatColor.GRAY + ReanimateMC.lang.get("reset_desc2"),
                "",
                ChatColor.RED + ReanimateMC.lang.get("reset_warning")
        ));
        reset.setItemMeta(resetMeta);
        inv.setItem(RESET_SLOT, reset);

        // Fermer
        ItemStack close = new ItemStack(Material.RED_STAINED_GLASS);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + ReanimateMC.lang.get("close"));
        closeMeta.setLore(Arrays.asList(ChatColor.GRAY + ReanimateMC.lang.get("close_desc")));
        close.setItemMeta(closeMeta);
        inv.setItem(CLOSE_SLOT, close);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;

        String title = ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("gui_title"));
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        // Gestion des clics spéciaux
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (slot == SAVE_SLOT) {
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("config_saved"));
            return;
        }

        if (slot == RELOAD_SLOT) {
            plugin.reloadConfig();
            player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("config_reloaded"));
            openGUI(player); // Rouvrir l'interface
            return;
        }

        if (slot == RESET_SLOT) {
            if (event.isShiftClick()) {
                resetAllOptions();
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("config_reset"));
                openGUI(player);
            } else {
                player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("reset_confirm"));
            }
            return;
        }

        if (slot == LANG_SLOT) {
            handleLanguageChange(inv, player);
            return;
        }

        // Gestion des options configurables
        handleOptionToggle(inv, player, slot);
    }

    private void handleLanguageChange(Inventory inv, Player player) {
        String current = cfg.getString("language", "en").toLowerCase();
        int idx = LANGS.indexOf(current);
        if (idx == -1) idx = 0;
        String next = LANGS.get((idx + 1) % LANGS.size());
        cfg.set("language", next);
        plugin.saveConfig();
        ReanimateMC.lang.loadLanguage();

        ItemStack langItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta lMeta = langItem.getItemMeta();
        lMeta.setDisplayName(ChatColor.YELLOW + ReanimateMC.lang.get("change_language"));
        lMeta.setLore(Arrays.asList(
                ChatColor.WHITE + ReanimateMC.lang.get("current_lang", "lang", next),
                "",
                ChatColor.GRAY + ReanimateMC.lang.get("click_change_lang")
        ));
        langItem.setItemMeta(lMeta);
        inv.setItem(LANG_SLOT, langItem);

        player.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("language_changed", "lang", next));
    }

    private void handleOptionToggle(Inventory inv, Player player, int slot) {
        String configPath = null;
        String langKey = null;

        // Chercher dans toutes les catégories
        for (Map.Entry<String, GuiOption> entry : REANIMATION_OPTIONS.entrySet()) {
            if (entry.getValue().slot == slot) {
                configPath = entry.getKey();
                langKey = entry.getValue().langKey;
                break;
            }
        }
        if (configPath == null) {
            for (Map.Entry<String, GuiOption> entry : KNOCKOUT_OPTIONS.entrySet()) {
                if (entry.getValue().slot == slot) {
                    configPath = entry.getKey();
                    langKey = entry.getValue().langKey;
                    break;
                }
            }
        }
        if (configPath == null) {
            for (Map.Entry<String, GuiOption> entry : GAMEPLAY_OPTIONS.entrySet()) {
                if (entry.getValue().slot == slot) {
                    configPath = entry.getKey();
                    langKey = entry.getValue().langKey;
                    break;
                }
            }
        }
        if (configPath == null) {
            for (Map.Entry<String, GuiOption> entry : INTERFACE_OPTIONS.entrySet()) {
                if (entry.getValue().slot == slot) {
                    configPath = entry.getKey();
                    langKey = entry.getValue().langKey;
                    break;
                }
            }
        }

        if (configPath == null) return;

        boolean current = cfg.getBoolean(configPath, false);
        boolean next = !current;
        cfg.set(configPath, next);

        // Mettre à jour l'affichage
        Material mat = next ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String displayName = ReanimateMC.lang.get(langKey);
        meta.setDisplayName(ChatColor.YELLOW + displayName);
        meta.setLore(Arrays.asList(
                next
                        ? ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_on"))
                        : ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_off")),
                "",
                ChatColor.GRAY + ReanimateMC.lang.get("click_toggle"),
                ChatColor.GRAY + ReanimateMC.lang.get("config_path", "path", configPath)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inv.setItem(slot, item);

        String optionName = ReanimateMC.lang.get(langKey);
        String stateMsg = next ? ReanimateMC.lang.get("toggle_on") : ReanimateMC.lang.get("toggle_off");
        String msg = ReanimateMC.lang.get("message_gui_toggle",
                "option", optionName,
                "state", ChatColor.stripColor(stateMsg)
        );
        player.sendMessage(ChatColor.GRAY + msg);
    }

    private void resetAllOptions() {
        for (String path : REANIMATION_OPTIONS.keySet()) {
            cfg.set(path, false);
        }
        for (String path : KNOCKOUT_OPTIONS.keySet()) {
            cfg.set(path, false);
        }
        for (String path : GAMEPLAY_OPTIONS.keySet()) {
            cfg.set(path, false);
        }
        for (String path : INTERFACE_OPTIONS.keySet()) {
            cfg.set(path, false);
        }
        plugin.saveConfig();
    }
}