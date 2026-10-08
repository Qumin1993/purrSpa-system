# Android CI (GitHub Actions)

Workflow: [Android CI](../.github/workflows/android-ci.yml)

On every push to `main`, pull request, or manual **Run workflow**, GitHub Actions provisions Java 17, Android SDK 35 and Gradle 8.11.1, then runs:

```sh
gradle --no-daemon --stacktrace :app:testDebugUnitTest :app:assembleDebug
```

## Where to check

1. Open the repository **Actions** tab, then **Android CI**.
2. Open the newest run. A green check means both JVM tests and debug APK build succeeded. A red cross means a failure; open **Compile, test and package** for its logs.
3. Under **Artifacts**, download `unit-test-reports` for test output and, on successful builds, `purr-spa-debug-apk` to install on an Android test device.

The debug APK is for testing only, not a signed Play Store release. GitHub Actions does **not** run UI/instrumented tests on an Android device or emulator in this workflow. The project has no Gradle wrapper yet, so CI installs a pinned Gradle version via `gradle/actions/setup-gradle`. Local developers can use their own Gradle 8.11.1 installation.

**Important:** This workflow was added before a confirmed successful CI run. Until the first green run, compilation and test status remain unverified. Do not use real customer data for tests or upload production databases as artifacts.

CI smoke test requested on 2026-10-08: verify the first Actions build before trusting the pipeline.
