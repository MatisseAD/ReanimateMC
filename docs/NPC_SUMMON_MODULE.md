# Module de Réanimation par NPC / Golem Invoqué

## Vue d'ensemble

Ce module permet aux joueurs d'invoquer des NPCs (golems de fer) qui peuvent :
- Réanimer automatiquement les joueurs en état K.O.
- Suivre et protéger leur invocateur
- Attaquer les mobs hostiles (type Protecteur)
- Soigner les joueurs (type Soigneur)

## Architecture

### Classes Principales

1. **ReanimatorNPC** (`data/ReanimatorNPC.java`)
   - Représente un NPC invoqué
   - Stocke: propriétaire, type, entité, cible, temps d'invocation
   - Types disponibles: GOLEM, HEALER, PROTECTOR

2. **NPCSummonManager** (`managers/NPCSummonManager.java`)
   - Gère le cycle de vie des NPCs
   - Contrôle les invocations, renvois, cooldowns
   - Implémente le comportement IA basique
   - Nettoyage automatique des NPCs invalides

### Approche Technique

**Choix d'implémentation**: Sans dépendance Citizens2

Cette implémentation utilise des entités natives Minecraft (IronGolem) avec :
- API Bukkit/Paper Pathfinder pour le déplacement
- Comportement personnalisé via BukkitRunnable
- Effets visuels avec Particles et Sons

**Avantages**:
- Pas de dépendance externe
- Compatible avec tout serveur Paper/Spigot 1.20+
- Léger et performant

**Inconvénients**:
- Pathfinding moins fluide que Citizens
- Pas de skins personnalisés
- Animations limitées

## Commandes

### Commandes Utilisateur

```
/reanimatemc summon <type> [player]
```
Invoque un NPC réanimateur
- `<type>`: golem, healer, ou protector
- `[player]`: joueur cible optionnel à réanimer

```
/reanimatemc dismiss all
```
Renvoie tous vos NPCs actifs

```
/reanimatemc npcs
```
Affiche la liste de vos NPCs actifs avec leur âge

### Commandes Admin

Les admins avec `reanimate.summon.admin` peuvent :
- Renvoyer les NPCs d'autres joueurs
- Bypass les cooldowns avec `reanimate.summon.overridecost`

## Permissions

```yaml
permissions:
  # Permission de base pour invoquer
  reanimate.summon: false
  
  # Permissions par type
  reanimate.summon.use.golem: false
  reanimate.summon.use.healer: false
  reanimate.summon.use.protector: false
  
  # Permissions admin
  reanimate.summon.overridecost: op  # Bypass coûts/cooldowns
  reanimate.summon.admin: op         # Contrôle total
```

## Configuration

Dans `config.yml`:

```yaml
npc_summon:
  enabled: true                    # Activer le système
  max_summons_per_player: 1        # Limite d'invocations par joueur
  summon_cooldown: 300             # Cooldown en secondes (5 min)
  offline_timeout: 300             # Timeout si propriétaire déconnecté
  require_item: false              # Nécessite un item pour invoquer
  required_item: NETHER_STAR       # Item requis (si activé)
```

## Comportements des NPCs

### Type: GOLEM (Réanimateur Standard)
- Suit le joueur invocateur
- Se déplace vers les joueurs K.O. ciblés
- Réanime au contact (< 3 blocs)
- Effets: particules de cœur + son d'enchantement

### Type: HEALER (Soigneur)
- Même comportement que GOLEM
- Spécialisé dans la réanimation
- Future amélioration: soins périodiques

### Type: PROTECTOR (Protecteur)
- Suit le joueur invocateur
- Peut réanimer comme GOLEM
- **Attaque les mobs hostiles** dans un rayon de 10 blocs
- Santé augmentée: 200 HP (vs 100 HP normal)

### Comportement Général

1. **Suivi du propriétaire**
   - Distance > 10 blocs: se déplace vers le propriétaire
   - Distance < 3 blocs: s'arrête
   
2. **Réanimation**
   - Si cible K.O. assignée: pathfind vers la cible
   - Au contact (< 3 blocs): appelle `koManager.revive()`
   - Après réanimation: reprend le suivi du propriétaire

3. **Nettoyage automatique**
   - NPCs invalides retirés automatiquement (toutes les secondes)
   - Timeout si propriétaire déconnecté (default: 5 min)

## Effets Visuels

### Rituel d'invocation
- Cercle de particules `SOUL_FIRE_FLAME`
- Son: `BLOCK_BEACON_ACTIVATE`
- Animation de 1 seconde (20 ticks)

### Réanimation
- Particules: `HEART` (10 particules)
- Son: `BLOCK_ENCHANTMENT_TABLE_USE`
- Pitch élevé (1.5f)

### Visibilité NPC
- Nom personnalisé visible (couleur or)
- Effet Glowing activé
- Exemple: "§6Iron Golem Reanimator"

## Messages (Multilingue)

### Français
```yaml
npc_summoned: "%type% a été invoqué !"
npc_dismissed: "NPC renvoyé avec succès."
npc_summon_cooldown: "Vous devez attendre %time% secondes..."
npc_max_summons: "Limite d'invocations atteinte (%max%)."
npc_status_header: "NPCs actifs :"
```

### English
```yaml
npc_summoned: "%type% has been summoned!"
npc_dismissed: "NPC dismissed successfully."
npc_summon_cooldown: "You must wait %time% seconds..."
npc_max_summons: "Maximum summons reached (%max%)."
npc_status_header: "Active NPCs:"
```

## Intégration avec ReanimateMC

### Interaction avec KOManager
```java
// Réanime un joueur via le NPC
koManager.revive(target, owner);
```

### Cycle de Vie
```java
// Initialisation (onEnable)
npcSummonManager = new NPCSummonManager(this, koManager);

// Nettoyage (onDisable)
npcSummonManager.cleanup();
```

## Cas d'Usage

### Scénario 1: Réanimation d'urgence
```
1. Joueur A est K.O. en combat
2. Joueur B invoque: /reanimatemc summon healer JoueurA
3. Le NPC se déplace vers Joueur A
4. Réanimation automatique au contact
5. Joueur A est sauvé, NPC retourne à Joueur B
```

### Scénario 2: Protection en exploration
```
1. Joueur explore une grotte dangereuse
2. Invoque: /reanimatemc summon protector
3. Le Protector suit et attaque les mobs hostiles
4. Fournit sécurité supplémentaire
```

### Scénario 3: Support d'équipe
```
1. Raid de groupe en donjon
2. Plusieurs joueurs invoquent des healers
3. Réanimations automatiques si KO
4. Améliore survie du groupe
```

## Limitations Connues

1. **Pathfinding**: Utilise le pathfinding vanilla (moins fluide que Citizens)
2. **Persistance**: NPCs ne persistent pas au redémarrage
3. **Multi-monde**: Le suivi inter-monde peut être imparfait
4. **Apparence**: Limité aux modèles vanilla (Iron Golem)

## Évolutions Futures Possibles

### Court Terme
- [ ] Système de coût en items consommables
- [ ] Statistiques d'utilisation des NPCs
- [ ] Sons personnalisés pour chaque type

### Moyen Terme
- [ ] Hook optionnel Citizens2 pour features avancées
- [ ] Persistance des NPCs (sauvegarde en base)
- [ ] Plus de types (Archer, Mage, Tank)
- [ ] Système de level/upgrade pour NPCs

### Long Terme
- [ ] Modèles personnalisés (ModelEngine, ItemsAdder)
- [ ] IA avancée avec objectifs multiples
- [ ] Compétences spéciales par type
- [ ] Interface GUI pour gestion NPCs

## Code Exemple

### Invocation Programmatique
```java
NPCSummonManager manager = ReanimateMC.getInstance().getNpcSummonManager();
Player player = ...;
Player target = ...;
boolean success = manager.summon(player, ReanimatorType.HEALER, target);
```

### Vérification NPCs Actifs
```java
List<ReanimatorNPC> npcs = manager.getPlayerSummons(player);
for (ReanimatorNPC npc : npcs) {
    player.sendMessage("NPC: " + npc.getType().getDisplayName());
}
```

## Support & Bugs

Pour signaler des bugs ou suggérer des améliorations:
- GitHub Issues: [ReanimateMC Repository]
- Format: Description détaillée, étapes de reproduction, logs si possible

## Licence

Ce module fait partie de ReanimateMC et est soumis à la même licence propriétaire.
