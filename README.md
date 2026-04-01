# Nitnem Sahib

A Sikh daily prayer (Nitnem) app for Android, built with Samsung OneUI design.

## Features

- **10 Banis**: Japji Sahib, Jaap Sahib, Sukhmani Sahib, Shabad Hazare, Chaupai Sahib, Rehras Sahib, Kirtan Sohila, Anand Sahib, and both Tavprasad Savaiye
- **Transliteration**: English, Hindi, and Punjabi — switch in settings
- **Auto-scroll**: Smooth auto-scroll with adjustable speed
- **Sukhmani Sahib**: Scrollable Astpadi tabs with auto-switching
- **Resume reading**: Pick up where you left off with a suggestion card
- **Fullscreen mode**: Immersive reading with system bars hidden
- **Adjustable font size**: A-/A+ controls in the toolbar
- **Per-bani settings**: Optional per-bani font size and scroll speed
- **Samsung OneUI**: Native OneUI look and feel with collapsing toolbar, search mode, and themed icons

## Screenshots

*Coming soon*

## Build

Requires JDK 17+ and a GitHub token for SESL Maven packages.

```bash
# Set credentials (or use GITHUB_ACTOR / GITHUB_TOKEN env vars)
echo "gpr.user=YOUR_GITHUB_USERNAME" >> gradle.properties
echo "gpr.token=YOUR_GITHUB_TOKEN" >> gradle.properties

# Build
gradle assembleDebug
```

## Data

Bani text is sourced from the [BaniDB API](https://github.com/KhalisFoundation/banidb-api) and stored as static JSON files in the app assets.

```bash
# Refresh bani data
python3 scripts/fetch_bani_data.py
```

## License

This project is open source. Bani data is sourced from BaniDB (GPL-3.0).

## Credits

- [BaniDB](https://github.com/KhalisFoundation/banidb-api) for Gurbani data
- [SESL/OneUI libraries](https://github.com/tribalfs) by tribalfs
- Ik Onkar icon from [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Ek_onkar.svg)
