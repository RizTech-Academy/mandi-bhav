package com.riztechacademy.mandibhav.model

/** Which side of the level the user wants to hear about. */
enum class AlertDirection {
    /** A farmer waiting to sell: tell me when the price goes above my number. */
    Above,

    /** A trader waiting to buy: tell me when it falls below. */
    Below,
}

/**
 * "Tell me when onion at Lasalgaon goes above ₹2,500 a quintal."
 *
 * [level] is whole rupees per quintal, compared with the modal price.
 * [isCrossed] is whether the last price the phone saw was already past the
 * level. It is what stops the same crossing being announced every day: the
 * alert fires when it changes from false to true, and is ready to fire again
 * only after the price has come back.
 */
data class PriceAlert(
    val id: Long,
    val commodity: String,
    val market: String,
    val direction: AlertDirection,
    val level: Long,
    val isCrossed: Boolean = false,
)

/** Whether [modalPrice] is past this alert's level. Exactly at the level is not past it. */
fun PriceAlert.isMetBy(modalPrice: Long): Boolean = when (direction) {
    AlertDirection.Above -> modalPrice > level
    AlertDirection.Below -> modalPrice < level
}
