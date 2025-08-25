package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.managers.KOManager;
import fr.jachou.reanimatemc.managers.LootManager;
import fr.jachou.reanimatemc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ReanimationListener implements Listener {
    private KOManager koManager;
    private final Map<UUID, BukkitTask> activeReviveTasks = new HashMap<>();
    private final Map<UUID, Long> lastInteract = new HashMap<>();

    public ReanimationListener(KOManager koManager) {
        this.koManager = koManager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player target = (Player) event.getRightClicked();
        Player reviver = event.getPlayer();

        long now = System.currentTimeMillis();
        Long last = lastInteract.get(reviver.getUniqueId());
        if (last != null && now - last < 5) {
            return;
        }
        lastInteract.put(reviver.getUniqueId(), now);

        // Ignore NPCs
        if (Utils.isNPC(target) || Utils.isNPC(reviver)) return;

        // Only interact with KO players
        if (!koManager.isKO(target)) return;

        // Must be sneaking
        if (!reviver.isSneaking()) {
            reviver.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_sneaking"));
            return;
        }

        // Check if reviver is already running a revive task
        if (activeReviveTasks.containsKey(reviver.getUniqueId())) {
            reviver.sendMessage(ChatColor.YELLOW + "You are already reviving someone!");
            return;
        }

        // Check required item (if configured)
        boolean requireSpecial = ReanimateMC.getInstance().getConfig()
                .getBoolean("reanimation.require_special_item", true);
        String requiredItemName = ReanimateMC.getInstance().getConfig()
                .getString("reanimation.required_item", "GOLDEN_APPLE");
        ItemStack inHand = reviver.getInventory().getItemInMainHand();

        if (requireSpecial) {
            if (inHand == null || !inHand.getType().toString().equalsIgnoreCase(requiredItemName)) {
                reviver.sendMessage(ChatColor.RED +
                        ReanimateMC.lang.get("special_item_required", "item", requiredItemName));
                return;
            }
        }

        // All preliminary checks passed. Begin the holding process.
        int durationTicks = ReanimateMC.getInstance().getConfig()
                .getInt("reanimation.duration_ticks", 100);
        // Capture the stack in hand now; we will consume one at the end if successful
        ItemStack requiredStack = requireSpecial ? inHand.clone() : null;

        // We also need to remember the reviver and target for each tick
        StartReviveTask task = new StartReviveTask(reviver, target, requiredItemName, requiredStack,
                durationTicks);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(
                ReanimateMC.getInstance(),
                task,
                0L,
                1L
        );
        task.setTaskReference(bukkitTask);

        // Store so we can cancel if needed
        activeReviveTasks.put(reviver.getUniqueId(), bukkitTask);
        reviver.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("revive_start"));
    }

    /**
     * Inner class representing the repeating task that checks each tick whether the reviver
     * is still valid (sneaking, holding item, still target in sight, target still KO).
     * If duration fully passes, completes the revival; else cancels on any failure.
     */
    private class StartReviveTask implements Runnable {
        private final Player reviver;
        private final Player target;
        private final String requiredItemName;
        private final ItemStack requiredStack; // clone of original stack to compare type
        private final int totalTicks;
        private int ticksElapsed = 0;
        private BukkitTask taskRef;

        StartReviveTask(Player reviver, Player target, String requiredItemName, ItemStack requiredStack, int totalTicks) {
            this.reviver = reviver;
            this.target = target;
            this.requiredItemName = requiredItemName;
            this.requiredStack = requiredStack;
            this.totalTicks = totalTicks;
        }

        void setTaskReference(BukkitTask ref) {
            this.taskRef = ref;
        }

        @Override
        public void run() {
            if (!reviver.isOnline() || !target.isOnline()) {
                cancelRevive("Revival canceled: player logged off.");
                return;
            }
            if (!koManager.isKO(target)) {
                cancelRevive("Revival canceled: target is no longer KO.");
                return;
            }
            if (!reviver.isSneaking()) {
                cancelRevive("Revival canceled: you stopped sneaking.");
                return;
            }
            if (requiredStack != null) {
                ItemStack current = reviver.getInventory().getItemInMainHand();
                if (current == null || !current.getType().toString().equalsIgnoreCase(requiredItemName)) {
                    cancelRevive("Revival canceled: you no longer hold the required item.");
                    return;
                }
            }

            RayTraceResult ray = reviver.getWorld().rayTraceEntities(
                    reviver.getEyeLocation(),
                    reviver.getEyeLocation().getDirection(),
                    4.0,
                    entity -> entity instanceof Player && entity.equals(target)
            );
            if (ray == null || !(ray.getHitEntity() instanceof Player)
                    || !((Player) ray.getHitEntity()).getUniqueId().equals(target.getUniqueId())) {
                cancelRevive("Revival canceled: you looked away from the target.");
                return;
            }

            ticksElapsed++;
            int percent = (int) (((double) ticksElapsed / totalTicks) * 100);
            Utils.sendActionBar(reviver,
                    ChatColor.YELLOW + "Reviving... " + percent + "%");

            if (ticksElapsed >= totalTicks) {
                if (requiredStack != null) {
                    ItemStack inHandNow = reviver.getInventory().getItemInMainHand();
                    if (inHandNow != null && inHandNow.getType().toString().equalsIgnoreCase(requiredItemName)) {
                        int newAmount = inHandNow.getAmount() - 1;
                        if (newAmount <= 0) {
                            reviver.getInventory().setItemInMainHand(null);
                        } else {
                            inHandNow.setAmount(newAmount);
                            reviver.getInventory().setItemInMainHand(inHandNow);
                        }
                    }
                }
                koManager.revive(target, reviver);
                target.sendMessage(ChatColor.GREEN +
                        ReanimateMC.lang.get("revived_by", "player", reviver.getName()));
                reviver.sendMessage(ChatColor.GREEN +
                        ReanimateMC.lang.get("revived_confirmation", "player", target.getName()));

                taskRef.cancel();
                activeReviveTasks.remove(reviver.getUniqueId());
            }
        }

        private void cancelRevive(String message) {
            if (reviver.isOnline()) {
                reviver.sendMessage(ChatColor.RED + message);
            }
            taskRef.cancel();
            activeReviveTasks.remove(reviver.getUniqueId());
        }
    }
}