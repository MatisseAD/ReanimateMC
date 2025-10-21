# ReanimateMC Documentation Website

Welcome to the ReanimateMC documentation! This is a comprehensive, multilingual documentation website for the ReanimateMC Minecraft plugin.

## 🌐 Viewing the Documentation

### Online (GitHub Pages)

Once deployed with GitHub Pages, the documentation will be available at:
```
https://matissead.github.io/ReanimateMC/
```

### Locally

To view the documentation locally:

1. Clone the repository
2. Navigate to the `docs` directory
3. Open `index.html` in your web browser
4. Or use a local server:
   ```bash
   cd docs
   python3 -m http.server 8000
   # Then visit http://localhost:8000
   ```

## 📚 Documentation Structure

### Main Pages (English)

- **index.html** - Homepage with plugin overview and features
- **commands.html** - Complete commands reference
- **permissions.html** - Detailed permissions documentation
- **configuration.html** - Configuration guide with all settings
- **npc-system.html** - NPC summon system documentation
- **api.html** - Developer API documentation

### Language Directories

- **fr/** - French translations (Complete example provided)
- **es/** - Spanish translations (To be added)
- **de/** - German translations (To be added)
- **it/** - Italian translations (To be added)
- **ru/** - Russian translations (To be added)
- **zh/** - Chinese translations (To be added)
- **kr/** - Korean translations (To be added)
- **pt/** - Portuguese translations (To be added)

### Assets

- **assets/css/style.css** - Modern, responsive stylesheet
- **assets/js/main.js** - JavaScript for language switching and interactivity

## 🎨 Features

### Design & UI

- ✅ Modern, clean design with gradient accents
- ✅ Fully responsive (mobile, tablet, desktop)
- ✅ Dark navbar with primary color scheme
- ✅ Card-based layout for features
- ✅ Beautiful hero section with stats
- ✅ Smooth animations and transitions
- ✅ Font Awesome icons throughout
- ✅ Syntax-highlighted code blocks

### Functionality

- ✅ Language switcher in navigation
- ✅ Smooth scrolling navigation
- ✅ Back-to-top button
- ✅ Copy code blocks on click
- ✅ Responsive navigation menu
- ✅ Comprehensive linking between pages

### Content Coverage

- ✅ Complete command reference with examples
- ✅ Detailed permissions documentation
- ✅ Full configuration guide
- ✅ NPC system comprehensive docs
- ✅ Developer API with code examples
- ✅ LuckPerms setup examples
- ✅ Integration patterns and best practices

## 🌍 Supported Languages

Currently implemented:
- 🇬🇧 **English** (en) - Complete ✅
- 🇫🇷 **French** (fr) - Example complete ✅

Planned/Template Ready:
- 🇪🇸 Spanish (es)
- 🇩🇪 German (de)
- 🇮🇹 Italian (it)
- 🇷🇺 Russian (ru)
- 🇨🇳 Chinese (zh)
- 🇰🇷 Korean (kr)
- 🇵🇹 Portuguese (pt)

See [TRANSLATION_GUIDE.md](TRANSLATION_GUIDE.md) for how to add new translations.

## 📖 Documentation Sections

### 1. Commands
- All plugin commands with detailed descriptions
- Usage examples for each command
- Permission requirements
- Administrative, player, NPC, and utility commands
- Command aliases

### 2. Permissions
- Complete permission list
- Default values for each permission
- Permission group examples
- LuckPerms configuration examples
- NPC system permissions
- Best practices for permission setup

### 3. Configuration
- All config.yml settings explained
- Default values and descriptions
- Configuration tips by server type
- NPC system configuration
- Performance optimization tips

### 4. NPC System
- Three NPC types: Golem, Healer, Protector
- How to summon and use NPCs
- NPC behavior and AI
- Requirements and restrictions
- Configuration options
- Use cases and scenarios

### 5. Developer API
- Getting started guide
- Maven/Gradle dependency setup
- Event API (PlayerKOEvent, PlayerReanimatedEvent)
- KOManager API methods
- Integration examples
- Best practices for developers

## 🚀 Deployment

### GitHub Pages

To deploy the documentation with GitHub Pages:

1. Go to repository Settings
2. Navigate to Pages section
3. Select source: Deploy from branch
4. Select branch: main (or your branch)
5. Select folder: /docs
6. Save

The documentation will be live at:
```
https://[username].github.io/[repository]/
```

### Custom Domain

To use a custom domain:

1. Add a `CNAME` file in the docs directory with your domain
2. Configure DNS records at your domain provider
3. Enable HTTPS in GitHub Pages settings

## 🔧 Customization

### Colors

Main colors are defined in CSS variables in `assets/css/style.css`:

```css
:root {
    --primary-color: #e74c3c;    /* Red */
    --secondary-color: #3498db;  /* Blue */
    --dark-color: #2c3e50;       /* Dark gray */
    --success-color: #27ae60;    /* Green */
    /* ... */
}
```

### Logo

To add a custom logo:
1. Add logo image to `assets/images/`
2. Update `.nav-brand` in HTML files
3. Adjust CSS if needed

### Content

To update content:
1. Edit the HTML files directly
2. Maintain the existing structure
3. Test changes locally before committing
4. Update translations accordingly

## 📝 Contributing

### Adding Content

1. Update the relevant HTML file
2. Test locally
3. Update translations if needed
4. Submit a pull request

### Adding Translations

1. Follow the [TRANSLATION_GUIDE.md](TRANSLATION_GUIDE.md)
2. Create language directory
3. Translate all pages
4. Test language switching
5. Submit a pull request

### Reporting Issues

- Typos or errors: Open an issue
- Missing translations: Open an issue or contribute
- Suggestions: Open a discussion or issue

## 🔗 Links

- **Plugin Repository**: https://github.com/MatisseAD/ReanimateMC
- **Issues**: https://github.com/MatisseAD/ReanimateMC/issues
- **Releases**: https://github.com/MatisseAD/ReanimateMC/releases

## 📄 License

The documentation follows the same license as the plugin itself. See the main repository LICENSE file.

## 👤 Author

**Jachou**

- GitHub: [@MatisseAD](https://github.com/MatisseAD)
- Plugin: ReanimateMC

## 🙏 Credits

- **Icons**: Font Awesome
- **Design**: Custom CSS with modern web standards
- **Hosting**: GitHub Pages

---

**Need help?** Open an issue on the GitHub repository or check the plugin documentation at the main README.md
