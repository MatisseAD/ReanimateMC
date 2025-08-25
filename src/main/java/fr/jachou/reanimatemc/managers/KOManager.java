package fr.jachou.reanimatemc.managers;

import java.util.HashMap;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

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
                removeMount(player, data);
                restoreListName(player, data);
                player.setHealth(0);
                koPlayers.remove(player.getUniqueId());
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
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, fatigueLvl - 1, false, false));
        }


        // Application de la posture prone avec une option de ramper
        boolean blind = plugin.getConfig().getBoolean("knockout.blindness", true);
        if (plugin.getConfig().getBoolean("prone.enabled", false)) {
            boolean allowCrawl = plugin.getConfig().getBoolean("prone.allow_crawl", false);
            if (data.isCrawling() && allowCrawl) {
                int crawlLevel = plugin.getConfig().getInt("prone.crawl_slowness_level", 5);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, crawlLevel, false, false));
                player.setSwimming(true);
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
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
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
            }
            if (blind) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
            }
        }

        // Rendre le joueur KO plus visible pour les autres
        player.setGlowing(true);

        ArmorStand seat = createMount(player.getLocation());
        if (!data.isCrawling()) {
            seat.addPassenger(player);
            data.setMount(seat);
        } else {
            data.setMount(null);
            seat.remove();
        }

        player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("ko_set"));

        ReanimateMC.getInstance().getStatsManager().addKnockout();
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

    public void revive(final Player player, final Player playerWhoRevive) {
        if (!isKO(player))
            return;

        PlayerReanimatedEvent event = new PlayerReanimatedEvent(player, playerWhoRevive, true);
        Bukkit.getPluginManager().callEvent(event);


        KOData data = koPlayers.get(player.getUniqueId());
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        if (data.getSuicideTaskId() != -1) {
            plugin.getServer().getScheduler().cancelTask(data.getSuicideTaskId());
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
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
        // Désactiver l'effet de glow
        player.setGlowing(false);
        player.setSwimming(false);


        // Restauration du nom de la liste du joueur
        restoreListName(player, data);

        player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("revived"));

        // Restauration des points de vie (configurables)
        double healthRestored = plugin.getConfig().getDouble("reanimation.health_restored", 4);
        player.setHealth(Math.min(player.getMaxHealth(), healthRestored));

        // Application d’effets temporaires sur le joueur réanimé
        int nauseaDuration = plugin.getConfig().getInt("effects_on_revive.nausea", 5);
        int slownessDuration = plugin.getConfig().getInt("effects_on_revive.slowness", 10);
        int resistanceDuration = plugin.getConfig().getInt("effects_on_revive.resistance", 10);

        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, nauseaDuration * 20, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slownessDuration * 20, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, resistanceDuration * 20, 1));

        ReanimateMC.getInstance().getStatsManager().addRevive();
    }

    public void execute(final Player victim) {
        if (!isKO(victim))
            return;
        KOData data = koPlayers.get(victim.getUniqueId());
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        if (data.getSuicideTaskId() != -1) {
            plugin.getServer().getScheduler().cancelTask(data.getSuicideTaskId());
            data.setSuicideTaskId(-1);
        }
        removeMount(victim, data);
        ArmorStand label = data.getLabel();
        if (label != null && label.isValid()) {
            label.remove();
        }
        ArmorStand marker = data.getHelpMarker();
        if (marker != null && marker.isValid()) {
            marker.remove();
            data.setHelpMarker(null);
        }
        victim.removePotionEffect(PotionEffectType.WEAKNESS);
        victim.removePotionEffect(PotionEffectType.SLOW_DIGGING);
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
        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
        restoreListName(player, data);
        koPlayers.remove(player.getUniqueId());

        player.setHealth(0);
        player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("suicide_complete"));
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
            ArmorStand seat = data.getMount();
            if (seat != null && seat.isValid()) {
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
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.WEAKNESS);
                p.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                p.removePotionEffect(PotionEffectType.SLOW);
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.setGlowing(false);
                p.setSwimming(false);
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
        player.removePotionEffect(PotionEffectType.SLOW);

        if (data.isCrawling()) {
            // Mode crawl : appliquer un effet de SLOW de niveau configuré (laisser un minimum de déplacement)
            int crawlLevel = plugin.getConfig().getInt("prone.crawl_slowness_level", 5);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, crawlLevel, false, false));
            removeMount(player, data);
            player.setSwimming(true);
            player.sendMessage(ChatColor.GREEN + ReanimateMC.lang.get("crawl_enabled"));
        } else {
            // Retour à l'immobilisation complète (prone non-crawling)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
            if (data.getMount() == null || !data.getMount().isValid()) {
                ArmorStand seat = createMount(player.getLocation());
                seat.addPassenger(player);
                data.setMount(seat);
            } else if (!data.getMount().getPassengers().contains(player)) {
                data.getMount().addPassenger(player);
            }
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
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
        player.setGlowing(false);
        player.setSwimming(false);
        restoreListName(player, data);
        koPlayers.remove(player.getUniqueId());
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

    public void sendDistress(Player player) {
        if (!isKO(player)) return;
        KOData data = koPlayers.get(player.getUniqueId());
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