# Nitnem Sahib

Sikh daily prayer (Nitnem) Android app built with Samsung OneUI design libraries.

## Build

No local Android Studio needed. CI handles builds via GitHub Actions.

```bash
# Local build (requires JDK 17+ and Gradle 8.7+)
gradle assembleDebug
gradle assembleRelease

# Update bani data from BaniDB API
python3 scripts/fetch_bani_data.py
```

## Architecture

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target/Compile SDK**: 35
- **UI**: Samsung OneUI via [sesl-material-components-android](https://github.com/tribalfs/sesl-material-components-android) and [sesl-androidx](https://github.com/tribalfs/sesl-androidx)
- **Data**: Static JSON files in `app/src/main/assets/banis/`, generated from [BaniDB API](https://github.com/KhalisFoundation/banidb-api)

## Project Structure

```
app/src/main/java/ltd/realquick/nitnem/
├── NitnemApp.kt              # Application class
├── MainActivity.kt           # Home screen with bani list
├── ui/
│   ├── bani/BaniActivity.kt  # Bani reading screen
│   ├── home/BaniAdapter.kt   # RecyclerView adapter
│   ├── settings/SettingsActivity.kt
│   └── about/
│       ├── AboutActivity.kt
│       └── OssLicensesActivity.kt
├── data/
│   ├── model/                # Data classes
│   ├── BaniRepository.kt     # Loads JSON from assets
│   └── PrefsManager.kt       # SharedPreferences wrapper
└── util/                     # Extensions, AutoScroller
```

## Data Pipeline

1. `scripts/fetch_bani_data.py` fetches from `https://api.banidb.com/v2/banis/{id}`
2. Outputs JSON files to `app/src/main/assets/banis/`
3. App reads these at runtime — no network calls needed

## Key Conventions

- Reuse single BaniActivity for all banis; load data by bani slug
- Sukhmani Sahib gets special handling (Astpadi tabs) via same activity with conditional UI
- Transliteration-only display (no Gurmukhi dual view) — one language at a time
- Settings: English/Hindi/Punjabi transliteration toggle
- All font size and scroll speed settings stored in SharedPreferences
- Per-bani overrides gated behind Advanced settings toggles

## CI/CD

- `dev.yml`: On push to main — builds debug APK, uploads as artifact
- `release.yml`: On tag push (v*) — builds release APK + AAB, creates GitHub release
