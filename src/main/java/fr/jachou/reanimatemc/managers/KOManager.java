package fr.jachou.reanimatemc.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import fr.jachou.reanimatemc.ReanimateMC;
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

public class KOManager {
    private JavaPlugin plugin;
    private Map<UUID, KOData> koPlayers;

    public KOManager(JavaPlugin plugin) {
        this.plugin = plugin;
        koPlayers = new HashMap<>();
    }

    public void setKO(final Player player) {
        if (isKO(player))
            return;

        KOData data = new KOData();
        data.setKo(true);
        data.setCrawling(false);

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
        long durationSeconds = plugin.getConfig().getLong("knockout.duration_seconds", 30);
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
        int barTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int sec = secondsLeft.getAndDecrement();
            if (sec >= 0 && koPlayers.containsKey(player.getUniqueId())) {
                Utils.sendActionBar(player,
                        ReanimateMC.lang.get("actionbar_ko_countdown", "time", String.valueOf(sec))
                );
            }
        }, 0L, 20L);

        data.setBarTaskId(barTaskId);

        // Application de la posture prone avec une option de ramper
        boolean blind = plugin.getConfig().getBoolean("knockout.blindness", true);
        if (plugin.getConfig().getBoolean("prone.enabled", false)) {
            // Si l'option de crawl est activée, on applique par défaut un effet très fort pour ne pas permettre le déplacement
            boolean allowCrawl = plugin.getConfig().getBoolean("prone.allow_crawl", false);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
            if (allowCrawl) {
                player.setSwimming(true);
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
        seat.addPassenger(player);
        data.setMount(seat);

        player.sendMessage(ChatColor.RED + ReanimateMC.lang.get("ko_set"));
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
        org.bukkit.Location seatLoc = loc.clone().subtract(0, 1.0, 0);
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

    public void revive(final Player player) {
        if (!isKO(player))
            return;

        KOData data = koPlayers.get(player.getUniqueId());
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        removeMount(player, data);
        koPlayers.remove(player.getUniqueId());

        plugin.getServer().getScheduler().cancelTask(data.getBarTaskId());

        // Suppression des effets d'immobilisation et d'aveuglement
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
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
    }

    public void execute(final Player victim) {
        if (!isKO(victim))
            return;
        KOData data = koPlayers.get(victim.getUniqueId());
        plugin.getServer().getScheduler().cancelTask(data.getTaskId());
        removeMount(victim, data);
        koPlayers.remove(victim.getUniqueId());

        victim.setHealth(0);
        victim.sendMessage(ChatColor.RED + ReanimateMC.lang.get("executed"));

        if (plugin.getConfig().getBoolean("execution.message_broadcast", true)) {
            Bukkit.broadcastMessage(ChatColor.DARK_RED + ReanimateMC.lang.get("execution_broadcast", "player", victim.getName()));
        }
    }

    public void cancelAllTasks() {
        for (KOData data : koPlayers.values()) {
            plugin.getServer().getScheduler().cancelTask(data.getTaskId());
            ArmorStand seat = data.getMount();
            if (seat != null && seat.isValid()) {
                seat.remove();
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
}