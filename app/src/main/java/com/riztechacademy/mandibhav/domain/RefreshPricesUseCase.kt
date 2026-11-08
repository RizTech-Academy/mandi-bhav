package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.data.AlertRepository
import com.riztechacademy.mandibhav.data.PriceRepository
import com.riztechacademy.mandibhav.notifications.AlertNotifier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A refresh, and what follows it: the prices are downloaded through the
 * repository's one single-flight refresh, then every alert is compared with
 * the new prices, and each one that has newly crossed its level is announced.
 *
 * The Prices screen and the daily worker both refresh through here, so an
 * alert is checked whichever of them brought the new price.
 */
@Singleton
class RefreshPricesUseCase @Inject constructor(
    private val prices: PriceRepository,
    private val alerts: AlertRepository,
    private val notifier: AlertNotifier,
) {
    /**
     * Two refreshes share one download, so they finish together and would
     * check together; one at a time, and the second finds nothing new.
     */
    private val checking = Mutex()

    /** Refreshes, checks the alerts, and returns how many prices changed. Throws [com.riztechacademy.mandibhav.data.RefreshException]. */
    suspend operator fun invoke(): Int {
        val changed = prices.refresh()
        checkAlerts()
        return changed
    }

    /**
     * Compares the alerts with the saved prices. The notifications go out
     * before the crossed states are saved: if the process dies in between,
     * the next check announces the crossing again, which is better than a
     * crossing that is recorded and never announced.
     */
    suspend fun checkAlerts() = checking.withLock {
        val check = checkAlerts(alerts.allAlerts(), prices.todaysPrices().first())
        notifier.notify(check.newlyCrossed)
        alerts.setCrossed(check.crossedChanges)
    }
}
