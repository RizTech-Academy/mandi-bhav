package com.riztechacademy.mandibhav.data

import kotlinx.coroutines.delay

/**
 * Why a refresh brought no prices, in the app's own words. The network
 * layer translates Retrofit's, OkHttp's and the JSON parser's exceptions into
 * one of these, so nothing above the data layer needs to know they exist.
 */
sealed interface RefreshFailure {
    /** The phone has no working connection: airplane mode, no signal. */
    data object NoConnection : RefreshFailure

    /** The phone is online, but the server could not be reached: refused, reset, DNS. */
    data object Unreachable : RefreshFailure

    /** The server took too long: connecting, between bytes, or overall. */
    data object Timeout : RefreshFailure

    /** The server answered with an HTTP error, such as 404 or 503. */
    data class Server(val code: Int) : RefreshFailure

    /** The server answered 200, but with something the app cannot read. */
    data object BadData : RefreshFailure
}

/** A refresh that failed, and why. The only exception the data layer lets out of [PriceRepository.refresh]. */
class RefreshException(val failure: RefreshFailure, cause: Throwable? = null) :
    Exception("Refresh failed: $failure", cause)

/**
 * Whether trying again in a moment could help. Fast failures that are
 * usually brief (a 503, a refused connection) are worth a retry; a timeout
 * on a slow connection would probably time out again and cost the farmer
 * more data; no connection, a 404 or bad data will not change in seconds.
 */
val RefreshFailure.isWorthRetryingSoon: Boolean
    get() = when (this) {
        RefreshFailure.Unreachable -> true
        is RefreshFailure.Server -> code >= 500 || code == 408 || code == 429
        RefreshFailure.NoConnection, RefreshFailure.Timeout, RefreshFailure.BadData -> false
    }

/**
 * Whether the same refresh is worth trying again in some minutes, by a
 * background job that is not keeping anybody waiting. Broader than
 * [isWorthRetryingSoon]: a timeout or a lost connection often clears up in
 * ten minutes, though not in two seconds. A 404 or a file the app cannot
 * read will not, so there is no point waking the phone up for it.
 */
val RefreshFailure.isWorthRetryingLater: Boolean
    get() = when (this) {
        RefreshFailure.NoConnection, RefreshFailure.Unreachable, RefreshFailure.Timeout -> true
        is RefreshFailure.Server -> code >= 500 || code == 408 || code == 429
        RefreshFailure.BadData -> false
    }

/**
 * Runs [block], and if it fails with a failure worth retrying, waits and
 * runs it again: at most [attempts] times in all, waiting 1 s, then 2 s,
 * then 4 s. `delay` is a suspension point, so cancelling the caller stops
 * the waiting at once; [RefreshException] is the only thing caught, so a
 * cancellation is never mistaken for a failure.
 */
suspend fun <T> retryingTransientFailures(
    attempts: Int = 3,
    firstDelayMillis: Long = 1_000,
    block: suspend () -> T,
): T {
    var wait = firstDelayMillis
    repeat(attempts - 1) {
        try {
            return block()
        } catch (e: RefreshException) {
            if (!e.failure.isWorthRetryingSoon) throw e
        }
        delay(wait)
        wait *= 2
    }
    return block()
}
