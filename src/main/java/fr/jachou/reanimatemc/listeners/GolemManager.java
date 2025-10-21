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

import java.util.Random;

public class GolemManager implements Listener {

    @EventHandler
    public void onRightClickGolem(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        Entity e = event.getRightClicked();


        // Le joueur doit être accroupi
        if (!p.isSneaking()) {
            return;
        }

        // Vérifie que la fonctionnalité golem est activée
        if (!ReanimateMC.getInstance().getConfig().getBoolean("npc_summon.enabled", false)) {
            return;
        }

        // Vérifie que l'entité est bien un golem de fer
        if (!(e instanceof IronGolem)) {
            return;
        }

        // Récupère le matériau requis depuis la configuration
        String itemName = ReanimateMC.getInstance().getConfig().getString("npc_summon.item_to_summon_golem", "GOLD_INGOT");

        Material requiredMaterial = Material.matchMaterial(itemName);

        if (requiredMaterial == null) {
            p.sendMessage(ChatColor.RED + "Item for summoning golem is not defined correctly.");
            return;
        }

        ItemStack inHand = p.getInventory().getItemInMainHand();

        if (inHand == null || inHand.getType() != requiredMaterial) {
            return; // Le joueur n'a pas le bon item
        }

        if (ReanimateMC.getInstance().getNpcSummonManager().getPlayerGolems(p) >= ReanimateMC.getInstance().getConfig().getInt("npc_summon.max_summons_per_player")) {
            return;
        }

        // Supprime l'entité golem actuelle
        e.remove();

        // Choisit un type de réanimateur aléatoire
        ReanimatorNPC.ReanimatorType type = getReanimatorType();

        // Vérifie et invoque le PNJ réanimateur
        if (ReanimateMC.getInstance().getNpcSummonManager() != null) {
            ReanimateMC.getInstance().getNpcSummonManager().summon(p, type, p);
        } else {
            p.sendMessage(ChatColor.RED + "Summon manager is not initialized.");
        }

        // Consomme un item de la main et met à jour l'inventaire
        int newAmount = inHand.getAmount() - 1;

        if (newAmount <= 0) {
            p.getInventory().setItemInMainHand(null);
        } else {
            inHand.setAmount(newAmount);
            p.getInventory().setItemInMainHand(inHand);
        }
        p.updateInventory();
    }

    private static ReanimatorNPC.ReanimatorType getReanimatorType() {
        Random random = new Random();
        int level = random.nextInt(3) + 1;

        if (level == 1) {
            return ReanimatorNPC.ReanimatorType.HEALER;
        } else if (level == 2) {
            return ReanimatorNPC.ReanimatorType.GOLEM;
        } else {
            return ReanimatorNPC.ReanimatorType.PROTECTOR;
        }
    }
}