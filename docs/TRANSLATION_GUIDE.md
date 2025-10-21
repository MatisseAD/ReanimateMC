# ReanimateMC Documentation Translation Guide

This guide explains how to translate the ReanimateMC documentation into other languages.

## Current Status

- ✅ **English (en)**: Complete (root directory)
- ✅ **French (fr)**: Complete example provided
- 🔄 **Spanish (es)**: Template ready
- 🔄 **German (de)**: Template ready
- 🔄 **Italian (it)**: Template ready
- 🔄 **Russian (ru)**: Template ready
- 🔄 **Chinese (zh)**: Template ready
- 🔄 **Korean (kr)**: Template ready
- 🔄 **Portuguese (pt)**: Template ready

## Directory Structure

```
docs/
├── index.html                 # English (default)
├── commands.html
├── permissions.html
├── configuration.html
├── npc-system.html
├── api.html
├── assets/
│   ├── css/
│   └── js/
├── fr/                        # French translations
│   ├── index.html
│   ├── commands.html
│   ├── permissions.html
│   ├── configuration.html
│   ├── npc-system.html
│   └── api.html
├── es/                        # Spanish translations (to be created)
│   └── ...
└── [other language codes]/
```

## How to Translate

### Step 1: Create Language Directory

Create a new directory for your language using its ISO 639-1 code:

```bash
mkdir -p docs/[language-code]
```

Examples:
- `docs/es` for Spanish
- `docs/de` for German
- `docs/it` for Italian
- `docs/ru` for Russian
- `docs/zh` for Chinese
- `docs/kr` for Korean
- `docs/pt` for Portuguese

### Step 2: Copy English Files

Copy all HTML files from the root docs directory to your language directory:

```bash
cp docs/*.html docs/[language-code]/
```

### Step 3: Translate Content

Open each HTML file and translate the following sections:

#### In Every File:

1. **Page Title** (in `<title>` tag)
2. **Navigation Menu** items
3. **Main Content**:
   - Headings (`<h1>`, `<h2>`, `<h3>`, etc.)
   - Paragraphs (`<p>`)
   - Lists (`<ul>`, `<ol>`)
   - Table headers and content
4. **Footer** content

#### Important Notes:

- **DO NOT** translate:
  - Code examples (keep in English)
  - Command syntax (keep as-is)
  - Permission names (keep as-is)
  - Configuration keys (keep as-is)
  - HTML tags and attributes
  - CSS classes
  - JavaScript code

- **DO** translate:
  - All descriptive text
  - Button labels
  - Section titles
  - Instructions
  - Error messages in examples (optional)

### Step 4: Update Links

Update all internal links to point to the correct language directory:

**Before** (English):
```html
<a href="commands.html">Commands</a>
```

**After** (Your language):
```html
<a href="commands.html">Commandes</a>
```

The link target stays the same (`commands.html`) because files are in the same directory.

For links to other languages or the root:
```html
<a href="../index.html">English</a>
<a href="../fr/index.html">Français</a>
```

### Step 5: Update Language Selector

In the navbar, make sure the current language is pre-selected in the language dropdown.

### Example Translation Comparison

#### English (index.html):
```html
<h1 class="section-title">Core Features</h1>
<p>Players enter a knockout state instead of dying instantly when health reaches zero.</p>
```

#### French (fr/index.html):
```html
<h1 class="section-title">Fonctionnalités Principales</h1>
<p>Les joueurs entrent dans un état KO au lieu de mourir instantanément lorsque leur santé atteint zéro.</p>
```

## Translation Priority

Translate pages in this order for best user experience:

1. **index.html** - Homepage (most important)
2. **commands.html** - Commands reference
3. **permissions.html** - Permissions reference
4. **configuration.html** - Configuration guide
5. **npc-system.html** - NPC system documentation
6. **api.html** - Developer API

## Quality Checklist

Before submitting translations:

- [ ] All user-facing text is translated
- [ ] Code examples remain in English
- [ ] Commands and permissions are NOT translated
- [ ] Links work correctly within the language directory
- [ ] Language selector displays correctly
- [ ] No broken images or missing assets
- [ ] Text fits well in buttons and navigation
- [ ] Special characters display correctly
- [ ] Formatting is preserved (bold, italic, code blocks)

## Testing Your Translation

1. Open your translated HTML file in a browser
2. Check all links work
3. Verify language selector changes language correctly
4. Ensure code blocks are readable
5. Check responsive design on mobile
6. Validate HTML if possible

## Contributing Translations

To contribute a translation:

1. Create your language directory with translated files
2. Test thoroughly
3. Create a pull request on GitHub
4. Include "Translation: [Language Name]" in PR title
5. Mention which pages you translated

## Getting Help

- Review the French (fr/) translation as a reference
- Check plugin language files: `src/main/resources/lang/`
- Ask in GitHub Issues if you need clarification
- Consult the existing README.md for context

## Language-Specific Notes

### Right-to-Left (RTL) Languages

For RTL languages (Arabic, Hebrew, etc.), you'll need to:

1. Add `dir="rtl"` to the `<html>` tag
2. Possibly adjust CSS for proper alignment
3. Consider mirroring layouts where appropriate

### CJK Languages (Chinese, Japanese, Korean)

- Ensure font support for your character set
- May need to adjust line-height in CSS
- Consider word-breaking rules

### Cyrillic (Russian, Bulgarian, etc.)

- Verify character encoding (UTF-8)
- Check font rendering
- Test on different browsers

## Maintenance

When the English documentation is updated:

1. Check for changes in English files
2. Update corresponding translated files
3. Mark the translation as updated in this guide
4. Submit a pull request with updates

## Questions?

- Open an issue on GitHub
- Tag @MatisseAD for translation questions
- Check existing translations for examples

Thank you for contributing to ReanimateMC documentation! 🎉
