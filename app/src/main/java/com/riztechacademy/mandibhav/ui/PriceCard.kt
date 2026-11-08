package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

private val priceDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/**
 * One price as a card. With an [onClick] the whole card is one tap target;
 * without one (in previews) it is not clickable at all.
 */
@Composable
fun PriceCard(
    price: CommodityPrice,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val description = priceCardDescription(price)
    // TalkBack reads the card as one sentence, not a string of fragments.
    // clearAndSetSemantics also removes the click the card adds, so it is
    // declared again here, with a label TalkBack reads as "double-tap to …".
    val cardModifier = modifier.clearAndSetSemantics {
        contentDescription = description
        if (onClick != null) {
            role = Role.Button
            onClick(label = "open price details") {
                onClick()
                true
            }
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier) { PriceCardContent(price) }
    } else {
        Card(modifier = cardModifier) { PriceCardContent(price) }
    }
}

@Composable
private fun PriceCardContent(price: CommodityPrice) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = price.commodity,
            style = MaterialTheme.typography.titleLarge,
        )
        // Market and date on separate lines: at a large font size a
        // shared line wrapped in the middle of the date.
        Text(
            text = price.market,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = priceDateFormat.format(price.date),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = formatRupees(price.modalPrice),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = "per quintal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Min ${formatRupees(price.minPrice)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Max ${formatRupees(price.maxPrice)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PriceCardPreview() {
    MandiBhavTheme {
        PriceCard(price = SamplePrices.lasalgaonOnion)
    }
}

@Preview(name = "Large text", showBackground = true, fontScale = 1.5f, widthDp = 320)
@Composable
private fun PriceCardLargeTextPreview() {
    MandiBhavTheme {
        PriceCard(price = SamplePrices.puneOnion)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PriceCardSamplesPreview(
    @PreviewParameter(SamplePriceProvider::class) price: CommodityPrice,
) {
    MandiBhavTheme {
        PriceCard(price = price)
    }
}
