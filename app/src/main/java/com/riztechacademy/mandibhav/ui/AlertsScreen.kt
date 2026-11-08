package com.riztechacademy.mandibhav.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import com.riztechacademy.mandibhav.notifications.areAlertNotificationsOn
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme

/**
 * The Alerts destination. Besides the ViewModel's state it owns one thing
 * the ViewModel cannot: whether the system lets the app notify, which only
 * the system knows, and which the user can change in Settings at any time.
 *
 * The notification permission is asked for at one moment: when the user
 * saves their first alert, the moment the reason for it is on the screen.
 */
@Composable
fun AlertsRoute(
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var notificationsOn by remember { mutableStateOf(areAlertNotificationsOn(context)) }
    var askingPermission by remember { mutableStateOf(false) }

    // Asked again each time the screen comes back: the user may have been
    // to Settings and turned notifications on, or off.
    LifecycleResumeEffect(Unit) {
        notificationsOn = areAlertNotificationsOn(context)
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askingPermission = false
        notificationsOn = areAlertNotificationsOn(context)
    }

    AlertsScreen(
        uiState = uiState,
        // While the system dialog is up, the card would only repeat it.
        showNotificationsOff = !notificationsOn && !askingPermission && uiState.alerts.isNotEmpty(),
        onCommodityChosen = viewModel::onCommodityChosen,
        onMarketChosen = viewModel::onMarketChosen,
        onDirectionChosen = viewModel::onDirectionChosen,
        onLevelChange = viewModel::onLevelChange,
        onSave = {
            val first = uiState.alerts.isEmpty()
            if (viewModel.save()) {
                focusManager.clearFocus()
                if (first && !notificationsOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    askingPermission = true
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        },
        onDelete = viewModel::delete,
        onOpenSettings = { context.openNotificationSettings() },
        modifier = modifier,
    )
}

/** This app's page in the system's notification settings. */
private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    uiState: AlertsUiState,
    showNotificationsOff: Boolean,
    onCommodityChosen: (String) -> Unit,
    onMarketChosen: (String) -> Unit,
    onDirectionChosen: (AlertDirection) -> Unit,
    onLevelChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (id: Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Alerts") }) },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        // The warning is added above the form; without this, the list would
        // keep the form where it was and the warning would start off screen.
        LaunchedEffect(showNotificationsOff) {
            if (showNotificationsOff) listState.animateScrollToItem(0)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (showNotificationsOff) {
                item(key = "notifications-off") {
                    NotificationsOffCard(onOpenSettings, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
            item(key = "form") {
                NewAlertForm(
                    uiState = uiState,
                    onCommodityChosen = onCommodityChosen,
                    onMarketChosen = onMarketChosen,
                    onDirectionChosen = onDirectionChosen,
                    onLevelChange = onLevelChange,
                    onSave = onSave,
                )
            }
            item(key = "saved-heading") {
                Text(
                    text = "Your alerts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            if (uiState.alerts.isEmpty() && !uiState.isLoading) {
                item(key = "none") {
                    Text(
                        text = "No alerts yet. Mandi Bhav checks your alerts each time the prices " +
                            "are refreshed, including once every evening when the app is closed.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(items = uiState.alerts, key = { it.alert.id }) { row ->
                ListItem(
                    headlineContent = { Text("${row.alert.commodity} at ${row.alert.market}") },
                    supportingContent = { Text(row.description()) },
                    trailingContent = {
                        IconButton(onClick = { onDelete(row.alert.id) }) {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                contentDescription = "Delete the alert for ${row.alert.commodity} at ${row.alert.market}",
                            )
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

/** Said only after there is an alert to miss: before that, there is nothing to warn about. */
@Composable
private fun NotificationsOffCard(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notifications are off", style = MaterialTheme.typography.titleSmall)
            Text(
                "Your alerts are saved, and this screen shows when a price is past your level, " +
                    "but Mandi Bhav cannot tell you when the app is closed.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenSettings) { Text("Turn on in Settings") }
        }
    }
}

@Composable
private fun NewAlertForm(
    uiState: AlertsUiState,
    onCommodityChosen: (String) -> Unit,
    onMarketChosen: (String) -> Unit,
    onDirectionChosen: (AlertDirection) -> Unit,
    onLevelChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val form = uiState.form
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("New alert", style = MaterialTheme.typography.titleMedium)
        Text("Commodity", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(uiState.commodities, form.commodity, onCommodityChosen)
        if (form.commodity != null) {
            Text("Market", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(uiState.markets, form.market, onMarketChosen)
        }
        Text("Tell me when the modal price goes", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = form.direction == AlertDirection.Above,
                    onClick = { onDirectionChosen(AlertDirection.Above) },
                    label = { Text("Above") },
                )
            }
            item {
                FilterChip(
                    selected = form.direction == AlertDirection.Below,
                    onClick = { onDirectionChosen(AlertDirection.Below) },
                    label = { Text("Below") },
                )
            }
        }
        OutlinedTextField(
            value = form.level,
            onValueChange = onLevelChange,
            label = { Text("Rupees a quintal") },
            prefix = { Text("₹") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = form.error != null,
            supportingText = {
                val guide = uiState.todaysPrice?.let { "Today: ${formatRupees(it)} a quintal." }
                Text(form.error ?: guide.orEmpty())
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSave) { Text("Save alert") }
    }
}

@Composable
private fun ChoiceChips(options: List<String>, selected: String?, onChosen: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = options, key = { it }) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onChosen(option) },
                label = { Text(option) },
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AlertsScreenPreview() {
    MandiBhavTheme {
        AlertsScreen(
            uiState = AlertsUiState(
                isLoading = false,
                alerts = listOf(
                    AlertRow(
                        PriceAlert(1, "Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500, isCrossed = false),
                        todaysPrice = SamplePrices.lasalgaonOnion.modalPrice,
                    ),
                ),
                commodities = listOf("Onion", "Tomato", "Potato"),
                markets = listOf("Lasalgaon APMC", "Nashik APMC", "Pune (Gultekdi) APMC"),
                form = NewAlertForm(commodity = "Onion", market = "Lasalgaon APMC", level = "2500"),
                todaysPrice = SamplePrices.lasalgaonOnion.modalPrice,
            ),
            showNotificationsOff = true,
            onCommodityChosen = {},
            onMarketChosen = {},
            onDirectionChosen = {},
            onLevelChange = {},
            onSave = {},
            onDelete = {},
            onOpenSettings = {},
        )
    }
}
