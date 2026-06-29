package fr.jachou.reanimatemc.behavior;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import fr.jachou.reanimatemc.managers.KOManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * HEALER — support reanimator.
 *
 * <p>Behavior priorities (evaluated every behavior tick):
 * <ol>
 *   <li>Never attack — target is always cleared.</li>
 *   <li>If owner or any ally is below {@code low_hp_threshold} HP and healer is
 *       beyond {@code periodic_heal_range}, move toward the most-injured target
 *       in range. Otherwise stay near owner.</li>
 *   <li>Heal tick (throttled by {@code periodic_heal_interval}).</li>
 *   <li>Continuous visual aura — a rotating HEART ring is shown whenever any
 *       healable target (owner or ally) is below max HP within
 *       {@code ally_heal_range}. Runs every behavior tick independently of the
 *       heal cooldown so the aura is always visible during active healing.</li>
 * </ol>
 *
 * <p>Movement constraint — the healer will not pursue a KO'd ally beyond
 * {@code max_leash_distance} blocks from the owner. If an ally falls outside
 * that radius the healer stays near the owner and relies on the KO priority
 * path in {@link fr.jachou.reanimatemc.managers.NPCSummonManager} to handle
 * when {@code canReviveAllies()} is true and the ally is set as a target.
 *
 * <p>Config keys (all under {@code npc_summon.healer.*}):
 * <ul>
 *   <li>{@code max_hp} — golem max HP (default 120)</li>
 *   <li>{@code max_leash_distance} — max blocks from owner the healer will wander (default 12)</li>
 *   <li>{@code low_hp_threshold} — HP below which the healer moves toward a target (default 16)</li>
 *   <li>{@code periodic_heal_interval} — seconds between heal ticks (default 10)</li>
 *   <li>{@code periodic_heal_amount} — HP healed to owner per tick (default 2)</li>
 *   <li>{@code periodic_heal_range} — max distance from golem to owner to apply heal (default 8)</li>
 *   <li>{@code ally_heal_amount} — HP healed to each ally per tick (default 1)</li>
 *   <li>{@code ally_heal_range} — radius to scan allies (default 12)</li>
 *   <li>{@code heal_self} — healer restores its own HP (default true)</li>
 *   <li>{@code self_heal_amount} — HP per tick to self (default 1)</li>
 *   <li>{@code heal_golems} — healer restores HP to allied golems (default true)</li>
 *   <li>{@code golem_heal_amount} — HP per tick to each allied golem (default 3)</li>
 *   <li>{@code aura_hp_threshold} — owner HP below which Regeneration fires (default 6)</li>
 *   <li>{@code bonus_hp_on_revive} — extra HP granted to revived player (default 4)</li>
 *   <li>{@code scan_radius} — radius for KO ally auto-scan (default 16)</li>
 *   <li>{@code aura_particle_enabled} — show circular heal aura (default true)</li>
 *   <li>{@code aura_particle_radius} — circle radius in blocks (default 3)</li>
 * </ul>
 */
public class HealerBehavior implements NPCBehavior {

    private final KOManager koManager;

    public HealerBehavior(KOManager koManager) {
        this.koManager = koManager;
    }

    @Override
    public void onIdleTick(ReanimatorNPC npc, Mob mob, Player owner) {
        double scanRadius     = cfg("healer.scan_radius", 16.0);
        double auraHpThresh   = cfg("healer.aura_hp_threshold", 6.0);
        double maxLeash       = cfg("healer.max_leash_distance", 12.0);
        double lowHpThreshold = cfg("healer.low_hp_threshold", 16.0);
        double allyHealRange  = cfg("healer.ally_heal_range", 12.0);

        // ── 1. Never attack ───────────────────────────────────────────────────
        mob.setTarget(null);

        // ── 2. KO ally scan — populate targetPlayerId for NPCSummonManager ───
        // Only set a target if the ally is within leash range from owner.
        if (npc.getTargetPlayerId() == null) {
            owner.getWorld()
                    .getNearbyEntities(mob.getLocation(), scanRadius, scanRadius, scanRadius)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .filter(p -> !p.getUniqueId().equals(owner.getUniqueId()))
                    .filter(koManager::isKO)
                    .filter(p -> p.getLocation().distance(owner.getLocation()) <= maxLeash)
                    .min(Comparator.comparingDouble(p -> p.getLocation().distance(mob.getLocation())))
                    .ifPresent(ko -> npc.setTargetPlayerId(ko.getUniqueId()));
        }

        // ── 3. Move toward most-injured nearby target if they need healing ───
        // This overrides the default followOwner in NPCSummonManager for
        // the one tick where someone is injured and within leash range.
        Player injuredTarget = findMostInjured(mob, owner, allyHealRange);
        if (injuredTarget != null) {
            double distToInjured = mob.getLocation().distance(injuredTarget.getLocation());
            double distToOwner   = mob.getLocation().distance(owner.getLocation());
            boolean injuredIsLow = injuredTarget.getHealth() < lowHpThreshold;

            // Move toward injured target only if within leash and they are actually low
            if (injuredIsLow && distToOwner <= maxLeash) {
                mob.getPathfinder().moveTo(injuredTarget.getLocation());
            }
        }

        // ── 4. Regeneration aura on owner when HP critically low ─────────────
        boolean ownerLow = owner.getHealth() < auraHpThresh;
        if (ownerLow && !npc.isHealerAuraActive()) {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, true));
            npc.setHealerAuraActive(true);
        } else if (!ownerLow && npc.isHealerAuraActive()) {
            npc.setHealerAuraActive(false);
        }

        // ── 5. Continuous visual aura — shown every idle tick when someone ────
        // has less than max HP within range. Independent of heal cooldown.
        if (cfgBool("healer.aura_particle_enabled", true)) {
            boolean anyoneNeedsHeal = anyoneNeedsHeal(mob, owner, allyHealRange);
            if (anyoneNeedsHeal) {
                spawnHealAura(mob, (int) cfg("healer.aura_particle_radius", 3));
            }
        }

        // ── 6. Periodic heal throttle ─────────────────────────────────────────
        int interval        = (int) cfg("healer.periodic_heal_interval", 10.0);
        int idleTicksNeeded = Math.max(1, interval / 3);
        npc.incrementIdleTick();
        if (npc.getIdleTick() % idleTicksNeeded != 0) return;

        runHealTick(mob, owner, allyHealRange);
    }

    // ── Heal tick ─────────────────────────────────────────────────────────────

    private void runHealTick(Mob mob, Player owner, double allyHealRange) {
        double ownerHealAmount = cfg("healer.periodic_heal_amount", 2.0);
        double ownerHealRange  = cfg("healer.periodic_heal_range", 8.0);
        double allyHealAmount  = cfg("healer.ally_heal_amount", 1.0);
        boolean healSelf       = cfgBool("healer.heal_self", true);
        double selfHealAmount  = cfg("healer.self_heal_amount", 1.0);
        boolean healGolems     = cfgBool("healer.heal_golems", true);
        double golemHealAmount = cfg("healer.golem_heal_amount", 3.0);

        // Owner
        if (mob.getLocation().distance(owner.getLocation()) <= ownerHealRange) {
            double ownerMax = Objects.requireNonNull(owner.getAttribute(Attribute.MAX_HEALTH)).getValue();
            boolean healed = owner.getHealth() < ownerMax;
            if (healed) {
                owner.setHealth(Math.min(ownerMax, owner.getHealth() + ownerHealAmount));
            }
            // Particles on owner: always shown so the aura range is visible
            owner.getWorld().spawnParticle(Particle.HEART,
                    owner.getLocation().add(0, 1.5, 0), 4, 0.3, 0.2, 0.3);
        }

        // Allies
        List<Player> allies = mob.getWorld()
                .getNearbyEntities(mob.getLocation(), allyHealRange, allyHealRange, allyHealRange)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(p -> !p.getUniqueId().equals(owner.getUniqueId()))
                .filter(p -> !koManager.isKO(p))
                .collect(Collectors.toList());

        for (Player ally : allies) {
            double allyMax = Objects.requireNonNull(ally.getAttribute(Attribute.MAX_HEALTH)).getValue();
            boolean healed = ally.getHealth() < allyMax;
            if (healed) {
                ally.setHealth(Math.min(allyMax, ally.getHealth() + allyHealAmount));
            }
            // Particles on ally: always shown
            ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    ally.getLocation().add(0, 1.5, 0), 4, 0.3, 0.2, 0.3);
        }

        // Self
        if (healSelf) {
            LivingEntity self = (LivingEntity) mob;
            double selfMax = Objects.requireNonNull(self.getAttribute(Attribute.MAX_HEALTH)).getValue();
            if (self.getHealth() < selfMax) {
                self.setHealth(Math.min(selfMax, self.getHealth() + selfHealAmount));
                mob.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        mob.getLocation().add(0, 2.2, 0), 2, 0.2, 0.1, 0.2);
            }
        }

        // Allied golems
        if (healGolems) {
            mob.getWorld()
                    .getNearbyEntities(mob.getLocation(), allyHealRange, allyHealRange, allyHealRange)
                    .stream()
                    .filter(e -> e instanceof IronGolem)
                    .filter(e -> !e.getUniqueId().equals(mob.getUniqueId()))
                    .map(e -> (IronGolem) e)
                    .forEach(golem -> {
                        double gMax = Objects.requireNonNull(
                                golem.getAttribute(Attribute.MAX_HEALTH)).getValue();
                        if (golem.getHealth() < gMax) {
                            golem.setHealth(Math.min(gMax, golem.getHealth() + golemHealAmount));
                            golem.getWorld().spawnParticle(Particle.HEART,
                                    golem.getLocation().add(0, 2.5, 0), 2, 0.2, 0.1, 0.2);
                        }
                    });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the player (owner or ally) with the lowest HP percentage within
     * {@code range} blocks, or {@code null} if everyone is at max HP.
     */
    private Player findMostInjured(Mob mob, Player owner, double range) {
        double ownerMax = Objects.requireNonNull(owner.getAttribute(Attribute.MAX_HEALTH)).getValue();
        Player best   = owner.getHealth() < ownerMax ? owner : null;
        double bestRatio = best != null ? owner.getHealth() / ownerMax : 1.0;

        for (Object e : mob.getWorld().getNearbyEntities(
                mob.getLocation(), range, range, range)) {
            if (!(e instanceof Player)) continue;
            Player p = (Player) e;
            if (p.getUniqueId().equals(owner.getUniqueId())) continue;
            if (koManager.isKO(p)) continue;
            double max   = Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double ratio = p.getHealth() / max;
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best      = p;
            }
        }
        return best;
    }

    /**
     * Returns true if at least one player within {@code range} blocks (owner
     * included) has HP below max — meaning the heal aura should be visible.
     */
    private boolean anyoneNeedsHeal(Mob mob, Player owner, double range) {
        double ownerMax = Objects.requireNonNull(owner.getAttribute(Attribute.MAX_HEALTH)).getValue();
        if (owner.getHealth() < ownerMax) return true;

        return mob.getWorld()
                .getNearbyEntities(mob.getLocation(), range, range, range)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(p -> !koManager.isKO(p))
                .anyMatch(p -> {
                    double max = Objects.requireNonNull(
                            p.getAttribute(Attribute.MAX_HEALTH)).getValue();
                    return p.getHealth() < max;
                });
    }

    /**
     * Spawns a full rotating circle of HEART + HAPPY_VILLAGER particles around
     * the golem over 16 ticks (one particle per tick, completing one full ring).
     * Called every idle tick while someone needs healing, so the aura is
     * continuous and always visible during active healing sessions.
     */
    private void spawnHealAura(Mob mob, int radius) {
        new BukkitRunnable() {
            int step      = 0;
            final int steps = 16;
            @Override
            public void run() {
                if (step >= steps || !mob.isValid()) { cancel(); return; }
                double angle = 2 * Math.PI * step / steps;
                double x     = mob.getLocation().getX() + radius * Math.cos(angle);
                double z     = mob.getLocation().getZ() + radius * Math.sin(angle);
                double y     = mob.getLocation().getY() + 0.3;
                World world  = mob.getWorld();
                world.spawnParticle(Particle.HEART,         x, y,        z, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.HAPPY_VILLAGER, x, y + 0.4, z, 1, 0, 0, 0, 0);
                step++;
            }
        }.runTaskTimer(ReanimateMC.getInstance(), 0L, 1L);
    }

    // ── NPCBehavior contract ──────────────────────────────────────────────────

    @Override
    public void onSpawn(ReanimatorNPC npc, IronGolem golem) {
        double maxHp = cfg("healer.max_hp", 120.0);
        var attr = golem.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(maxHp);
            golem.setHealth(maxHp);
        }
    }

    @Override
    public void onRevive(ReanimatorNPC npc, Player target, Player reviver) {
        double bonus     = cfg("healer.bonus_hp_on_revive", 4.0);
        double targetMax = Objects.requireNonNull(target.getAttribute(Attribute.MAX_HEALTH)).getValue();
        target.setHealth(Math.min(targetMax, target.getHealth() + bonus));
        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                target.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4);
    }

    @Override public boolean canRevive()       { return true; }
    @Override public boolean canReviveAllies() { return true; }

    @Override
    public String buildNameplate(ReanimatorNPC npc, double hp, double maxHp, long remSec) {
        return ChatColor.AQUA + "✚ " + npc.getType().getDisplayName()
                + ChatColor.DARK_GRAY + " [" + ChatColor.WHITE + npc.getOwnerName() + ChatColor.DARK_GRAY + "]"
                + ChatColor.GRAY + " | " + ChatColor.RED + "❤ " + (int) hp
                + timeSuffix(remSec);
    }

    @Override public Particle getSpawnParticle() { return Particle.HAPPY_VILLAGER; }
    @Override public Sound    getSpawnSound()    { return Sound.BLOCK_BEACON_ACTIVATE; }
    @Override public double   getMaxHp() {
        return ReanimateMC.getInstance().getConfig().getDouble("npc_summon.healer.max_hp", 120.0);
    }

    private String timeSuffix(long remSec) {
        if (remSec == Long.MAX_VALUE || remSec < 0) return "";
        return ChatColor.GRAY + " | " + ChatColor.YELLOW + "⏱ " + formatTime(remSec);
    }

    private String formatTime(long s) {
        return s >= 60 ? (s / 60) + "m" + (s % 60) + "s" : s + "s";
    }

    private double cfg(String key, double def) {
        return ReanimateMC.getInstance().getConfig().getDouble("npc_summon." + key, def);
    }

    private boolean cfgBool(String key, boolean def) {
        return ReanimateMC.getInstance().getConfig().getBoolean("npc_summon." + key, def);
    }
}
