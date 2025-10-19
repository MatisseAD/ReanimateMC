# NPC Summon System - Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         ReanimateMC Plugin                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────┐      ┌──────────────────┐                   │
│  │  KOManager     │◄─────│ NPCSummonManager │                   │
│  │                │      │                  │                   │
│  │ - isKO()       │      │ - summon()       │                   │
│  │ - revive()     │      │ - dismiss()      │                   │
│  └────────────────┘      │ - cleanup()      │                   │
│                          │ - updateAI()     │                   │
│                          └──────────────────┘                   │
│                                   │                              │
│                                   │ manages                      │
│                                   ▼                              │
│                          ┌──────────────────┐                   │
│                          │ ReanimatorNPC    │                   │
│                          │ ┌──────────────┐ │                   │
│                          │ │ - id         │ │                   │
│                          │ │ - owner      │ │                   │
│                          │ │ - entity     │ │                   │
│                          │ │ - type       │ │                   │
│                          │ │ - target     │ │                   │
│                          │ └──────────────┘ │                   │
│                          └──────────────────┘                   │
│                                                                   │
│  ┌────────────────────────────────────────────────────┐         │
│  │           ReanimateMCCommand                        │         │
│  │  ┌──────────────────────────────────────────────┐  │         │
│  │  │ /reanimatemc summon <type> [player]          │  │         │
│  │  │ /reanimatemc dismiss all                     │  │         │
│  │  │ /reanimatemc npcs                            │  │         │
│  │  └──────────────────────────────────────────────┘  │         │
│  └────────────────────────────────────────────────────┘         │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Summon Flow
```
Player executes command
    │
    ▼
ReanimateMCCommand validates
    │
    ├─► Check permission (reanimate.summon + reanimate.summon.use.<type>)
    ├─► Check cooldown (unless overridecost)
    └─► Check max summons limit
    │
    ▼
NPCSummonManager.summon()
    │
    ├─► Spawn IronGolem entity
    ├─► Create ReanimatorNPC data object
    ├─► Store in activeNPCs map
    ├─► Play ritual effects (particles + sound)
    └─► Return success
```

### 2. Behavior Update Flow (every 1 second)
```
BukkitRunnable timer tick
    │
    ▼
For each NPC in activeNPCs:
    │
    ├─► Is entity valid?
    │   ├─► No: Remove from map
    │   └─► Yes: Continue
    │
    ├─► Is owner online?
    │   ├─► No: Check timeout
    │   └─► Yes: Continue
    │
    ├─► Has target player?
    │   ├─► Yes: Pathfind to target
    │   │   └─► Close enough? Revive!
    │   └─► No: Follow owner
    │
    └─► Is Protector type?
        └─► Yes: Find & attack nearby hostile mobs
```

### 3. Revive Flow
```
NPC within 3 blocks of KO'd player
    │
    ▼
NPCSummonManager checks distance
    │
    ▼
Call KOManager.revive(target, owner)
    │
    ├─► Remove KO status
    ├─► Restore health
    ├─► Apply effects
    └─► Broadcast message
    │
    ▼
Spawn heart particles + enchantment sound
    │
    ▼
Clear target from NPC
    │
    ▼
NPC returns to following owner
```

### 4. Dismiss Flow
```
Player executes /reanimatemc dismiss all
    │
    ▼
ReanimateMCCommand validates ownership
    │
    ▼
NPCSummonManager.dismissAll()
    │
    ├─► For each NPC owned by player:
    │   ├─► Remove entity from world
    │   └─► Remove from activeNPCs map
    │
    └─► Clear from playerSummons map
```

## NPC Types Comparison

```
┌──────────────┬──────────┬──────────┬────────────┬──────────────┐
│ Type         │ Health   │ Follows  │ Auto-Revive│ Special      │
├──────────────┼──────────┼──────────┼────────────┼──────────────┤
│ GOLEM        │ 100 HP   │ Yes      │ Yes        │ Standard     │
│ HEALER       │ 100 HP   │ Yes      │ Yes        │ Specialized  │
│ PROTECTOR    │ 200 HP   │ Yes      │ Yes        │ Attacks Mobs │
└──────────────┴──────────┴──────────┴────────────┴──────────────┘
```

## Permission Hierarchy

```
reanimate.summon (base)
    │
    ├─► reanimate.summon.use.golem
    ├─► reanimate.summon.use.healer
    └─► reanimate.summon.use.protector
    │
    ├─► reanimate.summon.overridecost (bypass)
    └─► reanimate.summon.admin (full control)
```

## Configuration Impact

```yaml
npc_summon:
  enabled: true/false        ──► Enable/disable entire system
  max_summons_per_player: N  ──► Limit NPCs per player
  summon_cooldown: seconds   ──► Time between summons
  offline_timeout: seconds   ──► Auto-remove when owner offline
  require_item: true/false   ──► Require item to summon
  required_item: MATERIAL    ──► Which item required
```

## State Management

```
┌─────────────────────────────────────────────────────────────┐
│                    NPCSummonManager                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  activeNPCs: Map<UUID, ReanimatorNPC>                        │
│    └─► Key: NPC ID, Value: NPC data                          │
│                                                               │
│  playerSummons: Map<UUID, List<UUID>>                        │
│    └─► Key: Player UUID, Value: List of NPC IDs              │
│                                                               │
│  summonCooldowns: Map<UUID, Long>                            │
│    └─► Key: Player UUID, Value: Cooldown end timestamp       │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Lifecycle States

```
NPC Lifecycle:

    SPAWN
      │
      ▼
   ACTIVE ◄─────┐
      │         │
      │    (behavior loop)
      │         │
      ├─────────┘
      │
      ├─► Owner logs out ──► TIMEOUT (if > offline_timeout)
      ├─► Entity dies ──────► DEAD
      ├─► Dismiss command ──► DISMISSED
      └─► Server shutdown ──► CLEANUP
      
      │
      ▼
   REMOVED
```

## Integration Points

```
┌─────────────────────────────────────────────────────────┐
│                External Integrations                     │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  KOManager Interface                                     │
│    ├─► isKO(Player) - Check if player is knocked out    │
│    └─► revive(Player, Player) - Revive KO'd player      │
│                                                           │
│  Bukkit/Paper APIs                                       │
│    ├─► Pathfinder API - NPC movement                    │
│    ├─► Particle API - Visual effects                    │
│    ├─► Sound API - Audio effects                        │
│    └─► Entity API - Spawn/manage entities               │
│                                                           │
│  ReanimateMC Systems                                     │
│    ├─► Lang - Localized messages                        │
│    ├─► Config - Settings management                     │
│    └─► Permissions - Access control                     │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

## Performance Characteristics

```
Resource Usage:

Memory:
  - ~500 bytes per active NPC (data object)
  - ~100 bytes per player summon tracking
  - Minimal: 1-10 NPCs = <10 KB

CPU:
  - Single BukkitRunnable (1s interval)
  - O(n) iteration over active NPCs
  - Pathfinding: Bukkit native (optimized)
  - Minimal: 1-20 NPCs = negligible impact

Network:
  - Particle packets per summon (ritual)
  - Sound packets per summon/revive
  - Entity spawn/despawn packets
  - Minimal bandwidth impact
```

## Error Handling

```
Error Scenarios Handled:

1. Invalid Entity
   └─► Auto-remove from tracking maps

2. Owner Offline
   └─► Timeout → auto-remove after configured time

3. Permission Denied
   └─► Graceful message, no NPC spawn

4. Cooldown Active
   └─► Informative message with time remaining

5. Max Summons Reached
   └─► Clear error with current limit

6. Invalid NPC Type
   └─► Suggest valid types in error message

7. Target Not Found
   └─► NPC continues following owner

8. Pathfinding Failure
   └─► NPC stops, waits for next update cycle
```

## Extension Points

Future enhancements can add:

```
1. Custom Behaviors
   - Add new behavior methods in updateNPCBehavior()
   - Define new NPC types in ReanimatorType enum

2. Citizens Integration
   - Implement CitizensNPCAdapter
   - Fall back to current system if Citizens unavailable

3. Economy Costs
   - Add EconomyManager dependency
   - Charge on summon in NPCSummonManager.summon()

4. Custom Models
   - Hook ModelEngine/ItemsAdder
   - Replace spawnReanimatorEntity() entity type

5. Persistence
   - Add database/file storage
   - Load/save in onEnable()/onDisable()
```

## File Organization

```
ReanimateMC/
├── src/main/java/fr/jachou/reanimatemc/
│   ├── data/
│   │   └── ReanimatorNPC.java          ◄─── NPC data model
│   ├── managers/
│   │   ├── KOManager.java              ◄─── Existing (used)
│   │   └── NPCSummonManager.java       ◄─── New manager
│   ├── commands/
│   │   └── ReanimateMCCommand.java     ◄─── Modified
│   └── ReanimateMC.java                ◄─── Modified (init)
│
├── src/main/resources/
│   ├── plugin.yml                      ◄─── Permissions added
│   ├── config.yml                      ◄─── NPC settings added
│   └── lang/
│       ├── en.yml                      ◄─── Messages added
│       └── fr.yml                      ◄─── Messages added
│
└── docs/
    ├── NPC_SUMMON_MODULE.md            ◄─── Technical docs
    ├── NPC_SUMMON_TESTING.md           ◄─── Test guide
    ├── QUICK_START_NPC.md              ◄─── Quick start
    ├── NPC_IMPLEMENTATION_SUMMARY.md   ◄─── Summary
    └── README.md                       ◄─── Updated index
```

This architecture provides a clean, maintainable, and extensible foundation for the NPC summoning system.
