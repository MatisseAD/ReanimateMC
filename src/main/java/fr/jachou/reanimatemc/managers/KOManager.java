package fr.jachou.reanimatemc.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.IOException;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.api.PlayerKOEvent;
import fr.jachou.reanimatemc.api.PlayerReanimatedEvent;
import fr.jachou.reanimatemc.data.KOData;
import fr.jachou.reanimatemc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class KOManager {
    private JavaPlugin plugin;
    private Map<UUID, KOData> koPlayers;
    private final File offlineFile;
    private final org.bukkit.configuration.file.YamlConfiguration offlineConfig;

    public KOManager(JavaPlugin plugin) {
        this.plugin = plugin;
        koPlayers = new HashMap<>();
        offlineFile = new File(plugin.getDataFolder(), "offlineko.yml");
        if (!offlineFile.exists()) {
            try {
                offlineFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        offlineConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(offlineFile);
    }

    public void setKO(final Player player) {
        long durationSeconds = plugin.getConfig().getLong("knockout.duration_seconds", 30);
        setKO(player, (int) durationSeconds);
    }

    public void setKO(final Player player, int durationSeconds) {
        if (isKO(player))
            return;

        // Fire the event
        PlayerKOEvent event = new PlayerKOEvent(player, durationSeconds);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        KOData data = new KOData();
        data.setKo(true);
        boolean autoCrawl = plugin.getConfig().getBoolean("prone.auto_crawl", false);
        data.setCrawling(autoCrawl);

        if (plugin.getConfig().getBoolean("tablist.enabled")) {
            String currentListName = player.getPlayerListName();
            if (currentListName.isEmpty()) {
                currentListName = player.getName();
            }
            data.setOriginalListName(currentListName);

            String koTagName = ChatColor.RED + "[KO] " + player.getName();
            player.setPlayerListName(koTagName);
        }

        // Programmation de la mort naturelle après un délai (en secondes)
        data.setEndTimestamp(System.currentTimeMillis() + (durationSeconds * 1000L));
        int taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (isKO(player)) {
                KOData d = koPlayers.get(player.getUniqueId());
                plugin.getServer().getScheduler().cancelTask(d.getBarTaskId());
                ArmorStand lb = d.getLabel();
                if (lb != null && lb.isValid()) lb.remove();
                ArmorStand mk = d.getHelpMarker();
                if (mk != null && mk.isValid()) { mk.remove(); d.setHelpMarker(null); }
                removeMount(player, d);
                restoreListName(player, d);
                koPlayers.remove(player.getUniqueId());
                player.setHealth(0);
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("death_natural"));
            }
        }, durationSeconds * 20L);
        data.setTaskId(taskId);
        koPlayers.put(player.getUniqueId(), data);

        // Envoi de l'Action Bar
        AtomicInteger secondsLeft = new AtomicInteger((int) durationSeconds);

        // Tâche répétitive pour le countdown
        ArmorStand label = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(0, 2.1, 0), EntityType.ARMOR_STAND);
        label.setInvisible(true);
        label.setMarker(true);
        label.setCustomNameVisible(true);
        label.setGravity(false);

        int barTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int sec = secondsLeft.getAndDecrement();
            if (sec >= 0 && koPlayers.containsKey(player.getUniqueId())) {
                Utils.sendActionBar(player,
                        ReanimateMC.lang.get("actionbar_ko_countdown", "time", String.valueOf(sec))
                );
                label.setCustomName(ChatColor.RED + "KO - " + sec + "s");
                label.teleport(player.getLocation().add(0, 2.1, 0));
            } else {
                label.remove();
            }
        }, 0L, 20L);

        data.setBarTaskId(barTaskId);
        data.setLabel(label);

        // Additional negative effects during KO
        int weaknessLvl = plugin.getConfig().getInt("knockout.weakness_level", 0);
        if (weaknessLvl > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, weaknessLvl - 1, false, false));
        }
        int fatigueLvl = plugin.getConfig().getInt("knockout.fatigue_level", 0);
        if (fatigueLvl > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, fatigueLvl - 1, false, false));
        }

        // Nausea — only while crawling and when enabled in config
        boolean nauseaEnabled = plugin.getConfig().getBoolean("knockout.crawl_nausea_enabled", true);
        boolean isCrawlingNow = data.isCrawling() && plugin.getConfig().getBoolean("prone.allow_crawl", false);
        if (nauseaEnabled && isCrawlingNow) {
            int nauseaLvl = plugin.getConfig().getInt("knockout.crawl_nausea_level", 0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, Integer.MAX_VALUE, nauseaLvl, false, false));
        }

        // Persistent enforcer — re-applies slowness and swimming every 20 ticks
        int enforcerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!isKO(player)) return;
            KOData d = koPlayers.get(player.getUniqueId());
            if (d == null) return;
            neutralizeNearbyWardens(player);
            boolean allowCrawl = plugin.getConfig().getBoolean("prone.allow_crawl", false);
            boolean crawling = d.isCrawling() && allowCrawl;
            if (crawling) {
                int crawlLvl = plugin.getConfig().getInt("prone.crawl_slowness_level", 5);
                PotionEffect cur = player.getPotionEffect(PotionEffectType.SLOWNESS);
                if (cur == null || cur.getAmplifier() != crawlLvl) {
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, crawlLvl, false, false));
                }
                if (!player.isSwimming()) player.setSwimming(true);
                // Nausea only while crawling
                boolean nEnabled = plugin.getConfig().getBoolean("knockout.crawl_nausea_enabled", true);
                if (nEnabled && !player.hasPotionEffect(PotionEffectType.NAUSEA)) {
                    int nLvl = plugin.getConfig().getInt("knockout.crawl_nausea_level", 0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, Integer.MAX_VALUE, nLvl, false, false));
                } else if (!nEnabled) {
                    player.removePotionEffect(PotionEffectType.NAUSEA);
                }
            } else {
                // Not crawling — full stop, remove nausea
                PotionEffect cur = player.getPotionEffect(PotionEffectType.SLOWNESS);
                if (cur == null || cur.getAmplifier() != 255) {
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
                }
                player.removePotionEffect(PotionEffectType.NAUSEA);
            }
        }, 20L, 20L);
        data.setEffectEnforcerId(enforcerId);
        boolean blind = plugin.getConfig().getBoolean("knockout.blindness", true);
        if (plugin.getConfig().getBoolean("prone.enabled", false)) {
            boolean allowCrawl = plugin.getConfig().getBoolean("prone.allow_crawl", false);
            if (data.isCrawling() && allowCrawl) {
                int crawlLevel = plugin.getConfig().getInt("prone.crawl_slowness_level", 5);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, crawlLevel, false, false));
                player.setSwimming(true);
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
                if (allowCrawl) {
                    player.setSwimming(true);
                }
            }
            if (blind) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
            }
        } else {
            // Comportement initial (pour les cas où prone n'est pas activé)
            if (plugin.getConfig().getBoolean("knockout.movement_disabled", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
            }
            if (blind) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
            }
        }

        // Make the KO'd player glow so teammates can spot them
        player.setGlowing(true);

        // Force the prone/lying-down animation using the swim pose.
        // setSwimming(true) renders the player horizontal on both Java and Bedrock
        // without any mount entity. This is the closest approximation to a
        // "lying down" animation available in the Paper API.
        player.setSwimming(true);

        data.setMount(null);

        player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("ko_set"));

        ReanimateMC.getInstance().getStatsManager().addKnockout();
    }

    /**
     * Resets any nearby Warden's anger and target toward a KO'd player.
     * The Warden builds anger from vibrations and smell independently of
     * the normal target-selection goal, so it can still lock onto a player
     * without ever firing an EntityTarget event. Running this alongside the
     * reactive listeners keeps the player effectively invisible to it while KO'd.
     */
    private void neutralizeNearbyWardens(Player player) {
        if (plugin.getConfig().getBoolean("knockout.mobs_attack_ko", false)) return;
        double radius = 32.0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Warden warden)) continue;
            if (warden.getAnger(player) > 0) {
                warden.clearAnger(player);
            }
            if (warden.getTarget() == player) {
                warden.setTarget(null);
            }
        }
    }

    private void restoreListName(Player player, KOData data) {
        if (plugin.getConfig().getBoolean("tablist.enabled")) {
            String originalName = data.getOriginalListName();
            if (originalName != null && !originalName.isEmpty()) {
                player.setPlayerListName(originalName);
            } else {
                player.setPlayerListName(player.getName());
            }
        }
    }

    private void removeMount(Player player, KOData data) {
        ArmorStand seat = data.getMount();
        if (seat != null && seat.isValid()) {
            seat.removePassenger(player);
            seat.remove();
            data.setMount(null);
        }

    }

    /**
     * Create an invisible armor stand used as mount for immobilising the player.
     * The stand is spawned slightly lower to avoid floating.
     */
    private ArmorStand createMount(org.bukkit.Location loc) {
        org.bukkit.Location seatLoc = loc.clone().subtract(0, 0, 0);
        ArmorStand seat = (ArmorStand) loc.getWorld().spawnEntity(seatLoc, EntityType.ARMOR_STAND);
        seat.setInvisible(true);
        seat.setSmall(true);
        seat.setGravity(false);
        seat.setInvulnerable(true);
        seat.setMarker(true);
        return seat;
    }

    public boolean isKO(Player player) {
        return koPlayers.containsKey(player.getUniqueId());
    }

    public KOData getKOData(Player player) {
        return koPlayers.get(player.getUniqueId());
    }

    /** Returns seconds remaining in the KO state, or 0 if not KO or no timestamp set. */
    public long getRemainingKOSeconds(Player player) {
        KOData data = koPlayers.get(player.getUniqueId());
        if (data == null || data.getEndTimestamp() <= 0) return 0;
        long rem = (data.getEndTimestamp() - System.currentTimeMillis()) / 1000L;
        return Math.max(0, rem);
    }

    public void revive(final Player player, final Player playerWhoRevive) {
        if (!isKO(player))
            return;

        PlayerReanimatedEvent event = new PlayerReanimatedEvent(player, playerWhoRevive, true);
        Bukkit.getPluginManager().callEvent(event);


        KOData data = koPlayers.get(player.getUniqueId());
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        if (data.getSuicideTaskId() != -1) {
            plugin.getServer().getScheduler().cancelTask(data.getSuicideTaskId());
            if (data.getEffectEnforcerId() != -1) plugin.getServer().getScheduler().cancelTask(data.getEffectEnforcerId());
            if (data.getSelfReviveTaskId() != -1) { plugin.getServer().getScheduler().cancelTask(data.getSelfReviveTaskId()); data.setSelfReviveTaskId(-1); }
            data.setSuicideTaskId(-1);
        }
        removeMount(player, data);
        ArmorStand label = data.getLabel();
        if (label != null && label.isValid()) {
            label.remove();
        }
        ArmorStand marker = data.getHelpMarker();
        if (marker != null && marker.isValid()) {
            marker.remove();
            data.setHelpMarker(null);
        }
        koPlayers.remove(player.getUniqueId());

        plugin.getServer().getScheduler().cancelTask(data.getBarTaskId());

        // Suppression des effets d'immobilisation et d'aveuglement
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        // Désactiver l'effet de glow
        player.setGlowing(false);
        player.setSwimming(false);


        // Restauration du nom de la liste du joueur
        restoreListName(player, data);

        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("revived"));

        // Restauration des points de vie (configurables)
        double healthRestored = plugin.getConfig().getDouble("reanimation.health_restored", 4);
        player.setHealth(Math.min(player.getMaxHealth(), healthRestored));

        // Application d'effets temporaires sur le joueur réanimé
        int nauseaDuration = plugin.getConfig().getInt("effects_on_revive.nausea", 5);
        int slownessDuration = plugin.getConfig().getInt("effects_on_revive.slowness", 10);
        int resistanceDuration = plugin.getConfig().getInt("effects_on_revive.resistance", 10);

        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration * 20, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration * 20, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, resistanceDuration * 20, 1));

        ReanimateMC.getInstance().getStatsManager().addRevive();
    }

    public void execute(final Player victim) {
        if (!isKO(victim)) return;
        KOData data = koPlayers.get(victim.getUniqueId());
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        plugin.getServer().getScheduler().cancelTask(data.getBarTaskId());
        if (data.getSuicideTaskId() != -1) {
            plugin.getServer().getScheduler().cancelTask(data.getSuicideTaskId());
            if (data.getEffectEnforcerId() != -1) plugin.getServer().getScheduler().cancelTask(data.getEffectEnforcerId());
            if (data.getSelfReviveTaskId() != -1) { plugin.getServer().getScheduler().cancelTask(data.getSelfReviveTaskId()); data.setSelfReviveTaskId(-1); }
            data.setSuicideTaskId(-1);
        }
        removeMount(victim, data);
        cleanupKOEffects(victim, data);
        restoreListName(victim, data);
        koPlayers.remove(victim.getUniqueId());

        victim.setHealth(0);
        victim.sendMessage(ChatColor.RED + ReanimateMC.lang.get("executed"));

        if (plugin.getConfig().getBoolean("execution.message_broadcast", true)) {
            Bukkit.broadcastMessage(ChatColor.DARK_RED + ReanimateMC.lang.get("execution_broadcast", "player", victim.getName()));
        }
    }

    public void suicide(Player player) {
        if (!isKO(player))
            return;
        KOData data = koPlayers.get(player.getUniqueId());
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        plugin.getServer().getScheduler().cancelTask(data.getBarTaskId());
        if (data.getSuicideTaskId() != -1) {
            plugin.getServer().getScheduler().cancelTask(data.getSuicideTaskId());
            if (data.getEffectEnforcerId() != -1) plugin.getServer().getScheduler().cancelTask(data.getEffectEnforcerId());
            if (data.getSelfReviveTaskId() != -1) { plugin.getServer().getScheduler().cancelTask(data.getSelfReviveTaskId()); data.setSelfReviveTaskId(-1); }
            data.setSuicideTaskId(-1);
        }
        removeMount(player, data);
        ArmorStand label = data.getLabel();
        if (label != null && label.isValid()) {
            label.remove();
        }
        ArmorStand marker = data.getHelpMarker();
        if (marker != null && marker.isValid()) {
            marker.remove();
            data.setHelpMarker(null);
        }
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        restoreListName(player, data);
        koPlayers.remove(player.getUniqueId());

        player.setHealth(0);
        player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("suicide_complete"));
    }

    private void cleanupKOEffects(Player player, KOData data) {
        ArmorStand label = data.getLabel();
        if (label != null && label.isValid()) {
            label.remove();
        }
        ArmorStand marker = data.getHelpMarker();
        if (marker != null && marker.isValid()) {
            marker.remove();
            data.setHelpMarker(null);
        }
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.setGlowing(false);
        player.setSwimming(false);
    }

    public void cancelAllTasks() {
        for (Map.Entry<UUID, KOData> entry : koPlayers.entrySet()) {
            UUID uuid = entry.getKey();
            KOData data = entry.getValue();
            plugin.getServer().getScheduler().cancelTask(data.getTaskId());
            plugin.getServer().getScheduler().cancelTask(data.getBarTaskId());
            if (data.getSuicideTaskId() != -1) {
                plugin.getServer().getScheduler().cancelTask(data.getSuicideTaskId());
            }
            if (data.getEffectEnforcerId() != -1) {
                plugin.getServer().getScheduler().cancelTask(data.getEffectEnforcerId());
            }
            ArmorStand seat = data.getMount();
            if (seat != null && seat.isValid()) {
                seat.remove();
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                cleanupKOEffects(p, data);
            }
        }
        koPlayers.clear();
    }

    // Méthode pour basculer l'état de "crawl" d'un joueur KO
    public void toggleCrawl(Player player) {
        if (!isKO(player))
            return;

        KOData data = koPlayers.get(player.getUniqueId());
        boolean currentState = data.isCrawling();
        data.setCrawling(!currentState);

        // Retirer l'effet de lenteur actuel
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        if (data.isCrawling()) {
            int crawlLevel = plugin.getConfig().getInt("prone.crawl_slowness_level", 5);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, crawlLevel, false, false));
            removeMount(player, data);
            player.setSwimming(true);
            // Apply nausea when crawl activates if enabled
            boolean nauseaEnabled = plugin.getConfig().getBoolean("knockout.crawl_nausea_enabled", true);
            if (nauseaEnabled) {
                int nauseaLvl = plugin.getConfig().getInt("knockout.crawl_nausea_level", 0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, Integer.MAX_VALUE, nauseaLvl, false, false));
            }
            player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("crawl_enabled"));
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
            // Remove nausea when crawl deactivates
            player.removePotionEffect(PotionEffectType.NAUSEA);
            // No mount — see setKO comment
            player.setSwimming(false);
            player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("crawl_disabled"));
        }
    }

    // Handle player disconnect while KO
    public void handleLogout(Player player) {
        if (!isKO(player)) return;
        KOData data = koPlayers.get(player.getUniqueId());
        long endTs = data.getEndTimestamp();
        offlineConfig.set(player.getUniqueId().toString(), endTs);
        try {
            offlineConfig.save(offlineFile);
        } catch (IOException ignored) {
        }
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        plugin.getServer().getScheduler().cancelTask(data.getBarTaskId());
        if (data.getSuicideTaskId() != -1) {
            plugin.getServer().getScheduler().cancelTask(data.getSuicideTaskId());
            if (data.getEffectEnforcerId() != -1) plugin.getServer().getScheduler().cancelTask(data.getEffectEnforcerId());
            if (data.getSelfReviveTaskId() != -1) { plugin.getServer().getScheduler().cancelTask(data.getSelfReviveTaskId()); data.setSelfReviveTaskId(-1); }
        }
        ArmorStand seat = data.getMount();
        if (seat != null && seat.isValid()) {
            seat.removePassenger(player);
            seat.remove();
        }
        ArmorStand label = data.getLabel();
        if (label != null && label.isValid()) {
            label.remove();
        }
        ArmorStand marker = data.getHelpMarker();
        if (marker != null && marker.isValid()) {
            marker.remove();
            data.setHelpMarker(null);
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setGlowing(false);
        player.setSwimming(false);
        restoreListName(player, data);
        koPlayers.remove(player.getUniqueId());
    }

    /**
     * Scans all loaded worlds for orphaned KO ArmorStands — invisible marker stands
     * whose custom name matches the KO countdown pattern ("KO - Xs") or the HELP!
     * distress marker — and removes them.
     *
     * <p>Also removes any label/helpMarker entities tracked in active KOData records
     * that are no longer valid, as a secondary safety net.
     *
     * @return total number of entities removed
     */
    public int purgeOrphanedHolograms() {
        int removed = 0;

        // Collect UUIDs of currently tracked labels so we don't double-remove them
        java.util.Set<UUID> trackedIds = new java.util.HashSet<>();
        for (KOData d : koPlayers.values()) {
            if (d.getLabel() != null)      trackedIds.add(d.getLabel().getUniqueId());
            if (d.getHelpMarker() != null) trackedIds.add(d.getHelpMarker().getUniqueId());
        }

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                ArmorStand stand = (ArmorStand) entity;
                if (!stand.isMarker() || !stand.isInvisible()) continue;

                String name = stand.getCustomName();
                if (name == null) continue;

                boolean isKOLabel    = name.matches("(?i).*KO\\s*-\\s*\\d+s?.*");
                boolean isHelpMarker = name.contains("HELP!");

                if ((isKOLabel || isHelpMarker) && !trackedIds.contains(stand.getUniqueId())) {
                    stand.remove();
                    removed++;
                }
            }
        }

        // Also clean up stale tracked entities that are invalid
        for (KOData d : koPlayers.values()) {
            ArmorStand lb = d.getLabel();
            if (lb != null && !lb.isValid()) { d.setLabel(null); removed++; }
            ArmorStand mk = d.getHelpMarker();
            if (mk != null && !mk.isValid()) { d.setHelpMarker(null); removed++; }
        }

        return removed;
    }

    /** Returns a read-only view of currently KO'd player UUIDs. */
    public java.util.Set<UUID> getKOPlayerUUIDs() {
        return java.util.Collections.unmodifiableSet(koPlayers.keySet());
    }

    public long pullOfflineKO(UUID uuid) {
        if (!offlineConfig.contains(uuid.toString())) {
            return -1L;
        }

        Object raw = offlineConfig.get(uuid.toString());
        offlineConfig.set(uuid.toString(), null);
        try {
            offlineConfig.save(offlineFile);
        } catch (IOException ignored) {
        }

        if (raw instanceof Number) {
            long val = ((Number) raw).longValue();
            long now = System.currentTimeMillis();
            if (val > now) {
                return (val - now) / 1000L;
            }
            return val;
        }

        return -1L;
    }

    /**
     * Sends a distress signal on behalf of the player without checking the
     * per-player cooldown. Used by the PROTECTOR golem so it can always alert
     * teammates when the owner goes KO, regardless of recent signals.
     */
    // ── Self-revive system ────────────────────────────────────────────────────

    /** Called by PlayerDamageListener to track when the player last took damage. */
    public void trackLastDamage(Player player) {
        KOData data = koPlayers.get(player.getUniqueId());
        if (data != null) data.setLastDamageTime(System.currentTimeMillis());
    }

    /**
     * Attempts to start a self-revive for the given K.O.'d player.
     *
     * <p>Validates: system enabled, player is KO, not already channeling,
     * max uses not exceeded, cooldown not active, combat check, items present.
     * On success, starts a timed progress bar. Consuming items and applying
     * post-revive effects happen on completion.
     *
     * @return {@code true} if the channel started, {@code false} with message sent.
     */
    public boolean startSelfRevive(Player player) {
        if (!plugin.getConfig().getBoolean("self_revive.enabled", true)) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_disabled"));
            return false;
        }
        if (!isKO(player)) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("not_in_ko"));
            return false;
        }

        KOData data = koPlayers.get(player.getUniqueId());

        if (data.getSelfReviveTaskId() != -1) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_already_channeling"));
            return false;
        }

        int maxUses = plugin.getConfig().getInt("self_revive.max_uses_per_ko", 1);
        if (maxUses > 0 && data.getSelfReviveUses() >= maxUses) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_max_uses",
                    "max", String.valueOf(maxUses)));
            return false;
        }

        long cooldownMs = plugin.getConfig().getLong("self_revive.cooldown_seconds", 120) * 1000L;
        // Per-player cooldown stored in offlineConfig (reuses lastDistressTime slot for simplicity —
        // we use a dedicated config key to avoid collision with distress)
        if (cooldownMs > 0 && hasSelfReviveCooldown(player, cooldownMs)) {
            long remaining = selfReviveCooldownRemaining(player, cooldownMs);
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_cooldown",
                    "time", String.valueOf(remaining)));
            return false;
        }

        int combatBlock = plugin.getConfig().getInt("self_revive.combat_block_seconds", 0);
        if (combatBlock > 0) {
            long elapsed = System.currentTimeMillis() - data.getLastDamageTime();
            if (elapsed < combatBlock * 1000L) {
                long remaining = combatBlock - (elapsed / 1000L);
                player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_combat_block",
                        "time", String.valueOf(remaining)));
                return false;
            }
        }

        // Item check
        boolean requireItems = plugin.getConfig().getBoolean("self_revive.require_items", true);
        List<ItemStack> requiredItems = new ArrayList<>();
        if (requireItems) {
            List<java.util.Map<?, ?>> itemList = plugin.getConfig()
                    .getMapList("self_revive.required_items");
            for (java.util.Map<?, ?> entry : itemList) {
                String matName = String.valueOf(entry.get("material"));
                int amount = entry.get("amount") instanceof Number
                        ? ((Number) entry.get("amount")).intValue() : 1;
                Material mat = Material.matchMaterial(matName);
                if (mat == null) continue;
                requiredItems.add(new ItemStack(mat, amount));
            }
            for (ItemStack req : requiredItems) {
                if (!player.getInventory().containsAtLeast(req, req.getAmount())) {
                    player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_missing_items",
                            "item", req.getType().name().replace("_", " ").toLowerCase(),
                            "amount", String.valueOf(req.getAmount())));
                    return false;
                }
            }
        }

        // All checks passed — start channel
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("selfrevive_start"));
        int totalTicks = plugin.getConfig().getInt("self_revive.duration_ticks", 200);
        final int[] elapsed = {0};
        final List<ItemStack> finalItems = requiredItems;
        final boolean requireFinal = requireItems;

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!isKO(player)) { cancelSelfRevive(player, false); return; }
            elapsed[0]++;
            int pct = (int) ((elapsed[0] / (double) totalTicks) * 100);
            String bar = buildSelfReviveBar(pct);
            Utils.sendActionBar(player, ChatColor.YELLOW + ReanimateMC.lang.get(
                    "selfrevive_progress_bar", "bar", bar, "pct", String.valueOf(pct)));

            if (elapsed[0] >= totalTicks) {
                completeSelfRevive(player, data, finalItems, requireFinal);
            }
        }, 0L, 1L);

        data.setSelfReviveTaskId(taskId);
        setSelfReviveCooldownStart(player);
        return true;
    }

    /**
     * Cancels an in-progress self-revive channel.
     *
     * @param sendMessage whether to notify the player of the cancellation.
     */
    public void cancelSelfRevive(Player player, boolean sendMessage) {
        KOData data = koPlayers.get(player.getUniqueId());
        if (data == null || data.getSelfReviveTaskId() == -1) return;
        Bukkit.getScheduler().cancelTask(data.getSelfReviveTaskId());
        data.setSelfReviveTaskId(-1);
        if (sendMessage) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("selfrevive_cancelled"));
        }
    }

    /** Returns true if the player is currently channeling a self-revive. */
    public boolean isChannelingSelfRevive(Player player) {
        KOData data = koPlayers.get(player.getUniqueId());
        return data != null && data.getSelfReviveTaskId() != -1;
    }

    private void completeSelfRevive(Player player, KOData data,
                                     List<ItemStack> items, boolean consume) {
        cancelSelfRevive(player, false);
        data.incrementSelfReviveUses();

        if (consume) {
            for (ItemStack req : items) {
                player.getInventory().removeItem(req);
            }
        }

        // Revive the player
        revive(player, player);

        // Post-revive effects (different from teammate revive — harsher)
        int nauseaSec  = plugin.getConfig().getInt("self_revive.effects_on_selfrevive.nausea", 10);
        int slowSec    = plugin.getConfig().getInt("self_revive.effects_on_selfrevive.slowness", 15);
        int resSec     = plugin.getConfig().getInt("self_revive.effects_on_selfrevive.resistance", 5);
        if (nauseaSec  > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,     nauseaSec * 20,  0));
        if (slowSec    > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   slowSec * 20,    1));
        if (resSec     > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, resSec * 20,     0));

        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("selfrevive_success"));
    }

    private String buildSelfReviveBar(int pct) {
        int total  = 10;
        int filled = (int) Math.round(pct / 10.0);
        return ChatColor.YELLOW + "█".repeat(filled) + ChatColor.DARK_GRAY + "█".repeat(total - filled);
    }

    // Cooldown stored per-player using lastDistressTime reuse would conflict;
    // store as a transient long in KOData via lastDamageTime (separate field).
    private final Map<UUID, Long> selfReviveCooldownMap = new HashMap<>();

    private void setSelfReviveCooldownStart(Player player) {
        selfReviveCooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean hasSelfReviveCooldown(Player player, long cooldownMs) {
        Long start = selfReviveCooldownMap.get(player.getUniqueId());
        if (start == null) return false;
        return (System.currentTimeMillis() - start) < cooldownMs;
    }

    private long selfReviveCooldownRemaining(Player player, long cooldownMs) {
        Long start = selfReviveCooldownMap.get(player.getUniqueId());
        if (start == null) return 0;
        long elapsed = System.currentTimeMillis() - start;
        return Math.max(0, (cooldownMs - elapsed) / 1000L);
    }

    public void sendDistressForced(Player player) {
        if (!isKO(player)) return;
        if (!ReanimateMC.getInstance().getConfig().getBoolean("knockout.distress.enabled", true)) return;

        KOData data = koPlayers.get(player.getUniqueId());
        data.setLastDistressTime(System.currentTimeMillis());

        ArmorStand existing = data.getHelpMarker();
        if (existing != null && existing.isValid()) existing.remove();
        data.setHelpMarker(null);

        ArmorStand marker = (ArmorStand) player.getWorld().spawnEntity(
                player.getLocation(), EntityType.ARMOR_STAND);
        marker.setInvisible(true);
        marker.setMarker(true);
        marker.setCustomNameVisible(true);
        marker.setGravity(false);
        marker.setGlowing(true);
        marker.setCustomName(ChatColor.RED + "HELP!");
        data.setHelpMarker(marker);

        String msg = ReanimateMC.lang.get("distress_broadcast", "player", player.getName(),
                "x", String.valueOf(player.getLocation().getBlockX()),
                "y", String.valueOf(player.getLocation().getBlockY()),
                "z", String.valueOf(player.getLocation().getBlockZ()));
        Bukkit.broadcastMessage(ChatColor.GOLD + msg);
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("distress_sent"));
    }

    public void sendDistress(Player player) {
        if (!isKO(player)) return;
        if (!ReanimateMC.getInstance().getConfig().getBoolean("knockout.distress.enabled", true)) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("distress_disabled"));
            return;
        }
        if (!player.hasPermission("reanimatemc.distress") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("no_permission"));
            return;
        }

        KOData data = koPlayers.get(player.getUniqueId());
        long cooldownMs = ReanimateMC.getInstance().getConfig()
                .getLong("knockout.distress.cooldown_seconds", 15) * 1000L;
        long now = System.currentTimeMillis();
        long elapsed = now - data.getLastDistressTime();
        if (elapsed < cooldownMs) {
            long remaining = (cooldownMs - elapsed) / 1000L;
            player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("distress_cooldown",
                    "time", String.valueOf(remaining)));
            return;
        }
        data.setLastDistressTime(now);

        ArmorStand existing = data.getHelpMarker();
        if (existing != null && existing.isValid()) {
            existing.remove();
        }
        data.setHelpMarker(null);

        ArmorStand marker = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        marker.setInvisible(true);
        marker.setMarker(true);
        marker.setCustomNameVisible(true);
        marker.setGravity(false);
        marker.setGlowing(true);
        marker.setCustomName(ChatColor.RED + "HELP!");
        data.setHelpMarker(marker);

        String msg = ReanimateMC.lang.get("distress_broadcast", "player", player.getName(),
                "x", String.valueOf(player.getLocation().getBlockX()),
                "y", String.valueOf(player.getLocation().getBlockY()),
                "z", String.valueOf(player.getLocation().getBlockZ()));
        Bukkit.broadcastMessage(ChatColor.GOLD + msg);
        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("distress_sent"));
    }
}