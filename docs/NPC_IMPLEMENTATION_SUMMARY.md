# NPC Summon System - Implementation Summary

## What Was Implemented

This implementation adds a complete NPC/Golem summoning system to ReanimateMC, allowing players to summon Iron Golem-based reanimators that can automatically revive knocked-out players, follow their owner, and protect them from hostile mobs.

## New Files Created

### Core Implementation
1. **`src/main/java/fr/jachou/reanimatemc/data/ReanimatorNPC.java`**
   - Data class representing a summoned NPC
   - Stores: UUID, owner info, entity reference, type, summon time, target
   - Three types: GOLEM, HEALER, PROTECTOR

2. **`src/main/java/fr/jachou/reanimatemc/managers/NPCSummonManager.java`**
   - Core manager for NPC lifecycle
   - Handles: summoning, dismissing, cooldowns, permissions
   - Implements AI behavior: following, pathfinding, auto-revive
   - Periodic cleanup task for invalid NPCs
   - Visual effects: ritual summoning particles + sounds

### Documentation
3. **`docs/NPC_SUMMON_MODULE.md`** - Comprehensive technical documentation (FR)
4. **`docs/NPC_SUMMON_TESTING.md`** - Testing guide with scenarios
5. **`docs/QUICK_START_NPC.md`** - Quick start guide with examples

## Modified Files

### Core Plugin
1. **`src/main/java/fr/jachou/reanimatemc/ReanimateMC.java`**
   - Added `NPCSummonManager` instance
   - Initialized manager in `onEnable()`
   - Added cleanup in `onDisable()`
   - Added getter method for manager access

2. **`src/main/java/fr/jachou/reanimatemc/commands/ReanimateMCCommand.java`**
   - Added `NPCSummonManager` dependency
   - New commands: `summon`, `dismiss`, `npcs`
   - Updated tab completion for new commands
   - Type parsing and validation

### Configuration
3. **`src/main/resources/plugin.yml`**
   - Updated command usage description
   - Added 7 new permissions:
     - `reanimate.summon` (base)
     - `reanimate.summon.use.golem`
     - `reanimate.summon.use.healer`
     - `reanimate.summon.use.protector`
     - `reanimate.summon.overridecost`
     - `reanimate.summon.admin`

4. **`src/main/resources/config.yml`**
   - Added `npc_summon` section with 6 options:
     - enabled, max_summons_per_player, summon_cooldown
     - offline_timeout, require_item, required_item

### Localization
5. **`src/main/resources/lang/en.yml`**
   - Added 16 new message keys for NPC system

6. **`src/main/resources/lang/fr.yml`**
   - Added 16 new message keys (French translations)

## Features Implemented

### Commands
✅ `/reanimatemc summon <type> [player]` - Summon NPC reanimator
✅ `/reanimatemc dismiss all` - Dismiss all player's NPCs
✅ `/reanimatemc npcs` - Show active NPCs status

### NPC Types
✅ **GOLEM** - Standard reanimator, follows owner
✅ **HEALER** - Same as golem, specialized for healing (future enhancements)
✅ **PROTECTOR** - Follows owner + attacks hostile mobs, 200 HP

### Behaviors
✅ **Following** - NPCs follow owner (distance-based)
✅ **Auto-Revive** - NPCs pathfind to and revive KO'd players
✅ **Protection** - Protector type attacks nearby hostile mobs
✅ **Cleanup** - Auto-remove invalid/dead NPCs
✅ **Timeout** - Remove NPCs after owner offline timeout

### Permissions System
✅ Base permission check (`reanimate.summon`)
✅ Type-specific permissions (per NPC type)
✅ Cooldown bypass (`overridecost`)
✅ Admin controls (`summon.admin`)

### Cooldown & Limits
✅ Configurable summon cooldown (default: 5 minutes)
✅ Max summons per player limit (default: 1)
✅ Permission-based bypass for both

### Visual Effects
✅ Ritual summoning particles (soul fire circle)
✅ Beacon activation sound on summon
✅ Heart particles on revive
✅ Enchantment sound on revive
✅ Glowing NPCs with custom names

### Configuration
✅ Enable/disable system
✅ Adjustable limits and cooldowns
✅ Optional item requirement
✅ Offline timeout setting

### Localization
✅ English translations
✅ French translations
✅ Template for other languages

## Technical Details

### Architecture Choices

**No Citizens Dependency**
- Uses native Bukkit/Paper IronGolem entities
- Bukkit Pathfinder API for movement
- BukkitRunnable for behavior updates
- Simpler, more portable solution

**Event-Driven Design**
- Periodic task (1s interval) for NPC behavior
- Checks validity, updates AI, handles cleanup
- Minimal performance impact

**Integration Points**
- Directly calls `KOManager.revive()` for consistency
- Respects existing permission system
- Uses existing Lang system for messages

### Performance Considerations
- Single task for all NPCs (efficient)
- Cleanup on every cycle (prevents memory leaks)
- Distance-based behavior (reduces pathfinding calls)
- Configurable limits prevent spam

### Code Quality
- Consistent with existing codebase style
- Proper encapsulation (data classes, managers)
- Null safety checks throughout
- Clear method documentation

## Testing Recommendations

### Manual Testing Checklist
- [ ] Summon each NPC type (golem, healer, protector)
- [ ] Verify NPCs follow owner correctly
- [ ] Test auto-revive on KO'd player
- [ ] Verify protector attacks hostile mobs
- [ ] Test cooldown enforcement
- [ ] Test max summons limit
- [ ] Test permission checks
- [ ] Test dismiss commands
- [ ] Test offline timeout
- [ ] Test NPC death cleanup

### Edge Cases Covered
✅ NPC entity death/removal
✅ Owner logout/disconnect
✅ Invalid targets
✅ Permission changes mid-game
✅ Max limit reached
✅ Cooldown active

## Configuration Examples

### Default (Balanced)
```yaml
npc_summon:
  enabled: true
  max_summons_per_player: 1
  summon_cooldown: 300
  offline_timeout: 300
  require_item: false
  required_item: NETHER_STAR
```

### Casual/Creative
```yaml
npc_summon:
  enabled: true
  max_summons_per_player: 5
  summon_cooldown: 60
  offline_timeout: 600
  require_item: false
```

### Hardcore/Survival
```yaml
npc_summon:
  enabled: true
  max_summons_per_player: 1
  summon_cooldown: 1800
  offline_timeout: 180
  require_item: true
  required_item: NETHER_STAR
```

## Future Enhancement Opportunities

### High Priority
- [ ] Item consumption on summon (if require_item: true)
- [ ] NPC persistence across restarts
- [ ] Admin command to dismiss any player's NPCs
- [ ] Statistics tracking (summons, revives by NPC)

### Medium Priority
- [ ] Healer periodic healing ability
- [ ] Custom NPC names (player-defined)
- [ ] NPC leveling/upgrade system
- [ ] Economy integration (cost money to summon)

### Low Priority / Advanced
- [ ] Citizens2 hook (optional, for advanced features)
- [ ] Custom models via ModelEngine/ItemsAdder
- [ ] More NPC types (Archer, Mage, Tank)
- [ ] NPC equipment/upgrades
- [ ] Team/guild shared NPCs

## Known Limitations

1. **Pathfinding**: Vanilla pathfinding less smooth than Citizens
2. **Persistence**: NPCs don't survive server restarts
3. **Cross-world**: May not follow perfectly across dimensions
4. **Appearance**: Limited to Iron Golem model
5. **Item Requirement**: Not yet consuming items (placeholder)

## Compatibility

- **Minimum**: Paper/Spigot 1.20+
- **Recommended**: Paper 1.20.2+
- **Dependencies**: None (optional Citizens2 support not yet implemented)
- **Conflicts**: None known

## Changelog

### Version 1.0 (Initial Implementation)
- Added NPC summon system with 3 types
- Implemented following, auto-revive, and protection behaviors
- Added commands: summon, dismiss, npcs
- Added 7 permissions for granular control
- Configuration options for limits and cooldowns
- English and French translations
- Comprehensive documentation

## Credits

Implementation based on requirements from MatisseAD for ReanimateMC plugin.

**Approach**: Custom implementation using native Bukkit entities
**Target**: Paper 1.20+
**Language**: Java 16+
**License**: Same as ReanimateMC (proprietary)

## Support

For issues, bugs, or suggestions, refer to:
- `docs/NPC_SUMMON_TESTING.md` for testing procedures
- `docs/NPC_SUMMON_MODULE.md` for technical details
- `docs/QUICK_START_NPC.md` for usage examples
