package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.domain.AlertCrossing
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import com.riztechacademy.mandibhav.notifications.AlertNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** An [AlertRepository] in memory. New alerts start not crossed; a test sets [alerts] directly for anything else. */
class FakeAlertRepository(initial: List<PriceAlert> = emptyList()) : AlertRepository {

    private val current = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    var alerts: List<PriceAlert>
        get() = current.value
        set(value) {
            current.value = value
        }

    override fun alerts(): Flow<List<PriceAlert>> = current

    override suspend fun allAlerts(): List<PriceAlert> = current.value

    override suspend fun add(commodity: String, market: String, direction: AlertDirection, level: Long): Long {
        val id = nextId++
        current.update { it + PriceAlert(id, commodity, market, direction, level) }
        return id
    }

    override suspend fun delete(id: Long) {
        current.update { alerts -> alerts.filterNot { it.id == id } }
    }

    override suspend fun setCrossed(changes: Map<Long, Boolean>) {
        current.update { alerts -> alerts.map { alert -> changes[alert.id]?.let { alert.copy(isCrossed = it) } ?: alert } }
    }
}

/** An [AlertNotifier] that remembers what it was asked to show. */
class FakeAlertNotifier : AlertNotifier {
    val shown = mutableListOf<AlertCrossing>()

    override fun notify(crossings: List<AlertCrossing>) {
        shown += crossings
    }
}
