# ReanimateMC Setup Guide

This guide provides detailed instructions for setting up and configuring ReanimateMC on your Minecraft server.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Installation Process](#installation-process)
- [Initial Configuration](#initial-configuration)
- [Server Type Configurations](#server-type-configurations)
- [Permission Setup](#permission-setup)
- [Testing Your Setup](#testing-your-setup)
- [Common Setup Issues](#common-setup-issues)

## Prerequisites

### Server Requirements
- **Minecraft Version**: 1.20.1 or higher
- **Server Software**: Spigot, Paper, Bukkit, or compatible fork
- **Java Version**: 16 or higher
- **RAM**: Minimum 2GB available (more for larger servers)
- **Plugins**: No conflicting death/respawn plugins

### Recommended Plugins
- **Permission Manager**: LuckPerms, GroupManager, or similar
- **WorldGuard**: For region-specific KO rules (optional)
- **EssentialsX**: For enhanced server management (optional)

## Installation Process

### Step 1: Download
1. Visit the [ReanimateMC releases page](https://github.com/MatisseAD/ReanimateMC/releases)
2. Download the latest `ReanimateMC-X.X.XX.jar` file
3. Verify the file is complete (check file size matches the release)

### Step 2: Install
1. **Stop your server** (important for clean installation)
2. **Upload** the JAR file to your server's `plugins/` directory
3. **Start your server**
4. **Check the console** for any error messages during loading

### Step 3: Verify Installation
```
[INFO] [ReanimateMC] ReanimateMC has been enabled!
[INFO] [ReanimateMC] Language loaded: en
[INFO] [ReanimateMC] Statistics manager initialized
```

If you see these messages, the plugin has loaded successfully.

## Initial Configuration

### First Run Setup
When ReanimateMC starts for the first time:

1. **Configuration files** are created in `plugins/ReanimateMC/`
2. **Language files** are extracted to `plugins/ReanimateMC/lang/`
3. **Operators receive setup reminder** when they join

### Quick Configuration via GUI
The fastest way to configure the plugin:

1. **Join as an operator**
2. **Run command**: `/reanimatemc config`
3. **Configure basic settings**:
   - Enable/disable KO system
   - Set KO duration
   - Configure revival requirements
   - Set execution settings

### Manual Configuration
Edit `plugins/ReanimateMC/config.yml` for advanced configuration:

```yaml
# Basic settings
language: "en"
setup_completed: false

# Core KO mechanics
knockout:
  enabled: true
  duration_seconds: 30
  movement_disabled: true
  
# Revival system
reanimation:
  require_special_item: true
  required_item: GOLDEN_APPLE
  duration_ticks: 100
```

After editing, use `/reanimatemc reload` to apply changes.

## Server Type Configurations

### Survival/SMP Server
**Recommended Settings:**
```yaml
knockout:
  enabled: true
  duration_seconds: 45
  movement_disabled: false
  use_particles: true
  heartbeat_sound: true

prone:
  enabled: true
  allow_crawl: true
  auto_crawl: true

reanimation:
  require_special_item: true
  required_item: GOLDEN_APPLE
  health_restored: 6
```

**Why these settings:**
- Longer KO duration for rescue attempts
- Crawling enabled for strategic positioning
- Golden Apple requirement adds resource cost

### PvP Server  
**Recommended Settings:**
```yaml
knockout:
  enabled: true
  duration_seconds: 20
  movement_disabled: true
  blindness: true

execution:
  enabled: true
  hold_duration_ticks: 30
  message_broadcast: true

reanimation:
  duration_ticks: 60
  cooldown: 120
```

**Why these settings:**
- Shorter KO duration for faster gameplay
- Execution enabled for competitive play
- Revival cooldowns prevent spam

### Roleplay Server
**Recommended Settings:**
```yaml
knockout:
  enabled: true
  duration_seconds: 60
  use_particles: true
  heartbeat_sound: true

tablist:
  enabled: true

effects_on_revive:
  nausea: 10
  slowness: 15
  resistance: 20
```

**Why these settings:**
- Longer KO for dramatic scenes
- Visual effects for immersion
- Realistic recovery effects

### Hardcore Server
**Recommended Settings:**
```yaml
knockout:
  enabled: true
  duration_seconds: 15
  suicide_hold_seconds: 5

execution:
  enabled: true
  hold_duration_ticks: 20

prone:
  enabled: false
```

**Why these settings:**
- Short KO duration maintains difficulty
- Quick execution for hardcore feel
- No crawling for true immobilization

## Permission Setup

### Basic Permission Groups

#### Default Players
```yaml
permissions:
  - reanimatemc.revive
  - reanimatemc.execute
  - reanimatemc.status
  - reanimatemc.crawl
```

#### Moderators
```yaml
permissions:
  - reanimatemc.revive
  - reanimatemc.execute
  - reanimatemc.status
  - reanimatemc.crawl
  - reanimatemc.knockout
  - reanimatemc.loot
  - reanimatemc.removeGlowingEffect
```

#### Administrators
```yaml
permissions:
  - reanimatemc.*
  - reanimatemc.admin
  - reanimatemc.bypass
```

### LuckPerms Example
```bash
# Create groups
/lp creategroup medic
/lp creategroup admin

# Set medic permissions
/lp group medic permission set reanimatemc.revive true
/lp group medic permission set reanimatemc.status true

# Set admin permissions  
/lp group admin permission set reanimatemc.admin true
/lp group admin permission set reanimatemc.bypass true

# Assign users to groups
/lp user PlayerName parent add medic
```

## Testing Your Setup

### Basic Functionality Test
1. **Join as a non-op player**
2. **Take damage until health reaches 0**
3. **Verify you enter KO state** (don't die)
4. **Have another player revive you**
5. **Verify revival works correctly**

### Command Testing
```bash
# Test admin commands (as op)
/reanimatemc reload
/reanimatemc config
/reanimatemc status PlayerName

# Test player commands
/reanimatemc crawl
/reanimatemc status
```

### Permission Testing
1. **Remove op from test account**
2. **Verify appropriate commands work**
3. **Verify restricted commands are blocked**
4. **Test revival permissions**

## Common Setup Issues

### Plugin Not Loading
**Symptoms:** No ReanimateMC messages in console
**Solutions:**
- Check Java version (must be 16+)
- Verify server software compatibility
- Check for conflicting plugins
- Review console for error messages

### Players Die Instead of KO
**Symptoms:** Players die normally instead of entering KO
**Solutions:**
- Check `knockout.enabled: true` in config
- Verify no `reanimatemc.bypass` permission
- Disable conflicting death plugins
- Check WorldGuard region flags

### Revival Not Working
**Symptoms:** Cannot revive KO'd players
**Solutions:**
- Verify `reanimation.require_special_item` setting
- Check required item in config
- Ensure reviver has permission
- Test without item requirement first

### GUI Not Opening
**Symptoms:** `/reanimatemc config` shows error
**Solutions:**
- Verify `reanimatemc.admin` permission
- Check console for GUI loading errors
- Try `/reanimatemc reload` first
- Ensure using compatible client version

### Language Issues
**Symptoms:** Messages in wrong language or not loading
**Solutions:**
- Check `language:` setting in config.yml
- Verify language file exists in `/lang/` folder
- Use `/reanimatemc reload` after changes
- Check for YAML syntax errors

### Performance Issues
**Symptoms:** Server lag when plugin active
**Solutions:**
- Disable particles: `use_particles: false`
- Reduce heartbeat frequency
- Check for excessive KO'd players
- Monitor server resources

## Advanced Configuration

### Custom Items for Revival
```yaml
reanimation:
  require_special_item: true
  required_item: ENCHANTED_GOLDEN_APPLE  # More expensive
  # or
  required_item: DIAMOND                 # Alternative item
```

### Multiple Revival Items
The plugin supports only one item at a time, but you can:
1. Use plugins like ItemsAdder for custom items
2. Create different server areas with different configs
3. Use WorldGuard regions with different settings

### Integration with Other Plugins
```yaml
# Example integration settings
worldguard:
  respect_regions: true
  
essentials:
  prevent_teleport_when_ko: true
  
mcmmo:
  gain_xp_on_revive: true
```

## Maintenance

### Regular Tasks
- **Check logs** for errors or warnings
- **Monitor statistics** for plugin usage
- **Update language files** if needed
- **Review configuration** for optimization

### Updates
1. **Backup current config** before updating
2. **Download new version**
3. **Replace JAR file**
4. **Restart server**
5. **Check for new config options**

### Backup Strategy
```bash
# Backup entire plugin folder
cp -r plugins/ReanimateMC/ backups/ReanimateMC-$(date +%Y%m%d)/

# Backup just config files
cp plugins/ReanimateMC/config.yml backups/
cp -r plugins/ReanimateMC/lang/ backups/lang/
```

## Getting Help

If you encounter issues not covered in this guide:

1. **Check the console** for detailed error messages
2. **Search existing issues** on the GitHub repository
3. **Create a new issue** with:
   - Server version and software
   - Plugin version
   - Full error logs
   - Configuration files
   - Steps to reproduce

## Next Steps

After successful setup:
- Read [GAMEPLAY.md](GAMEPLAY.md) for detailed gameplay mechanics
- Review [API.md](API.md) if you're a developer
- Check [EXAMPLES.md](EXAMPLES.md) for configuration examples
- Join the community for tips and support