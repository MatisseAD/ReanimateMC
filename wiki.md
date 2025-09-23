# ReanimateMC – Comprehensive Documentation (English & French)

## Overview

ReanimateMC is a Minecraft plugin that replaces the vanilla instant-death mechanic with a knockout (KO) system that allows other players to revive or execute incapacitated teammates. When a player's health reaches zero, they fall unconscious instead of dying outright. Allies can revive them by crouching and holding a special item, while enemies can finish them off. This creates a more strategic, cooperative gameplay experience – ideal for role‑play (RP), hardcore survival, and tactical PvP servers.

## Official Compatibility

- **Native MC version:** 1.20 (compatible with Spigot/Paper)
- **Tested versions:** 1.17 to 1.21
- **Languages supported:** French (native), English, Spanish, German, Italian, Dutch, Russian, Chinese, Korean, Polish
- **Server platforms:** Bukkit, Spigot, Paper, Magma, Sponge

## Installation

1. Download `ReanimateMC-<version>.jar` from the plugin's release page.
2. Place the jar in your server's `plugins/` directory.
3. Start or restart the server. A `ReanimateMC/` folder is created automatically with `config.yml` and `lang` subfolder.
4. **(Optional)** To use another language, copy an existing language file (e.g., `fr.yml` or `en.yml`) in `plugins/ReanimateMC/lang/`, rename it (e.g., `es.yml`), translate the values, and set `language: "es"` in `config.yml`.

## Features

### Knockout State

- Players do not die instantly when health reaches zero. Instead, they enter a KO state.
- KO players cannot move or interact and receive blindness and slowness effects. A glowing outline highlights them for others.
- A survival timer starts (default 30s), after which the player dies if not revived.
- KO state persists if a player disconnects, with the timer continuing to count down.

### Revival System

- A teammate can revive a KO player by crouching (Shift) and holding right‑click while wielding a configured item (default: Golden Apple). The plugin checks that the reviver stays crouched, keeps the required item, and maintains focus on the target.
- Reviving requires a configurable duration (default 5s). An action bar or boss bar can show progress.
- Upon successful revival, the KO player stands up, regains partial health (configurable), receives temporary debuffs (nausea, slowness, resistance), and loses the glowing effect.

### Execution

- Any player (ally or enemy) may execute a KO player by holding left‑click on them. Execution requires holding for a configured duration (default 2s). Progress appears in the action bar.
- If completed, the victim dies immediately, possibly dropping inventory depending on configuration.

### Distress Signal

- KO players can press the swap-hand key (F by default) to send a distress signal.
- This places a glowing beacon with "HELP!" text and broadcasts the player's coordinates to all players.
- Provides a way for KO players to alert teammates of their location.

### Suicide Mechanism

- KO players can hold Shift to initiate a suicide timer (default 3 seconds).
- If they continue holding Shift for the full duration, they will die.
- Releasing Shift cancels the suicide attempt.

### Looting (Optional)

- If enabled, players can open the inventory of a KO player to take items. Items removed are removed from the KO player's inventory. 
- Looting can be restricted by permission.

### Crawling and Prone (Optional)

- The plugin can simulate a "prone" position. KO players sit on an invisible entity (armor stand) so they appear sitting or lying. When revived, they stand up.
- An optional "crawl" command (`/reanimatemc crawl`) allows KO players to toggle between immobility and crawling slowly if enabled.

### ActionBar and Messages

- Clear feedback via chat, action bar, and boss bar: progress bars, countdown of time remaining in KO state, instructions to crouch or hold the required item, and cancel messages if conditions fail.
- Multilingual support: messages come from a language file (see below).

### GUI Configuration

- Admins can run `/reanimatemc gui` (requires `reanimatemc.admin` permission) to open an inventory-based interface.
- Each slot toggles a boolean option in `config.yml` (KO enabled, require special item, particles, heartbeat sound, execution enabled, broadcast messages, prone/crawl, looting etc.).
- The GUI also shows statistics: number of KO's suffered, revivals performed/received, executions, etc., and allows changing the language on the fly.

### Statistics

- The plugin tracks per-player statistics: total times knocked out, number of successful revivals performed and received, and executions.
- Stats are viewable via the GUI and can be exported or used by other addons.

## Configuration

### config.yml

Below is a typical configuration file with key options:

```yaml
language: "en"   # Set your preferred language file (e.g., "en", "fr")
first_run: true
setup_completed: false

reanimation:
  require_special_item: true        # Require a specific item for reviving?
  required_item: "GOLDEN_APPLE"     # Item used for reviving
  duration_ticks: 100               # How long revival takes (20 ticks = 1 second)
  health_restored: 4                # Health restored to the revived player (half-hearts)
  cooldown: 60                      # Cooldown after reviving (seconds)
  revive_cooldown: 60               # Cooldown between revival attempts

knockout:
  enabled: true
  duration_seconds: 30              # Time until natural death if not revived
  movement_disabled: true           # Immobilize KO players
  use_particles: true               # Show particles around KO players
  heartbeat_sound: true             # Play heartbeat sound while KO
  blindness: true                   # Apply blindness effect to KO players
  suicide_hold_seconds: 3           # Time to hold Shift for suicide
  weakness_level: 1                 # Weakness effect level
  fatigue_level: 1                  # Mining fatigue effect level

execution:
  enabled: true
  hold_duration_ticks: 40           # Time needed to execute (ticks)
  message_broadcast: true           # Announce execution to all players

looting:
  enabled: true                     # Allow looting KO players' inventories

prone:
  enabled: true
  allow_crawl: true                 # Allow KO players to crawl
  crawl_slowness_level: 5           # Slowness level while crawling
  auto_crawl: false                 # Automatically enable crawl mode

tablist:
  enabled: true                     # Show KO status in tab list

# Additional effects applied on revival
effects_on_revive:
  nausea: 5
  slowness: 10
  resistance: 10
```

### Language Files

Language files are located in `plugins/ReanimateMC/lang/`. Each file maps keys to messages displayed in game. For example, `fr.yml` for French and `en.yml` for English. A few key entries include:

```yaml
ko_set: "You are K.O.! You can no longer walk or see your surroundings."
revived: "You have been revived!"
revive_progress: "Revival in progress..."
revived_by: "You have been revived by %player%!"
execution_in_progress: "Execution in progress... Keep left-clicking!"
execution_broadcast: "%player% has been executed!"
distress_sent: "Distress signal sent!"
distress_broadcast: "%player% needs help at %x%, %y%, %z%!"
suicide_start: "Giving up... you will die in %time%s"
actionbar_ko_countdown: "KO: %time%s left!"
# etc.
```

To add a new language, duplicate an existing file, translate each value, then update `language: <code>` in `config.yml`.

## Commands and Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/reanimatemc reload` | `reanimatemc.admin` | Reloads the plugin's configuration and language files. |
| `/reanimatemc revive <player>` | `reanimatemc.admin` | Instantly revives a KO'd player. |
| `/reanimatemc knockout <player>` | `reanimatemc.admin` | Forces a player into the KO state. |
| `/reanimatemc execute <player>` | `reanimatemc.admin` | Instantly executes a KO'd player. |
| `/reanimatemc status <player>` | `reanimatemc.admin` | Shows whether the target is KO or normal. |
| `/reanimatemc gui` | `reanimatemc.admin` | Opens the graphical configuration interface. |
| `/reanimatemc crawl` | none | If prone/crawl is enabled, toggles crawling for the player currently KO. |
| `/reanimatemc removeGlowingEffect <player>` | operator | Forces removal of glowing effect from a player. |

### Available Permissions

- `reanimatemc.admin` - Full administrative access to all commands and GUI
- `reanimatemc.revive` - Allows using the revive command
- `reanimatemc.knockout` - Allows using the knockout command  
- `reanimatemc.status` - Allows checking player status
- `reanimatemc.bypass` - Bypasses the KO system (dies normally)

Permissions can be granted via a permission plugin like LuckPerms or directly via the server console.

## Usage Scenarios

### Hardcore Survival
Players rely on teammates to survive; KO adds tension as players must decide between continuing combat or saving their ally.

### Role‑play Servers
Simulate medical or rescue scenarios. Use bandages, medics, or clinics where players are revived.

### PvP Arenas
Add strategic depth; finishing downed opponents or capturing them becomes part of the tactics.

### Minigames
Integrate KO mechanics into custom minigames (last man standing, rescue missions, etc.).

## Future Ideas

- **Bandages & bleeding:** Items that stop bleeding or slow death while KO.
- **Localized injuries:** Distinct debuffs based on damage type (e.g., leg injury causing slowness after revival).
- **Medical classes:** Roles with special revival perks.
- **Webhook integration:** Send alerts to Discord when players are knocked out or revived.
- **Corpse inventory:** Create a temporary chest with the dead player's items after natural death.

## Contact & Support

- **Project page:** [Spigot resource page](https://www.spigotmc.org/resources/reanimatemc.108041/) – shows plugin details and states that the plugin supports multiple languages.
- **Source code:** [GitHub repository](https://github.com/MatisseAD/ReanimateMC) (open‑source; contributions welcome).
- **Report issues:** Use the issues tab on GitHub or the discussion thread on SpigotMC.
- **Statistics:** Plugin usage statistics available at [bStats](https://bstats.org/plugin/bukkit/ReanimateMC)

---

# Documentation en français

## Présentation

ReanimateMC est un plugin Minecraft qui remplace la mort instantanée par un système de K.O. Au lieu de mourir, les joueurs tombent dans un état d'inconscience et peuvent être réanimés ou exécutés. Parfait pour les serveurs RP, de survie hardcore ou de PvP stratégique, il ajoute une dimension tactique et collaborative au jeu.

## Installation

1. Téléchargez `ReanimateMC-<version>.jar` et placez-le dans `plugins/`.
2. Lancez ou redémarrez votre serveur pour générer le dossier ReanimateMC.
3. Modifiez `config.yml` et copiez/éditez les fichiers de langue dans `ReanimateMC/lang/` si nécessaire.

## Fonctionnalités principales

### État K.O.
- Le joueur ne meurt pas instantanément mais est immobilisé (assis/au sol), avec un halo lumineux et un compte à rebours.
- L'état K.O. persiste si le joueur se déconnecte, avec le timer qui continue de décompter.

### Réanimation
- Un coéquipier accroupi, tenant l'objet défini (ex : pomme dorée) et maintenant le clic droit, peut le sauver. La réanimation dure X secondes et n'est réussie que si toutes les conditions sont respectées (sneak, item, visée). Une barre de progression s'affiche.

### Exécution
- Un joueur peut tuer un K.O. en maintenant le clic gauche pendant Y secondes.

### Signal de détresse
- Les joueurs K.O. peuvent appuyer sur la touche d'échange d'objets (F par défaut) pour envoyer un signal de détresse.
- Cela place une balise lumineuse avec le texte "HELP!" et diffuse les coordonnées du joueur à tous les joueurs.

### Mécanisme de suicide
- Les joueurs K.O. peuvent maintenir Shift pour initier un timer de suicide (3 secondes par défaut).
- S'ils continuent à maintenir Shift pendant toute la durée, ils mourront.

### Option de pillage
- Permet d'ouvrir l'inventaire du joueur K.O. pour prendre ses objets.

### Mode rampé
- Possibilité pour le joueur K.O. de ramper lentement s'il le souhaite (configurable).

### GUI d'administration
- Commande `/reanimatemc gui` pour accéder à un menu où l'administrateur peut activer/désactiver les fonctionnalités en jeu et voir les statistiques.

### Support multilangue
- Français par défaut, fichier anglais fourni. Ajoutez vos propres traductions dans `lang/`.

## Configuration (extraits)

- `language: "fr"` définit la langue utilisée par le plugin.
- `reanimation.require_special_item:` doit‑on tenir un objet spécifique ? (true/false)
- `knockout.duration_seconds:` durée de l'état K.O. avant la mort naturelle.
- `execution.enabled:` active ou désactive l'exécution des joueurs K.O.
- `looting.enabled:` autorise le pillage des joueurs K.O.
- `prone.allow_crawl:` autorise le mode ramper pour les joueurs K.O.

## Commandes

- `/reanimatemc reload` : recharge la configuration et les langues.
- `/reanimatemc revive <joueur>` : force la réanimation d'un joueur K.O.
- `/reanimatemc knockout <joueur>` : force un joueur à tomber K.O.
- `/reanimatemc execute <joueur>` : tue immédiatement un joueur K.O.
- `/reanimatemc status <joueur>` : indique si le joueur est K.O. ou non.
- `/reanimatemc gui` : ouvre l'interface graphique de configuration.
- `/reanimatemc crawl` : permet au joueur K.O. de basculer entre immobilisé et rampant.

## Permissions

- `reanimatemc.revive` : réanimer des joueurs.
- `reanimatemc.execute` : exécuter des joueurs.
- `reanimatemc.bypass` : ignorer l'état K.O. (meurt directement).
- `reanimatemc.admin` : accès aux commandes d'administration et au GUI.

## Suggestions d'utilisation

### Survie hardcore
Ajoute un enjeu dramatique lorsque chaque mort peut être évitée.

### Role‑play
Crée des scénarios de sauvetage, d'assassinat ou d'intervention médicale.

### PvP stratégique
Donne un choix tactique entre sauver son allié ou finir un ennemi.

## Plus d'informations

- **Page officielle :** [Page SpigotMC](https://www.spigotmc.org/resources/reanimatemc.108041/) pour obtenir des détails officiels et les versions supportées.
- **Code source :** [Dépôt GitHub](https://github.com/MatisseAD/ReanimateMC) (open-source ; contributions bienvenues).
- **Signaler des problèmes :** Utilisez l'onglet issues sur GitHub ou le fil de discussion sur SpigotMC.
- **Statistiques :** Statistiques d'utilisation du plugin disponibles sur [bStats](https://bstats.org/plugin/bukkit/ReanimateMC)