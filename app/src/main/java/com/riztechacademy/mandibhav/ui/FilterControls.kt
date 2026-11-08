package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme

/**
 * The search box. Stateless: it shows [query] and reports each edit to
 * [onQueryChange]; whoever calls it decides what to keep.
 */
@Composable
fun PriceSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search commodity or market") },
        singleLine = true,
        modifier = modifier,
    )
}

/**
 * "All" plus one chip per commodity, in one line that scrolls sideways.
 * Stateless: [selected] is the chosen commodity, or null for all, and a tap
 * is reported to [onCommodityClick] (null for "All"). Tapping the selected
 * chip again is reported too; the caller decides that it clears the filter.
 */
@Composable
fun CommodityChips(
    commodities: List<String>,
    selected: String?,
    onCommodityClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onCommodityClick(null) },
                label = { Text("All") },
            )
        }
        items(items = commodities, key = { it }) { commodity ->
            FilterChip(
                selected = selected == commodity,
                onClick = { onCommodityClick(commodity) },
                label = { Text(commodity) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun FilterControlsPreview() {
    MandiBhavTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PriceSearchField(
                query = "pune",
                onQueryChange = {},
                modifier = Modifier.fillMaxWidth(),
            )
            CommodityChips(
                commodities = listOf("Onion", "Tomato", "Potato", "Wheat", "Soybean", "Tur dal"),
                selected = "Tomato",
                onCommodityClick = {},
            )
        }
    }
}
