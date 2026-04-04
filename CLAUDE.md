# Nitnem Sahib

Sikh daily prayer Android app using One UI style libraries.

## Build

```bash
# Java 21, Gradle 8.12, GitHub package creds required
gradle assembleDebug
gradle assembleRelease
gradle bundleRelease

# refresh bundled bani JSON
python3 scripts/fetch_bani_data.py
```

## Stack

- Kotlin
- min SDK 26, target/compile SDK 35
- One UI via SESL / tribalfs libraries
- Offline JSON assets in `app/src/main/assets/banis/`

## Structure

```text
app/src/main/java/ltd/realquick/nitnem/
├── MainActivity.kt
├── data/
│   ├── BaniRepository.kt
│   ├── PrefsManager.kt
│   └── model/Bani.kt
├── ui/
│   ├── about/
│   │   ├── AboutActivity.kt
│   │   └── OssLicensesActivity.kt
│   ├── bani/
│   │   ├── BaniActivity.kt
│   │   ├── ReaderAdapter.kt
│   │   └── ScrollSpeedDialogFragment.kt
│   ├── home/BaniAdapter.kt
│   └── settings/SettingsActivity.kt
└── util/AutoScroller.kt
```

## Reader Notes

- Reader is `RecyclerView`-based, not `NestedScrollView`-based.
- Resume card is rendered as a real header item in `ReaderAdapter`.
- Saved resume state now uses:
  - overall scroll fraction only for the `5%..85%` gate
  - paragraph anchor + intra-paragraph offset for accurate restore
- Do not revert resume logic back to fraction-only restore unless the reader architecture changes again.
- `saveScrollPosition()` intentionally skips saving while the resume card header is visible.

## Sections

- Sukhmani tabs are anchored from `salok ||`
- Asa Di Vaar tabs are anchored after `pauRee ||`
  - skip the marker paragraph
  - skip the first Pauri content block
  - skip separator-only rows like `❁`
  - anchor to the next real content paragraph

## Reader Controls

- Titlebar:
  - play/pause auto-scroll action
  - overflow for font size and fullscreen
- Bottom-end FAB:
  - speed dialog trigger
  - only visible while auto-scroll is active
- Back-to-top:
  - uses `RecyclerView.seslSetGoToTopEnabled(...)`
  - no reflection path anymore

## Speed

- Auto-scroll speed is stored as a percentage of current text line height per second
- Range: `60..160`
- Default: `100`
- Dialog uses `ScrollSpeedDialogFragment` with:
  - `SeekBarPreferencePro`
  - `EditTextPreference`

## Font

- Range: `12sp..32sp`
- Default: `18sp`
- Step: `1sp`

## Settings

- General:
  - transliteration
  - bani length
  - center align
  - keep screen on
  - remember reading position
- Features:
  - auto-scroll
  - back-to-top
- Advanced:
  - per-bani speed
  - per-bani font size

Changing transliteration or bani length clears all remembered reading positions.

## Data Model

- Each bani JSON stores the full verse set once
- Length inclusion flags:
  - `es` short
  - `em` medium
  - `el` long
  - `ex` extra long
- Missing flag means `true`
- Filtering happens client-side in `BaniRepository`

## Bundled Banis

- Japji Sahib
- Shabad Hazare
- Jaap Sahib
- Tavprasad Savaiye (Sraavag Sudh)
- Tavprasad Savaiye (Deenan Kee)
- Chaupai Sahib
- Anand Sahib
- Rehras Sahib
- Aarti
- Kirtan Sohila
- Ardas
- Sukhmani Sahib
- Asa Di Vaar

## Misc

- Home, settings, and OSS licenses start collapsed.
- Bani reader starts expanded.
- About screen GitHub link: `https://github.com/RealQuickLtd/nitnem`
- CI:
  - `dev.yml` builds debug APK
  - `release.yml` builds release APK + AAB and publishes release artifacts