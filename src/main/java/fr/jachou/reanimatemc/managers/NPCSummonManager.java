/* LICENSE BEGIN
    * This file is part of ReanimateMC.
    * ReanimateMC is under the proprietary of Frouzie.
    * You are not allowed to redistribute it and/or modify it.
LICENSE END
 */

package fr.jachou.reanimatemc.managers;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import fr.jachou.reanimatemc.data.ReanimatorNPC.ReanimatorType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager for summoning and managing NPC/Golem reanimators
 */
public class NPCSummonManager {
    private final JavaPlugin plugin;
    private final KOManager koManager;
    private final Map<UUID, ReanimatorNPC> activeNPCs;
    private final Map<UUID, List<UUID>> playerSummons; // Player UUID -> List of NPC IDs
    private final Map<UUID, Long> summonCooldowns; // Player UUID -> Cooldown end time
    
    public NPCSummonManager(JavaPlugin plugin, KOManager koManager) {
        this.plugin = plugin;
        this.koManager = koManager;
        this.activeNPCs = new HashMap<>();
        this.playerSummons = new HashMap<>();
        this.summonCooldowns = new HashMap<>();
        
        // Start periodic task to manage NPC behavior
        startNPCBehaviorTask();
    }
    
    /**
     * Summon a reanimator NPC for a player
     */
    public boolean summon(Player summoner, ReanimatorType type, Player targetPlayer) {
        // Check permission
        if (!summoner.hasPermission("reanimate.summon")) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
            return false;
        }
        
        if (!summoner.hasPermission("reanimate.summon.use." + type.name().toLowerCase())) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_no_type_permission", "type", type.getDisplayName()));
            return false;
        }
        
        // Check cooldown
        if (!summoner.hasPermission("reanimate.summon.overridecost")) {
            long now = System.currentTimeMillis();
            if (summonCooldowns.containsKey(summoner.getUniqueId())) {
                long cooldownEnd = summonCooldowns.get(summoner.getUniqueId());
                if (now < cooldownEnd) {
                    long remainingSeconds = (cooldownEnd - now) / 1000;
                    summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_summon_cooldown", 
                        "time", String.valueOf(remainingSeconds)));
                    return false;
                }
            }
        }
        
        // Check max summons limit
        int maxSummons = plugin.getConfig().getInt("npc_summon.max_summons_per_player", 1);
        List<UUID> playerNPCs = playerSummons.getOrDefault(summoner.getUniqueId(), new ArrayList<>());
        if (playerNPCs.size() >= maxSummons && !summoner.hasPermission("reanimate.summon.overridecost")) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_max_summons", 
                "max", String.valueOf(maxSummons)));
            return false;
        }
        
        // Spawn the NPC entity
        Location spawnLoc = summoner.getLocation().add(2, 0, 0);
        Entity entity = spawnReanimatorEntity(spawnLoc, type);
        
        if (entity == null) {
            summoner.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_summon_failed"));
            return false;
        }
        
        // Create NPC data
        ReanimatorNPC npc = new ReanimatorNPC(summoner.getUniqueId(), summoner.getName(), entity, type);
        if (targetPlayer != null) {
            npc.setTargetPlayerId(targetPlayer.getUniqueId());
        }
        
        // Store NPC
        activeNPCs.put(npc.getId(), npc);
        playerNPCs.add(npc.getId());
        playerSummons.put(summoner.getUniqueId(), playerNPCs);
        
        // Set cooldown
        if (!summoner.hasPermission("reanimate.summon.overridecost")) {
            long cooldownSeconds = plugin.getConfig().getLong("npc_summon.summon_cooldown", 300);
            summonCooldowns.put(summoner.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000));
        }
        
        // Visual effects
        playRitualEffects(spawnLoc);
        
        // Notify player
        summoner.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("npc_summoned", 
            "type", type.getDisplayName()));
        
        return true;
    }
    
    /**
     * Dismiss a specific NPC
     */
    public boolean dismiss(Player player, UUID npcId) {
        ReanimatorNPC npc = activeNPCs.get(npcId);
        if (npc == null) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_not_found"));
            return false;
        }
        
        // Check ownership
        if (!npc.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("reanimate.summon.admin")) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_not_owner"));
            return false;
        }
        
        // Remove NPC
        npc.remove();
        activeNPCs.remove(npcId);
        
        List<UUID> playerNPCs = playerSummons.get(npc.getOwnerId());
        if (playerNPCs != null) {
            playerNPCs.remove(npcId);
        }
        
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("npc_dismissed"));
        return true;
    }
    
    /**
     * Dismiss all NPCs for a player
     */
    public int dismissAll(Player player) {
        List<UUID> playerNPCs = playerSummons.get(player.getUniqueId());
        if (playerNPCs == null || playerNPCs.isEmpty()) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("npc_none_active"));
            return 0;
        }
        
        int count = 0;
        List<UUID> toRemove = new ArrayList<>(playerNPCs);
        for (UUID npcId : toRemove) {
            ReanimatorNPC npc = activeNPCs.get(npcId);
            if (npc != null) {
                npc.remove();
                activeNPCs.remove(npcId);
                count++;
            }
        }
        
        playerNPCs.clear();
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("npc_dismissed_all", "count", String.valueOf(count)));
        return count;
    }
    
    /**
     * Get status of player's summons
     */
    public List<ReanimatorNPC> getPlayerSummons(Player player) {
        List<UUID> npcIds = playerSummons.getOrDefault(player.getUniqueId(), Collections.emptyList());
        return npcIds.stream()
            .map(activeNPCs::get)
            .filter(Objects::nonNull)
            .filter(ReanimatorNPC::isValid)
            .collect(Collectors.toList());
    }

    public int getPlayerGolems(Player player) {
        List<UUID> npcIds = playerSummons.getOrDefault(player.getUniqueId(), Collections.emptyList());
        int count = 0;
        for (UUID id : npcIds) {
            ReanimatorNPC npc = activeNPCs.get(id);
            if (npc != null && npc.isValid() && npc.getEntity() instanceof IronGolem) {
                count++;
            }
        }
        return count;
    }


    /**
     * Spawn the appropriate entity based on type
     */
    private Entity spawnReanimatorEntity(Location location, ReanimatorType type) {
        World world = location.getWorld();
        if (world == null) return null;
        
        Entity entity;
        switch (type) {
            case GOLEM:
            case HEALER:
            case PROTECTOR:
                IronGolem golem = (IronGolem) world.spawnEntity(location, EntityType.IRON_GOLEM);
                golem.setCustomName(ChatColor.GOLD + type.getDisplayName());
                golem.setCustomNameVisible(true);
                golem.setPlayerCreated(true);
                
                // Make it glow for visibility
                golem.setGlowing(true);
                
                // Increase health for protector
                if (type == ReanimatorType.PROTECTOR && golem.getAttribute(Attribute.MAX_HEALTH) != null) {
                    golem.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200.0);
                    golem.setHealth(200.0);
                }
                
                entity = golem;
                break;
            default:
                entity = null;
        }
        
        return entity;
    }
    
    /**
     * Play visual ritual effects when summoning
     */
    private void playRitualEffects(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        // Particle circle
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20) {
                    this.cancel();
                    return;
                }
                
                // Circle of particles
                for (int i = 0; i < 36; i++) {
                    double angle = 2 * Math.PI * i / 36;
                    double x = location.getX() + Math.cos(angle) * 2;
                    double z = location.getZ() + Math.sin(angle) * 2;
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, x, location.getY(), z, 1, 0, 0.5, 0, 0.01);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        // Sound
        world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }
    
    /**
     * Start the behavior task that manages NPC actions
     */
    private void startNPCBehaviorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up invalid NPCs
                Iterator<Map.Entry<UUID, ReanimatorNPC>> iterator = activeNPCs.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, ReanimatorNPC> entry = iterator.next();
                    ReanimatorNPC npc = entry.getValue();
                    
                    if (!npc.isValid()) {
                        iterator.remove();
                        List<UUID> playerNPCs = playerSummons.get(npc.getOwnerId());
                        if (playerNPCs != null) {
                            playerNPCs.remove(npc.getId());
                        }
                        continue;
                    }
                    
                    // Execute NPC behavior
                    updateNPCBehavior(npc);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }
    
    /**
     * Update NPC behavior based on its type and situation
     */
    private void updateNPCBehavior(ReanimatorNPC npc) {
        Entity entity = npc.getEntity();
        if (!(entity instanceof Mob)) return;
        
        Mob mob = (Mob) entity;
        Player owner = Bukkit.getPlayer(npc.getOwnerId());
        
        // If owner is offline, remove NPC after timeout
        if (owner == null || !owner.isOnline()) {
            long timeout = plugin.getConfig().getLong("npc_summon.offline_timeout", 300) * 1000;
            if (System.currentTimeMillis() - npc.getSummonTime() > timeout) {
                npc.remove();
            }
            return;
        }

        if (koManager.isKO(owner)) {
            mob.setTarget(null);
            mob.getPathfinder().moveTo(owner.getLocation());

            if (entity.getLocation().distance(owner.getLocation()) < 5.0) {
                if (npc.getType() == ReanimatorType.GOLEM || npc.getType() == ReanimatorType.HEALER) {

                    koManager.revive(owner, owner);

                    owner.getWorld().spawnParticle(Particle.HEART, owner.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
                    owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);

                }
            }
        }
        
        // Check if there's a target player to help
        Player target = null;
        if (npc.getTargetPlayerId() != null) {
            target = Bukkit.getPlayer(npc.getTargetPlayerId());
        }
        
        // If target exists and is KO, move towards them
        if (target != null && koManager.isKO(target)) {
            mob.setTarget(null); // Don't attack anyone
            mob.getPathfinder().moveTo(target.getLocation());
            target.sendMessage("Le golem arrive");
            
            // Check if close enough to revive
            if (entity.getLocation().distance(target.getLocation()) < 5.0) {
                // Perform reanimation based on type
                target.sendMessage("Le golem arrive est à bonne distance");
                if (npc.getType() == ReanimatorType.GOLEM || npc.getType() == ReanimatorType.HEALER) {
                    koManager.revive(target, owner);
                    
                    // Visual effect
                    target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
                    
                    // Clear target
                    npc.setTargetPlayerId(null);
                }
            } else {
                // Path to target (using Bukkit pathfinding)
                mob.getPathfinder().moveTo(target.getLocation());
            }
        } else {
            // Follow owner if no target
            if (entity.getLocation().distance(owner.getLocation()) > 10) {
                mob.getPathfinder().moveTo(owner.getLocation());
            } else if (entity.getLocation().distance(owner.getLocation()) < 3) {
                // Stop if too close
                mob.getPathfinder().stopPathfinding();
            }
            
            // Protector type: attack nearby hostile mobs
            if (npc.getType() == ReanimatorType.PROTECTOR) {
                Entity nearbyHostile = owner.getWorld().getNearbyEntities(entity.getLocation(), 10, 10, 10).stream()
                    .filter(e -> e instanceof Monster)
                    .filter(e -> e.getLocation().distance(owner.getLocation()) < 15)
                    .findFirst()
                    .orElse(null);
                
                if (nearbyHostile != null && nearbyHostile instanceof LivingEntity) {
                    mob.setTarget((LivingEntity) nearbyHostile);
                }
            }
        }
    }
    
    /**
     * Clean up all NPCs (called on plugin disable)
     */
    public void cleanup() {
        for (ReanimatorNPC npc : activeNPCs.values()) {
            npc.remove();
        }
        activeNPCs.clear();
        playerSummons.clear();
        summonCooldowns.clear();
    }
}
