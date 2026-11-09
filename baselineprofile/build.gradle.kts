plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

/**
 * Tests that drive the real app from outside, on an emulator or phone:
 * one generates the Baseline Profile that ships in the app, the others
 * measure cold start and scrolling. They run against a release-like build
 * of :app (built the way release is, not debuggable), never against a debug
 * build, whose numbers mean nothing. That build is signed with the debug key,
 * or with the upload key when keystore.properties configures one; then it
 * cannot be installed over a debug build until the debug build is removed.
 *
 *   ./gradlew :app:generateBaselineProfile
 *   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 */
android {
    namespace = "com.riztechacademy.mandibhav.baselineprofile"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        // Generating a profile needs Android 9 (API 28) or later, and a
        // device that is either rooted or on Android 13 (API 33) or later.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // The emulator or phone you have connected, not a Gradle-managed device.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
