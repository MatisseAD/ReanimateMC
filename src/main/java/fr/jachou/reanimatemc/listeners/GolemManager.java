package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class GolemManager implements Listener {

    @EventHandler
    public void onRightClickGolem(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        Entity e = event.getRightClicked();

        if (!p.isSneaking()) {
            return;
        }

        if (!ReanimateMC.getInstance().getConfig().getBoolean("npc_summon.enabled", false)) {
            return;
        }

        if (!(e instanceof IronGolem)) {
            return;
        }

        String itemName = ReanimateMC.getInstance().getConfig().getString("npc_summon.item_to_summon_golem", "GOLD_INGOT");
        Material requiredMaterial = Material.matchMaterial(itemName);

        if (requiredMaterial == null) {
            p.sendMessage(ChatColor.RED + "Item for summoning golem is not defined correctly.");
            return;
        }

        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (inHand.getType() != requiredMaterial) {
            return;
        }

        int maxSummons = ReanimateMC.getInstance().getConfig().getInt("npc_summon.max_summons_per_player", 1);
        if (ReanimateMC.getInstance().getNpcSummonManager().getPlayerGolems(p) >= maxSummons) {
            return;
        }

        // Pick the highest-tier type the player has permission for
        ReanimatorNPC.ReanimatorType type = resolveAllowedType(p);
        if (type == null) {
            p.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
            return;
        }

        e.remove();

        if (ReanimateMC.getInstance().getNpcSummonManager() != null) {
            ReanimateMC.getInstance().getNpcSummonManager().summon(p, type, null);
        } else {
            p.sendMessage(ChatColor.RED + "Summon manager is not initialized.");
            return;
        }

        int newAmount = inHand.getAmount() - 1;
        if (newAmount <= 0) {
            p.getInventory().setItemInMainHand(null);
        } else {
            inHand.setAmount(newAmount);
            p.getInventory().setItemInMainHand(inHand);
        }
        p.updateInventory();
    }

    /**
     * Returns the highest-tier ReanimatorType the player has permission for,
     * or null if they have no type permission at all.
     */
    private ReanimatorNPC.ReanimatorType resolveAllowedType(Player player) {
        if (player.hasPermission("reanimatemc.summon.use.protector")) {
            return ReanimatorNPC.ReanimatorType.PROTECTOR;
        }
        if (player.hasPermission("reanimatemc.summon.use.healer")) {
            return ReanimatorNPC.ReanimatorType.HEALER;
        }
        if (player.hasPermission("reanimatemc.summon.use.golem")) {
            return ReanimatorNPC.ReanimatorType.GOLEM;
        }
        return null;
    }
}
