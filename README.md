# ReanimateMC

![Version](https://img.shields.io/badge/version-Release_1.1.00-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.5-blue.svg)
![Spigot](https://img.shields.io/badge/Spigot-1.21.5-orange.svg)

# ReanimateMC

![ReanimateMC Cover](https://i.postimg.cc/3RHh8WJy/reanimate-mc-cover.jpg)

![Cover help](https://i.postimg.cc/WzLwfL8c/Chat-GPT-Image-12-avr-2025-18-34-14.png)

### Overview
ReanimateMC is a revolutionary plugin that transforms Minecraft’s conventional death system. Instead of a player dying instantly when their health reaches zero, the plugin introduces an intermediary state—KO (knockout). This innovative mechanic allows for dynamic role-play and strategic decision-making by giving players a chance to be revived by teammates or executed by adversaries. Perfect for Hardcore Survival, role-play, or tactical PVP servers, ReanimateMC injects a new level of depth into the gameplay experience.


## Commands

<details>
<summary>Spoiler</summary>

### /reanimatemc reload

-   **Permission:** `reanimatemc.admin`

-   **Usage:** `/reanimatemc reload`

-   **Description:** Reloads the plugin’s configuration and language files.


----------

### /reanimatemc revive <player>

-   **Permission:** `reanimatemc.revive`

-   **Usage:** `/reanimatemc revive <player>`

-   **Description:** Forcefully revives a player who is in the KO state.


----------

### /reanimatemc knockout <player>

-   **Permission:** Typically requires admin privileges (suggested: `reanimatemc.admin`)

-   **Usage:** `/reanimatemc knockout <player>`

-   **Description:** Forces a player to enter the KO (knockout) state.


----------

### /reanimatemc status <player>

-   **Permission:** No special permission required (accessible to all users)

-   **Usage:** `/reanimatemc status <player>`

-   **Description:** Displays the current state of the player (KO, revived, or normal).


----------

### /reanimatemc crawl

-   **Permission:** No special permission required (command can only be executed by a player in the KO state)

-   **Usage:** `/reanimatemc crawl`

-   **Description:** Toggles the KO player's state between being fully immobilized (prone) and crawling slowly.

----------

### /reanimatemc removeGlowingEffect <player>

-   **Permission:** Operator

-   **Usage:** `/reanimatemc removeGlowingEffect <player>`

-   **Description:** In order to force removing the glowing effect on a plyer


</details>


## Compatibility:
Bukkit, Spigot, Magma, Sponge

## Native Version :
1.20.1

## Author:
Jachou

# Statistics

<img src="https://bstats.org/signatures/bukkit/ReanimateMC.svg" alt="BStats">
## Automated Modrinth Upload

A helper script is provided in `scripts/upload_to_modrinth.sh` to publish a new plugin version directly to Modrinth. The script uses the Modrinth HTTP API and requires `curl` to be installed.

### Usage

```bash
export MODRINTH_TOKEN=your_token_here
export PROJECT_ID=your_project_id
export VERSION_NAME="Release 1.0"
export VERSION_NUMBER="1.0.0"
# Optional settings
export GAME_VERSIONS_JSON='["1.20.1"]'
export LOADERS_JSON='["spigot"]'
export VERSION_TYPE=release
export CHANGELOG_FILE=changelog.txt
export JAR_PATH=target/ReanimateMC.jar

./scripts/upload_to_modrinth.sh
```

The script uploads the specified JAR file and creates a new version on Modrinth with the provided metadata.
