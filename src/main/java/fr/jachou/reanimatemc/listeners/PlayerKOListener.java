package fr.jachou.reanimatemc.listeners;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.KOData;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerKOListener implements Listener {

    private final KOManager koManager;

    // Minimum ms between throttled events (cancel message / distress signal).
    private static final long CANCEL_THROTTLE_MS = 3000L;
    private final Map<UUID, Long> lastCancelTime = new HashMap<>();

    public PlayerKOListener(KOManager koManager) {
        this.koManager = koManager;
    }

    // ── Crawl state persistence ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleSwim(EntityToggleSwimEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        Player player = (Player) entity;
        if (!koManager.isKO(player)) return;
        KOData data = koManager.getKOData(player);
        if (!data.isCrawling()) return;
        if (!event.isSwimming()) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(ReanimateMC.getInstance(), () -> {
                if (koManager.isKO(player) && data.isCrawling()) {
                    player.setSwimming(true);
                }
            }, 1L);
        }
    }

    // ── Surrender: sneak-down starts, sneak-up cancels ───────────────────────
    //
    // Crawl mode: the swim state generates repeated isSneaking=true events.
    // Guard: if a surrender task is already running, ignore additional sneak-down
    // events so the timer is not restarted. Sneak-up always cancels.

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!koManager.isKO(player)) return;
        event.setCancelled(true);
        KOData data = koManager.getKOData(player);

        if (event.isSneaking()) {
            // Ignore repeated sneak-down if timer is already running (crawl spam fix)
            if (data.getSuicideTaskId() != -1) return;

            long seconds = ReanimateMC.getInstance().getConfig()
                    .getLong("knockout.suicide_hold_seconds", 3);
            int task = Bukkit.getScheduler().scheduleSyncDelayedTask(
                    ReanimateMC.getInstance(),
                    () -> { if (koManager.isKO(player)) koManager.suicide(player); },
                    seconds * 20L
            );
            data.setSuicideTaskId(task);
            player.sendMessage(ReanimateMC.lang.get("suicide_start", "time", String.valueOf(seconds)));

        } else {
            // Sneak-up: explicit resistance (Java Edition reliable path)
            cancelSurrender(player, data, true);
        }
    }

    // ── Surrender cancel: Q / drop key (Bedrock primary cancel) ──────────────
    //
    // On Bedrock, when seated on the ArmorStand mount (non-crawl KO), the client
    // stays visually crouched and never fires a sneak-up event. The drop button
    // (Q) is always accessible and consistently fires PlayerDropItemEvent via
    // Geyser. We cancel the item drop and use it as the cancel trigger.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!koManager.isKO(player)) return;
        KOData data = koManager.getKOData(player);

        // Always cancel the drop while KO
        event.setCancelled(true);

        // If a surrender is active, cancel it (resist)
        if (data.getSuicideTaskId() != -1) {
            cancelSurrender(player, data, true);
        } else {
            // No active surrender — send distress if drop_key_trigger enabled
            if (ReanimateMC.getInstance().getConfig()
                    .getBoolean("knockout.distress.drop_key_trigger", true)) {
                koManager.sendDistress(player);
            }
        }
    }

    // ── Surrender cancel: RIGHT_CLICK_AIR with empty hand (Bedrock secondary) ─

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player player = event.getPlayer();
        if (!koManager.isKO(player)) return;
        KOData data = koManager.getKOData(player);
        if (data.getSuicideTaskId() == -1) return;
        if (player.getInventory().getItemInMainHand().getType() != org.bukkit.Material.AIR) return;
        event.setCancelled(true);
        cancelSurrender(player, data, true);
    }

    // ── Surrender cancel: arm-swing (Bedrock tertiary fallback) ──────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        if (!koManager.isKO(player)) return;
        KOData data = koManager.getKOData(player);
        if (data.getSuicideTaskId() == -1) return;
        // No distress on arm-swing — too easy to trigger accidentally
        cancelSurrender(player, data, false);
    }

    // ── Distress: F key (Java Edition) ───────────────────────────────────────

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (!koManager.isKO(event.getPlayer())) return;
        event.setCancelled(true);
        koManager.sendDistress(event.getPlayer());
    }

    // ── Movement: cancel surrender on move ───────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!koManager.isKO(player)) return;
        KOData data = koManager.getKOData(player);

        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to   = event.getTo();
        if (to == null) return;

        boolean movedH = Math.abs(to.getX() - from.getX()) > 0.001
                || Math.abs(to.getZ() - from.getZ()) > 0.001;
        boolean movedY = to.getY() > from.getY() + 0.01;

        boolean movementDisabled = ReanimateMC.getInstance().getConfig()
                .getBoolean("knockout.movement_disabled", true);
        boolean crawling = data.isCrawling()
                && ReanimateMC.getInstance().getConfig().getBoolean("prone.allow_crawl", false);
        boolean surrenderRequireStill = ReanimateMC.getInstance().getConfig()
                .getBoolean("knockout.surrender_require_still", true);
        boolean crawlAllowJump = ReanimateMC.getInstance().getConfig()
                .getBoolean("prone.crawl_allow_jump", false);

        if (!crawling && movementDisabled) {
            // Full lock mode — block XZ and Y, preserve rotation
            if (movedH || movedY) {
                org.bukkit.Location locked = from.clone();
                locked.setYaw(to.getYaw());
                locked.setPitch(to.getPitch());
                event.setTo(locked);
            }
            // Even though movement is blocked server-side, if surrender_require_still
            // is true we still cancel the surrender when any move attempt is detected
            if ((movedH || movedY) && surrenderRequireStill) {
                if (data.getSuicideTaskId() != -1) cancelSurrender(player, data, false);
                if (koManager.isChannelingSelfRevive(player)
                        && ReanimateMC.getInstance().getConfig()
                                .getBoolean("self_revive.cancel_on_move", true)) {
                    koManager.cancelSelfRevive(player, true);
                }
            }

        } else if (!crawling && !movementDisabled) {
            // Movement allowed — but surrender still cancelled on move if configured
            if ((movedH || movedY) && surrenderRequireStill) {
                if (data.getSuicideTaskId() != -1) cancelSurrender(player, data, false);
            }
            if (movedH && koManager.isChannelingSelfRevive(player)
                    && ReanimateMC.getInstance().getConfig()
                            .getBoolean("self_revive.cancel_on_move", true)) {
                koManager.cancelSelfRevive(player, true);
            }

        } else {
            // Crawl mode — allow XZ; optionally block jump
            if (movedY && !crawlAllowJump && player.getVelocity().getY() > 0.1) {
                org.bukkit.Location noJump = to.clone();
                noJump.setY(from.getY());
                event.setTo(noJump);
            }
            // Cancel surrender on movement while crawling if configured
            if (movedH && surrenderRequireStill) {
                if (data.getSuicideTaskId() != -1) cancelSurrender(player, data, false);
            }
            if (movedH && koManager.isChannelingSelfRevive(player)
                    && ReanimateMC.getInstance().getConfig()
                            .getBoolean("self_revive.cancel_on_move", true)) {
                koManager.cancelSelfRevive(player, true);
            }
            }
        }
    }

    // ── Command whitelist ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        boolean restricted = ReanimateMC.getInstance().getConfig()
                .getBoolean("knockout.enable_commands_allowed");
        if (!koManager.isKO(player) || player.isOp() || !restricted) return;

        List<String> allowed = ReanimateMC.getInstance().getConfig()
                .getStringList("knockout.allowed_commands");
        String msg   = event.getMessage();
        String cmd   = msg.startsWith("/") ? msg.substring(1) : msg;
        String label = cmd.split(" ")[0].toLowerCase();
        String base  = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;

        boolean isAllowed = allowed.stream()
                .map(String::toLowerCase)
                .anyMatch(a -> a.equals(label) || a.equals(base));

        if (!isAllowed) {
            event.setCancelled(true);
            player.sendMessage(ReanimateMC.lang.get("no_permission_ko"));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Cancels the active surrender task.
     * The cancel message is always sent (never throttled) so the player always
     * knows the surrender was cancelled.
     * The distress signal is throttled to once per CANCEL_THROTTLE_MS to avoid
     * spam when multiple cancel triggers fire in quick succession.
     *
     * @param sendDistress true when the player explicitly resisted
     *                     (sneak-up, Q, right-click). False for passive cancels.
     */
    private void cancelSurrender(Player player, KOData data, boolean sendDistress) {
        if (data.getSuicideTaskId() == -1) return;
        Bukkit.getScheduler().cancelTask(data.getSuicideTaskId());
        data.setSuicideTaskId(-1);

        // Message is always sent so the player knows the surrender was cancelled
        player.sendMessage(ReanimateMC.lang.get("ko_shift_click_cancelled"));

        if (sendDistress) {
            // Throttle only the distress signal, not the message
            long now = System.currentTimeMillis();
            Long last = lastCancelTime.get(player.getUniqueId());
            if (last == null || (now - last) >= CANCEL_THROTTLE_MS) {
                lastCancelTime.put(player.getUniqueId(), now);
                koManager.sendDistress(player);
            }
        }
    }
}
