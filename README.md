# WickKit

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jsanzo97/wickkit-core?label=Maven%20Central&labelColor=4CAF50&color=555555)](https://central.sonatype.com/search?namespace=io.github.jsanzo97)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-555555?logo=kotlin&logoColor=white&labelColor=7F52FF)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.3.2-555555?logo=android&logoColor=white&labelColor=3DDC84)](https://developer.android.com/build)
[![API](https://img.shields.io/badge/API-21%2B-555555?labelColor=2ea44f)](https://developer.android.com/about/versions/lollipop)
[![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.08.00-555555?labelColor=4285F4)](https://developer.android.com/jetpack/compose/bom)
[![License](https://img.shields.io/badge/License-Apache%202.0-555555?labelColor=0057D8)](https://www.apache.org/licenses/LICENSE-2.0)
[![Coverage](https://img.shields.io/codecov/c/github/Jsanzo97/WickKit?label=Coverage&labelColor=F01F7A&color=555555&logo=codecov&logoColor=white)](https://codecov.io/gh/Jsanzo97/WickKit)

WickKit is a debug overlay SDK for Android that surfaces real-time diagnostics inside your app during development. A notification appears automatically on first launch — tap it to open a bottom-sheet panel with seven inspection tabs. Swipe the panel down to dismiss it, or drag it partially and release to snap it back. Zero configuration needed: the SDK self-initializes via a `ContentProvider`. The panel remembers the last tab you had open and the exact screen you were on within each tab — re-opening the overlay always picks up exactly where you left off, including active search text and filters. The overlay UI is available in English, Spanish, French, German, and Italian.

No more switching to Logcat, no more attaching a profiler, no more writing one-off debug screens. WickKit keeps everything in one persistent panel that stays out of the way in production via a no-op stub.

---

## Tabs

### Logs
Streams Logcat output in real time. Entries are color-coded by level (Verbose / Debug / Info / Warn / Error) and filterable by level chip or by text. The search field accepts plain text or a `tag:` prefix to narrow by tag (e.g. `tag:OkHttp`). The **App / All** toggle suppresses noisy system tags automatically. Long-press any entry to copy it to the clipboard; the share button exports the current filtered view as plain text. Search text, selected level, and the App/All toggle are preserved when you close and reopen the overlay.

**How it works:** `WickKitLogcat` opens a `ProcessBuilder` to `logcat -v time` and parses each line into a typed `LogEntry` held in a `StateFlow`, capped at a fixed ring-buffer size so memory stays bounded.

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/logs.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/logs-filtered.png" width="275">

---

### Network
Lists every HTTP request captured since the session started: method badge, status code, URL, duration, and timestamp. Tap a row to see the full detail screen (request headers, request body, response headers, response body). Long-press a row — or tap **Mock this request** from the detail screen — to pre-fill a mock rule for that URL.

The **Mocks** screen lets you define URL pattern rules (substring match) with a method filter, a custom status code, a response body, and an optional artificial delay. Rules can be individually toggled on or off without deleting them. Active rules are counted on the toolbar button. The active search text and method filter are preserved when you close and reopen the overlay.

**How it works:** `WickKitNetworkInterceptor` (OkHttp) and `WickKitKtorInterceptor` (Ktor) capture requests and push `NetworkEntry` objects into `WickKitNetworkManager`, a `StateFlow`-backed ring buffer. Mock interception runs before the real network call when a matching enabled rule exists.

> **Requires `wickkit-network`**. Without it the tab is always empty.

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/network.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/network-details.png" width="275">
<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/network-mocking.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/network-mocked.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/network-mock-rules.png" width="275">

---

### Database
Browses every SQLite database found in the app's private data directory. The list shows each database name and its file size. Databases protected by SQLCipher or other encryption are marked as encrypted and cannot be opened.

Navigate into a database to see its tables with row counts, then into a table to view the data in a horizontally scrollable grid. Cells are editable inline — tap to start editing, confirm with the keyboard **Done** action. All tables are editable, including those without a declared primary key (WickKit uses SQLite's implicit `rowid` as the edit anchor, hidden from the UI). Edits are written back via a raw `UPDATE` SQL statement and the row is highlighted to confirm the change. Row highlights are preserved when you close and reopen the overlay — a highlighted row stays marked until an external change reverts it in the database.

**How it works:** `DatabaseDiscovery` scans `context.databasePath("")` to enumerate `.db` files. `DatabaseManager` wraps a `SQLiteDatabase` in read-write mode. When a supported ORM has already opened the database, WickKit reuses the same connection and notifies the ORM so that edits made in the overlay propagate live to the app's UI — no polling or restart needed.

**Live update support by ORM:**

| ORM | Live update in the app | Prerequisite |
|---|---|---|
| **Room** | ✅ | Query must return `Flow<...>` or `LiveData<...>` and be actively observed |
| **SQLDelight** | ✅ | Query must use `.asFlow()` and be actively collected |
| Raw `SQLiteDatabase` / other ORMs | ❌ | Edits are written to the database but the app's UI will not refresh automatically |

WickKit plugs into each ORM's own notification mechanism: for Room it relies on `InvalidationTracker` (SQLite DDL triggers fired on any write to the shared connection); for SQLDelight it calls `notifyListeners(tableName)` on the driver via reflection after each edit. Both mechanisms are wired automatically by the Gradle plugin — no changes to app code are needed.

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/database.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/database-edited.png" width="275">

---

### Flags
Shows two sections with a unified search bar at the top. Type any text to filter both SharedPreferences entries and Remote Config entries simultaneously by key name — no need to know which section a key belongs to. The search text is preserved when you close and reopen the overlay.

**SharedPreferences** — enumerates every `.xml` file in the app's shared-prefs directory. Each file expands to show all key-value pairs with their type (Boolean / Int / Long / Float / String / StringSet). Values are editable inline and written back immediately via the standard SharedPreferences editor.

**Remote Config** — shows the current value of every Firebase Remote Config key, plus the override value if one has been set. Typing a new value in the override field stores it locally so the next `getValue` call for that key returns the overridden value instead of the fetched one, without touching the real remote config.

**How it works:** SharedPreferences files are read directly from the private directory. Remote Config wrapping is provided by `WickKitRemoteConfig.wrap(context, firebaseRc)`, which delegates all calls to the real object but intercepts `getValue`-style calls to check for local overrides first.

> **`wickkit-flags` required for Firebase Remote Config integration.** SharedPreferences always works with `wickkit-core` alone.

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/shared-preferences.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/remote-config.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/flags-search.png" width="275">

---

### Leaks
Tracks potential memory leaks in Activities and Fragments. When an Activity or Fragment is destroyed, WickKit stores a `WeakReference` to it. After a short delay a GC is triggered; if the reference has not been cleared, the object is reported as a likely leak with the class name and the time it was detected. Tap a leak to see its full detail entry.

**How it works:** `ObjectWatcher` is called from `ActivityLifecycleCallbacks.onActivityDestroyed` and from a `FragmentManager.FragmentLifecycleCallbacks.onFragmentDestroyed`. It schedules a `Handler` post to check whether the weak reference has been collected after a configurable delay.

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/memory-leaks.png" width="275">

---

### Performance
Displays live runtime metrics grouped in three sections.

**FPS** — current frames per second and a color-coded status (green ≥ 55 fps, orange ≥ 30, red below).

**Memory** — Java heap used and maximum, native heap used, and a low-memory flag.

**Compose** — lists every tracked composable by name with its current recomposition rate (recompositions per second) and its peak rate since the last Activity resume. The list can be sorted by name or by peak rate. This section is only populated when the WickKit Gradle plugin is applied — without it the section shows an instructional message.

**How it works:** `WickKitPerformanceManager` posts a repeating runnable on the `Choreographer` to count frames. Memory is read from `ActivityManager.MemoryInfo` and `Debug.getNativeHeapAllocatedSize()`. Compose recomposition tracking is done at bytecode level by the `wickkit-gradle-plugin`, which uses ASM to instrument every `@Composable` function and report calls to `WickKitComposeTracker`.

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/performance.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/performance-issues.png" width="275">

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

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/device-info.png" width="275">

---

### Test App
The repository includes a sample app (`app` module) that exercises every WickKit feature from a single screen. It uses the full SDK setup — `wickkit-core`, `wickkit-network`, `wickkit-flags`, `wickkit-compose`, and the Gradle plugin — so it serves as a living reference for integration.

Each button on the main screen triggers a specific scenario:

| Button | What it does |
|---|---|
| **Open Debug Panel** | Opens the WickKit overlay directly via `WickKit.open()` |
| **Generate Logs** | Emits a batch of log lines at all levels through both Timber and `android.util.Log` |
| **Make Network Requests** | Fires five HTTP calls to `jsonplaceholder.typicode.com` (GETs, a POST, and a deliberate 404) via OkHttp with `WickKitNetworkInterceptor` attached |
| **Reseed Sample Database** | Resets a bundled SQLite database with fresh rows across multiple tables |
| **Seed Sample Preferences** | Populates a set of SharedPreferences files with typed values (boolean, int, string, etc.) |
| **Fetch Remote Config** | Triggers a Firebase Remote Config fetch using the wrapped `WickKitRemoteConfig` instance |
| **Simulate Memory Leak** | Starts a `LeakedActivity` that immediately finishes but stores a static reference to itself, triggering a leak report in the Leaks tab after a few seconds |
| **Simulate Performance Issues** | Opens a `JankActivity` that recomposes two composables every 16 ms and provides a button to intentionally block the main thread for 300 ms, producing measurable slow frames |

<img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/notification.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/test-app-home.png" width="275"> <img src="https://github.com/Jsanzo97/WickKit/blob/develop/screenshots/test-app-performance.png" width="275">

---

## Installation

Add the dependencies you need in your module's `build.gradle.kts`. Use `debugImplementation` for the real modules and `releaseImplementation` for the no-op stubs so nothing reaches production.

### Minimum — core only

```kotlin
dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-core:1.3.0")
    releaseImplementation("io.github.jsanzo97:wickkit-no-op:1.3.0")
}
```

This gives you: Logs, Database, Leaks, Performance (FPS + memory), Device. The Network tab appears but stays empty (no interceptors). The Flags tab shows SharedPreferences only (no Firebase RC).

### With network inspection

```kotlin
dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-core:1.3.0")
    debugImplementation("io.github.jsanzo97:wickkit-network:1.3.0")
    releaseImplementation("io.github.jsanzo97:wickkit-no-op:1.3.0")
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
    debugImplementation("io.github.jsanzo97:wickkit-core:1.3.0")
    debugImplementation("io.github.jsanzo97:wickkit-flags:1.3.0")
    releaseImplementation("io.github.jsanzo97:wickkit-no-op:1.3.0")
}
```

Wrap your `FirebaseRemoteConfig` instance once at startup:

```kotlin
val rc = WickKitRemoteConfig.wrap(context, FirebaseRemoteConfig.getInstance())
// Use rc everywhere instead of the original instance
```

### With Compose recomposition tracking

The plugin instruments every `@Composable` function at build time so the Performance tab can show per-composable recomposition rates. It only runs on the debug variant.

Apply the plugin in the **app module**. It uses `InstrumentationScope.ALL`, which covers the app module and all its dependency modules automatically — no need to apply the plugin to each module individually.

```kotlin
// app/build.gradle.kts
plugins {
    id("io.github.jsanzo97.wickkit") version "1.3.0"
}
```

This works for both single-module and multi-module projects. Composables defined in feature modules, shared UI libraries, or any other dependency are all tracked automatically.

To disable all instrumentation (e.g. for a specific build variant or CI environment):

```kotlin
// app/build.gradle.kts
wickKit {
    enabled = false
}
```

Add the Compose no-op stub for release:

```kotlin
dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-compose:1.3.0")
    releaseImplementation("io.github.jsanzo97:wickkit-compose-no-op:1.3.0")
}
```

### Full setup

```kotlin
// app/build.gradle.kts
plugins {
    id("io.github.jsanzo97.wickkit") version "1.3.0"
}

dependencies {
    debugImplementation("io.github.jsanzo97:wickkit-core:1.3.0")
    debugImplementation("io.github.jsanzo97:wickkit-network:1.3.0")
    debugImplementation("io.github.jsanzo97:wickkit-flags:1.3.0")
    debugImplementation("io.github.jsanzo97:wickkit-compose:1.3.0")

    releaseImplementation("io.github.jsanzo97:wickkit-no-op:1.3.0")
    releaseImplementation("io.github.jsanzo97:wickkit-compose-no-op:1.3.0")
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

## Localization

The overlay UI adapts to the device language automatically. Supported locales:

| Language | Locale |
|---|---|
| English | `en` (default) |
| Spanish | `es` |
| French | `fr` |
| German | `de` |
| Italian | `it` |

Devices configured to any other locale fall back to English. Technical terms (`SharedPreferences`, `Firebase Remote Config`, `OkHttp`, `Compose`, `ABI`, `RAM`) are kept in English across all languages.

---

## Performance considerations

WickKit is built to stay out of the way. A few things worth keeping in mind:

**Zero impact in production.** The no-op stubs (`wickkit-no-op`, `wickkit-compose-no-op`) are empty shells — no threads, no callbacks, no overhead. The `FLAG_DEBUGGABLE` check inside `WickKit.init()` acts as a second safety net: if the real SDK is accidentally included in a release build, it returns immediately without starting anything.

**Debug cold start.** The SDK auto-initializes via a `ContentProvider`, which Android runs synchronously before `Application.onCreate()`. Expect roughly **15–40 ms** added to cold start on debug builds. Nothing runs at startup in release.

**Background behaviour.** The 2-second polling loop continues running when the app is backgrounded, but it performs no system calls while the app is not in the foreground — only a `delay`. The Choreographer frame callback also stops automatically when the current Activity reaches `onStop`. The device can enter deep idle normally.

**Network tab overhead.** Every captured response reads up to 50 KB of the body (via `peekBody` on OkHttp or `bodyAsText` on Ktor), adding roughly **1–3 ms per request** on the HTTP dispatcher thread. The network interceptor is never injected automatically — it only runs if you add it to your client.

**Memory is bounded.** All buffers have hard caps: 100 network entries, 500 log entries, 600 frame-duration samples. There is no unbounded growth regardless of how long the app runs.

**Compose tracking adds build time.** The Gradle plugin's bytecode transform runs only on the debug variant. Depending on project size, it adds roughly 2–5 s (small), 5–15 s (medium), or 15–30 s (large) to debug builds. Release builds are not instrumented.

**Metrics while the overlay is open.** The host activity enters `onPause` but not `onStop` because the overlay is translucent. As a result, FPS and frame-duration data shown in the Performance tab while the panel is open includes the overlay's own rendering cost — not only the host app.

---

## Security considerations

WickKit captures sensitive debug data by design, and that access is deliberately scoped to the debug session only.

**External apps cannot reach the overlay or its data.** Every Android component (`ContentProvider`, `Activity`) is declared with `exported="false"` and no `<intent-filter>`. No external app can start the overlay, query the provider, or access any captured data. All state lives in in-process `StateFlow` objects — there is no `ContentProvider`, `BroadcastReceiver`, or file that exposes data outside the process.

**Nothing runs in production.** The intended setup uses `debugImplementation` / `releaseImplementation` to ensure the real SDK never reaches a release APK. As a second line of defence, `WickKit.init()` checks `ApplicationInfo.FLAG_DEBUGGABLE` and returns immediately if the flag is not set — so a misconfigured build does not accidentally activate the SDK.

**Captured data never touches disk.** Network entries, log lines, database rows, flag values, leak entries, and performance snapshots all live exclusively in memory. When the process is killed, everything is gone.

**Logcat is filtered to the app's own PID.** WickKit reads `logcat --pid=<pid>`, so logs from other installed apps, the system server, or any other process on the device are never captured.

**Database access is confined to the app sandbox.** `DatabaseDiscovery` roots its scan at `context.getDatabasePath("_").parentFile` — the app's private `databases/` directory. It cannot reach databases belonging to other apps. All dynamic SQL identifiers are double-quoted per the SQL standard, and all data values are passed as bind parameters, so there is no SQL injection surface.

**SharedPreferences and Remote Config stay within the app.** SP discovery reads `applicationInfo.dataDir/shared_prefs/` — a directory that Android's filesystem permissions keep private to the app's UID. Firebase Remote Config is accessed exclusively via its public API (`getAll()`, `asString()`); `setAccessible(true)` is never called and no private fields are read.

**The network interceptor is opt-in.** It is never injected automatically. Only requests that pass through a client where you have explicitly added `WickKitNetworkInterceptor` (OkHttp) or installed `WickKitKtorInterceptor` (Ktor) are captured. One-shot request bodies (e.g. multipart uploads) are never read — only a `[one-shot body]` placeholder is stored.

**Mock rules cannot block the network indefinitely.** `delayMs` is capped at 30 seconds regardless of the value configured in the UI. A blank `urlPattern` is rejected at creation time and skipped at match time, so a misconfigured rule cannot silently intercept all traffic.

---

## License

```
Copyright 2026 Jorge Sanzo Hernando

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
