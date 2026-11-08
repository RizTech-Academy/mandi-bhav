# Mandi Bhav

Mandi Bhav shows agricultural market (APMC mandi) prices for farmers and traders: one commodity at
one market on one date, in rupees per quintal (100 kg), with the minimum, maximum and modal (most
common) price. It is built for a farmer deciding where to sell and a trader comparing markets,
often on a mid-range phone with a weak signal.

It is the reference app for RizTech Academy's
[Android Development with Kotlin](https://www.riztechacademy.com/learn/android-kotlin-development)
course. The course builds it module by module, and this repository holds the code as each module
leaves it.

The app has three destinations, Prices, Markets and Alerts, in a navigation bar on a phone and a
navigation rail from 600 dp wide. A price opens a detail screen with its last seven days and the
best market for that commodity today. A market's screen shows its district and lets the user make
it their home market, whose prices then come first on the Prices tab. The Alerts tab keeps price
alerts, such as "onion at Lasalgaon above ₹2,500 a quintal", and the app sends a notification when
a refresh brings a price that newly crosses one. The Markets tab can sort the markets by distance
from the user's approximate location, and a market opens in a maps app. A price can be shared as
text through the share sheet, and the farmer can keep a photo of her sale slip with each commodity
and market, taken with the camera app or chosen with the photo picker.

Prices come from a small static JSON API committed to this repository, in `api/`, and served by
GitHub (`api/README.md` describes it). They are kept on the phone in a Room database, so they
survive the app being closed, the process being killed and the phone restarting, and the app works
with no connection: it says it is offline, shows what it has, and refreshes by itself when the
connection returns. The Prices tab says when they were last fetched. A new install downloads
today's prices and the week before on first launch, and WorkManager refreshes them once a day,
in the evening, whether or not the app is open. Each price card shows a picture of the
commodity, and the detail screen a larger one, loaded with Coil. The home market is kept in
Preferences DataStore.

The prices in the API are realistic, not real: sample data in the shape of the APMC daily prices
the Government publishes on data.gov.in. The app does not call data.gov.in itself, because its API
needs a registered key, and a key in an app is a key anyone can read. `data/SamplePrices.kt` holds
the same kind of data for previews and tests.

## How the code is organised

| Package | Layer | What is in it |
|---|---|---|
| `ui` | UI | Screens (stateless composables), their routes, one `ViewModel` per screen, and one UI-state class per screen; `ui.permissions`, the runtime-permission helper; `Intents.kt`, sharing and maps |
| `navigation` | UI | Navigation 3 keys, the per-tab back stacks, and `DeepLinks.kt`, which builds and checks the app's `mandibhav://` links |
| `domain` | Domain | `bestMarketFor` and `FindBestMarketUseCase`; `distanceKm` and `nearestMarkets`; `checkAlerts`, which decides which alerts have newly crossed; and `RefreshPricesUseCase`, the one refresh the screen and the worker both call |
| `data` | Data | `PriceRepository`, the interface every screen gets its prices from; `RoomPriceRepository`, its implementation over the database; `RefreshFailure` and the retry policy; `NetworkMonitor`; `UserPreferencesRepository` over DataStore; `AlertRepository`, the price alerts in Room; `LocationSource`, one location fix from the framework's `LocationManager`; and `SaleSlipRepository`, slip photos as private files with a row each in Room |
| `data.network` | Data | The Retrofit interface, the JSON classes in the server's field names, and `RetrofitPriceNetworkDataSource`, which turns every failure into a `RefreshFailure` |
| `data.local` | Data | The Room database: entities, the DAO, type converters and the migration |
| `model` | Shared | `CommodityPrice`, `DayPrice`, `Market`, `GeoPoint`, `PriceAlert` and `SaleSlip`, plain data classes every layer uses |
| `work` | Background | `PriceRefreshWorker`, the daily refresh, and `DailyRefresh`, its schedule |
| `notifications` | Platform | The notification channel, and `SystemAlertNotifier`, which posts one notification per crossed alert |
| `di` | Wiring | The Hilt modules that build the database, the DataStore, the HTTP client and Retrofit, the app-wide coroutine scope, and say which implementations the app uses |

Dependencies point one way: `ui` → `domain` → `data`. The database is the single source of truth:
every screen watches a Room query through the repository, and a refresh writes to the database, so
it reaches every screen that is open. A failed refresh writes nothing and the screens keep the
saved prices.

## The network

| Failure | What the app says | Retried at once? |
|---|---|---|
| No connection | "No connection. Showing the prices saved on this phone." | No; it refreshes by itself when the phone is back online, while the Prices tab is open |
| Server unreachable (refused, reset) | "Could not reach the prices server. …" | Yes, up to 3 tries, 1 s then 2 s apart |
| Timeout (10 s to connect or between reads, 20 s in all) | "The prices server is taking too long to answer. …" | No |
| HTTP 5xx, 408, 429 | "The prices server has a problem (error 503). …" | Yes, as above |
| Other HTTP errors (404) | The same, with the code | No |
| JSON the app cannot read | "The prices server sent prices this app cannot read. …" | No |

One refresh runs at a time across the app, in an app-wide coroutine scope, so a download finishes
and is saved even if the screen that started it goes away. When a new day's prices arrive, the
previous day's price moves into `price_history` rather than staying beside the new one.

Debug builds log every request (tag `OkHttp`), turn on StrictMode for the main thread (tag
`StrictMode`), and can read the API from your computer: see `api/README.md`. Release builds allow
HTTPS only.

## Work in the background

`PriceRefreshWorker` is a Hilt-injected `CoroutineWorker`. It calls `RefreshPricesUseCase`, the same
function as the Refresh button: the repository's single-flight refresh, then the alert check. It
downloads nothing itself.

| | |
|---|---|
| Schedule | Unique periodic work `daily-price-refresh`, every 24 hours from the next 7 pm, `ExistingPeriodicWorkPolicy.KEEP` |
| Constraint | Any working network (`NetworkType.CONNECTED`) |
| Retry | No connection, unreachable, timeout and 5xx/408/429 retry with exponential backoff from 10 minutes, at most 4 tries a day; 404 and bad data do not |
| Survives | The app being swiped away mid-download (it runs again after the backoff) and process death; a force-stop cancels it until the app is next opened |

`MandiBhavApplication` gives WorkManager Hilt's worker factory, so the manifest removes WorkManager's
default start-up initialiser. Debug builds add `RunRefreshReceiver`, to run the worker once on demand
(WorkManager will not run periodic work before its time, even when JobScheduler is told to):

```bash
adb shell am broadcast -n com.riztechacademy.mandibhav/.work.RunRefreshReceiver
adb shell am broadcast -n com.riztechacademy.mandibhav/.work.RunRefreshReceiver --el delay 120
```

The API is static files, so a daily refresh brings new prices only when the files in `api/v1/`
change. To see a new day arrive locally, serve a changed `prices.json` (see `api/README.md`).

## Notifications

One channel, "Price alerts" (`price_alerts`). The `POST_NOTIFICATIONS` permission (Android 13 and
later) is asked for when the first alert is saved, never at launch; if it is refused, the Alerts
tab says so and links to the app's notification settings. A crossing is announced once: an alert
fires when the modal price goes past its level, and again only after the price has come back.
Each alert has its own notification, in one group with a summary; a tap opens today's price for
that commodity and market (not the notified day's price, which the next refresh replaces), and a
tap on the summary opens the Alerts tab. `MainActivity` is `singleTop`, so a tap while the app is
open arrives through `onNewIntent`.

## Permissions and device features

| Feature | Permission | Asked when |
|---|---|---|
| Price alerts | `POST_NOTIFICATIONS` (Android 13 and later) | The first alert is saved |
| Nearest market | `ACCESS_COARSE_LOCATION` only: a market is chosen by town, not street | "Use my location" on the Markets tab |
| Sale slip photo, camera | None: the camera app takes the photo and writes it into a file the app offers through a `FileProvider` (`ActivityResultContracts.TakePicture`) | Never |
| Sale slip photo, gallery | None: the photo picker hands over only the picture chosen (`PickVisualMedia`) | Never |
| Share, Show on map | None: implicit intents (`ACTION_SEND`, `geo:`) | Never |

The app never declares `CAMERA`: an app that declares it and has not been granted it cannot start
the camera app at all (`SecurityException`). A picked picture is copied into `files/slips/` at once,
because the picker's Uri stops being readable when the app's process ends. Android does not tell an
app whether a permission is "never asked" or "refused twice"; the app remembers whether the user has
refused location in DataStore (`refused_permissions`). A refused-once permission gets an explanation
before the second and last system dialog; a blocked one sends the user to the app's page in
Settings. The location is used once, on the phone, and is not saved or sent anywhere.

## Deep links

`mandibhav://price?market=<market>&commodity=<commodity>` opens today's price for that commodity and
market; `mandibhav://alerts` opens the Alerts tab. Notifications use the same links, as explicit
intents to `MainActivity`. A custom scheme, not a verified https App Link, because the app has no
website to verify; any app could claim the scheme, so the links carry nothing private and
`parseDeepLink` checks every one. Try them with

```bash
adb shell am start -a android.intent.action.VIEW -d "mandibhav://price?market=Lasalgaon%20APMC&commodity=Onion" com.riztechacademy.mandibhav
adb shell am start -a android.intent.action.VIEW -d "mandibhav://alerts" com.riztechacademy.mandibhav
```

## The database

The schema is exported to `app/schemas/`, one JSON file per version, and committed: the migration
tests build old databases from these files. Version 1 had prices and their history; version 2 adds
the `markets` table, through a hand-written migration (`data/local/Migrations.kt`); version 3 adds
`prices.fetchedAt` through a Room auto-migration; version 4 adds the two picture URLs, also
automatically; version 5 adds the `price_alerts` table, automatically; version 6 adds each market's `latitude` and
`longitude` and the `sale_slips` table, automatically. A new database starts empty and is filled by the first download. After changing an entity, bump the version, build
once so the new JSON exists, then add and test the migration.

## Running it

1. Install a current Android Studio.
2. In the SDK Manager, install the **Android 37.2** platform (API 37, minor level 2). The project
   compiles against it and the build fails without it.
3. Open this folder in Android Studio (`File > Open`), let the Gradle sync finish, and run the
   `app` configuration on an emulator or a phone running Android 8.0 (API 26) or later.

From the command line:

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests: Room and files under Robolectric, the network against MockWebServer, the worker with work-testing, Compose UI under Robolectric
./gradlew installDebug         # install on a connected device or emulator
./gradlew connectedDebugAndroidTest   # instrumented tests on the connected device (Compose UI, and the whole app under Hilt)
```

## Testing, profiling and debugging

- **Compose UI tests** in `app/src/sharedTest` run twice: on the JVM with Robolectric, as part of
  `testDebugUnitTest`, and on a device with `connectedDebugAndroidTest`. The screen with its real
  ViewModel needs the fakes in `app/src/test`, so it runs on the JVM only; the accessibility checks
  run on a device only, because under Robolectric they pass whatever the screen does. `app/src/androidTest` starts the whole app under Hilt with the price repository
  replaced (`TestPriceRepositoryModule`), through `HiltTestRunner`.
- **The `baselineprofile` module** drives a release-like build from outside. It generates the
  Baseline Profile in `app/src/release/generated/baselineProfiles/` and measures cold start and
  scrolling with and without it. On an emulator, add
  `-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR`, and
  treat the numbers as indicative only:

  ```bash
  ./gradlew :app:generateBaselineProfile
  ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
  ```
- **LeakCanary** is in debug builds only. It adds its own "Leaks" launcher icon and writes leaks
  to logcat under the tag `LeakCanary`.
- **Debug builds log how the last process ended** (Android 11 and later), under the tag
  `LastExit`, when it was a crash or an ANR, with the ANR's main thread.

Android Studio writes `local.properties` with your SDK path on first sync; it is not committed.

The database, migration, DataStore and worker tests run on the JVM with Robolectric, so they need no
emulator. Robolectric's Android 36 sandbox needs Java 21, and this project builds on JDK 17, so
`app/src/test/resources/robolectric.properties` runs them on API 35.

## Checkpoints: one branch per module

Each course module that changes the app has a branch named `module-NN-<module-slug>`, for example
`module-03-compose-fundamentals`, holding the app exactly as that module leaves it. Modules that
teach concepts without changing the app have no branch. `main` is the latest state, and its
history is linear, so the difference between two checkpoints is what one module added.

**Use the checkpoints to compare, not to start from.** Type the code from the lessons yourself.
When you are stuck, open the branch for the module you are on and compare your file with it, or
look at the diff between two checkpoint branches on GitHub. Copying a checkpoint gets you a
running app and none of the understanding the module is for.

## Versions

| Tool or library | Version |
|---|---|
| Android Gradle Plugin | 9.5.1 |
| Gradle (wrapper) | 9.9.0 |
| Kotlin | 2.4.21 |
| JDK | 17 |
| Compose BOM | 2026.09.00 (Compose 1.12, Material 3 1.4, Material 3 adaptive 1.3) |
| activity-compose | 1.13.0 |
| core-ktx | 1.19.1 |
| lifecycle (runtime, viewmodel-compose, runtime-compose, viewmodel-navigation3) | 2.11.0 |
| Navigation 3 (navigation3-runtime, navigation3-ui) | 1.2.1 |
| kotlinx-serialization-core, -json (with the Kotlin serialization plugin) | 1.12.0 |
| Retrofit (retrofit, converter-kotlinx-serialization) | 3.0.0 |
| OkHttp (BOM, okhttp, logging-interceptor in debug, mockwebserver3 in tests) | 5.5.1 |
| Coil (coil-compose, coil-network-okhttp) | 3.6.3 |
| kotlinx-coroutines (android, test) | 1.11.0 |
| Hilt (hilt-android, hilt-compiler, Gradle plugin) | 2.60.1 |
| KSP | 2.3.13 |
| androidx.hilt (hilt-lifecycle-viewmodel-compose) | 1.4.0 |
| WorkManager (work-runtime-ktx; work-testing in tests) | 2.12.0 |
| androidx.hilt (hilt-work, hilt-compiler) | 1.4.0 |
| Room 3 (room3-runtime, room3-compiler, room3-testing, Gradle plugin `androidx.room3`) | 3.0.3 |
| DataStore (datastore-preferences) | 1.3.0 |
| Robolectric (tests) | 4.17 |
| androidx.test core, ext-junit (tests) | 1.7.0, 1.3.0 |
| androidx.test runner (instrumented tests) | 1.7.0 |
| Compose UI test (ui-test-junit4, -accessibility, ui-test-manifest; from the BOM) | 1.12.1 |
| hilt-android-testing (instrumented tests) | 2.60.1 |
| LeakCanary (debug only) | 2.14 |
| profileinstaller | 1.4.1 |
| Benchmark (benchmark-macro-junit4, Gradle plugin `androidx.baselineprofile`) | 1.5.0 |
| UI Automator (benchmarks) | 2.4.1 |
| compileSdk | 37.2 |
| targetSdk | 36 |
| minSdk | 26 |

All versions live in `gradle/libs.versions.toml`.
