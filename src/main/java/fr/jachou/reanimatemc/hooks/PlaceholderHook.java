package fr.jachou.reanimatemc.hooks;

import fr.jachou.reanimatemc.ReanimateMC;
import fr.jachou.reanimatemc.data.ReanimatorNPC;
import fr.jachou.reanimatemc.managers.KOManager;
import fr.jachou.reanimatemc.managers.NPCSummonManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PlaceholderAPI expansion for ReanimateMC.
 *
 * <p>Available placeholders (prefix: {@code %reanimatemc_<key>%}):
 * <ul>
 *   <li>{@code is_ko}              – "true" / "false"</li>
 *   <li>{@code ko_time_remaining}  – seconds left in KO, or "0"</li>
 *   <li>{@code npc_count}          – number of active NPCs for this player</li>
 *   <li>{@code npc_type}           – type of first active NPC, or "none"</li>
 *   <li>{@code npc_time_remaining} – seconds left on first active NPC, or "0"</li>
 *   <li>{@code npc_hp}             – HP of first active NPC, or "0"</li>
 * </ul>
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final ReanimateMC plugin;
    private final KOManager koManager;
    private final NPCSummonManager npcSummonManager;

    public PlaceholderHook(ReanimateMC plugin, KOManager koManager, NPCSummonManager npcSummonManager) {
        this.plugin          = plugin;
        this.koManager       = koManager;
        this.npcSummonManager = npcSummonManager;
    }

    @Override public @NotNull String getIdentifier() { return "reanimatemc"; }
    @Override public @NotNull String getAuthor()     { return "Jachou"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }
    @Override public boolean canRegister()           { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "is_ko":
                return String.valueOf(koManager.isKO(player));

            case "ko_time_remaining": {
                if (!koManager.isKO(player)) return "0";
                long rem = koManager.getRemainingKOSeconds(player);
                return String.valueOf(Math.max(0, rem));
            }

            case "npc_count": {
                return String.valueOf(npcSummonManager.getPlayerSummons(player).size());
            }

            case "npc_type": {
                List<ReanimatorNPC> npcs = npcSummonManager.getPlayerSummons(player);
                if (npcs.isEmpty()) return "none";
                return npcs.get(0).getType().name().toLowerCase();
            }

            case "npc_time_remaining": {
                List<ReanimatorNPC> npcs = npcSummonManager.getPlayerSummons(player);
                if (npcs.isEmpty()) return "0";
                long rem = npcs.get(0).getRemainingSeconds();
                return String.valueOf(Math.max(0, rem));
            }

            case "npc_hp": {
                List<ReanimatorNPC> npcs = npcSummonManager.getPlayerSummons(player);
                if (npcs.isEmpty()) return "0";
                org.bukkit.entity.Entity e = npcs.get(0).getEntity();
                if (e instanceof org.bukkit.entity.LivingEntity) {
                    return String.valueOf((int) ((org.bukkit.entity.LivingEntity) e).getHealth());
                }
                return "0";
            }

            default:
                return null;
        }
    }
}
