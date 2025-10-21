package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class GolemManager implements Listener {

    @EventHandler
    public void onRightClickGolem(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        Entity e = event.getRightClicked();

        if (!p.isSneaking()) return;
        if (!(e instanceof org.bukkit.entity.IronGolem)) return;

        if (!ReanimateMC.getInstance().getConfig().getBoolean("golem.enabled")) return;

        String itemName = ReanimateMC.getInstance().getConfig().getString("item_to_summon_golem", "GOLD_INGOT");
        Material mat = Material.matchMaterial(itemName);
        if (mat == null) return;

        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() != mat) return;

        e.remove();
        ReanimatorNPC.ReanimatorType type = getReanimatorType();

        if (ReanimateMC.getInstance().getNpcSummonManager() != null) {
            ReanimateMC.getInstance().getNpcSummonManager().summon(p, type, p);
        } else {
            p.sendMessage("§cErreur : npcSummonManager non initialisé !");
        }

        inHand.setAmount(inHand.getAmount() - 1);
        p.getInventory().setItemInMainHand(inHand);
        p.updateInventory();
    }

    private static ReanimatorNPC.@NotNull ReanimatorType getReanimatorType() {
        Random random = new Random();
        ReanimatorNPC.ReanimatorType type;

        int level = random.nextInt(3) + 1;

        if (level == 1) {
            type = ReanimatorNPC.ReanimatorType.HEALER;
        } else if (level == 2) {
            type = ReanimatorNPC.ReanimatorType.GOLEM;
        } else {
            type = ReanimatorNPC.ReanimatorType.PROTECTOR;
        }
        return type;
    }

}