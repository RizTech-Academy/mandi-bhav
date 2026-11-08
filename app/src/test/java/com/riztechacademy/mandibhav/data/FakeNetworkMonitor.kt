package com.riztechacademy.mandibhav.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [NetworkMonitor] a test switches on and off. */
class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    private val state = MutableStateFlow(online)

    /** Setting it is "the connection changed". */
    var online: Boolean
        get() = state.value
        set(value) {
            state.value = value
        }

    override val isOnline: Flow<Boolean> = state

    override fun isOnlineNow(): Boolean = state.value
}
