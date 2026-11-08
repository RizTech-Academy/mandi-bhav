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
best market for that commodity today. Alerts is a placeholder until the notifications module.

The prices and the seven-day histories in `data/SamplePrices.kt` are realistic, not real. The
Refresh button applies a set of "4 pm closing prices" from the same file after a one-second pause
that stands in for the network. The app fetches real prices in the networking module.

## How the code is organised

| Package | Layer | What is in it |
|---|---|---|
| `ui` | UI | Screens (stateless composables), their routes, one `ViewModel` per screen, and one UI-state class per screen |
| `navigation` | UI | Navigation 3 keys and the per-tab back stacks |
| `domain` | Domain | `bestMarketFor` and `FindBestMarketUseCase`: the one business rule two screens share |
| `data` | Data | `PriceRepository`, the interface every screen gets its prices from, and `SamplePriceRepository`, its in-memory implementation |
| `model` | Shared | `CommodityPrice` and `DayPrice`, plain data classes every layer uses |
| `di` | Wiring | The Hilt module that says which `PriceRepository` the app uses |

Dependencies point one way: `ui` → `domain` → `data`. The repository is the single source of
truth: one instance, bound `@Singleton` in `di/DataModule.kt`, exposing every answer as a `Flow`,
so a refresh reaches every screen that is open. Module 8 replaces `SamplePriceRepository` with a
Room-backed one by changing the binding; no screen changes.

## Running it

1. Install a current Android Studio.
2. In the SDK Manager, install the **Android 37.2** platform (API 37, minor level 2). The project
   compiles against it and the build fails without it.
3. Open this folder in Android Studio (`File > Open`), let the Gradle sync finish, and run the
   `app` configuration on an emulator or a phone running Android 8.0 (API 26) or later.

From the command line:

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew installDebug         # install on a connected device or emulator
```

Android Studio writes `local.properties` with your SDK path on first sync; it is not committed.

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
| kotlinx-serialization-core (with the Kotlin serialization plugin) | 1.12.0 |
| kotlinx-coroutines (android, test) | 1.11.0 |
| Hilt (hilt-android, hilt-compiler, Gradle plugin) | 2.60.1 |
| KSP | 2.3.13 |
| androidx.hilt (hilt-lifecycle-viewmodel-compose) | 1.4.0 |
| compileSdk | 37.2 |
| targetSdk | 36 |
| minSdk | 26 |

All versions live in `gradle/libs.versions.toml`.
