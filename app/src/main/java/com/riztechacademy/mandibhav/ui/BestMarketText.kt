package com.riztechacademy.mandibhav.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.riztechacademy.mandibhav.model.CommodityPrice

/**
 * The best market for a commodity, in words: "Best today: Lasalgaon APMC,
 * ₹2,450". If [here] is the best market itself, says so instead.
 */
fun describeBestMarket(best: CommodityPrice, here: CommodityPrice? = null): String =
    if (here != null && here.id == best.id) {
        "The best price for ${best.commodity.lowercase()} today"
    } else {
        "Best today: ${best.market}, ${formatRupees(best.modalPrice)}"
    }

/** [describeBestMarket] as a line of text, read with spoken rupees by TalkBack. */
@Composable
fun BestMarketText(best: CommodityPrice, modifier: Modifier = Modifier, here: CommodityPrice? = null) {
    val spoken = if (here != null && here.id == best.id) {
        describeBestMarket(best, here)
    } else {
        "Best today: ${best.market}, ${spokenRupees(best.modalPrice)}"
    }
    Text(
        text = describeBestMarket(best, here),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { contentDescription = spoken },
    )
}
