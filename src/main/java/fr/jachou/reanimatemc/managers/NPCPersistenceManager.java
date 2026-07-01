package fr.jachou.reanimatemc.managers;

import fr.jachou.reanimatemc.data.ReanimatorNPC;
import fr.jachou.reanimatemc.data.ReanimatorNPC.ReanimatorType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Serializes active NPCs to {@code npc_data.yml} on server shutdown and
 * restores them on startup for owners who are online.
 *
 * <p>Each entry stores: owner UUID, owner name, NPC type, remaining lifetime
 * in seconds (0 = unlimited), and the target player UUID if set.
 *
 * <p>Entries whose owner is offline at load time are discarded silently.
 * Entries with 0 remaining seconds (already expired) are also discarded.
 */
public class NPCPersistenceManager {

    private static final String FILE_NAME = "npc_data.yml";
    private static final String KEY_NPCS  = "npcs";

    private final JavaPlugin plugin;
    private final File dataFile;

    public NPCPersistenceManager(JavaPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), FILE_NAME);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Serializes {@code npcs} to disk. Called from {@code NPCSummonManager.cleanup()}.
     */
    public void save(Iterable<ReanimatorNPC> npcs) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Object> entries   = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (ReanimatorNPC npc : npcs) {
            if (!npc.isValid()) continue;
            long expiresAt = npc.getExpiresAt(); // epoch ms, 0 = unlimited
            if (expiresAt > 0 && (expiresAt - now) < 5000) continue;

            YamlConfiguration entry = new YamlConfiguration();
            entry.set("npcId",     npc.getId().toString());
            entry.set("entityUuid", npc.getEntity().getUniqueId().toString());
            entry.set("owner",     npc.getOwnerId().toString());
            entry.set("ownerName", npc.getOwnerName());
            entry.set("type",      npc.getType().name());
            entry.set("expiresAt", expiresAt); // absolute epoch ms
            if (npc.getTargetPlayerId() != null) {
                entry.set("target", npc.getTargetPlayerId().toString());
            }
            entries.add(entry);
        }

        yaml.set(KEY_NPCS, entries);
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[ReanimateMC] Failed to save NPC data.", e);
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Reads persisted NPC entries and returns a list of {@link PendingRestore}
     * records. Caller is responsible for actually summoning them.
     *
     * <p>Entries whose owner is offline or whose remaining time has elapsed
     * are filtered out here.
     */
    public List<PendingRestore> load() {
        List<PendingRestore> result = new ArrayList<>();
        if (!dataFile.exists()) return result;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        List<?> raw = yaml.getList(KEY_NPCS);
        if (raw == null) return result;

        long now = System.currentTimeMillis();

        for (Object obj : raw) {
            if (!(obj instanceof YamlConfiguration)) continue;
            YamlConfiguration entry = (YamlConfiguration) obj;
            try {
                UUID npcId      = UUID.fromString(entry.getString("npcId", ""));
                UUID entityUuid = UUID.fromString(entry.getString("entityUuid", ""));
                UUID ownerId    = UUID.fromString(entry.getString("owner", ""));
                String ownerName= entry.getString("ownerName", "unknown");
                ReanimatorType type = ReanimatorType.valueOf(entry.getString("type", "GOLEM"));
                long expiresAt  = entry.getLong("expiresAt", 0L);
                String targetStr= entry.getString("target");
                UUID targetId   = targetStr != null ? UUID.fromString(targetStr) : null;

                // Filter entries that expired while the server was down or owner was offline
                if (expiresAt > 0 && expiresAt <= now) continue;

                // Compute remaining seconds from absolute timestamp
                long remainingSeconds = expiresAt > 0 ? (expiresAt - now) / 1000L : 0L;
                if (remainingSeconds == 1) continue; // < 1s left — not worth restoring

                Player owner = Bukkit.getPlayer(ownerId);
                if (owner == null || !owner.isOnline()) continue;

                result.add(new PendingRestore(owner, type, remainingSeconds, targetId, npcId, entityUuid));
            } catch (IllegalArgumentException ignored) { }
        }

        // Clear file after successful load
        try { new YamlConfiguration().save(dataFile); } catch (IOException ignored) { }

        return result;
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public static final class PendingRestore {
        public final Player owner;
        public final ReanimatorType type;
        public final long lifetimeSeconds; // 0 = unlimited
        public final UUID targetId;
        public final UUID npcId;
        public final UUID entityUuid;

        public PendingRestore(Player owner, ReanimatorType type, long lifetimeSeconds, UUID targetId,
                              UUID npcId, UUID entityUuid) {
            this.owner           = owner;
            this.type            = type;
            this.lifetimeSeconds = lifetimeSeconds;
            this.targetId        = targetId;
            this.npcId           = npcId;
            this.entityUuid      = entityUuid;
        }
    }
}
