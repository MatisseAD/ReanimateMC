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

    // Map each config‐path → pair(displayKey, slotIndex)
    private final static Map<String, GuiOption> OPTIONS = new LinkedHashMap<>();
    static {
        // order matters: fill in order of appearance in the GUI
        OPTIONS.put("reanimation.require_special_item",
                new GuiOption("option_reanimation_require", 0));
        OPTIONS.put("knockout.enabled",
                new GuiOption("option_knockout_enabled", 1));
        OPTIONS.put("knockout.movement_disabled",
                new GuiOption("option_knockout_move", 2));
        OPTIONS.put("knockout.use_particles",
                new GuiOption("option_knockout_particles", 3));
        OPTIONS.put("knockout.heartbeat_sound",
                new GuiOption("option_knockout_sound", 4));
        OPTIONS.put("execution.enabled",
                new GuiOption("option_execution_enabled", 5));
        OPTIONS.put("execution.message_broadcast",
                new GuiOption("option_execution_broadcast", 6));
        OPTIONS.put("prone.enabled",
                new GuiOption("option_prone_enabled", 7));
        OPTIONS.put("prone.allow_crawl",
                new GuiOption("option_prone_crawl", 8));
        OPTIONS.put("looting.enabled",
                new GuiOption("option_looting_enabled", 9));
    }

    // Represents one toggleable boolean option in the GUI
    private static class GuiOption {
        final String langKey;  // e.g. "option_reanimation_require"
        final int slot;        // where in the inventory to place this icon

        GuiOption(String langKey, int slot) {
            this.langKey = langKey;
            this.slot = slot;
        }
    }

    public ConfigGUI(ReanimateMC plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    /** Open the configuration GUI for the given player. */
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9*2,
                ChatColor.translateAlternateColorCodes('&',
                        ReanimateMC.lang.get("gui_title"))
        );

        // Populate each option as a colored wool block
        for (Map.Entry<String, GuiOption> entry : OPTIONS.entrySet()) {
            String path = entry.getKey();
            GuiOption opt = entry.getValue();

            boolean enabled = cfg.getBoolean(path, false);
            Material mat = enabled ? Material.GREEN_WOOL : Material.RED_WOOL;
            ItemStack wool = new ItemStack(mat);
            ItemMeta meta = wool.getItemMeta();

            // Display name: e.g. "Require Special Item" in correct language
            String name = ReanimateMC.lang.get(opt.langKey);
            meta.setDisplayName(ChatColor.YELLOW + name);
            meta.setLore(Arrays.asList(
                    enabled
                            ? ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_on"))
                            : ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_off"))
            ));
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ENCHANTS);
            wool.setItemMeta(meta);

            inv.setItem(opt.slot, wool);
        }

        player.openInventory(inv);
    }

    /** Handle inventory clicks inside our custom GUI. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;

        // Check that the clicked inventory is our GUI (by title)
        String title = ChatColor.translateAlternateColorCodes('&',
                ReanimateMC.lang.get("gui_title")
        );
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true); // prevent taking items

        // Determine which slot was clicked
        int slot = event.getSlot();
        String configPath = null;
        String langKey = null;

        for (Map.Entry<String, GuiOption> entry : OPTIONS.entrySet()) {
            if (entry.getValue().slot == slot) {
                configPath = entry.getKey();
                langKey = entry.getValue().langKey;
                break;
            }
        }
        if (configPath == null) return;  // not a toggle‐slot

        // Toggle the boolean in the configuration
        boolean current = cfg.getBoolean(configPath, false);
        boolean next = !current;
        cfg.set(configPath, next);
        plugin.saveConfig();

        // Update the item in the GUI to reflect new state
        Material mat = next ? Material.GREEN_WOOL : Material.RED_WOOL;
        ItemStack wool = new ItemStack(mat);
        ItemMeta meta = wool.getItemMeta();
        String displayName = ReanimateMC.lang.get(langKey);
        meta.setDisplayName(ChatColor.YELLOW + displayName);
        meta.setLore(Arrays.asList(
                next
                        ? ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_on"))
                        : ChatColor.translateAlternateColorCodes('&', ReanimateMC.lang.get("toggle_off"))
        ));
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ENCHANTS);
        wool.setItemMeta(meta);

        inv.setItem(slot, wool);

        // Send confirmation message
        String optionName = ReanimateMC.lang.get(langKey);
        String stateMsg = next
                ? ReanimateMC.lang.get("toggle_on")
                : ReanimateMC.lang.get("toggle_off");
        String msg = ReanimateMC.lang.get("message_gui_toggle",
                "option", optionName,
                "state", ChatColor.stripColor(stateMsg)
        );
        player.sendMessage(ChatColor.GRAY + msg);
    }
}
