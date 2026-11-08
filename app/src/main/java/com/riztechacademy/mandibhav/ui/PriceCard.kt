package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice
import java.time.format.DateTimeFormatter
import java.util.Locale

private val priceDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun PriceCard(price: CommodityPrice, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = price.commodity,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${price.market} · ${priceDateFormat.format(price.date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${formatRupees(price.modalPrice)} / quintal",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Range ${formatRupees(price.minPrice)} – ${formatRupees(price.maxPrice)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceCardPreview() {
    MaterialTheme {
        PriceCard(price = SamplePrices.lasalgaonOnion)
    }
}

@Preview(name = "Large text", showBackground = true, fontScale = 1.5f, widthDp = 320)
@Composable
private fun PriceCardLargeTextPreview() {
    MaterialTheme {
        PriceCard(price = SamplePrices.puneOnion)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PriceCardSamplesPreview(
    @PreviewParameter(SamplePriceProvider::class) price: CommodityPrice,
) {
    MaterialTheme {
        PriceCard(price = price)
    }
}
