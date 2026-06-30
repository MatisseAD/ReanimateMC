# Changelog

All notable changes to ReanimateMC are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.2.13] — Unreleased

### Bug Fixes

#### Build

- `plugin.yml` used `${project.version}` as a literal string because Maven resource
  filtering was not enabled. Added `<resources><resource><filtering>true</filtering>`
  to `pom.xml`; the JAR now shows the real version in all log messages and
  `/rmc version` output.

#### K.O. System — Movement & Input

- **ArmorStand mount removed.** The previous implementation seated KO'd players
  on an invisible `ArmorStand` to prevent movement. On Bedrock Edition, any
  passenger relationship shows the dismount UI and maps the crouch button to
  "dismount" rather than "crouch", which caused immediate auto-cancellation of
  every surrender attempt and displayed a ride icon the player never requested.
  The mount is replaced by `event.setTo()` cancellation in `PlayerMoveEvent`,
  which blocks horizontal movement and vertical jumps server-side with no
  client-side side-effects on either platform.

- **Prone animation.** `player.setSwimming(true)` is called on KO entry
  regardless of crawl state, so the player renders horizontal ("lying down")
  on both Java Edition and Bedrock Edition.

- **Jump blocking in crawl mode.** When `prone.allow_crawl` is enabled,
  horizontal movement is permitted but upward velocity is cancelled unless
  `prone.crawl_allow_jump` is set to `true` (new config key, default `false`).

- **Surrender cancel while crawling.** The surrender timer was not cancelled by
  movement when crawl mode was active because the movement handler skipped the
  cancel check in the crawl branch. The check is now present in all three
  movement branches (full-lock, movement-allowed, crawl).

- **Surrender cancel message always shown.** The 3-second throttle previously
  covered both the cancel message and the distress signal, so rapid triggers
  silenced the message. The message is now sent unconditionally on every
  cancel; only the distress signal is throttled.

- **Repeated sneak-down events in crawl mode.** The swim state generated
  repeated `PlayerToggleSneakEvent(isSneaking=true)` events while crawling,
  restarting the surrender timer repeatedly. The handler now ignores sneak-down
  events when a surrender task is already running.

- **Bedrock surrender cancel.** On Bedrock, `sneak-up` does not fire while
  the player is in swim-crawl pose because the crouch button is also the
  "stop crawling" input. Three additional cancel triggers were added:
    - `PlayerDropItemEvent` (Q / drop button) — primary Bedrock cancel method;
      always fires reliably via Geyser and is the most natural action for a
      downed Bedrock player.
    - `PlayerInteractEvent(RIGHT_CLICK_AIR)` with empty main hand — tap on empty
      space.
    - `PlayerAnimationEvent(ARM_SWING)` — any tap gesture; does not send
      a distress signal to avoid noise.

#### K.O. System — Mob Targeting

- `KOProtectionListener.onEntityTarget` prevented mobs from retargeting a KO'd
  player but did not stop mobs that already had the player targeted before
  they went down. Added `EntityDamageByEntityEvent` handler that cancels
  melee damage from any `Mob` and projectile damage where the shooter is a
  `Mob`, then clears the mob's target in both cases.

#### NPC System

- **PROTECTOR immortal at 0 HP.** `applyTransferredDamage` floored health at
  0.5 using `Math.max(0.5, hp - absorbed)`. Changed to `entity.damage(absorbed)`
  so Bukkit processes the death event normally and `NPCDamageListener` handles
  cleanup.

- **Self-damage when stuck.** Iron Golems deal ENTITY_ATTACK damage to
  themselves when pathfinding traps them against a wall. `NPCDamageListener`
  now cancels any `EntityDamageByEntityEvent` where the damager UUID matches
  the victim (the golem hitting itself) and cancels friendly-fire between
  allied golems.

- **`ConcurrentModificationException` in behavior task.** The scheduler task
  iterated `activeNPCs.entrySet()` directly while `removeNPC()` mutated the
  map. Replaced with a snapshot copy before the loop; expired entries are
  collected in a separate list and removed after iteration completes.

- **`IllegalArgumentException: x not finite` in `ProtectorBehavior`.** The
  knockback burst called `normalize()` on a zero-length vector when an entity
  occupied the exact same XZ position as the golem. A guard now skips any
  entity where both X and Z delta are zero.

- **PROTECTOR reviving owner.** `updateBehavior()` applied the owner-KO revive
  path to all NPC types unconditionally. Added `canReviveOwner()` to
  `NPCBehavior`; PROTECTOR returns `false`. The path is now gated behind this
  check and also behind a live check for an active HEALER NPC when
  `protector.revive_if_no_healer` is enabled.

- **NPC persistence ignoring elapsed offline time.** `NPCPersistenceManager`
  saved remaining seconds at shutdown time, then restored that same value
  on startup regardless of how long the server was down. Changed to save
  absolute expiry timestamps (epoch ms). On load, entries whose timestamp
  has already passed are discarded; survivors have their remaining seconds
  computed from the delta between the timestamp and `System.currentTimeMillis()`.

- **HEALER heal timer unreliable.** Heal interval was checked against the raw
  `behaviorTick` counter (increments every second). If the interval was not
  a multiple of the idle-tick cadence (3 s), the heal could be skipped
  indefinitely. Fixed with a dedicated `idleTick` counter incremented only
  inside `onIdleTick()`.

- **HEALER particles invisible at full HP.** Particles were gated behind
  `health < max`. Particles now always spawn on each heal tick so players
  can see the aura range.

- **`EntityKnockbackEvent` wrong package.** Used Bukkit's deprecated
  `org.bukkit.event.entity.EntityKnockbackEvent` which has no `setKnockback()`.
  Changed to `io.papermc.paper.event.entity.EntityKnockbackEvent`.

- **`PlayerJumpEvent` does not exist in Paper 1.21.4.** Handler removed;
  Y-velocity check in `PlayerMoveEvent` covers both Java and Bedrock jump
  detection.

- **`api-version: 1.20` rejected by Paper 1.21+.** Corrected to `1.21`.

- **`GolemManager` random type and wrong summon target.** Shift-click on a
  natural Iron Golem picked a random type and passed the owner as the revive
  target (which was never KO'd). Now resolves the highest-tier type the
  player is permitted and passes `null` as the explicit target.

- **Permission namespace typo** `reanimate.summon` → `reanimatemc.summon`
  throughout `NPCSummonManager` and `ReanimateMCCommand`.

- **Duplicate `ReanimateMCCommand` instance.** `setExecutor` and
  `setTabCompleter` each constructed a new instance. A single shared instance
  is now registered for both.

- **Config new keys not merged into existing files.** Added
  `copyDefaults(true)` + `saveConfig()` on startup and `/rmc reload`.

- **Lang new keys not merged into existing translations.** `Lang.java`
  rewritten to use `setDefaults` + `copyDefaults(true)`; falls back to `en`
  for any missing key; never overwrites existing translations.

- **Duplicate `config_reloaded` key** in `kr.yml` and `pt.yml`. First
  duplicate removed.

- **Paper 1.21.4 particle renames.**
  `VILLAGER_HAPPY` → `HAPPY_VILLAGER`,
  `SMOKE_LARGE` → `LARGE_SMOKE`,
  `EXPLOSION_LARGE` → `EXPLOSION_EMITTER`.

- **`reanimatemc.config` and `reanimatemc.removeglow` missing from
  `plugin.yml`.** Both nodes added.

#### Commands

- **`removeGlowingEffect` command not found.** The switch case matched
  `removeglowingreffect` (double `r`) but the tab-complete list contained
  `removeGlowingEffect`, so the command was never routed correctly.
  All three variants now match in both the switch and the tab-complete list.

- **`/selfrevive` reported as unknown command.** The subcommand was handled
  inside the `reanimatemc` switch but not registered as a standalone command
  in `plugin.yml`. Added `selfrevive` and `cancelselfrevive` as first-class
  commands with `sr` / `cancelsr` aliases; both are registered in
  `ReanimateMC.java` with the shared `commandHandler`.

### Added

#### NPC Reanimator System

A full autonomous NPC system built on Iron Golems. Three types share a common
`NPCBehavior` strategy interface so each type is fully isolated.

##### GOLEM — Standard Reanimator

Follows owner, revives on K.O., defends from any attacker (not limited to
`instanceof Monster`).

Default HP: 80. Default revive time: ~5 s (100 ticks).

##### HEALER — Support Reanimator

All GOLEM capabilities plus: auto-scans for nearby K.O.'d allies within
`scan_radius` and rushes to the nearest one without manual assignment.
Periodic HP restoration every `periodic_heal_interval` seconds to the owner,
nearby allies, itself (`heal_self`), and allied golems (`heal_golems`).
Circular HEART + HAPPY_VILLAGER particle aura on each heal tick (configurable
radius and toggle). Regeneration potion when owner HP drops below
`aura_hp_threshold`. Bonus HP granted on revive.

Default HP: 120. Default revive time: ~3 s (60 ticks — fastest).

##### PROTECTOR — Tank Reanimator

Does not revive or heal. Intercepts 75% of every hit the owner takes
(`damage_transfer_ratio`, configurable) and applies that damage to the
golem's own HP via `entity.damage()`. Attacks any `LivingEntity` threatening
the owner; allied Iron Golems are excluded from targeting. When the owner
falls K.O., sends a forced distress signal on their behalf and notifies them
once per session that the PROTECTOR cannot revive. Optionally revives the
owner when no HEALER is active (`revive_if_no_healer`, default `true`).

Default HP: 200. Default revive time when fallback-reviving: ~8 s (160 ticks).

##### Shared NPC Features

- All HP values configurable per type via `npc_summon.<type>.max_hp`.
- Per-type summon cost (`summon_cost`) deducted via Vault on summon.
- Per-type cooldown and lifetime.
- LuckPerms lifetime overrides via `reanimatemc.summon.lifetime.<type>.<seconds>`.
- Timed revive with live action-bar progress bar (10-segment, percentage) shown
  to both the KO'd player and the NPC owner.
- Nameplate shows owner name: `✦ Healing Golem [PlayerName] | ❤ 87 | ⏱ 8m32s`.
  Color shifts green → yellow → red by HP percentage.
- Stuck detection: if the NPC does not move more than 0.5 blocks in
  `stuck_ticks_threshold` ticks while following, it teleports next to the owner
  with PORTAL particles.
- Persistence across restarts via `NPCPersistenceManager` using absolute expiry
  timestamps.
- `/rmc summon <type> [player]` — when `[player]` is specified the NPC belongs
  to that player; the summoner pays the cost and cooldown.
- `/rmc summon team <type> <p1> <p2> ...` — summons one NPC per team member;
  total cost = `summon_cost * team.size()`.
- `/rmc dismiss <all|golem|healer|protector>` — dismiss by type or all at once.
- `/rmc npcs` — per-NPC action-bar status with HP bar, countdown, and target name.
- `/rmc extend <seconds>` — extend lifetime; charges Vault economy cost.

#### Self-Revive System (`/rmc selfrevive`, alias `/sr`)

KO'd players can revive themselves at the cost of items and a longer channel
time. Every aspect is configurable:

- `require_items` — whether items are consumed (default `true`).
- `required_items` — list of `{material, amount}` entries; all must be present.
  Default: 2× Golden Apple.
- `duration_ticks` — channel time (default 200, ~10 s).
- `health_restored` — HP after self-revive (default 2).
- `cooldown_seconds` — per-player cooldown (default 120).
- `max_uses_per_ko` — limit per KO session, 0 = unlimited (default 1).
- `cancel_on_move` / `cancel_on_damage` — interrupt on movement or incoming hit.
- `combat_block_seconds` — block use if last damage was within this window.
- `effects_on_selfrevive` — separate post-revive effect set, harsher than
  teammate revive defaults.

`/rmc cancelselfrevive` (alias `/cancelsr`) cancels an active channel.

#### Distress Signal Rework

Four independent triggers, all respecting the same per-player cooldown and
`reanimatemc.distress` permission:

| Trigger | Platform | Config key |
|---|---|---|
| F (swap hands) | Java Edition | always active |
| Q / drop button | Java + Bedrock mobile | `knockout.distress.drop_key_trigger` |
| Double-tap sneak | Bedrock + Java | `knockout.distress.bedrock_doubletap_ms` |
| `/rmc distress` | All | `knockout.allowed_commands` (pre-added) |

When the player cancels a surrender by releasing the crouch button, a distress
signal is automatically sent on their behalf, so teammates are notified whenever
someone chooses to fight on.

The PROTECTOR NPC sends a forced distress signal (bypassing the player cooldown)
the first time the owner falls K.O. per session.

#### Command Improvements

`/rmc` with no arguments now shows a structured status panel:

```
━━━━━━━━━ ReanimateMC ━━━━━━━━━
  Version  v1.2.13  |  API 1.21.4  |  Author Jachou
  Language en
 ─── Systems ───────────────────
  K.O. System          ✔
  Execution System      ✔
  Crawl / Prone         ✔
  Distress Signal       ✔
  Self-Revive           ✔
  NPC Reanimators       ✔
 ─── Integrations ──────────────
  Vault Economy         ✘
  PlaceholderAPI        ✔
 ─── Live Status ───────────────
  Players currently K.O.: 0
  Type /rmc help for all commands
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

`/rmc help` reads all descriptions from the active language file; no English
is hardcoded. `/rmc version` shows plugin version, author, Minecraft version,
and API target.

#### New Permissions

| Permission | Default | Purpose |
|---|---|---|
| `reanimatemc.summon` | false | Base summon permission |
| `reanimatemc.summon.use.golem` | false | GOLEM type (requires base) |
| `reanimatemc.summon.use.healer` | false | HEALER type (requires base) |
| `reanimatemc.summon.use.protector` | false | PROTECTOR type (requires base) |
| `reanimatemc.distress` | true | Distress signal |
| `reanimatemc.selfrevive` | true | Self-revive |
| `reanimatemc.summon.overridecost` | op | Bypass cooldowns and economy costs |
| `reanimatemc.summon.admin` | op | Manage other players' NPCs |

#### PlaceholderAPI Integration

New expansion prefix `%reanimatemc_*%`:

- `%reanimatemc_is_ko%`
- `%reanimatemc_ko_time_remaining%`
- `%reanimatemc_npc_count%`
- `%reanimatemc_npc_type%`
- `%reanimatemc_npc_time_remaining%`
- `%reanimatemc_npc_hp%`

#### ConfigGUI Additions

- Three new K.O. toggles: Mobs Attack KO, Disable Knockback on KO,
  Must Be Still to Surrender.
- New `DOUBLE` option type (Comparator icon, left-click −0.05 / shift-click +0.05).
- New entries: Crawl Nausea Effect, Crawl Nausea Level, NPC System on/off,
  NPC Invulnerable Mode, Protector Damage Transfer ratio,
  Protector Revives (No Healer).

#### New Configuration Keys

All keys ship with documented inline comments in `config.yml`. A QUICK
REFERENCE block at the top of the file explains effect levels, duration
fields, and the distress subsection.

Highlights:

```yaml
knockout:
  crawl_nausea_enabled: true
  crawl_nausea_level: 1

  distress:
    enabled: true
    cooldown_seconds: 15
    drop_key_trigger: true
    bedrock_doubletap_ms: 400

prone:
  crawl_allow_jump: false

self_revive:
  enabled: true
  require_items: true
  required_items:
    - material: GOLDEN_APPLE
      amount: 2
  duration_ticks: 200
  health_restored: 2
  cooldown_seconds: 120
  max_uses_per_ko: 1
  cancel_on_move: true
  cancel_on_damage: true
  combat_block_seconds: 0
  effects_on_selfrevive:
    nausea: 10
    slowness: 15
    resistance: 5

npc_summon:
  golem:
    max_hp: 80
    revive_duration_ticks: 100
    combat_radius: 12.0
    summon_cost: 0.0
  healer:
    max_hp: 120
    revive_duration_ticks: 60
    heal_self: true
    heal_golems: true
    aura_particle_enabled: true
    aura_particle_radius: 3
  protector:
    max_hp: 200
    revive_duration_ticks: 160
    damage_transfer_ratio: 0.75
    revive_if_no_healer: true
```

#### New Files

| File | Description |
|---|---|
| `behavior/NPCBehavior.java` | Strategy interface: `onIdleTick`, `onSpawn`, `onRevive`, `canRevive`, `canReviveOwner` |
| `behavior/GolemBehavior.java` | GOLEM implementation |
| `behavior/HealerBehavior.java` | HEALER implementation |
| `behavior/ProtectorBehavior.java` | PROTECTOR implementation |
| `api/NPCSummonedEvent.java` | Cancellable summon event |
| `api/NPCDismissedEvent.java` | Dismissal event with `Reason` enum |
| `hooks/VaultHook.java` | Vault economy wrapper |
| `hooks/PlaceholderHook.java` | PlaceholderAPI expansion |
| `listeners/KOProtectionListener.java` | Knockback zeroing + mob targeting + projectile protection |
| `listeners/NPCDamageListener.java` | NPC self-damage prevention, low-HP alerts, death cleanup |
| `managers/NPCPersistenceManager.java` | YAML persistence with absolute expiry timestamps |
| `data/ReanimatorNPC.java` | NPC state record (LICENSE header) |
| `managers/NPCSummonManager.java` | NPC orchestration (LICENSE header) |
| `CLAUDE.md` | Development context document |

### Language Files

`en.yml` and `es.yml` have full translations for all new keys.
`de.yml`, `fr.yml`, `it.yml`, `ru.yml`, `zh.yml`, `kr.yml`, and `pt.yml`
have been updated with English fallback strings marked
`# EN — awaiting translation`. The plugin's `Lang.java` `copyDefaults(true)`
mechanism ensures existing translations are never overwritten.

---

## [1.2.10] — Previous Release

See the [GitHub releases page](https://github.com/MatisseAD/ReanimateMC/releases)
for earlier versions.
