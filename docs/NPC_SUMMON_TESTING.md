# NPC Summon System - Testing Guide

## Overview
The NPC Summon System allows players to summon Iron Golem-based reanimators that can:
- Follow their owner
- Automatically revive knocked-out players
- Protect their owner from hostile mobs (Protector type)
- Heal nearby players (Healer type)

## Prerequisites
- Server running Paper/Spigot 1.20+
- ReanimateMC plugin installed
- Required permissions configured

## Testing Steps

### 1. Basic Summon Testing

1. **Grant Permissions**
   ```
   /lp user <player> permission set reanimate.summon true
   /lp user <player> permission set reanimate.summon.use.golem true
   /lp user <player> permission set reanimate.summon.use.healer true
   /lp user <player> permission set reanimate.summon.use.protector true
   ```

2. **Test Summon Command**
   ```
   /reanimatemc summon golem
   ```
   - Expected: Iron Golem spawns near player with golden name tag
   - Expected: Particle effects and beacon activation sound
   - Expected: Success message in chat

3. **Test Different Types**
   ```
   /reanimatemc summon healer
   /reanimatemc summon protector
   ```
   - Expected: Different types spawn with appropriate names
   - Protector should have higher health (200 HP)

### 2. NPC Behavior Testing

1. **Following Behavior**
   - Walk away from the NPC
   - Expected: NPC follows when distance > 10 blocks
   - Expected: NPC stops when close (< 3 blocks)

2. **Reanimation Testing**
   - Have another player get knocked out
   - Summon NPC with target: `/reanimatemc summon golem <ko_player>`
   - Expected: NPC pathfinds to KO player
   - Expected: When close (<3 blocks), NPC revives the player
   - Expected: Heart particles and enchantment sound
   - Expected: NPC clears target and returns to following owner

3. **Protector Behavior**
   - Summon protector: `/reanimatemc summon protector`
   - Spawn hostile mobs nearby (zombies, skeletons)
   - Expected: Protector engages and fights hostile mobs near owner

### 3. Management Commands Testing

1. **Status Command**
   ```
   /reanimatemc npcs
   ```
   - Expected: List of active NPCs with type and age
   - Expected: Empty message if no NPCs active

2. **Dismiss Command**
   ```
   /reanimatemc dismiss all
   ```
   - Expected: All NPCs removed
   - Expected: Success message with count

### 4. Permission & Cooldown Testing

1. **Permission Check**
   - Remove specific type permission
   - Try to summon that type
   - Expected: Error message about missing permission

2. **Cooldown Testing**
   - Summon an NPC
   - Dismiss it immediately
   - Try to summon again
   - Expected: Cooldown message (default: 300s)
   
3. **Max Summons Limit**
   - Try to summon 2 NPCs (default max is 1)
   - Expected: Error about max summons reached

4. **Override Permissions**
   ```
   /lp user <player> permission set reanimate.summon.overridecost true
   ```
   - Try rapid summons
   - Expected: No cooldown, can exceed max limit

### 5. Edge Cases

1. **Owner Logout**
   - Summon NPC
   - Log out
   - Wait 5 minutes (default timeout: 300s)
   - Log back in
   - Expected: NPC should be removed (after timeout)

2. **NPC Death**
   - Summon NPC
   - Kill the NPC
   - Check `/reanimatemc npcs`
   - Expected: NPC auto-removed from active list

3. **World Change**
   - Summon NPC
   - Teleport to another world/dimension
   - Expected: NPC may not follow (pathfinding limitation)

## Configuration Options

In `config.yml`:
```yaml
npc_summon:
  enabled: true
  max_summons_per_player: 1      # Max NPCs per player
  summon_cooldown: 300            # Cooldown in seconds
  offline_timeout: 300            # Remove NPC after owner offline for X seconds
  require_item: false             # Whether summon requires item
  required_item: NETHER_STAR      # Item required if enabled
```

## Expected Console Output

On plugin load:
```
[ReanimateMC] NPCSummonManager initialized
```

On NPC summon:
```
[ReanimateMC] Player <player> summoned <type> NPC
```

## Known Limitations

1. NPCs use vanilla IronGolem pathfinding (not as smooth as Citizens)
2. NPCs don't persist across server restarts
3. Cross-world following may not work perfectly
4. No custom skins/models (just Iron Golem with custom names)

## Future Enhancements (Not Implemented)

- Citizens2 integration for better NPC features
- Custom NPC skins/models
- NPC persistence across restarts
- More NPC types (archer, mage, etc.)
- NPC leveling/upgrades
- Custom animations and behaviors
