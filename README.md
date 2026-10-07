# Purr Spa System

Native Android tablet-first management app for Purr Spa cat grooming.

**Current stage:** Phase 0 UI bootstrap, not production-ready. No customer database, PDFs, or backup implemented yet.

See [project plan](docs/PROJECT_PLAN.md).

## Open on desktop
1. Clone repository.
2. Open root folder in Android Studio with JDK 17 and Android SDK 35.
3. Sync Gradle and run `./gradlew assembleDebug` (requires Gradle wrapper to be generated/added locally).
4. Launch on an Android tablet/emulator (API 26+).

## Known gaps
- Gradle wrapper not yet committed.
- Build and instrumentation tests not run in this environment.
- UI is placeholder navigation, not completed modules.
- Decorative photography and logo assets to be created and licensed separately.
