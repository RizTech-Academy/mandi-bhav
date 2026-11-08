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
it their home market, whose prices then come first on the Prices tab. Alerts is a placeholder until
the notifications module.

Prices come from a small static JSON API committed to this repository, in `api/`, and served by
GitHub (`api/README.md` describes it). They are kept on the phone in a Room database, so they
survive the app being closed, the process being killed and the phone restarting, and the app works
with no connection: it says it is offline, shows what it has, and refreshes by itself when the
connection returns. The Prices tab says when they were last fetched. A new install downloads
today's prices and the week before on first launch. Each price card shows a picture of the
commodity, and the detail screen a larger one, loaded with Coil. The home market is kept in
Preferences DataStore.

The prices in the API are realistic, not real: sample data in the shape of the APMC daily prices
the Government publishes on data.gov.in. The app does not call data.gov.in itself, because its API
needs a registered key, and a key in an app is a key anyone can read. `data/SamplePrices.kt` holds
the same kind of data for previews and tests.

## How the code is organised

| Package | Layer | What is in it |
|---|---|---|
| `ui` | UI | Screens (stateless composables), their routes, one `ViewModel` per screen, and one UI-state class per screen |
| `navigation` | UI | Navigation 3 keys and the per-tab back stacks |
| `domain` | Domain | `bestMarketFor` and `FindBestMarketUseCase`: the one business rule two screens share |
| `data` | Data | `PriceRepository`, the interface every screen gets its prices from; `RoomPriceRepository`, its implementation over the database; `RefreshFailure` and the retry policy; `NetworkMonitor`; and `UserPreferencesRepository` over DataStore |
| `data.network` | Data | The Retrofit interface, the JSON classes in the server's field names, and `RetrofitPriceNetworkDataSource`, which turns every failure into a `RefreshFailure` |
| `data.local` | Data | The Room database: entities, the DAO, type converters and the migration |
| `model` | Shared | `CommodityPrice`, `DayPrice` and `Market`, plain data classes every layer uses |
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

## The database

The schema is exported to `app/schemas/`, one JSON file per version, and committed: the migration
tests build old databases from these files. Version 1 had prices and their history; version 2 adds
the `markets` table, through a hand-written migration (`data/local/Migrations.kt`); version 3 adds
`prices.fetchedAt` through a Room auto-migration; version 4 adds the two picture URLs, also
automatically. A new database starts empty and is filled by the first download. After changing an entity, bump the version, build
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
./gradlew testDebugUnitTest    # unit tests: Room under Robolectric, the network against MockWebServer
./gradlew installDebug         # install on a connected device or emulator
```

Android Studio writes `local.properties` with your SDK path on first sync; it is not committed.

The database, migration and DataStore tests run on the JVM with Robolectric, so they need no
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
| Room 3 (room3-runtime, room3-compiler, room3-testing, Gradle plugin `androidx.room3`) | 3.0.3 |
| DataStore (datastore-preferences) | 1.3.0 |
| Robolectric (tests) | 4.17 |
| androidx.test core, ext-junit (tests) | 1.7.0, 1.3.0 |
| compileSdk | 37.2 |
| targetSdk | 36 |
| minSdk | 26 |

All versions live in `gradle/libs.versions.toml`.
