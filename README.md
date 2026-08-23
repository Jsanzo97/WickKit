# WickKit

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jsanzo97/wickkit-core?label=Maven%20Central&labelColor=4CAF50&color=555555)](https://central.sonatype.com/search?namespace=io.github.jsanzo97)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-555555?logo=kotlin&logoColor=white&labelColor=7F52FF)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.3.1-555555?logo=android&logoColor=white&labelColor=3DDC84)](https://developer.android.com/build)
[![API](https://img.shields.io/badge/API-21%2B-555555?labelColor=2ea44f)](https://developer.android.com/about/versions/lollipop)
[![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.08.00-555555?labelColor=4285F4)](https://developer.android.com/jetpack/compose/bom)
[![License](https://img.shields.io/badge/License-Apache%202.0-555555?labelColor=0057D8)](https://www.apache.org/licenses/LICENSE-2.0)

WickKit is a debug overlay SDK for Android that surfaces real-time diagnostics inside your app during development. A notification appears automatically on first launch — tap it to open a bottom-sheet panel with seven inspection tabs. Zero configuration needed: the SDK self-initializes via a `ContentProvider`.

No more switching to Logcat, no more attaching a profiler, no more writing one-off debug screens. WickKit keeps everything in one persistent panel that stays out of the way in production via a no-op stub.

---

## Tabs

### Logs
Streams Logcat output in real time. Entries are color-coded by level (Verbose / Debug / Info / Warn / Error) and filterable by level chip or by text. The search field accepts plain text or a `tag:` prefix to narrow by tag (e.g. `tag:OkHttp`). The **App / All** toggle suppresses noisy system tags automatically. Long-press any entry to copy it to the clipboard; the share button exports the current filtered view as plain text.

**How it works:** `WickKitLogcat` opens a `ProcessBuilder` to `logcat -v time` and parses each line into a typed `LogEntry` held in a `StateFlow`, capped at a fixed ring-buffer size so memory stays bounded.

---

### Network
Lists every HTTP request captured since the session started: method badge, status code, URL, duration, and timestamp. Tap a row to see the full detail screen (request headers, request body, response headers, response body). Long-press a row — or tap **Mock this request** from the detail screen — to pre-fill a mock rule for that URL.

The **Mocks** screen lets you define URL pattern rules (substring match) with a method filter, a custom status code, a response body, and an optional artificial delay. Rules can be individually toggled on or off without deleting them. Active rules are counted on the toolbar button.

**How it works:** `WickKitNetworkInterceptor` (OkHttp) and `WickKitKtorInterceptor` (Ktor) capture requests and push `NetworkEntry` objects into `WickKitNetworkManager`, a `StateFlow`-backed ring buffer. Mock interception runs before the real network call when a matching enabled rule exists.

> **Requires `wickkit-network`**. Without it the tab is always empty.

---

### Database
Browses every SQLite database found in the app's private data directory. The list shows each database name and its file size. Databases protected by SQLCipher or other encryption are marked as encrypted and cannot be opened.

Navigate into a database to see its tables with row counts, then into a table to view the data in a horizontally scrollable grid. Cells in tables that have a primary key are editable inline — tap to start editing, confirm with the keyboard **Done** action. Edits are written back via a raw `UPDATE` SQL statement and the row is highlighted to confirm the change.

**How it works:** `DatabaseDiscovery` scans `context.databasePath("")` to enumerate `.db` files. `DatabaseManager` wraps a `SQLiteDatabase` opened in read-write mode to read columns, rows, and persist edits.

---

### Flags
Shows two sections side by side.

**SharedPreferences** — enumerates every `.xml` file in the app's shared-prefs directory. Each file expands to show all key-value pairs with their type (Boolean / Int / Long / Float / String / StringSet). Values are editable inline and written back immediately via the standard SharedPreferences editor.

**Remote Config** — shows the current value of every Firebase Remote Config key, plus the override value if one has been set. Typing a new value in the override field stores it locally so the next `getValue` call for that key returns the overridden value instead of the fetched one, without touching the real remote config.

**How it works:** SharedPreferences files are read directly from the private directory. Remote Config wrapping is provided by `WickKitRemoteConfig.wrap(context, firebaseRc)`, which delegates all calls to the real object but intercepts `getValue`-style calls to check for local overrides first.

> **`wickkit-flags` required for Firebase Remote Config integration.** SharedPreferences always works with `wickkit-core` alone.

---

### Leaks
Tracks potential memory leaks in Activities and Fragments. When an Activity or Fragment is destroyed, WickKit stores a `WeakReference` to it. After a short delay a GC is triggered; if the reference has not been cleared, the object is reported as a likely leak with the class name and the time it was detected. Tap a leak to see its full detail entry.

**How it works:** `ObjectWatcher` is called from `ActivityLifecycleCallbacks.onActivityDestroyed` and from a `FragmentManager.FragmentLifecycleCallbacks.onFragmentDestroyed`. It schedules a `Handler` post to check whether the weak reference has been collected after a configurable delay.

---

### Performance
Displays live runtime metrics grouped in three sections.

**FPS** — current frames per second and a color-coded status (green ≥ 55 fps, orange ≥ 30, red below).

**Memory** — Java heap used and maximum, native heap used, and a low-memory flag.

**Compose** — lists every tracked composable by name with its current recomposition rate (recompositions per second) and its peak rate since the last Activity resume. The list can be sorted by name or by peak rate. This section is only populated when the WickKit Gradle plugin is applied — without it the section shows an instructional message.

**How it works:** `WickKitPerformanceManager` posts a repeating runnable on the `Choreographer` to count frames. Memory is read from `ActivityManager.MemoryInfo` and `Debug.getNativeHeapAllocatedSize()`. Compose recomposition tracking is done at bytecode level by the `wickkit-gradle-plugin`, which uses ASM to instrument every `@Composable` function and report calls to `WickKitComposeTracker`.

---

### Device
Static information about the device and the running app, organised in sections:

| Section | Fields |
|---|---|
| **App** | Package name, version name + code, build type (debug / release) |
| **Device** | Manufacturer, model, Android version + API level, primary ABI, CPU core count |
| **Display** | Resolution (px), density (dpi + factor), refresh rate, font scale, orientation |
| **Memory** | Total RAM, available RAM, low-memory flag |
| **Storage** | Total internal storage, available internal storage |
| **Locale** | System language, timezone |

---

## Installation

Add the dependencies you need in your module's `build.gradle.kts`. Use `debugImplementation` for the real modules and `releaseImplementation` for the no-op stubs so nothing reaches production.

### Minimum — core only

```kotlin
dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-core:0.1.0")
    releaseImplementation("io.github.jsanzo97:wickkit-no-op:0.1.0")
}
```

This gives you: Logs, Database, Leaks, Performance (FPS + memory), Device. The Network tab appears but stays empty (no interceptors). The Flags tab shows SharedPreferences only (no Firebase RC).

### With network inspection

```kotlin
dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-core:0.1.0")
    debugImplementation("io.github.jsanzo97:wickkit-network:0.1.0")
    releaseImplementation("io.github.jsanzo97:wickkit-no-op:0.1.0")
}
```

Then add the interceptor to your HTTP client:

```kotlin
// OkHttp
val client = OkHttpClient.Builder()
    .addInterceptor(WickKitNetworkInterceptor())
    .build()

// Ktor
val client = HttpClient {
    install(WickKitKtorInterceptor)
}
```

### With Firebase Remote Config overrides

```kotlin
dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-core:0.1.0")
    debugImplementation("io.github.jsanzo97:wickkit-flags:0.1.0")
    releaseImplementation("io.github.jsanzo97:wickkit-no-op:0.1.0")
}
```

Wrap your `FirebaseRemoteConfig` instance once at startup:

```kotlin
val rc = WickKitRemoteConfig.wrap(context, FirebaseRemoteConfig.getInstance())
// Use rc everywhere instead of the original instance
```

### With Compose recomposition tracking

Apply the Gradle plugin in the module where your composables live:

```kotlin
// module-level build.gradle.kts
plugins {
    id("io.github.jsanzo97.wickkit") version "0.1.0"
}
```

Add the Compose no-op stub for release:

```kotlin
dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-compose:0.1.0")
    releaseImplementation("io.github.jsanzo97:wickkit-compose-no-op:0.1.0")
}
```

### Full setup

```kotlin
// module-level build.gradle.kts
plugins {
    id("io.github.jsanzo97.wickkit") version "0.1.0"
}

dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-core:0.1.0")
    debugImplementation("io.github.jsanzo97:wickkit-network:0.1.0")
    debugImplementation("io.github.jsanzo97:wickkit-flags:0.1.0")
    debugImplementation("io.github.jsanzo97:wickkit-compose:0.1.0")

    releaseImplementation("io.github.jsanzo97:wickkit-no-op:0.1.0")
    releaseImplementation("io.github.jsanzo97:wickkit-compose-no-op:0.1.0")
}
```

No further setup is required. The SDK initializes automatically via `WickKitInitializer` (a `ContentProvider`) on app startup.

---

### What happens if a module is missing?

| Missing module | Effect |
|---|---|
| `wickkit-network` | Network tab visible but always empty. No HTTP traffic is captured. |
| `wickkit-flags` | Flags tab shows SharedPreferences only. Firebase Remote Config section is hidden. |
| `wickkit-compose` + Gradle plugin | Performance tab shows FPS and Memory. Compose section shows a message explaining the plugin is not applied. |
| `wickkit-core` | Nothing works — core is the foundation and is required by all other modules. |

---

## License

```
Copyright 2025 Jorge Sanzo Hernando

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
