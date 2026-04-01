# Nitnem Sahib

Gurbani daily prayer app for Android, built with One UI design.

10 banis bundled offline from [BaniDB](https://github.com/KhalisFoundation/banidb-api). No network needed at runtime.

## Building

Needs Java 21 and Gradle 8.12. For local builds, you need GitHub package credentials for the One UI / SESL artifacts (set `gpr.user` and `gpr.token` in `gradle.properties` or export `GITHUB_ACTOR` / `GITHUB_TOKEN`). CI gets this automatically.

```bash
gradle assembleDebug
gradle assembleRelease
gradle bundleRelease
```

To refresh the bundled bani JSON from BaniDB:

```bash
python3 scripts/fetch_bani_data.py
```

## CI

- **dev.yml** -- builds debug APK on push/PR to `main`
- **release.yml** -- builds release APK + AAB on `v*` tags, creates a GitHub release

## Credits

- [BaniDB](https://github.com/KhalisFoundation/banidb-api) for Gurbani data
- [SESL/OneUI libraries](https://github.com/tribalfs) by tribalfs
- Ik Onkar icon from [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Ek_onkar.svg)
