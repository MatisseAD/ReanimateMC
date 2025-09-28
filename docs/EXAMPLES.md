# ReanimateMC Configuration Examples

This document provides practical configuration examples for different server types and use cases.

## Table of Contents
- [Server Type Examples](#server-type-examples)
- [Scenario-Based Configurations](#scenario-based-configurations)
- [Integration Examples](#integration-examples)
- [Custom Setups](#custom-setups)
- [Permission Examples](#permission-examples)

## Server Type Examples

### Hardcore Survival Server
**Goal:** Maintain difficulty while adding teamwork element

```yaml
language: "en"
setup_completed: true

reanimation:
  require_special_item: true
  required_item: TOTEM_OF_UNDYING    # Expensive revival item
  duration_ticks: 40                 # Fast revival (2 seconds)
  health_restored: 2                 # Minimal health restored
  cooldown: 300                      # 5-minute cooldown
  revive_cooldown: 600               # 10-minute personal cooldown

knockout:
  enabled: true
  duration_seconds: 20               # Short time to be rescued
  movement_disabled: true            # No movement
  use_particles: false               # Less visual noise
  heartbeat_sound: true              # Audio tension
  blindness: true                    # Full immersion
  suicide_hold_seconds: 10           # Prevent accidental suicide
  weakness_level: 3                  # Heavy debuff
  fatigue_level: 2

execution:
  enabled: true
  hold_duration_ticks: 20            # Quick execution (1 second)
  message_broadcast: false           # No broadcast for stealth

effects_on_revive:
  nausea: 15                         # Long recovery time
  slowness: 30
  resistance: 5                      # Minimal protection

prone:
  enabled: false                     # No crawling for maximum challenge

looting:
  enabled: true                      # Can loot KO'd players

tablist:
  enabled: true                      # Show KO status
```

### Casual Multiplayer Server
**Goal:** Fun mechanics without high stakes

```yaml
language: "en"
setup_completed: true

reanimation:
  require_special_item: true
  required_item: GOLDEN_APPLE        # Moderate cost
  duration_ticks: 100                # Standard revival time
  health_restored: 8                 # Good health restoration
  cooldown: 60                       # 1-minute cooldown
  revive_cooldown: 120               # 2-minute personal cooldown

knockout:
  enabled: true
  duration_seconds: 45               # Plenty of time for rescue
  movement_disabled: false           # Allow some movement
  use_particles: true                # Fun visual effects
  heartbeat_sound: true
  blindness: false                   # Keep visibility
  suicide_hold_seconds: 5
  weakness_level: 1                  # Light debuff
  fatigue_level: 0                   # No mining penalty

execution:
  enabled: true
  hold_duration_ticks: 60            # Slower execution (3 seconds)
  message_broadcast: true            # Announce executions

effects_on_revive:
  nausea: 5                          # Short recovery
  slowness: 10
  resistance: 15                     # Good protection after revival

prone:
  enabled: true
  allow_crawl: true                  # Allow strategic movement
  crawl_slowness_level: 3            # Moderate slowness
  auto_crawl: true                   # Automatic crawling

looting:
  enabled: false                     # Protect inventories

tablist:
  enabled: true
```

### PvP Arena Server
**Goal:** Fast-paced competitive gameplay

```yaml
language: "en"
setup_completed: true

reanimation:
  require_special_item: false        # No item required - speed priority
  duration_ticks: 40                 # Quick revival
  health_restored: 10                # Full health restoration
  cooldown: 30                       # Short cooldown
  revive_cooldown: 30

knockout:
  enabled: true
  duration_seconds: 15               # Short KO duration
  movement_disabled: true
  use_particles: true                # Visual feedback
  heartbeat_sound: false             # Reduce audio clutter
  blindness: false                   # Keep tactical awareness
  suicide_hold_seconds: 2            # Quick suicide option
  weakness_level: 0                  # No debuffs for competitive play
  fatigue_level: 0

execution:
  enabled: true
  hold_duration_ticks: 20            # Fast execution
  message_broadcast: true            # Competitive feedback

effects_on_revive:
  nausea: 0                          # No negative effects
  slowness: 0
  resistance: 5                      # Brief protection

prone:
  enabled: false                     # No crawling for speed

looting:
  enabled: true                      # Competitive looting

tablist:
  enabled: true
```

### Roleplay Server
**Goal:** Dramatic, immersive experience

```yaml
language: "en"
setup_completed: true

reanimation:
  require_special_item: true
  required_item: GOLDEN_APPLE
  duration_ticks: 200                # Long, dramatic revival (10 seconds)
  health_restored: 4                 # Realistic recovery
  cooldown: 180                      # 3-minute cooldown
  revive_cooldown: 300               # 5-minute personal cooldown

knockout:
  enabled: true
  duration_seconds: 90               # Long time for roleplay scenes
  movement_disabled: false
  use_particles: true                # Atmospheric effects
  heartbeat_sound: true              # Dramatic audio
  blindness: true                    # Immersive limitation
  suicide_hold_seconds: 15           # Prevent accidental suicide during RP
  weakness_level: 2
  fatigue_level: 1

execution:
  enabled: true
  hold_duration_ticks: 100           # Slow, dramatic execution (5 seconds)
  message_broadcast: true            # Dramatic announcements

effects_on_revive:
  nausea: 20                         # Long recovery for realism
  slowness: 30
  resistance: 30                     # Extended protection

prone:
  enabled: true
  allow_crawl: true
  crawl_slowness_level: 5            # Very slow crawling
  auto_crawl: false                  # Manual control for RP

looting:
  enabled: false                     # Protect roleplay items

tablist:
  enabled: true
```

## Scenario-Based Configurations

### Medical-Themed Server
```yaml
reanimation:
  require_special_item: true
  required_item: POTION              # Medical supplies
  duration_ticks: 150                # Medical procedure time
  health_restored: 6

knockout:
  duration_seconds: 60               # Time for medical response
  heartbeat_sound: true              # Medical monitoring
  weakness_level: 2                  # Post-trauma effects

effects_on_revive:
  nausea: 10                         # Side effects of treatment
  slowness: 20                       # Recovery time
  resistance: 25                     # Medical protection
```

### Military/Tactical Server
```yaml
reanimation:
  require_special_item: true
  required_item: IRON_INGOT          # Medical kit
  duration_ticks: 60                 # Quick field medicine
  cooldown: 240                      # Limited medical supplies

knockout:
  duration_seconds: 30               # Battlefield conditions
  movement_disabled: true            # Wounded soldier
  blindness: false                   # Maintain tactical awareness
  
execution:
  hold_duration_ticks: 40            # Military efficiency
  message_broadcast: false           # Stealth operations

prone:
  enabled: true
  allow_crawl: true                  # Tactical movement
  auto_crawl: false                  # Manual control
```

### Fantasy/Magic Server
```yaml
reanimation:
  require_special_item: true
  required_item: NETHER_STAR         # Magical reagent
  duration_ticks: 120                # Spell casting time
  health_restored: 10                # Magical healing

knockout:
  duration_seconds: 45
  use_particles: true                # Magical effects
  heartbeat_sound: false             # Mystical silence

effects_on_revive:
  nausea: 0                          # Magic has no side effects
  slowness: 5                        # Brief magical disorientation
  resistance: 20                     # Magical protection
```

## Integration Examples

### WorldGuard Integration
```yaml
# Hospital zone - enhanced healing
hospital_region:
  reanimation:
    duration_ticks: 60               # Faster in hospital
    health_restored: 10              # Better healing
    require_special_item: false      # Medical equipment available

# PvP zone - harsher conditions  
pvp_region:
  knockout:
    duration_seconds: 15             # Faster pace
  execution:
    hold_duration_ticks: 20          # Quick elimination
```

### Economy Integration
```yaml
reanimation:
  require_special_item: true
  required_item: EMERALD             # Currency as medical cost
  
knockout:
  duration_seconds: 30
  # Could integrate with economy plugins for revival costs
```

### Custom Items Integration
```yaml
reanimation:
  required_item: CUSTOM_MEDICAL_KIT  # From ItemsAdder/OraxenItems
  duration_ticks: 80
```

## Custom Setups

### Team-Based Setup
**Multiple teams with different revival items:**

```yaml
# Configure via permissions or regions
team_red:
  reanimation:
    required_item: RED_DYE
    
team_blue:
  reanimation:
    required_item: BLUE_DYE
```

### Progressive Difficulty
**Different areas with increasing difficulty:**

```yaml
# Spawn area - easy
spawn_zone:
  knockout:
    duration_seconds: 60
  reanimation:
    required_item: BREAD

# Mid-game area - moderate
adventure_zone:
  knockout:
    duration_seconds: 30
  reanimation:
    required_item: GOLDEN_APPLE

# End-game area - hard
endgame_zone:
  knockout:
    duration_seconds: 15
  reanimation:
    required_item: TOTEM_OF_UNDYING
```

### Event-Specific Configuration
```yaml
# Special events configuration
raid_event:
  knockout:
    duration_seconds: 20             # Fast-paced raids
    weakness_level: 0                # No debuffs during events
  
  execution:
    enabled: false                   # No permanent deaths during events
    
peaceful_event:
  knockout:
    enabled: false                   # No KO during peaceful events
```

## Permission Examples

### Medic Role
```yaml
# LuckPerms commands for medic role
permissions:
  - reanimatemc.revive
  - reanimatemc.status
  - reanimatemc.removeGlowingEffect
  
# Special medic abilities
group_permissions:
  medic:
    - faster_revival: true           # Custom integration
    - no_revival_cooldown: true
```

### VIP Players
```yaml
vip_permissions:
  - reanimatemc.bypass               # Can choose to skip KO
  - reanimatemc.no_execution         # Cannot be executed
  - reanimatemc.fast_revive          # Faster revival times
```

### Staff Permissions
```yaml
admin_permissions:
  - reanimatemc.admin
  - reanimatemc.config               # GUI access
  - reanimatemc.knockout             # Force KO players
  - reanimatemc.force_revive         # Bypass requirements

moderator_permissions:
  - reanimatemc.knockout
  - reanimatemc.status
  - reanimatemc.removeGlowingEffect
  - reanimatemc.loot
```

## Language Customization Examples

### Custom Messages
```yaml
# Custom language file (custom.yml)
ko_set: "You have been critically wounded!"
revived: "A medic has stabilized your condition!"
execution_broadcast: "%player% has been eliminated from the battlefield!"

# Medical-themed messages
revival_start: "Medical treatment in progress..."
revive_progress: "Treatment: %percent% complete"
```

### Multi-Language Server
```yaml
# Different languages for different regions
spawn_area:
  language: "en"
  
french_district:
  language: "fr"
  
spanish_quarter:
  language: "es"
```

## Testing Configurations

### Development Setup
```yaml
# Quick testing configuration
knockout:
  duration_seconds: 5                # Quick tests
  suicide_hold_seconds: 1            # Fast suicide for testing

reanimation:
  duration_ticks: 20                 # Fast revival
  cooldown: 5                        # Short cooldowns
  require_special_item: false        # No item needed for testing
```

### Debug Configuration
```yaml
# Enhanced logging and feedback
knockout:
  use_particles: true                # Visual confirmation
  heartbeat_sound: true              # Audio feedback
  
tablist:
  enabled: true                      # Status visibility

# Enable all features for comprehensive testing
execution:
  enabled: true
prone:
  enabled: true
  allow_crawl: true
looting:
  enabled: true
```

## Performance Optimization Examples

### High Population Server
```yaml
# Optimized for many players
knockout:
  use_particles: false               # Reduce particle load
  heartbeat_sound: false             # Reduce audio processing
  
# Shorter durations to reduce active KO players
knockout:
  duration_seconds: 20
  
# Reduce revival times to clear KO state faster
reanimation:
  duration_ticks: 60
```

### Low-Resource Server
```yaml
# Minimal resource usage
knockout:
  use_particles: false
  heartbeat_sound: false
  blindness: false                   # Reduce effect processing

prone:
  enabled: false                     # Disable complex movement system

# Disable optional features
looting:
  enabled: false
tablist:
  enabled: false
```

These examples should provide a solid foundation for configuring ReanimateMC for various server types and scenarios. Mix and match elements from different examples to create the perfect setup for your specific needs.