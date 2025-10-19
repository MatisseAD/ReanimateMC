# Quick Start Guide - NPC Summon System

## Installation & Setup

1. **Install Plugin**
   - Place `ReanimateMC-x.x.x.jar` in your `plugins/` folder
   - Restart server

2. **Configure Permissions** (LuckPerms example)

   ```bash
   # Basic player permissions (allow NPC summoning)
   /lp group default permission set reanimate.summon true
   /lp group default permission set reanimate.summon.use.golem true
   
   # VIP/Donor group (access to all types)
   /lp group vip permission set reanimate.summon true
   /lp group vip permission set reanimate.summon.use.golem true
   /lp group vip permission set reanimate.summon.use.healer true
   /lp group vip permission set reanimate.summon.use.protector true
   
   # Admin/Moderator (no cooldowns, unlimited summons)
   /lp group admin permission set reanimate.summon.admin true
   /lp group admin permission set reanimate.summon.overridecost true
   ```

3. **Configure Settings** (optional)
   
   Edit `plugins/ReanimateMC/config.yml`:
   ```yaml
   npc_summon:
     enabled: true
     max_summons_per_player: 1      # Increase for VIPs
     summon_cooldown: 300            # 5 minutes
     offline_timeout: 300            # Auto-remove after 5 min offline
     require_item: false             # Set true to require items
     required_item: NETHER_STAR
   ```

4. **Reload Configuration**
   ```
   /reanimatemc reload
   ```

## Basic Usage Examples

### Example 1: Solo Player Protection

**Scenario**: You're mining alone in a dangerous cave.

```
/reanimatemc summon protector
```

**Result**: 
- Iron Golem spawns and follows you
- Attacks zombies, skeletons, creepers nearby
- Provides extra security while mining

### Example 2: Emergency Rescue

**Scenario**: Your friend is knocked out in PvP.

```
/reanimatemc summon healer PlayerName
```

**Result**:
- Healing Golem spawns
- Automatically paths to PlayerName
- Revives them when close
- Returns to follow you

### Example 3: Team Dungeon Raid

**Scenario**: 4-player group exploring dungeon.

Each player:
```
/reanimatemc summon golem
```

**Result**:
- Each player has a personal golem
- Auto-revives if knocked out
- Better team survival

### Example 4: Managing Your NPCs

Check active NPCs:
```
/reanimatemc npcs
```

Output:
```
Active NPCs:
- Iron Golem Reanimator (45s)
```

Dismiss all NPCs:
```
/reanimatemc dismiss all
```

Output:
```
Dismissed 1 NPC(s).
```

## Common Permission Setups

### Setup 1: Free-to-Play Server
```yaml
# Only basic golem, limited use
reanimate.summon: true
reanimate.summon.use.golem: true
max_summons_per_player: 1
summon_cooldown: 600  # 10 minutes
```

### Setup 2: Premium/VIP Benefits
```yaml
# Regular players
default:
  - reanimate.summon
  - reanimate.summon.use.golem
  
# VIP players
vip:
  - reanimate.summon
  - reanimate.summon.use.golem
  - reanimate.summon.use.healer
  - reanimate.summon.use.protector
  
# Config
max_summons_per_player: 2  # VIPs can have 2 NPCs
summon_cooldown: 180       # 3 minutes for VIPs
```

### Setup 3: Hardcore/Survival Server
```yaml
# Item required to summon
require_item: true
required_item: NETHER_STAR  # Expensive item
max_summons_per_player: 1
summon_cooldown: 1800       # 30 minutes
```

### Setup 4: Creative/Event Server
```yaml
# No restrictions
default:
  - reanimate.summon.overridecost
  - reanimate.summon.use.*
  
max_summons_per_player: 5
summon_cooldown: 0  # No cooldown
```

## Troubleshooting

### Issue: "You do not have permission"
**Solution**: Check permissions with `/lp user <name> permission info`
- Need: `reanimate.summon`
- Need: `reanimate.summon.use.<type>`

### Issue: "You must wait X seconds"
**Solution**: 
- Wait for cooldown to expire, OR
- Grant `reanimate.summon.overridecost` permission

### Issue: "Maximum summons reached"
**Solution**:
- Dismiss existing NPCs first: `/reanimatemc dismiss all`
- Increase `max_summons_per_player` in config, OR
- Grant `reanimate.summon.overridecost` permission

### Issue: NPC doesn't follow me
**Check**:
- NPC must be valid (not dead)
- Distance must be > 10 blocks to trigger follow
- Check with `/reanimatemc npcs` if NPC is active

### Issue: NPC disappeared
**Possible causes**:
- NPC was killed by mobs/players
- You were offline > 5 minutes (offline_timeout)
- Server restarted (NPCs don't persist)

## Tips & Best Practices

1. **Use Protector for Exploration**: Great for caves, mining, exploring
2. **Use Healer for PvP/Combat**: Auto-revives for team fights
3. **Dismiss Before Logout**: Save your NPCs for next session (if < 5 min)
4. **Check Status Regularly**: Use `/reanimatemc npcs` to monitor
5. **Set Targets Wisely**: Target specific players for focused healing

## Admin Commands

### Force Summon for Player
```
/lp user <player> permission set reanimate.summon.admin true
```
Then summon on their behalf (future enhancement).

### Remove All NPCs (Server-Wide)
```
/reanimatemc reload
```
This reloads and cleans up all NPCs.

### Check Player's NPCs
Players can only see their own NPCs via `/reanimatemc npcs`.
Admins can dismiss any NPC if needed (future enhancement).

## Integration with Other Systems

### Economy Plugin
Future enhancement: Charge money for summons.

### Rank System
Use permission groups to tier access:
- Default: Golem only
- VIP: Golem + Healer
- MVP: All types + reduced cooldowns

### Quest/Achievement System
Grant permissions as quest rewards:
- "Unlock NPC Summoning" quest
- "Unlock Protector" achievement

## Performance Considerations

- **Recommended**: Max 10-20 NPCs per server (depending on player count)
- **max_summons_per_player**: Keep at 1-2 for best performance
- **offline_timeout**: Lower value (120-300s) to clean up NPCs faster
- **Behavior Task**: Runs every 1 second, minimal impact

## Config Template (Balanced)

```yaml
npc_summon:
  enabled: true
  max_summons_per_player: 1
  summon_cooldown: 300
  offline_timeout: 300
  require_item: false
  required_item: NETHER_STAR
  
# Example permission tiers in LuckPerms
# Tier 1 (Free): golem only
# Tier 2 (Donor): + healer
# Tier 3 (Premium): + protector
# Tier 4 (Admin): + overridecost
```

## Next Steps

1. Test with a small group of players
2. Adjust cooldowns based on server balance
3. Consider adding item costs if needed
4. Monitor performance with `/timings`
5. Gather player feedback for improvements

For more details, see `NPC_SUMMON_MODULE.md` and `NPC_SUMMON_TESTING.md`.
