package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class TotemListener implements Listener {
    private final KOManager koManager;
    public TotemListener(KOManager koManager) {
        this.koManager = koManager;
    }

    @EventHandler
    public void onUseTotem(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!koManager.isKO(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) {
            return;
        }

        event.setCancelled(true);
        // Consume one totem from the hand used
        if (event.getHand() == EquipmentSlot.HAND) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getAmount() <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack hand = player.getInventory().getItemInOffHand();
            if (hand.getAmount() <= 1) {
                player.getInventory().setItemInOffHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }
        }

        koManager.revive(player, player);
    }
}
