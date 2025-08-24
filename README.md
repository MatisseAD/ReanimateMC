# ReanimateMC

![Version](https://img.shields.io/badge/version-Release_1.1.00-green.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.5-blue.svg)
![Spigot](https://img.shields.io/badge/Spigot-1.21.5-orange.svg)

# ReanimateMC

![ReanimateMC Cover](https://i.postimg.cc/3RHh8WJy/reanimate-mc-cover.jpg)

![Cover help](https://i.postimg.cc/WzLwfL8c/Chat-GPT-Image-12-avr-2025-18-34-14.png)

### Overview
ReanimateMC is a revolutionary plugin that transforms Minecraft’s conventional death system. Instead of a player dying instantly when their health reaches zero, the plugin introduces an intermediary state—KO (knockout). This innovative mechanic allows for dynamic role-play and strategic decision-making by giving players a chance to be revived by teammates or executed by adversaries. Perfect for Hardcore Survival, role-play, or tactical PVP servers, ReanimateMC injects a new level of depth into the gameplay experience.

### New Features
- KO state now persists if a player disconnects.
- KO players can press the swap-hand key to send a distress signal that places a beacon and alerts allies.
- Offline KO timer now continues counting down and will kill the player if it expires before they reconnect.


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

## Permissions
<details>
<summary>Spoiler</summary>

### reanimatemc.admin
-   **Description:** Grants access to all administrative commands, including reloading the plugin and managing player states.
----------
### reanimatemc.revive
-   **Description:** Allows players to forcefully revive others who are in the KO state.
----------
### reanimatemc.knockout
-   **Description:** Permits players to force a player into the KO state.
----------
### reanimatemc.status
-   **Description:** Enables players to check the current state of any player (KO, revived, or normal).
----------
### reanimatemc.crawl
-   **Description:** Allows players in the KO state to toggle between being fully immobilized and crawling slowly.
----------
### reanimatemc.removeGlowingEffect
-   **Description:** Grants the ability to forcefully remove the glowing effect from a player, typically used by operators.
</details>


## Compatibility:
Bukkit, Spigot, Magma, Sponge

## Native Version :
1.20.1

## Author:
Jachou

# Statistics

<img src="https://bstats.org/signatures/bukkit/ReanimateMC.svg" alt="BStats">
