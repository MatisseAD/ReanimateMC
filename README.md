# ReanimateMC

New wiki page: https://matissead.github.io/ReanimateMC/

![Version](https://img.shields.io/badge/version-1.2.13-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4+-blue.svg)
![Paper](https://img.shields.io/badge/Paper-Required-orange.svg)

![ReanimateMC Cover](https://i.postimg.cc/3RHh8WJy/reanimate-mc-cover.jpg)

## Table of Contents
- [Overview](#overview)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Commands](#commands)
- [Permissions](#permissions)
- [K.O. System](#ko-system)
- [NPC Reanimator System](#npc-reanimator-system)
- [Self-Revive System](#self-revive-system)
- [Distress Signal](#distress-signal)
- [API for Developers](#api-for-developers)
- [Language Support](#language-support)
- [Compatibility](#compatibility)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Overview

ReanimateMC replaces instant death with a K.O. (Knockout) state. When a player's
health reaches zero they enter a downed state where teammates can revive them,
enemies can execute them, or a timer forces death. Version 1.2.13 adds a full
NPC Reanimator System, a configurable self-revive mechanic, and reworks the
distress signal for both Java Edition and Bedrock mobile.

---

## Requirements

- **Server:** Paper 1.21.4 or higher (Paper required for NPC pathfinder API and `EntityKnockbackEvent`)
- **Java:** 17 or higher
- **Optional:** Vault + economy plugin, PlaceholderAPI, LuckPerms

---

## Installation

1. Drop `ReanimateMC.jar` into `plugins/`.
2. Restart the server.
3. Edit `plugins/ReanimateMC/config.yml` or use `/rmc config` to open the GUI.

---

## Quick Start

```
/rmc config               open the in-game configuration GUI
/rmc reload               reload config and lang files without restart
/rmc help                 list all commands in the active language
/rmc summon golem         summon a GOLEM reanimator for yourself
/rmc summon healer        summon a HEALER reanimator for yourself
/rmc summon protector     summon a PROTECTOR reanimator for yourself
/rmc selfrevive           revive yourself while K.O.'d (uses items)
```

---

## Configuration

All settings live in `plugins/ReanimateMC/config.yml`. A `QUICK REFERENCE`
block at the top of the file explains effect levels, duration fields, and the
distress subsection. New keys are automatically merged into existing files on
startup and `/rmc reload`; your existing values are never overwritten.

### Key Sections

| Section | What it controls |
|---|---|
| `language` | Active lang file (`en`, `es`, `de`, `fr`, `it`, `nl`, `ru`, `zh`, `kr`, `pl`, `pt`) |
| `reanimation` | Item requirement, hold duration, HP restored, cooldown |
| `knockout` | Duration, movement lock, effects, surrender, mobs, distress |
| `execution` | Toggle, hold duration, broadcast |
| `effects_on_revive` | Temporary effects applied after revival (seconds) |
| `prone` | Crawl toggle, slowness level, auto-crawl, jump blocking |
| `self_revive` | Item list, channel time, cooldown, cancel conditions |
| `npc_summon` | Per-type HP, lifetime, cooldown, cost, heal amounts, revive speed |

---

## Commands

All commands run via `/rmc` or `/reanimatemc`.

### Player Commands

| Command | Permission | Description |
|---|---|---|
| `/rmc help` | — | List all commands in the active language |
| `/rmc status <player>` | `reanimatemc.status` | Check K.O. state |
| `/rmc crawl` | `reanimatemc.crawl` | Toggle crawl while K.O.'d |
| `/rmc distress` | `reanimatemc.distress` | Send a distress signal while K.O.'d |
| `/rmc selfrevive` | `reanimatemc.selfrevive` | Revive yourself using items |
| `/rmc cancelselfrevive` | `reanimatemc.selfrevive` | Cancel an active self-revive channel |
| `/rmc revive <player>` | `reanimatemc.revive` | Start a timed revive via your active HEALER NPC |
| `/rmc summon <type> [player]` | `reanimatemc.summon.use.<type>` | Summon a Reanimator NPC |
| `/rmc summon team <type> <p1> <p2>...` | `reanimatemc.summon.use.<type>` | Summon one NPC per team member |
| `/rmc dismiss <all\|golem\|healer\|protector>` | `reanimatemc.summon` | Dismiss NPC(s) |
| `/rmc extend <seconds>` | `reanimatemc.summon` | Extend active NPC lifetime |
| `/rmc npcs` | `reanimatemc.summon` | Show HP, countdown, and target for active NPCs |

Aliases: `/sr` = `/rmc selfrevive`, `/cancelsr` = `/rmc cancelselfrevive`.

### Admin Commands

| Command | Permission | Description |
|---|---|---|
| `/rmc reload` | `reanimatemc.admin` | Reload config and lang |
| `/rmc knockout <player>` | `reanimatemc.knockout` | Force K.O. |
| `/rmc kolist` | `reanimatemc.admin` | List K.O.'d players with time remaining |
| `/rmc purge` | `reanimatemc.admin` | Remove stale K.O. holograms from all worlds |
| `/rmc info` | `reanimatemc.admin` | Show plugin status and integration state |
| `/rmc version` | `reanimatemc.admin` | Show plugin version |
| `/rmc config` | `reanimatemc.config` | Open configuration GUI |
| `/rmc removeGlowingEffect <player>` | `reanimatemc.removeglow` | Remove glow from a player |

---

## Permissions

### Core

| Permission | Default | Description |
|---|---|---|
| `reanimatemc.revive` | true | Revive K.O.'d players |
| `reanimatemc.execute` | true | Execute K.O.'d players |
| `reanimatemc.status` | true | Check K.O. status |
| `reanimatemc.crawl` | true | Toggle crawl while K.O.'d |
| `reanimatemc.distress` | true | Send distress signals |
| `reanimatemc.selfrevive` | true | Self-revive while K.O.'d |
| `reanimatemc.bypass` | op | Die instantly, bypass K.O. |
| `reanimatemc.knockout` | op | Force K.O. via command |
| `reanimatemc.admin` | op | All admin commands |

### NPC Summon

`reanimatemc.summon` and all `summon.use.*` nodes default to `false`. Grant
explicitly via a permission plugin. OPs always have access.

| Permission | Default |
|---|---|
| `reanimatemc.summon` | false |
| `reanimatemc.summon.use.golem` | false |
| `reanimatemc.summon.use.healer` | false |
| `reanimatemc.summon.use.protector` | false |
| `reanimatemc.summon.overridecost` | op |
| `reanimatemc.summon.admin` | op |

**Recommended tier setup:**

```yaml
# VIP
reanimatemc.summon: true
reanimatemc.summon.use.golem: true

# Premium
reanimatemc.summon: true
reanimatemc.summon.use.golem: true
reanimatemc.summon.use.healer: true

# Elite
reanimatemc.summon: true
reanimatemc.summon.use.golem: true
reanimatemc.summon.use.healer: true
reanimatemc.summon.use.protector: true
```

**LuckPerms lifetime overrides** — grant
`reanimatemc.summon.lifetime.<type>.<seconds>` to override NPC lifetime per group.
The highest value across all of a player's permissions wins.

---

## K.O. System

When a player's health reaches zero:

1. Player enters K.O. state; health is set to 1.
2. Player renders horizontal (prone animation) on both Java and Bedrock.
3. Movement and jumping are blocked server-side (`movement_disabled: true`).
4. Countdown begins (`knockout.duration_seconds`).
5. Player can be revived, execute themselves (surrender), or die when the timer expires.

### Revival

A player reviving a teammate must crouch near them while holding the required
item (`reanimation.required_item`). A progress bar appears in the action bar.
Items are consumed on success. Post-revival effects (`effects_on_revive`) apply.

When using `/rmc revive <player>`:

- The caller must have an active HEALER NPC.
- The HEALER teleports to the target before the channel begins.
- If `reanimation.require_special_item` is enabled, the caller must hold the item.
- The target receives a notification naming the reviver.

### Surrender

Hold crouch for `knockout.suicide_hold_seconds` (default 3) to surrender.
Any movement cancels the timer if `surrender_require_still` is enabled.

To cancel on Bedrock (where sneak-up may not fire in prone state):
- Press Q (drop button) — primary method
- Tap empty space (RIGHT_CLICK_AIR with empty hand)
- Any arm-swing gesture

Cancelling a surrender automatically sends a distress signal once.

### Crawl Mode

Toggle with `/rmc crawl`. Allows slow horizontal movement while K.O.'d.
Configurable slowness level (`prone.crawl_slowness_level`). Jump blocking
is separate (`prone.crawl_allow_jump`, default `false`). Nausea applies only
while crawl is active (`knockout.crawl_nausea_enabled`).

### Mob Protection

When `knockout.mobs_attack_ko: false`, mobs cannot:

- Select a K.O.'d player as a new target (`EntityTargetEvent`).
- Deal melee damage to a K.O.'d player.
- Hit a K.O.'d player with a projectile.

Mobs that were already targeting the player before K.O. have their target cleared.

---

## NPC Reanimator System

### Summoning

```
/rmc summon golem [player]
/rmc summon healer [player]
/rmc summon protector
/rmc summon team <type> <p1> <p2> ...
```

When `[player]` is specified the NPC belongs to that player; the summoner pays
the cost and cooldown. Team summon charges `summon_cost * team_size` via Vault.

### Type Comparison

| Feature | GOLEM | HEALER | PROTECTOR |
|---|:---:|:---:|:---:|
| Default HP | 80 | 120 | 200 |
| Revives owner when K.O. | Yes | Yes | When no HEALER active |
| Revives explicit target | Yes | Yes | No |
| Auto-revives nearby K.O. allies | No | Yes | No |
| Heals owner / allies | No | Yes | No |
| Heals self / allied golems | No | Configurable | No |
| Circular heal aura particles | No | Yes | No |
| Defends owner (any attacker type) | Yes | No | Yes |
| Absorbs owner damage | No | No | 75% (configurable) |
| Sends distress on owner K.O. | No | No | Yes |
| Default revive time | ~5 s | ~3 s | ~8 s |
| Default summon cooldown | 5 min | 5 min | 1 min |

All HP values are configurable via `npc_summon.<type>.max_hp`.
All revive durations are configurable via `npc_summon.<type>.revive_duration_ticks`.

### GOLEM — Standard Reanimator

Follows the owner, revives them when K.O.'d, and defends against attackers.
Targets whoever last dealt damage to the owner (any entity type) and falls back
to the nearest hostile mob within `combat_radius`.

### HEALER — Support Reanimator

All GOLEM capabilities plus:

- Scans `scan_radius` blocks for any K.O.'d ally and rushes to the nearest one
  without manual assignment.
- Heals HP every `periodic_heal_interval` seconds to the owner, nearby allies,
  itself (`heal_self`), and allied Iron Golems (`heal_golems`). All amounts and
  ranges are configurable.
- Circular HEART + HAPPY_VILLAGER particle aura on every heal tick, always
  visible regardless of current HP so players can see the range.
- Applies Regeneration when the owner HP drops below `aura_hp_threshold`.
- Grants bonus HP to the revived player (`bonus_hp_on_revive`).
- The HEALER does not leave the owner more than `max_leash_distance` blocks away
  even when chasing a K.O.'d ally.

### PROTECTOR — Tank Reanimator

- Cannot revive or heal anyone except as a fallback when no HEALER is active
  (`revive_if_no_healer: true`).
- Intercepts `damage_transfer_ratio` (default 75%) of every hit the owner takes
  and applies it to the golem via `entity.damage()`. CRIT particles fire on impact.
- Attacks any `LivingEntity` threatening the owner. Allied Iron Golems are
  excluded from targeting.
- When the owner falls K.O., sends a forced distress signal and notifies the
  owner once per K.O. session that it cannot revive them.
- Dies normally at 0 HP; record is cleaned up by `NPCDamageListener`.

### Revive Progress Bar

```
Healing Golem reviving...  ██████░░░░  65%    <- shown to K.O.'d player
Reviving PlayerName...     ██████░░░░  65%    <- shown to NPC owner
```

### Nameplate Format

```
✦ Iron Golem Reanimator [PlayerName] | ❤ 67 | ⏱ 8m32s
```

HP color shifts green (> 50%) → yellow (> 25%) → red.
Nameplate updates every 2 seconds.

### Persistence

`NPCPersistenceManager` saves an absolute expiry timestamp for each active NPC
on shutdown. On startup, entries whose timestamp has passed are discarded;
survivors are restored with their correct remaining lifetime. Golems that
expire while the owner is offline are never restored.

### Status and Management

```
/rmc npcs                      show HP bar, time, and target for all golems
/rmc dismiss all               dismiss all active NPCs
/rmc dismiss healer            dismiss only HEALERs
/rmc extend 300                add 5 minutes (Vault cost if configured)
```

---

## Self-Revive System

KO'd players can revive themselves using items at the cost of a longer channel
time than a teammate revive.

### Flow

1. Player runs `/rmc selfrevive` (or `/sr`).
2. All preconditions are validated; failure sends a specific message.
3. A progress bar appears in the action bar for `duration_ticks` ticks.
4. On completion: items consumed, `revive()` called, `effects_on_selfrevive` applied.
5. Any movement or incoming damage cancels the channel (if configured).

### Configuration Reference

```yaml
self_revive:
  enabled: true
  require_items: true
  required_items:
    - material: GOLDEN_APPLE
      amount: 2
  duration_ticks: 200          # ~10 seconds
  health_restored: 2
  cooldown_seconds: 120
  max_uses_per_ko: 1            # 0 = unlimited
  cancel_on_move: true
  cancel_on_damage: true
  combat_block_seconds: 0       # 0 = disabled
  effects_on_selfrevive:
    nausea: 10
    slowness: 15
    resistance: 5
```

---

## Distress Signal

When sent, broadcasts the player's coordinates to all online players and places
a glowing marker at their location. Respects `distress.cooldown_seconds` and
`reanimatemc.distress` permission.

### Triggers

| Method | Platform | Config key |
|---|---|---|
| F key (swap hands) | Java | always active |
| Q / drop button | Java + Bedrock | `distress.drop_key_trigger` |
| Double-tap sneak | Bedrock + Java | `distress.bedrock_doubletap_ms` |
| Cancelling surrender | All | automatic |
| `/rmc distress` | All | pre-added to `allowed_commands` |

The PROTECTOR NPC sends a forced distress signal (bypassing the player cooldown)
the first time the owner falls K.O. in a session.

---

## API for Developers

### Events

```java
// Fired when a player enters K.O. state — cancellable
@EventHandler
public void onPlayerKO(PlayerKOEvent event) {
    Player player = event.getPlayer();
    event.setCancelled(true);
}

// Fired when a player is revived
@EventHandler
public void onRevived(PlayerReanimatedEvent event) {
    Player player     = event.getPlayer();
    Player reanimator = event.getReanimator();
}

// Fired when an NPC is summoned — cancellable
@EventHandler
public void onNPCSummoned(NPCSummonedEvent event) {
    ReanimatorNPC npc = event.getNPC();
    event.setCancelled(true);
}

// Fired when an NPC is removed
@EventHandler
public void onNPCDismissed(NPCDismissedEvent event) {
    ReanimatorNPC npc            = event.getNPC();
    NPCDismissedEvent.Reason why = event.getReason(); // MANUAL, EXPIRED, OFFLINE_TIMEOUT, PLUGIN_DISABLE
}
```

### Programmatic Access

```java
ReanimateMC plugin = (ReanimateMC) Bukkit.getPluginManager().getPlugin("ReanimateMC");

KOManager ko = plugin.getKoManager();
ko.isKO(player);
ko.setKO(player);
ko.revive(player, reviver);
ko.sendDistress(player);
ko.startSelfRevive(player);

NPCSummonManager npc = plugin.getNpcSummonManager();
npc.summon(player, ReanimatorNPC.ReanimatorType.HEALER, null);
npc.dismissAll(player);
npc.getPlayerSummons(player);
```

### PlaceholderAPI

| Placeholder | Returns |
|---|---|
| `%reanimatemc_is_ko%` | `true` / `false` |
| `%reanimatemc_ko_time_remaining%` | seconds remaining |
| `%reanimatemc_npc_count%` | number of active NPCs |
| `%reanimatemc_npc_type%` | type name of first active NPC |
| `%reanimatemc_npc_time_remaining%` | NPC lifetime remaining |
| `%reanimatemc_npc_hp%` | NPC current HP |

### Dependency

`plugin.yml`:
```yaml
softdepend: [ReanimateMC]
```

---

## Language Support

10 languages ship with the plugin. All new keys in v1.2.13 are present in every
file. `en.yml` and `es.yml` have full translations; other languages have English
fallback strings marked for community translation.

Change language: set `language: "es"` in `config.yml` and run `/rmc reload`.
Custom translations: copy `lang/en.yml`, rename, translate, set the file name
(without extension) as the `language` value.

---

## Compatibility

| Software | Status |
|---|---|
| Paper 1.21.4+ | Full support (recommended) |
| Spigot 1.21+ | Supported; NPC pathfinding limited |
| Purpur | Compatible |

Paper is required for `io.papermc.paper.event.entity.EntityKnockbackEvent`
and the Pathfinder API used by NPC navigation.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Version shows `${project.version}` | Old JAR built without filtering | Rebuild with this PR's `pom.xml` |
| Mobs still attack K.O.'d players | Mobs targeted player before K.O. | Set `mobs_attack_ko: false`; confirmed to clear existing targets |
| Bedrock ride icon on K.O. | Old JAR with ArmorStand mount | Upgrade to 1.2.13 |
| Self-revive says unknown command | Old JAR without `plugin.yml` registration | Upgrade to 1.2.13 |
| PROTECTOR doesn't die | Old `setHealth(0.5)` floor | Upgrade to 1.2.13 |
| New config keys missing | Config predates 1.2.13 | Run `/rmc reload`; keys merge automatically |
| HEALER not healing | Heal aura visible but no HP change? Player is at max HP. Check `periodic_heal_amount` | Increase `periodic_heal_amount` |
| NPC restored with wrong lifetime | Old persistence format stored relative seconds | Upgrade to 1.2.13; delete `npc_data.yml` once |

---

## Contributing

- Bug reports: [GitHub Issues](https://github.com/MatisseAD/ReanimateMC/issues)
- Lang improvements: submit updated `lang/<code>.yml` via pull request
- Code contributions: follow existing package structure; one behavior class per
  NPC type; no logic in listener classes beyond event routing

---

## Credits

**Author:** Jachou
**License:** Proprietary — all rights reserved
**Wiki:** https://matissead.github.io/ReanimateMC/

<img src="https://bstats.org/signatures/bukkit/ReanimateMC.svg" alt="BStats">
