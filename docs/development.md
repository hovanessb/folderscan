## Kotlin Jetpack Compose

The project is a single-module Gradle build using Jetpack Compose.

**Requirements**

1. JDK 17
2. Android SDK Platform 34 (`compileSdk`/`targetSdk` 34, `minSdk` 29)
3. Android Gradle Plugin 8.2.2 / Gradle 8.2 (via the wrapper)

**Recommended tools**

- Android Studio
- `adb` for debugging

### Building

```bash
./gradlew assembleDebug
```

The debug build signs with `app/debug.keystore` when that file is present and otherwise falls
back to the SDK's generated debug key. Release builds are signed only when
`app/signing.properties` exists (see [build.md](./build.md)); both files are gitignored.

### Layout

| Path | Contents |
|------|----------|
| `cs108library/` | Kotlin port of the CS108 SDK (`cslibrary4a`) that drives the handheld reader |
| `data/settings/` | Server address and credentials, encrypted at rest |
| `database/` | Room entities, DAOs and repositories |
| `services/` | REST client, DataWedge integration, upload worker, navigation graph |
| `ui/` | Compose screens, components and view models |

### Reading RFID tags

`ConvergenceHandgunViewModel` owns the reader session. It runs one coroutine that drains the
library's packet queue in bulk (`Cs108Library4A.drainRfidEvents`), de-duplicates off the main
thread, and publishes batches on `tagReads` at most ten times a second. Screens collect that
flow inside a `LaunchedEffect` -- never during composition.
