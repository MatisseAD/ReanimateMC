# ReanimateMC Developer API

This document provides comprehensive API documentation for developers who want to integrate with or extend ReanimateMC.

## Table of Contents
- [Getting Started](#getting-started)
- [Events](#events)
- [Manager Access](#manager-access)
- [Data Models](#data-models)
- [Code Examples](#code-examples)
- [Integration Patterns](#integration-patterns)
- [Best Practices](#best-practices)

## Getting Started

### Adding ReanimateMC as a Dependency

#### Plugin.yml
```yaml
name: YourPlugin
depend: [ReanimateMC]
# or for optional dependency
softdepend: [ReanimateMC]
```

#### Maven (pom.xml)
```xml
<dependencies>
    <dependency>
        <groupId>fr.jachou</groupId>
        <artifactId>ReanimateMC</artifactId>
        <version>1.2.02</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Gradle (build.gradle)
```groovy
dependencies {
    compileOnly 'fr.jachou:ReanimateMC:1.2.02'
}
```

### Basic Plugin Setup
```java
public class YourPlugin extends JavaPlugin {
    private ReanimateMC reanimateMC;
    private KOManager koManager;
    
    @Override
    public void onEnable() {
        // Get ReanimateMC instance
        reanimateMC = (ReanimateMC) getServer().getPluginManager().getPlugin("ReanimateMC");
        if (reanimateMC == null) {
            getLogger().warning("ReanimateMC not found!");
            return;
        }
        
        // Get KO Manager
        koManager = reanimateMC.getKoManager();
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new YourEventListener(), this);
    }
}
```

## Events

ReanimateMC provides two main events that you can listen to:

### PlayerKOEvent
Fired when a player enters the KO state.

```java
@EventHandler
public void onPlayerKO(PlayerKOEvent event) {
    Player player = event.getPlayer();
    int duration = event.getDuration();
    
    // Get player information
    String playerName = player.getName();
    UUID playerUUID = player.getUniqueId();
    
    // Check if event should be cancelled
    if (shouldCancelKO(player)) {
        event.setCancelled(true);
        return;
    }
    
    // Custom logic when player enters KO
    handlePlayerKO(player, duration);
}
```

**Event Properties:**
- `getPlayer()` - Returns the Player entering KO state
- `getDuration()` - Returns the KO duration in seconds
- `isCancelled()` - Check if event is cancelled
- `setCancelled(boolean)` - Cancel the KO event

### PlayerReanimatedEvent
Fired when a player is revived from KO state.

```java
@EventHandler
public void onPlayerRevived(PlayerReanimatedEvent event) {
    Player player = event.getPlayer();
    Player reanimator = event.getReanimator();
    boolean successful = event.isSuccessful();
    long timestamp = event.getTimestamp();
    
    // Handle successful revival
    if (successful) {
        handleSuccessfulRevival(player, reanimator);
    }
    
    // Log revival attempt
    logRevival(player, reanimator, successful, timestamp);
}
```

**Event Properties:**
- `getPlayer()` - Returns the Player being revived
- `getReanimator()` - Returns the Player performing the revival (can be null)
- `isSuccessful()` - Whether the revival was successful
- `getTimestamp()` - Timestamp of the revival attempt
- `getPlayerName()` - Convenience method for player name
- `getReanimatorName()` - Convenience method for reanimator name

## Manager Access

### KOManager
The main interface for interacting with the KO system:

```java
// Get the manager
KOManager koManager = reanimateMC.getKoManager();

// Check if player is KO'd
boolean isKO = koManager.isKO(player);

// Get KO data for a player
KOData koData = koManager.getKOData(player);

// Force a player into KO state
koManager.setKO(player); // Uses default duration
koManager.setKO(player, 30); // 30 seconds

// Revive a player
koManager.revive(player, reviverPlayer);

// Execute a KO'd player (permanent death)
koManager.execute(player);

// Toggle crawling for KO'd player
koManager.toggleCrawl(player);

// Send distress signal
koManager.sendDistress(player);

// Handle player logout while KO'd
koManager.handleLogout(player);

// Get offline KO duration for reconnecting player
long remainingSeconds = koManager.pullOfflineKO(playerUUID);
```

### StatsManager
Access plugin statistics:

```java
StatsManager statsManager = reanimateMC.getStatsManager();

// Get statistics
int totalKnockouts = statsManager.getKnockoutCount();
int totalRevives = statsManager.getReviveCount();

// Add to statistics (usually handled automatically)
statsManager.addKnockout();
statsManager.addRevive();
```

## Data Models

### KOData
Represents the state of a KO'd player:

```java
public class KOData {
    // Check if player is in KO state
    boolean isKo();
    
    // Get/set task IDs for scheduled tasks
    int getTaskId();
    void setTaskId(int taskId);
    int getBarTaskId();
    void setBarTaskId(int barTaskId);
    int getSuicideTaskId();
    void setSuicideTaskId(int suicideTaskId);
    
    // Crawling state
    boolean isCrawling();
    void setCrawling(boolean crawling);
    
    // Entity references
    ArmorStand getMount();          // Invisible mount for immobilization
    void setMount(ArmorStand mount);
    ArmorStand getLabel();          // Label entity
    void setLabel(ArmorStand label);
    ArmorStand getHelpMarker();     // Distress beacon marker
    void setHelpMarker(ArmorStand helpMarker);
    
    // Timing
    long getEndTimestamp();
    void setEndTimestamp(long endTimestamp);
    
    // Player attributes
    double getOriginalJumpStrength();
    void setOriginalJumpStrength(double originalJumpStrength);
    String getOriginalListName();
    void setOriginalListName(String originalListName);
}
```

## Code Examples

### Basic Integration Example
```java
public class MedicalPlugin extends JavaPlugin implements Listener {
    private KOManager koManager;
    
    @Override
    public void onEnable() {
        ReanimateMC reanimateMC = (ReanimateMC) getServer().getPluginManager().getPlugin("ReanimateMC");
        this.koManager = reanimateMC.getKoManager();
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @EventHandler
    public void onPlayerKO(PlayerKOEvent event) {
        Player player = event.getPlayer();
        
        // Notify medical team
        notifyMedicalTeam(player);
        
        // Give player special medical effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
    }
    
    @EventHandler
    public void onPlayerRevived(PlayerReanimatedEvent event) {
        if (event.isSuccessful()) {
            Player patient = event.getPlayer();
            Player medic = event.getReanimator();
            
            // Give XP to medic
            if (medic != null) {
                medic.giveExp(50);
                medic.sendMessage("You saved " + patient.getName() + "!");
            }
        }
    }
    
    private void notifyMedicalTeam(Player patient) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("medical.notification")) {
                online.sendMessage(patient.getName() + " needs medical attention at " + 
                    formatLocation(patient.getLocation()));
            }
        }
    }
}
```

### Custom Revival System
```java
public class CustomRevivalPlugin extends JavaPlugin implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerKO(PlayerKOEvent event) {
        Player player = event.getPlayer();
        
        // Cancel default KO if player has special item
        if (hasTotemOfUndying(player)) {
            event.setCancelled(true);
            consumeTotem(player);
            reviveWithTotem(player);
        }
    }
    
    private boolean hasTotemOfUndying(Player player) {
        return player.getInventory().contains(Material.TOTEM_OF_UNDYING);
    }
    
    private void consumeTotem(Player player) {
        player.getInventory().removeItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
    }
    
    private void reviveWithTotem(Player player) {
        player.setHealth(player.getMaxHealth());
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.sendMessage(ChatColor.GOLD + "Your totem saved you from KO!");
    }
}
```

### Team-Based Revival System
```java
public class TeamRevivalPlugin extends JavaPlugin implements Listener {
    private Map<UUID, String> playerTeams = new HashMap<>();
    
    @EventHandler
    public void onPlayerRevived(PlayerReanimatedEvent event) {
        Player patient = event.getPlayer();
        Player medic = event.getReanimator();
        
        if (medic == null) return;
        
        String patientTeam = getPlayerTeam(patient);
        String medicTeam = getPlayerTeam(medic);
        
        // Only allow team members to revive
        if (!patientTeam.equals(medicTeam)) {
            event.setCancelled(true); // If event was cancellable
            medic.sendMessage(ChatColor.RED + "You can only revive team members!");
            return;
        }
        
        // Team revival bonus
        if (event.isSuccessful()) {
            giveTeamBonus(medic, patient);
        }
    }
    
    private void giveTeamBonus(Player medic, Player patient) {
        // Give both players team coordination bonus
        medic.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
        patient.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 0));
    }
}
```

### Statistics Integration
```java
public class StatsIntegrationPlugin extends JavaPlugin implements Listener {
    
    @EventHandler
    public void onPlayerRevived(PlayerReanimatedEvent event) {
        if (event.isSuccessful()) {
            Player medic = event.getReanimator();
            if (medic != null) {
                // Add to custom statistics
                incrementPlayerStat(medic, "revivals_performed");
                
                // Check for achievements
                checkRevivalAchievements(medic);
            }
        }
    }
    
    private void checkRevivalAchievements(Player player) {
        int revivals = getPlayerStat(player, "revivals_performed");
        
        if (revivals == 10) {
            giveAchievement(player, "First Aid Certified");
        } else if (revivals == 50) {
            giveAchievement(player, "Life Saver");
        } else if (revivals == 100) {
            giveAchievement(player, "Guardian Angel");
        }
    }
}
```

## Integration Patterns

### Permission-Based Features
```java
// Check for custom permissions during KO events
@EventHandler
public void onPlayerKO(PlayerKOEvent event) {
    Player player = event.getPlayer();
    
    // VIP players get longer KO duration
    if (player.hasPermission("server.vip")) {
        // Extend duration (would need custom implementation)
        extendKODuration(player, 30); // Extra 30 seconds
    }
    
    // Staff bypass KO entirely
    if (player.hasPermission("server.staff")) {
        event.setCancelled(true);
    }
}
```

### Economy Integration
```java
@EventHandler
public void onPlayerRevived(PlayerReanimatedEvent event) {
    if (event.isSuccessful()) {
        Player medic = event.getReanimator();
        if (medic != null && economyAPI != null) {
            // Pay medic for revival
            economyAPI.depositPlayer(medic, 100.0);
            medic.sendMessage(ChatColor.GREEN + "You earned $100 for the revival!");
        }
    }
}
```

### WorldGuard Integration
```java
@EventHandler
public void onPlayerKO(PlayerKOEvent event) {
    Player player = event.getPlayer();
    Location location = player.getLocation();
    
    // Check if in safe zone
    if (worldGuard.isInRegion(location, "safe-zone")) {
        event.setCancelled(true);
        player.sendMessage(ChatColor.GREEN + "You are protected in the safe zone!");
    }
    
    // Hospital region has different rules
    if (worldGuard.isInRegion(location, "hospital")) {
        // Custom hospital logic
        handleHospitalKO(player);
    }
}
```

## Best Practices

### Event Handling
```java
// Always check for null values
@EventHandler
public void onPlayerRevived(PlayerReanimatedEvent event) {
    Player patient = event.getPlayer();
    Player medic = event.getReanimator(); // Can be null!
    
    if (medic != null) {
        // Safe to use medic
        handleMedicReward(medic);
    }
}

// Use appropriate event priorities
@EventHandler(priority = EventPriority.HIGH)
public void onHighPriorityKO(PlayerKOEvent event) {
    // Handle before other plugins
}

@EventHandler(priority = EventPriority.LOW)
public void onLowPriorityKO(PlayerKOEvent event) {
    // Handle after other plugins
}
```

### Manager Usage
```java
// Always check if player is KO'd before operations
public void customRevivePlayer(Player target, Player medic) {
    if (!koManager.isKO(target)) {
        medic.sendMessage("Player is not KO'd!");
        return;
    }
    
    // Perform custom revival logic
    koManager.revive(target, medic);
}

// Handle edge cases
public void handlePlayerDisconnect(Player player) {
    if (koManager.isKO(player)) {
        koManager.handleLogout(player);
    }
}
```

### Data Safety
```java
// Always check for null KOData
public void checkPlayerKOStatus(Player player) {
    KOData koData = koManager.getKOData(player);
    if (koData != null && koData.isKo()) {
        // Player is KO'd, handle accordingly
        handleKOPlayer(player, koData);
    }
}
```

### Performance Considerations
```java
// Cache manager references
private KOManager koManager;

@Override
public void onEnable() {
    this.koManager = reanimateMC.getKoManager(); // Cache this
}

// Avoid excessive API calls in loops
public void checkMultiplePlayers(List<Player> players) {
    for (Player player : players) {
        if (koManager.isKO(player)) { // Single call per player
            handleKOPlayer(player);
        }
    }
}
```

## Common Use Cases

### Medical System
- Track medical supplies
- Require special items for revival
- Add medical training requirements
- Create hospital safe zones

### Team Systems
- Restrict revival to team members
- Add team communication during KO
- Team-based statistics and achievements
- Cross-team interactions (prisoner exchange)

### RPG Integration
- Class-based revival abilities
- Experience gain for medics
- Magic-based healing systems
- Quest integration with revival mechanics

### PvP Enhancements
- Faction-based revival restrictions
- Tournament mode modifications
- Spectator systems during KO
- Enhanced execution mechanics

This API documentation should provide developers with everything they need to integrate with ReanimateMC effectively. The plugin's event system and manager access provide flexible options for customization while maintaining the core KO mechanics.