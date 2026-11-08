package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.RefreshFailure

/**
 * What to tell a farmer when a refresh fails. Each says what happened in
 * plain words and what to do; none blames the user or shows an exception.
 * [hasSavedPrices] decides whether there is something on screen to fall back on.
 */
fun refreshFailureMessage(failure: RefreshFailure, hasSavedPrices: Boolean): String = when (failure) {
    RefreshFailure.NoConnection ->
        if (hasSavedPrices) "No connection. Showing the prices saved on this phone."
        else "No connection. Today's prices will download when the phone is back online."
    RefreshFailure.Unreachable -> "Could not reach the prices server. Try again in a minute."
    RefreshFailure.Timeout -> "The prices server is taking too long to answer. Try again in a minute."
    is RefreshFailure.Server -> "The prices server has a problem (error ${failure.code}). Try again later."
    RefreshFailure.BadData -> "The prices server sent prices this app cannot read. Try again later, or update the app."
}
