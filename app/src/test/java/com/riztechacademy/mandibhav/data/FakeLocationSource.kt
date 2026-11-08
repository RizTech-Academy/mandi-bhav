package com.riztechacademy.mandibhav.data

import kotlinx.coroutines.CompletableDeferred

/**
 * A [LocationSource] for tests: answers [result], or, while [pending] is set,
 * waits until the test completes it, so a test can see the "finding" state.
 */
class FakeLocationSource(var result: LocationResult = LocationResult.NoFix) : LocationSource {
    var pending: CompletableDeferred<LocationResult>? = null
    var calls = 0
        private set

    override suspend fun currentLocation(): LocationResult {
        calls++
        return pending?.await() ?: result
    }
}
