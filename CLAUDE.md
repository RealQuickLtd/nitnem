# Nitnem Sahib

Sikh daily prayer Android app. Samsung One UI design via SESL libraries.

## Build

```bash
# requires Java 21, Gradle 8.12, GitHub package creds for SESL artifacts
gradle assembleDebug
gradle assembleRelease
gradle bundleRelease

# refresh bani JSON from BaniDB API
python3 scripts/fetch_bani_data.py
```

## Stack

- Kotlin, min SDK 26, target/compile SDK 35
- One UI via [sesl-material-components-android](https://github.com/tribalfs/sesl-material-components-android) and [sesl-androidx](https://github.com/tribalfs/sesl-androidx)
- Static JSON in `app/src/main/assets/banis/` -- no network at runtime

## Structure

```
app/src/main/java/ltd/realquick/nitnem/
├── NitnemApp.kt
├── MainActivity.kt
├── ui/
│   ├── bani/BaniActivity.kt
│   ├── bani/ScrollSpeedDialogFragment.kt
│   ├── home/BaniAdapter.kt
│   ├── settings/SettingsActivity.kt
│   └── about/
│       ├── AboutActivity.kt
│       └── OssLicensesActivity.kt
├── data/
│   ├── model/Bani.kt
│   ├── BaniRepository.kt
│   └── PrefsManager.kt
└── util/
    └── AutoScroller.kt
```

## How the reader works

One `BaniActivity` handles all banis, loaded by slug from assets. Sukhmani Sahib gets extra Astpadi tab handling inside the same activity.

Script display: `pn` = Gurmukhi only, `en`/`hi` = transliteration only.

### Controls

- Titlebar action: play/pause auto-scroll (starting scroll collapses titlebar)
- Overflow menu: font size up/down, fullscreen toggle
- Bottom FAB: speed control, only visible while auto-scrolling

### Speed

- Stored as % of current line height per second
- Range 60..160, default 100
- Configured via `ScrollSpeedDialogFragment` (PreferenceFragmentCompat with SeekBarPreferencePro + EditTextPreference)

### Font size

- Range 12sp..32sp, default 18sp, step 1sp

### Resume

- Card shown only when saved position is 5%..85%

### Per-bani overrides

Speed and font size can be set per bani, but only take effect when their Advanced toggles are on in settings.

## Settings

- General: transliteration, center align, keep screen on, remember position
- Features: auto-scroll, back-to-top
- Advanced: per-bani speed, per-bani font size

## Gotchas

- Auto-scroll uses `Choreographer` with fractional pixel accumulation for smoothness
- Back-to-top uses reflection to call `seslSetGoToTopEnabled(boolean)` on the scroll host -- fragile compared to the SESL RecyclerView path
- Keep-screen-on flag only applies on the reader screen
- About screen GitHub link: `https://github.com/RealQuickLtd/nitnem`

## CI

- `dev.yml`: debug APK on push/PR to `main`
- `release.yml`: release APK + AAB on `v*` tags, creates GitHub release
