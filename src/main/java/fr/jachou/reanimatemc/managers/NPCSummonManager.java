/* LICENSE BEGIN
    * This file is part of ReanimateMC.
    * ReanimateMC is under the proprietary of Frouzie.
    * You are not allowed to redistribute it and/or modify it.
LICENSE END
 */

package fr.jachou.reanimatemc.managers;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.api.NPCDismissedEvent;
import fr.jachou.reanimatemc.api.NPCSummonedEvent;
import fr.jachou.reanimatemc.behavior.GolemBehavior;
import fr.jachou.reanimatemc.behavior.HealerBehavior;
import fr.jachou.reanimatemc.behavior.NPCBehavior;
import fr.jachou.reanimatemc.behavior.ProtectorBehavior;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import fr.jachou.reanimatemc.data.ReanimatorNPC.ReanimatorType;
import fr.jachou.reanimatemc.hooks.VaultHook;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages summoning, behavior, nameplate, lifetime, events, and cleanup of NPC reanimators.
 *
 * <p>Type-specific logic is fully delegated to {@link NPCBehavior} implementations.
 * This class handles orchestration only: scheduling, routing, cooldowns, persistence,
 * stuck detection, damage intercept, and the public API consumed by commands and listeners.
 */
public class NPCSummonManager {

    private static final double DEFAULT_FOLLOW_START = 10.0;
    private static final double DEFAULT_FOLLOW_STOP  = 3.0;
    private static final double DEFAULT_REVIVE_DIST  = 5.0;

    private final JavaPlugin plugin;
    private final KOManager koManager;
    private final VaultHook vault;
    private final NPCPersistenceManager persistence;

    private final Map<UUID, ReanimatorNPC>             activeNPCs          = new HashMap<>();
    private final Map<UUID, List<UUID>>                playerSummons       = new HashMap<>();
    private final Map<UUID, Map<ReanimatorType, Long>> typeCooldowns       = new HashMap<>();
    private final Map<UUID, Set<UUID>>                 interceptedThisTick = new HashMap<>();
    /** Entity UUID → NPC — O(1) lookup for damage/death listeners. */
    private final Map<UUID, ReanimatorNPC>             entityIndex         = new HashMap<>();
    /** Per-type strategy implementations. */
    private final Map<ReanimatorType, NPCBehavior>     behaviors           = new EnumMap<>(ReanimatorType.class);
    /** NPC id → target being actively revived (progress in flight). */
    private final Map<UUID, UUID>    reviveInProgress = new HashMap<>();
    /** NPC id → BukkitTask id for the running revive progress runnable. */
    private final Map<UUID, Integer> reviveTasks      = new HashMap<>();

    public NPCSummonManager(JavaPlugin plugin, KOManager koManager, VaultHook vault) {
        this.plugin      = plugin;
        this.koManager   = koManager;
        this.vault       = vault;
        this.persistence = new NPCPersistenceManager(plugin);

        behaviors.put(ReanimatorType.GOLEM,     new GolemBehavior());
        behaviors.put(ReanimatorType.HEALER,    new HealerBehavior(koManager));
        behaviors.put(ReanimatorType.PROTECTOR, new ProtectorBehavior());

        startBehaviorTask();
        startNameplateTask();
        startInterceptResetTask();
        restoreFromDisk();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean summon(Player summoner, ReanimatorType type, Player targetPlayer) {
        if (!plugin.getConfig().getBoolean("npc_summon.enabled", false)) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_summon_disabled"));
            return false;
        }
        if (!summoner.hasPermission("reanimatemc.summon") && !summoner.isOp()) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
            return false;
        }
        if (!summoner.hasPermission("reanimatemc.summon.use." + type.name().toLowerCase()) && !summoner.isOp()) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_no_type_permission",
                    "type", type.getDisplayName()));
            return false;
        }

        boolean bypass = summoner.hasPermission("reanimatemc.summon.overridecost") || summoner.isOp();

        if (!bypass) {
            Long cooldownEnd = typeCooldowns
                    .getOrDefault(summoner.getUniqueId(), Collections.emptyMap())
                    .get(type);
            if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
                long rem = (cooldownEnd - System.currentTimeMillis()) / 1000L;
                summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_summon_cooldown",
                        "time", String.valueOf(rem)));
                return false;
            }
        }

        int maxSummons = plugin.getConfig().getInt("npc_summon.max_summons_per_player", 1);
        List<UUID> owned = playerSummons.computeIfAbsent(summoner.getUniqueId(), k -> new ArrayList<>());
        if (!bypass && owned.size() >= maxSummons) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_max_summons",
                    "max", String.valueOf(maxSummons)));
            return false;
        }

        // If a target is specified, the NPC belongs to the target, not the summoner.
        // The summoner is just the caster — they pay the cost and cooldown.
        Player owner = (targetPlayer != null) ? targetPlayer : summoner;

        long lifetime = resolveLifetime(summoner, type);
        Location spawnLoc = owner.getLocation().add(2, 0, 0);
        Entity entity = spawnEntity(spawnLoc, type);
        if (entity == null) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_summon_failed"));
            return false;
        }

        ReanimatorNPC npc = new ReanimatorNPC(owner.getUniqueId(), owner.getName(),
                entity, type, lifetime);

        NPCSummonedEvent event = new NPCSummonedEvent(summoner, npc);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            entity.remove();
            return false;
        }

        activeNPCs.put(npc.getId(), npc);
        entityIndex.put(entity.getUniqueId(), npc);
        owned.add(npc.getId());

        if (!bypass) {
            long cooldownSec = (long) cfg(type.name().toLowerCase() + ".summon_cooldown", 300.0);
            typeCooldowns
                    .computeIfAbsent(summoner.getUniqueId(), k -> new EnumMap<>(ReanimatorType.class))
                    .put(type, System.currentTimeMillis() + cooldownSec * 1000L);
        }

        playSpawnEffects(spawnLoc, type);
        String lifetimeMsg = lifetime > 0
                ? ReanimateMC.lang.get("npc_lifetime_remaining", "time", String.valueOf(lifetime))
                : "";
        summoner.sendMessage(ChatColor.GREEN
                + ReanimateMC.lang.get("npc_summoned", "type", type.getDisplayName())
                + (lifetimeMsg.isEmpty() ? "" : " " + lifetimeMsg));
        // If summoner summoned for someone else, notify the recipient
        if (targetPlayer != null && !targetPlayer.getUniqueId().equals(summoner.getUniqueId())) {
            targetPlayer.sendMessage(ChatColor.GREEN
                    + ReanimateMC.lang.get("npc_summoned_for_you",
                            "type", type.getDisplayName(),
                            "player", summoner.getName()));
        }
        return true;
    }

    /**
     * Summons one NPC of the given type for each player in {@code team},
     * charging the {@code summoner} for each one via Vault.
     *
     * <p>Cost: {@code npc_summon.<type>.summon_cost} per player in the team.
     * If the summoner has {@code reanimatemc.summon.overridecost} or is OP,
     * no charge is applied.
     *
     * @return number of NPCs successfully summoned
     */
    public int summonTeam(Player summoner, ReanimatorType type, List<Player> team) {
        if (!plugin.getConfig().getBoolean("npc_summon.enabled", false)) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_summon_disabled"));
            return 0;
        }
        if (!summoner.hasPermission("reanimatemc.summon") && !summoner.isOp()) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
            return 0;
        }
        if (!summoner.hasPermission("reanimatemc.summon.use." + type.name().toLowerCase()) && !summoner.isOp()) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_no_type_permission",
                    "type", type.getDisplayName()));
            return 0;
        }

        boolean bypass = summoner.hasPermission("reanimatemc.summon.overridecost") || summoner.isOp();
        double costPerPlayer = plugin.getConfig().getDouble(
                "npc_summon." + type.name().toLowerCase() + ".summon_cost", 0.0);
        double totalCost = bypass ? 0.0 : costPerPlayer * team.size();

        if (!bypass && totalCost > 0 && vault != null && vault.isEnabled()) {
            if (vault.getBalance(summoner) < totalCost) {
                summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get(
                        "npc_team_insufficient_funds",
                        "cost", vault.format(totalCost),
                        "count", String.valueOf(team.size())));
                return 0;
            }
            vault.withdraw(summoner, totalCost);
            summoner.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get(
                    "npc_team_charged",
                    "cost", String.valueOf(totalCost),
                    "count", String.valueOf(team.size())));
        }

        int count = 0;
        for (Player member : team) {
            if (summon(summoner, type, member)) count++;
        }
        summoner.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get(
                "npc_team_summoned",
                "count", String.valueOf(count),
                "type", type.getDisplayName()));
        return count;
    }

    public boolean dismiss(Player player, UUID npcId) {
        ReanimatorNPC npc = activeNPCs.get(npcId);
        if (npc == null) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_not_found"));
            return false;
        }
        if (!npc.getOwnerId().equals(player.getUniqueId())
                && !player.hasPermission("reanimatemc.summon.admin") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_not_owner"));
            return false;
        }
        removeNPC(npc, NPCDismissedEvent.Reason.MANUAL);
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("npc_dismissed"));
        return true;
    }

    public int dismissAll(Player player) {
        List<UUID> ids = playerSummons.get(player.getUniqueId());
        if (ids == null || ids.isEmpty()) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_none_active"));
            return 0;
        }
        int count = 0;
        for (UUID id : new ArrayList<>(ids)) {
            ReanimatorNPC npc = activeNPCs.get(id);
            if (npc != null) { removeNPC(npc, NPCDismissedEvent.Reason.MANUAL); count++; }
        }
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("npc_dismissed_all",
                "count", String.valueOf(count)));
        return count;
    }

    public int dismissByType(Player player, ReanimatorType type) {
        List<UUID> ids = playerSummons.get(player.getUniqueId());
        if (ids == null || ids.isEmpty()) return 0;
        int count = 0;
        for (UUID id : new ArrayList<>(ids)) {
            ReanimatorNPC npc = activeNPCs.get(id);
            if (npc != null && npc.getType() == type) {
                removeNPC(npc, NPCDismissedEvent.Reason.MANUAL);
                count++;
            }
        }
        if (count > 0) {
            player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("npc_dismissed_all",
                    "count", String.valueOf(count)));
        }
        return count;
    }

    public int extendNpcTime(Player player, long seconds, boolean useCost) {
        List<ReanimatorNPC> npcs = getPlayerSummons(player);
        if (npcs.isEmpty()) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_none_active"));
            return 0;
        }
        if (useCost && vault.isEnabled()) {
            double costPerMinute = plugin.getConfig().getDouble("npc_summon.extend_cost_per_minute", 100.0);
            double cost = costPerMinute * (seconds / 60.0);
            if (!vault.withdraw(player, cost)) {
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_extend_insufficient_funds",
                        "cost", vault.format(cost)));
                return -1;
            }
            player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("npc_extend_charged",
                    "cost", vault.format(cost)));
        }
        npcs.forEach(n -> n.extendTime(seconds));
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("npc_time_extended",
                "time", String.valueOf(seconds)));
        return npcs.size();
    }

    public void sendNpcStatus(Player player) {
        List<ReanimatorNPC> npcs = getPlayerSummons(player);
        if (npcs.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("npc_none_active"));
            return;
        }
        player.sendMessage(ChatColor.GOLD + "━━━━━ " + ReanimateMC.lang.get("npc_status_header") + " ━━━━━");
        for (ReanimatorNPC npc : npcs) {
            Entity entity = npc.getEntity();
            double hp    = entity instanceof LivingEntity ? ((LivingEntity) entity).getHealth() : 0;
            double maxHp = entity instanceof LivingEntity
                    ? Objects.requireNonNull(((LivingEntity) entity).getAttribute(Attribute.MAX_HEALTH)).getValue()
                    : behaviors.get(npc.getType()).getMaxHp();

            long remSec  = npc.getRemainingSeconds();
            String timeStr = remSec == Long.MAX_VALUE ? ChatColor.GRAY + "∞"
                    : (remSec > 120 ? ChatColor.GREEN : remSec > 30 ? ChatColor.YELLOW : ChatColor.RED)
                    + formatTime(remSec);

            player.sendMessage(behaviors.get(npc.getType()).buildNameplate(npc, hp, maxHp, remSec));
            player.sendMessage(ChatColor.GRAY + "  ❤ " + buildHpBar(hp, maxHp)
                    + ChatColor.GRAY + " " + (int) hp + "/" + (int) maxHp);
            player.sendMessage(ChatColor.GRAY + "  ⏱ " + timeStr);

            if (npc.getTargetPlayerId() != null) {
                Player t = Bukkit.getPlayer(npc.getTargetPlayerId());
                String tName = t != null ? t.getName() : npc.getTargetPlayerId().toString().substring(0, 8);
                player.sendMessage(ChatColor.GRAY + "  → "
                        + ReanimateMC.lang.get("npc_status_target", "player", tName));
            }
        }
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    public List<ReanimatorNPC> getPlayerSummons(Player player) {
        return playerSummons.getOrDefault(player.getUniqueId(), Collections.emptyList())
                .stream().map(activeNPCs::get)
                .filter(Objects::nonNull).filter(ReanimatorNPC::isValid)
                .collect(Collectors.toList());
    }

    public int getPlayerGolems(Player player) {
        return (int) playerSummons.getOrDefault(player.getUniqueId(), Collections.emptyList())
                .stream().map(activeNPCs::get)
                .filter(n -> n != null && n.isValid() && n.getEntity() instanceof IronGolem)
                .count();
    }

    /** O(1) lookup for NPCDamageListener. */
    public ReanimatorNPC getNPCByEntity(org.bukkit.entity.Entity entity) {
        return entityIndex.get(entity.getUniqueId());
    }

    /** Called by NPCDamageListener after the golem entity has already died. */
    public void onNPCDied(ReanimatorNPC npc) {
        fireNPCDismissedEvent(npc, NPCDismissedEvent.Reason.MANUAL);
        activeNPCs.remove(npc.getId());
        entityIndex.remove(npc.getEntity().getUniqueId());
        cleanupPlayerEntry(npc);
    }

    public double getInterceptRatio(Player victim, Entity attacker) {
        for (ReanimatorNPC npc : activeNPCs.values()) {
            if (npc.getType() != ReanimatorType.PROTECTOR) continue;
            if (!npc.getOwnerId().equals(victim.getUniqueId())) continue;
            if (!npc.isValid()) continue;
            if (npc.getEntity().getLocation().distance(victim.getLocation())
                    > cfg("protector.intercept_distance", 6.0)) continue;
            Set<UUID> seen = interceptedThisTick.computeIfAbsent(victim.getUniqueId(), k -> new HashSet<>());
            if (attacker != null && !seen.add(attacker.getUniqueId())) continue;
            return cfg("protector.damage_transfer_ratio", 0.75);
        }
        return 0.0;
    }

    public void applyTransferredDamage(Player victim, double absorbed) {
        for (ReanimatorNPC npc : activeNPCs.values()) {
            if (npc.getType() != ReanimatorType.PROTECTOR) continue;
            if (!npc.getOwnerId().equals(victim.getUniqueId())) continue;
            if (!npc.isValid() || !(npc.getEntity() instanceof LivingEntity)) continue;
            LivingEntity golem = (LivingEntity) npc.getEntity();
            // Use damage() so Bukkit processes the death event naturally when HP reaches 0
            golem.damage(absorbed);
            golem.getWorld().spawnParticle(Particle.CRIT,
                    golem.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3);
            break;
        }
    }

    public void cleanup() {
        persistence.save(activeNPCs.values());
        for (ReanimatorNPC npc : new ArrayList<>(activeNPCs.values())) {
            Player owner = Bukkit.getPlayer(npc.getOwnerId());
            Bukkit.getPluginManager().callEvent(
                    new NPCDismissedEvent(owner, npc, NPCDismissedEvent.Reason.PLUGIN_DISABLE));
            npc.remove();
        }
        activeNPCs.clear();
        entityIndex.clear();
        playerSummons.clear();
        typeCooldowns.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Persistence
    // ═══════════════════════════════════════════════════════════════════════════

    private void restoreFromDisk() {
        List<NPCPersistenceManager.PendingRestore> pending = persistence.load();
        for (NPCPersistenceManager.PendingRestore pr : pending) {
            Player target = pr.targetId != null ? Bukkit.getPlayer(pr.targetId) : null;
            // Use the saved remaining lifetime, not the config default
            summonWithLifetime(pr.owner, pr.type, target, pr.lifetimeSeconds);
        }
        if (!pending.isEmpty()) {
            plugin.getLogger().info("[ReanimateMC] Restored " + pending.size() + " NPC(s) from disk.");
        }
    }

    /**
     * Internal summon that overrides the lifetime from config with an explicit
     * value. Used by persistence restore so golems don't get a fresh lifetime
     * when the owner reconnects — they keep their remaining time.
     *
     * @param lifetimeSeconds remaining seconds (0 = unlimited)
     */
    private boolean summonWithLifetime(Player summoner, ReanimatorType type,
                                        Player targetPlayer, long lifetimeSeconds) {
        Player owner = (targetPlayer != null) ? targetPlayer : summoner;
        long lifetime = lifetimeSeconds > 0 ? lifetimeSeconds
                : plugin.getConfig().getLong("npc_summon." + type.name().toLowerCase() + ".lifetime_seconds", 600L);

        Location spawnLoc = owner.getLocation().add(2, 0, 0);
        Entity entity = spawnEntity(spawnLoc, type);
        if (entity == null) return false;

        ReanimatorNPC npc = new ReanimatorNPC(owner.getUniqueId(), owner.getName(),
                entity, type, lifetime);
        NPCSummonedEvent event = new NPCSummonedEvent(summoner, npc);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) { entity.remove(); return false; }

        activeNPCs.put(npc.getId(), npc);
        entityIndex.put(entity.getUniqueId(), npc);
        playerSummons.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>()).add(npc.getId());
        behaviors.get(type).onSpawn(npc, (IronGolem) entity);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Tasks
    // ═══════════════════════════════════════════════════════════════════════════

    private void startBehaviorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Snapshot to avoid ConcurrentModificationException:
                // removeNPC() inside updateBehavior() modifies activeNPCs and playerSummons.
                List<ReanimatorNPC> snapshot = new ArrayList<>(activeNPCs.values());
                List<ReanimatorNPC> expired  = new ArrayList<>();

                for (ReanimatorNPC npc : snapshot) {
                    if (!npc.isValid() || npc.isExpired()) {
                        expired.add(npc);
                        continue;
                    }
                    npc.incrementBehaviorTick();
                    updateBehavior(npc);
                }

                for (ReanimatorNPC npc : expired) {
                    if (!activeNPCs.containsKey(npc.getId())) continue; // already removed
                    if (npc.isExpired()) notifyExpiry(npc);
                    fireNPCDismissedEvent(npc, npc.isExpired()
                            ? NPCDismissedEvent.Reason.EXPIRED : NPCDismissedEvent.Reason.MANUAL);
                    entityIndex.remove(npc.getEntity().getUniqueId());
                    npc.remove();
                    activeNPCs.remove(npc.getId());
                    cleanupPlayerEntry(npc);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startNameplateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ReanimatorNPC npc : activeNPCs.values()) {
                    if (!npc.isValid() || !(npc.getEntity() instanceof LivingEntity)) continue;
                    LivingEntity entity = (LivingEntity) npc.getEntity();
                    double hp    = entity.getHealth();
                    double maxHp = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getValue();
                    entity.setCustomName(behaviors.get(npc.getType())
                            .buildNameplate(npc, hp, maxHp, npc.getRemainingSeconds()));
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private void startInterceptResetTask() {
        new BukkitRunnable() {
            @Override public void run() { interceptedThisTick.clear(); }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Behavior dispatch
    // ═══════════════════════════════════════════════════════════════════════════

    private void updateBehavior(ReanimatorNPC npc) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Mob)) return;
        Mob mob      = (Mob) entity;
        Player owner = Bukkit.getPlayer(npc.getOwnerId());

        if (owner == null || !owner.isOnline()) {
            long timeout = plugin.getConfig().getLong("npc_summon.offline_timeout", 300) * 1000L;
            if (System.currentTimeMillis() - npc.getSummonTime() > timeout) {
                removeNPC(npc, NPCDismissedEvent.Reason.OFFLINE_TIMEOUT);
            }
            return;
        }

        // Priority 1: owner KO — only types that can revive the owner do so.
        // For PROTECTOR: only revive if enabled in config AND no HEALER is active.
        boolean typeCanRevive = behaviors.get(npc.getType()).canReviveOwner();
        if (typeCanRevive && npc.getType() == ReanimatorType.PROTECTOR) {
            typeCanRevive = !hasActiveHealer(owner);
        }
        if (koManager.isKO(owner) && typeCanRevive) {
            mob.setTarget(null);
            mob.getPathfinder().moveTo(owner.getLocation());
            if (entity.getLocation().distance(owner.getLocation())
                    < cfg(npc, "revive_distance", DEFAULT_REVIVE_DIST)) {
                performRevive(npc, owner, owner);
            }
            return;
        }

        // Protector: when owner goes KO and it cannot revive, warn + forced distress
        if (koManager.isKO(owner) && !typeCanRevive && npc.getType() == ReanimatorType.PROTECTOR) {
            if (!npc.isProtectorWarned()) {
                npc.setProtectorWarned(true);
                owner.sendMessage(ChatColor.GOLD + ReanimateMC.lang.get("npc_protector_no_revive_warning",
                        "type", npc.getType().getDisplayName()));
                koManager.sendDistressForced(owner);
            }
        } else if (!koManager.isKO(owner)) {
            npc.setProtectorWarned(false);
        }

        // Priority 2: explicit target KO — behavior decides if this type can revive
        Player target = npc.getTargetPlayerId() != null ? Bukkit.getPlayer(npc.getTargetPlayerId()) : null;
        if (target != null && koManager.isKO(target) && behaviors.get(npc.getType()).canRevive()) {
            mob.setTarget(null);
            mob.getPathfinder().moveTo(target.getLocation());
            if (entity.getLocation().distance(target.getLocation())
                    < cfg(npc, "revive_distance", DEFAULT_REVIVE_DIST)) {
                performRevive(npc, target, owner);
                npc.setTargetPlayerId(null);
            }
            return;
        }

        // Priority 3: per-type idle (every 3 seconds)
        if (npc.getBehaviorTick() % 3 == 0) {
            behaviors.get(npc.getType()).onIdleTick(npc, mob, owner);
        }

        followOwner(mob, entity, npc, owner);
    }

    // ── Public: command-triggered revive ─────────────────────────────────────

    /**
     * Validates all preconditions for {@code /rmc revive <player>} and starts
     * the timed revive using the caller's active HEALER.
     *
     * <p>Preconditions:
     * <ul>
     *   <li>Caller must have an active HEALER NPC.</li>
     *   <li>Target must be KO'd.</li>
     *   <li>If {@code reanimation.require_special_item} is enabled, the <b>caller</b>
     *       must hold the required item in their main hand — it is consumed on
     *       completion. The target (KO'd player) does not need any item.</li>
     * </ul>
     *
     * @return {@code true} if the revive was started, {@code false} with an error message already sent.
     */
    public boolean startCommandRevive(Player caller, Player target) {
        ReanimatorNPC healer = getActiveHealerNPC(caller);
        if (healer == null) {
            caller.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_no_healer_active"));
            return false;
        }
        if (!koManager.isKO(target)) {
            caller.sendMessage(ChatColor.RED + ReanimateMC.lang.get("player_not_ko"));
            return false;
        }
        if (reviveInProgress.containsValue(target.getUniqueId())) {
            caller.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_already_reviving"));
            return false;
        }

        boolean requireItem = plugin.getConfig().getBoolean("reanimation.require_special_item", true);
        String requiredItem = plugin.getConfig().getString("reanimation.required_item", "GOLDEN_APPLE");
        org.bukkit.inventory.ItemStack inHand = caller.getInventory().getItemInMainHand();
        if (requireItem && (inHand == null || !inHand.getType().toString().equalsIgnoreCase(requiredItem))) {
            caller.sendMessage(ChatColor.RED + ReanimateMC.lang.get("special_item_required", "item", requiredItem));
            return false;
        }

        // Teleport healer to target
        healer.getEntity().teleport(target.getLocation().add(1.5, 0, 0));

        // Notify target
        target.sendMessage(ChatColor.GOLD + ReanimateMC.lang.get("npc_reviving_you",
                "player", caller.getName(), "type", healer.getType().getDisplayName()));

        startTimedRevive(healer, target, caller, requireItem ? inHand : null);
        caller.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("revive_start"));
        return true;
    }

    /** Returns true if the player has at least one valid HEALER NPC active. */
    public boolean hasActiveHealer(Player player) {
        return getActiveHealerNPC(player) != null;
    }

    private ReanimatorNPC getActiveHealerNPC(Player player) {
        return playerSummons.getOrDefault(player.getUniqueId(), Collections.emptyList())
                .stream()
                .map(activeNPCs::get)
                .filter(n -> n != null && n.isValid() && n.getType() == ReanimatorType.HEALER)
                .findFirst()
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void performRevive(ReanimatorNPC npc, Player target, Player reviver) {
        // If already reviving this target, skip
        if (reviveInProgress.containsKey(npc.getId())) return;
        startTimedRevive(npc, target, reviver, null);
    }

    /**
     * Starts a timed revive countdown for the given NPC → target.
     * Duration is read from {@code npc_summon.<type>.revive_duration_ticks}.
     * Shows action-bar progress to both the target and the reviver.
     * On completion calls {@link KOManager#revive} and fires behavior callback.
     */
    private void startTimedRevive(ReanimatorNPC npc, Player target, Player reviver,
                                   org.bukkit.inventory.ItemStack itemToConsume) {
        reviveInProgress.put(npc.getId(), target.getUniqueId());

        int totalTicks = (int) cfg(npc, "revive_duration_ticks", 100.0);
        final int[] elapsed = {0};

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Abort conditions
            if (!npc.isValid() || !target.isOnline() || !koManager.isKO(target)) {
                cancelReviveTask(npc);
                return;
            }

            elapsed[0]++;
            int pct = (int) ((elapsed[0] / (double) totalTicks) * 100);
            String bar = buildReviveBar(pct);

            // Action bar on target
            fr.jachou.reanimatemc.utils.Utils.sendActionBar(target,
                    ChatColor.GREEN + ReanimateMC.lang.get("npc_being_revived_bar",
                            "type", npc.getType().getDisplayName(), "bar", bar, "pct", String.valueOf(pct)));

            // Action bar on reviver if online and different from target
            if (reviver.isOnline() && !reviver.getUniqueId().equals(target.getUniqueId())) {
                fr.jachou.reanimatemc.utils.Utils.sendActionBar(reviver,
                        ChatColor.YELLOW + ReanimateMC.lang.get("npc_reviving_bar",
                                "player", target.getName(), "bar", bar, "pct", String.valueOf(pct)));
            }

            // Completion
            if (elapsed[0] >= totalTicks) {
                cancelReviveTask(npc);
                if (itemToConsume != null && reviver.isOnline()) {
                    org.bukkit.inventory.ItemStack current = reviver.getInventory().getItemInMainHand();
                    if (current != null && current.getType() == itemToConsume.getType()) {
                        int newAmt = current.getAmount() - 1;
                        reviver.getInventory().setItemInMainHand(newAmt <= 0 ? null : current);
                        if (newAmt > 0) current.setAmount(newAmt);
                    }
                }
                koManager.revive(target, reviver);
                target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
                behaviors.get(npc.getType()).onRevive(npc, target, reviver);
                target.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("revived_by", "player",
                        npc.getType().getDisplayName()));
            }
        }, 0L, 1L);

        reviveTasks.put(npc.getId(), taskId);
    }

    private void cancelReviveTask(ReanimatorNPC npc) {
        Integer taskId = reviveTasks.remove(npc.getId());
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        reviveInProgress.remove(npc.getId());
    }

    private String buildReviveBar(int pct) {
        int total  = 10;
        int filled = (int) Math.round(pct / 10.0);
        return ChatColor.GREEN + "█".repeat(filled) + ChatColor.DARK_GRAY + "█".repeat(total - filled);
    }

    /**
     * Follow owner with stuck detection.
     * If the NPC hasn't moved > 0.5 blocks in {@code stuck_ticks_threshold} ticks
     * while it should be following, it teleports to the owner.
     */
    private void followOwner(Mob mob, Entity entity, ReanimatorNPC npc, Player owner) {
        double dist = entity.getLocation().distance(owner.getLocation());

        if (dist <= cfg(npc, "follow_stop_distance", DEFAULT_FOLLOW_STOP)) {
            mob.getPathfinder().stopPathfinding();
            npc.resetStuckTicks();
            npc.setLastPosition(entity.getLocation().clone());
            return;
        }

        if (dist > cfg(npc, "follow_start_distance", DEFAULT_FOLLOW_START)) {
            mob.getPathfinder().moveTo(owner.getLocation());

            org.bukkit.Location lastPos = npc.getLastPosition();
            if (lastPos != null && lastPos.getWorld() != null
                    && lastPos.getWorld().equals(entity.getWorld())
                    && entity.getLocation().distanceSquared(lastPos) < 0.25) {
                npc.incrementStuckTicks();
            } else {
                npc.resetStuckTicks();
            }
            npc.setLastPosition(entity.getLocation().clone());

            int threshold = plugin.getConfig().getInt("npc_summon.stuck_ticks_threshold", 5);
            double teleportThreshold = cfg(npc, "teleport_threshold", 20.0);

            if (npc.getStuckTicks() >= threshold || dist > teleportThreshold) {
                double angle = Math.random() * Math.PI * 2;
                Location tp = owner.getLocation().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                entity.teleport(tp);
                npc.resetStuckTicks();
                entity.getWorld().spawnParticle(Particle.PORTAL, tp.add(0, 1, 0), 12, 0.3, 0.5, 0.3);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Spawn
    // ═══════════════════════════════════════════════════════════════════════════

    private Entity spawnEntity(Location location, ReanimatorType type) {
        World world = location.getWorld();
        if (world == null) return null;
        NPCBehavior behavior = behaviors.get(type);
        IronGolem golem = (IronGolem) world.spawnEntity(location, EntityType.IRON_GOLEM);
        golem.setPlayerCreated(true);
        golem.setGlowing(true);
        golem.setCustomNameVisible(true);
        behavior.onSpawn(null, golem);
        return golem;
    }

    private void playSpawnEffects(Location location, ReanimatorType type) {
        World world = location.getWorld();
        if (world == null) return;
        NPCBehavior behavior = behaviors.get(type);
        final Particle particle = behavior.getSpawnParticle();
        final Sound sound       = behavior.getSpawnSound();
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks >= 20) { cancel(); return; }
                for (int i = 0; i < 36; i++) {
                    double a = 2 * Math.PI * i / 36;
                    world.spawnParticle(particle,
                            location.getX() + Math.cos(a) * 2, location.getY(),
                            location.getZ() + Math.sin(a) * 2, 1, 0, 0.3, 0, 0.01);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        world.playSound(location, sound, 1.0f, 1.0f);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Lifetime
    // ═══════════════════════════════════════════════════════════════════════════

    private long resolveLifetime(Player player, ReanimatorType type) {
        long configLifetime = (long) cfg(type.name().toLowerCase() + ".lifetime_seconds", 0.0);
        long metaLifetime   = 0L;
        String prefix = "reanimatemc.summon.lifetime." + type.name().toLowerCase() + ".";
        for (org.bukkit.permissions.PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String perm = pai.getPermission().toLowerCase();
            if (perm.startsWith(prefix) && pai.getValue()) {
                try {
                    long val = Long.parseLong(perm.substring(prefix.length()));
                    if (val > metaLifetime) metaLifetime = val;
                } catch (NumberFormatException ignored) { }
            }
        }
        return metaLifetime > 0 ? metaLifetime : configLifetime;
    }

    private void notifyExpiry(ReanimatorNPC npc) {
        Player owner = Bukkit.getPlayer(npc.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(ChatColor.YELLOW + ReanimateMC.lang.get("npc_expired",
                    "type", npc.getType().getDisplayName()));
            if (npc.isValid()) {
                npc.getEntity().getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        npc.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3);
                npc.getEntity().getWorld().playSound(npc.getEntity().getLocation(),
                        Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 1.2f);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Display
    // ═══════════════════════════════════════════════════════════════════════════

    private String buildHpBar(double hp, double maxHp) {
        int total  = 10;
        int filled = Math.max(0, Math.min(total, (int) Math.round((hp / maxHp) * total)));
        ChatColor color = hp / maxHp > 0.5 ? ChatColor.GREEN : hp / maxHp > 0.25 ? ChatColor.YELLOW : ChatColor.RED;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++)  bar.append(color).append("❚");
        for (int i = filled; i < total; i++) bar.append(ChatColor.DARK_GRAY).append("❚");
        return bar.toString();
    }

    private String formatTime(long s) {
        if (s == Long.MAX_VALUE || s < 0) return "∞";
        return s >= 60 ? (s / 60) + "m" + (s % 60) + "s" : s + "s";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Config helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private double cfg(String key, double def) {
        return plugin.getConfig().getDouble("npc_summon." + key, def);
    }

    private double cfg(ReanimatorNPC npc, String key, double def) {
        return cfg(npc.getType().name().toLowerCase() + "." + key, def);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════════════════

    private void removeNPC(ReanimatorNPC npc, NPCDismissedEvent.Reason reason) {
        fireNPCDismissedEvent(npc, reason);
        entityIndex.remove(npc.getEntity().getUniqueId());
        npc.remove();
        activeNPCs.remove(npc.getId());
        cleanupPlayerEntry(npc);
    }

    private void cleanupPlayerEntry(ReanimatorNPC npc) {
        List<UUID> list = playerSummons.get(npc.getOwnerId());
        if (list != null) list.remove(npc.getId());
    }

    private void fireNPCDismissedEvent(ReanimatorNPC npc, NPCDismissedEvent.Reason reason) {
        Player owner = Bukkit.getPlayer(npc.getOwnerId());
        Bukkit.getPluginManager().callEvent(new NPCDismissedEvent(owner, npc, reason));
    }
}
