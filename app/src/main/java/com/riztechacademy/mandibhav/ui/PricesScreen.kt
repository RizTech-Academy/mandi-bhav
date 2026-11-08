package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(prices: List<CommodityPrice>, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (price in prices) {
                PriceCard(price = price, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PricesScreenPreview() {
    MaterialTheme {
        PricesScreen(prices = SamplePrices.all)
    }
}
