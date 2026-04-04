# Nitnem Sahib

Offline Nitnem Android app built with Samsung One UI style components.

Bundled banis are sourced from [BaniDB](https://github.com/KhalisFoundation/banidb-api), converted into compact JSON, and shipped in the app. No runtime network is required.

## Included Banis

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

## Reader

- One shared reader screen for all banis
- Transliteration switch: Punjabi shows Gurmukhi, English/Hindi show transliteration only
- Bani length switch for supported banis, default `Long`
- Resume card shown only for saved positions between `5%` and `85%`
- Sukhmani bottom tabs follow `Salok`
- Asa Di Vaar bottom tabs follow `Pauri` content blocks
- Titlebar play/pause action for auto-scroll
- Bottom-end speed FAB shown only while auto-scroll is active

## Settings

- General: transliteration, bani length, center align, keep screen on, remember reading position
- Features: auto-scroll, back-to-top
- Advanced: per-bani speed, per-bani font size

Changing transliteration or bani length clears remembered reading positions to avoid stale restores.

## Data

- Assets live in `app/src/main/assets/banis/`
- Each bani stores the full verse set once
- Length filtering is done client-side using compact flags:
  - missing flag = included
  - `es` / `em` / `el` / `ex` only appear when a verse is excluded for that length

Refresh bundled JSON from BaniDB:

```bash
python3 scripts/fetch_bani_data.py
```

## Build

Requires:

- Java 21
- Gradle 8.12
- GitHub Packages credentials for SESL / One UI dependencies

Provide either:

- `gpr.user` and `gpr.token` in `gradle.properties`, or
- `GITHUB_ACTOR` and `GITHUB_TOKEN` in the environment

Build commands:

```bash
gradle assembleDebug
gradle assembleRelease
gradle bundleRelease
```

## CI

- `dev.yml`: build debug APK on push and PR to `main`
- `release.yml`: build release APK + AAB on `v*` tags and publish a GitHub release

## Credits

- [BaniDB API](https://github.com/KhalisFoundation/banidb-api)
- [SESL Material Components for Android](https://github.com/tribalfs/sesl-material-components-android)
- [SESL AndroidX](https://github.com/tribalfs/sesl-androidx)
- [OneUI Design Library](https://github.com/tribalfs/oneui-design)
- Ik Onkar icon from [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Ek_onkar.svg)