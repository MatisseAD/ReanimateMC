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
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ConfigGUI implements Listener {
    private final ReanimateMC plugin;
    private final FileConfiguration cfg;

    // Map pour stocker les joueurs en mode édition
    private final Map<Player, String> editingPlayers = new HashMap<>();

    // Organiser les options par catégories
    private final static Map<String, GuiOption> REANIMATION_OPTIONS = new LinkedHashMap<>();
    private final static Map<String, GuiOption> KNOCKOUT_OPTIONS = new LinkedHashMap<>();
    private final static Map<String, GuiOption> GAMEPLAY_OPTIONS = new LinkedHashMap<>();
    private final static Map<String, GuiOption> INTERFACE_OPTIONS = new LinkedHashMap<>();
    private final static Map<String, GuiOption> EFFECTS_OPTIONS = new LinkedHashMap<>();

    static {
        // Réanimation (Ligne 1: slots 1-8)
        REANIMATION_OPTIONS.put("reanimation.require_special_item",
                new GuiOption("option_reanimation_require", 1, OptionType.BOOLEAN));
        REANIMATION_OPTIONS.put("reanimation.required_item",
                new GuiOption("option_reanimation_item", 2, OptionType.MATERIAL));
        REANIMATION_OPTIONS.put("reanimation.duration_ticks",
                new GuiOption("option_reanimation_duration", 3, OptionType.INTEGER));
        REANIMATION_OPTIONS.put("reanimation.health_restored",
                new GuiOption("option_reanimation_health", 4, OptionType.INTEGER));
        REANIMATION_OPTIONS.put("reanimation.cooldown",
                new GuiOption("option_reanimation_cooldown", 5, OptionType.INTEGER));
        REANIMATION_OPTIONS.put("reanimation.revive_cooldown",
                new GuiOption("option_revive_cooldown", 6, OptionType.INTEGER));
        REANIMATION_OPTIONS.put("execution.enabled",
                new GuiOption("option_execution_enabled", 7, OptionType.BOOLEAN));
        REANIMATION_OPTIONS.put("execution.hold_duration_ticks",
                new GuiOption("option_execution_duration", 8, OptionType.INTEGER));

        // Knockout (Ligne 2: slots 10-17)
        KNOCKOUT_OPTIONS.put("knockout.enabled",
                new GuiOption("option_knockout_enabled", 10, OptionType.BOOLEAN));
        KNOCKOUT_OPTIONS.put("knockout.duration_seconds",
                new GuiOption("option_knockout_duration", 11, OptionType.INTEGER));
        KNOCKOUT_OPTIONS.put("knockout.movement_disabled",
                new GuiOption("option_knockout_move", 12, OptionType.BOOLEAN));
        KNOCKOUT_OPTIONS.put("knockout.use_particles",
                new GuiOption("option_knockout_particles", 13, OptionType.BOOLEAN));
        KNOCKOUT_OPTIONS.put("knockout.heartbeat_sound",
                new GuiOption("option_knockout_sound", 14, OptionType.BOOLEAN));
        KNOCKOUT_OPTIONS.put("knockout.blindness",
                new GuiOption("option_knockout_blindness", 15, OptionType.BOOLEAN));
        KNOCKOUT_OPTIONS.put("knockout.suicide_hold_seconds",
                new GuiOption("option_knockout_suicide", 16, OptionType.INTEGER));
        KNOCKOUT_OPTIONS.put("knockout.weakness_level",
                new GuiOption("option_knockout_weakness", 17, OptionType.INTEGER));

        // Gameplay (Ligne 3: slots 19-26)
        GAMEPLAY_OPTIONS.put("prone.enabled",
                new GuiOption("option_prone_enabled", 19, OptionType.BOOLEAN));
        GAMEPLAY_OPTIONS.put("prone.allow_crawl",
                new GuiOption("option_prone_crawl", 20, OptionType.BOOLEAN));
        GAMEPLAY_OPTIONS.put("prone.crawl_slowness_level",
                new GuiOption("option_prone_slowness", 21, OptionType.INTEGER));
        GAMEPLAY_OPTIONS.put("prone.auto_crawl",
                new GuiOption("option_prone_auto", 22, OptionType.BOOLEAN));
        GAMEPLAY_OPTIONS.put("looting.enabled",
                new GuiOption("option_looting_enabled", 23, OptionType.BOOLEAN));
        GAMEPLAY_OPTIONS.put("knockout.fatigue_level",
                new GuiOption("option_knockout_fatigue", 24, OptionType.INTEGER));
        GAMEPLAY_OPTIONS.put("execution.message_broadcast",
                new GuiOption("option_execution_broadcast", 25, OptionType.BOOLEAN));

        // Interface (Ligne 4: slots 28-29)
        INTERFACE_OPTIONS.put("tablist.enabled",
                new GuiOption("option_tablist_enabled", 28, OptionType.BOOLEAN));

        // Effets (Ligne 5: slots 37-39)
        EFFECTS_OPTIONS.put("effects_on_revive.nausea",
                new GuiOption("option_effect_nausea", 37, OptionType.INTEGER));
        EFFECTS_OPTIONS.put("effects_on_revive.slowness",
                new GuiOption("option_effect_slowness", 38, OptionType.INTEGER));
        EFFECTS_OPTIONS.put("effects_on_revive.resistance",
                new GuiOption("option_effect_resistance", 39, OptionType.INTEGER));
    }

    // Slots spéciaux
    private static final int CATEGORY_REANIMATION_SLOT = 0;
    private static final int CATEGORY_KNOCKOUT_SLOT = 9;
    private static final int CATEGORY_GAMEPLAY_SLOT = 18;
    private static final int CATEGORY_INTERFACE_SLOT = 27;
    private static final int CATEGORY_EFFECTS_SLOT = 36;

    private static final int STATS_SLOT = 4;
    private static final int INFO_SLOT = 22;
    private static final int LANG_SLOT = 31;
    private static final int RELOAD_SLOT = 32;
    private static final int SAVE_SLOT = 49;
    private static final int RESET_SLOT = 50;
    private static final int CLOSE_SLOT = 53;

    private static final List<String> LANGS = Arrays.asList("en", "fr", "es", "de", "pt", "it");

    private enum OptionType {
        BOOLEAN, INTEGER, MATERIAL
    }

    private static class GuiOption {
        final String langKey;
        final int slot;
        final OptionType type;

        GuiOption(String langKey, int slot, OptionType type) {
            this.langKey = langKey;
            this.slot = slot;
            this.type = type;
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
        populateOptions(inv, EFFECTS_OPTIONS);

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

        // Indicateur Effets
        ItemStack effectsCat = new ItemStack(Material.POTION);
        ItemMeta eMeta = effectsCat.getItemMeta();
        eMeta.setDisplayName(ChatColor.LIGHT_PURPLE + ReanimateMC.lang.get("category_effects"));
        eMeta.setLore(Arrays.asList(ChatColor.GRAY + ReanimateMC.lang.get("category_effects_desc")));
        effectsCat.setItemMeta(eMeta);
        inv.setItem(CATEGORY_EFFECTS_SLOT, effectsCat);
    }

    private void populateOptions(Inventory inv, Map<String, GuiOption> options) {
        for (Map.Entry<String, GuiOption> entry : options.entrySet()) {
            String path = entry.getKey();
            GuiOption opt = entry.getValue();

            ItemStack item = createOptionItem(path, opt);
            inv.setItem(opt.slot, item);
        }
    }

    private ItemStack createOptionItem(String path, GuiOption opt) {
        ItemStack item;
        ItemMeta meta;
        String name = ReanimateMC.lang.get(opt.langKey);
        List<String> lore = new ArrayList<>();

        switch (opt.type) {
            case BOOLEAN:
                boolean enabled = cfg.getBoolean(path, false);
                Material mat = enabled ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA;
                item = new ItemStack(mat);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + name);
                lore.add(enabled
                        ? ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_on"))
                        : ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_off")));
                lore.add("");
                lore.add(ChatColor.GRAY + ReanimateMC.lang.get("click_toggle"));
                break;

            case INTEGER:
                int value = cfg.getInt(path, 0);
                item = new ItemStack(Material.REPEATER);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + name);
                lore.add(ChatColor.WHITE + ReanimateMC.lang.get("current_value", "value", String.valueOf(value)));
                lore.add("");
                lore.add(ChatColor.GRAY + ReanimateMC.lang.get("click_modify_number"));
                lore.add(ChatColor.GRAY + ReanimateMC.lang.get("shift_click_increment"));
                break;

            case MATERIAL:
                String materialName = cfg.getString(path, "STONE");
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = Material.STONE;
                }
                item = new ItemStack(material);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + name);
                lore.add(ChatColor.WHITE + ReanimateMC.lang.get("current_material", "material", materialName));
                lore.add("");
                lore.add(ChatColor.GRAY + ReanimateMC.lang.get("click_modify_material"));
                break;

            default:
                item = new ItemStack(Material.BARRIER);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "Erreur");
                break;
        }

        lore.add(ChatColor.DARK_GRAY + ReanimateMC.lang.get("config_path", "path", path));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
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
            openGUI(player);
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
        handleOptionClick(inv, player, slot, event.isShiftClick());
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!editingPlayers.containsKey(player)) return;

        event.setCancelled(true);
        String configPath = editingPlayers.remove(player);
        String input = event.getMessage();

        // Annuler l'édition
        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("annuler")) {
            player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("edit_cancelled"));
            openGUI(player);
            return;
        }

        GuiOption option = findOptionByPath(configPath);
        if (option == null) {
            player.sendMessage(ChatColor.RED + "Erreur: Option introuvable");
            openGUI(player);
            return;
        }

        boolean success = false;
        switch (option.type) {
            case INTEGER:
                try {
                    int value = Integer.parseInt(input);
                    if (value < 0) {
                        player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("invalid_number_negative"));
                    } else {
                        cfg.set(configPath, value);
                        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("value_updated",
                                "option", ReanimateMC.lang.get(option.langKey),
                                "value", String.valueOf(value)));
                        success = true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("invalid_number"));
                }
                break;

            case MATERIAL:
                try {
                    Material material = Material.valueOf(input.toUpperCase());
                    cfg.set(configPath, material.name());
                    player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("material_updated",
                            "option", ReanimateMC.lang.get(option.langKey),
                            "material", material.name()));
                    success = true;
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("invalid_material", "material", input));
                }
                break;
        }

        openGUI(player);
    }

    private GuiOption findOptionByPath(String path) {
        for (GuiOption opt : REANIMATION_OPTIONS.values()) {
            if (REANIMATION_OPTIONS.entrySet().stream()
                    .anyMatch(e -> e.getKey().equals(path) && e.getValue() == opt)) {
                return opt;
            }
        }
        for (GuiOption opt : KNOCKOUT_OPTIONS.values()) {
            if (KNOCKOUT_OPTIONS.entrySet().stream()
                    .anyMatch(e -> e.getKey().equals(path) && e.getValue() == opt)) {
                return opt;
            }
        }
        for (GuiOption opt : GAMEPLAY_OPTIONS.values()) {
            if (GAMEPLAY_OPTIONS.entrySet().stream()
                    .anyMatch(e -> e.getKey().equals(path) && e.getValue() == opt)) {
                return opt;
            }
        }
        for (GuiOption opt : INTERFACE_OPTIONS.values()) {
            if (INTERFACE_OPTIONS.entrySet().stream()
                    .anyMatch(e -> e.getKey().equals(path) && e.getValue() == opt)) {
                return opt;
            }
        }
        for (GuiOption opt : EFFECTS_OPTIONS.values()) {
            if (EFFECTS_OPTIONS.entrySet().stream()
                    .anyMatch(e -> e.getKey().equals(path) && e.getValue() == opt)) {
                return opt;
            }
        }
        return null;
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

    private void handleOptionClick(Inventory inv, Player player, int slot, boolean shiftClick) {
        String configPath = null;
        GuiOption option = null;

        // Chercher dans toutes les catégories
        for (Map.Entry<String, GuiOption> entry : REANIMATION_OPTIONS.entrySet()) {
            if (entry.getValue().slot == slot) {
                configPath = entry.getKey();
                option = entry.getValue();
                break;
            }
        }
        if (configPath == null) {
            for (Map.Entry<String, GuiOption> entry : KNOCKOUT_OPTIONS.entrySet()) {
                if (entry.getValue().slot == slot) {
                    configPath = entry.getKey();
                    option = entry.getValue();
                    break;
                }
            }
        }
        if (configPath == null) {
            for (Map.Entry<String, GuiOption> entry : GAMEPLAY_OPTIONS.entrySet()) {
                if (entry.getValue().slot == slot) {
                    configPath = entry.getKey();
                    option = entry.getValue();
                    break;
                }
            }
        }
        if (configPath == null) {
            for (Map.Entry<String, GuiOption> entry : INTERFACE_OPTIONS.entrySet()) {
                if (entry.getValue().slot == slot) {
                    configPath = entry.getKey();
                    option = entry.getValue();
                    break;
                }
            }
        }
        if (configPath == null) {
            for (Map.Entry<String, GuiOption> entry : EFFECTS_OPTIONS.entrySet()) {
                if (entry.getValue().slot == slot) {
                    configPath = entry.getKey();
                    option = entry.getValue();
                    break;
                }
            }
        }

        if (configPath == null || option == null) return;

        switch (option.type) {
            case BOOLEAN:
                handleBooleanToggle(inv, player, configPath, option);
                break;
            case INTEGER:
                if (shiftClick) {
                    handleIntegerIncrement(inv, player, configPath, option);
                } else {
                    handleIntegerEdit(player, configPath, option);
                }
                break;
            case MATERIAL:
                handleMaterialEdit(player, configPath, option);
                break;
        }
    }

    private void handleBooleanToggle(Inventory inv, Player player, String configPath, GuiOption option) {
        boolean current = cfg.getBoolean(configPath, false);
        boolean next = !current;
        cfg.set(configPath, next);

        // Mettre à jour l'affichage
        ItemStack item = createOptionItem(configPath, option);
        inv.setItem(option.slot, item);

        String optionName = ReanimateMC.lang.get(option.langKey);
        String stateMsg = next ? ReanimateMC.lang.get("toggle_on") : ReanimateMC.lang.get("toggle_off");
        String msg = ReanimateMC.lang.get("message_gui_toggle",
                "option", optionName,
                "state", ChatColor.stripColor(stateMsg)
        );
        player.sendMessage(ChatColor.GRAY + msg);
    }

    private void handleIntegerIncrement(Inventory inv, Player player, String configPath, GuiOption option) {
        int current = cfg.getInt(configPath, 0);
        int next = current + 1;
        cfg.set(configPath, next);

        // Mettre à jour l'affichage
        ItemStack item = createOptionItem(configPath, option);
        inv.setItem(option.slot, item);

        player.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("value_incremented",
                "option", ReanimateMC.lang.get(option.langKey),
                "value", String.valueOf(next)));
    }

    private void handleIntegerEdit(Player player, String configPath, GuiOption option) {
        editingPlayers.put(player, configPath);
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("enter_new_value",
                "option", ReanimateMC.lang.get(option.langKey),
                "current", String.valueOf(cfg.getInt(configPath, 0))));
        player.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("type_cancel_to_abort"));
    }

    private void handleMaterialEdit(Player player, String configPath, GuiOption option) {
        editingPlayers.put(player, configPath);
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("enter_new_material",
                "option", ReanimateMC.lang.get(option.langKey),
                "current", cfg.getString(configPath, "STONE")));
        player.sendMessage(ChatColor.GRAY + ReanimateMC.lang.get("type_cancel_to_abort"));
    }

    private void resetAllOptions() {
        // Valeurs par défaut
        cfg.set("reanimation.require_special_item", true);
        cfg.set("reanimation.required_item", "GOLDEN_APPLE");
        cfg.set("reanimation.duration_ticks", 100);
        cfg.set("reanimation.health_restored", 4);
        cfg.set("reanimation.cooldown", 60);
        cfg.set("reanimation.revive_cooldown", 60);
        cfg.set("execution.enabled", true);
        cfg.set("execution.hold_duration_ticks", 40);
        cfg.set("execution.message_broadcast", true);

        cfg.set("knockout.enabled", true);
        cfg.set("knockout.duration_seconds", 30);
        cfg.set("knockout.movement_disabled", true);
        cfg.set("knockout.use_particles", true);
        cfg.set("knockout.heartbeat_sound", true);
        cfg.set("knockout.blindness", true);
        cfg.set("knockout.suicide_hold_seconds", 3);
        cfg.set("knockout.weakness_level", 1);
        cfg.set("knockout.fatigue_level", 1);

        cfg.set("prone.enabled", true);
        cfg.set("prone.allow_crawl", true);
        cfg.set("prone.crawl_slowness_level", 5);
        cfg.set("prone.auto_crawl", false);
        cfg.set("looting.enabled", true);
        cfg.set("tablist.enabled", true);

        cfg.set("effects_on_revive.nausea", 5);
        cfg.set("effects_on_revive.slowness", 10);
        cfg.set("effects_on_revive.resistance", 10);

        plugin.saveConfig();
    }
}