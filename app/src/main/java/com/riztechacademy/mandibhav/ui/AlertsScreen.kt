package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme

/**
 * The Alerts destination. Price alerts need storage and notifications, which
 * later modules build, so for now the screen says what it will do and when.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Alerts") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Price alerts are not built yet.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Later versions of Mandi Bhav will tell you when a price crosses a " +
                    "level you choose, such as onion at Lasalgaon above ₹2,500 a quintal.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AlertsScreenPreview() {
    MandiBhavTheme {
        AlertsScreen()
    }
}
