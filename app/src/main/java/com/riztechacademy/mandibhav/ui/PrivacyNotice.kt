package com.riztechacademy.mandibhav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme

/**
 * Where the full privacy policy is published: PRIVACY.md in the app's own
 * repository, a public page anyone can read without an account. Google Play
 * needs this address in the Play Console and a link or text in the app.
 */
const val PRIVACY_POLICY_URL = "https://github.com/RizTech-Academy/mandi-bhav/blob/main/PRIVACY.md"

/**
 * What the app does with data, in plain words. It must say the same as
 * PRIVACY.md and the Data safety form in the Play Console: change all three
 * together, or none.
 */
val privacySummary = listOf(
    "Mandi Bhav collects no personal data and shares none. There are no accounts, ads, analytics or crash reports.",
    "Prices and pictures are downloaded from Mandi Bhav's server on GitHub. The app sends nothing about you; " +
        "like any website, the server sees your phone's internet address.",
    "If you allow it, your approximate location is used once, on this phone, to find the nearest market. " +
        "It is not saved or sent anywhere.",
    "Your price alerts and sale slip photos stay on this phone. Only your home market is included in your " +
        "phone's own backup, if backup is on.",
)

/** The privacy notice, with a way to the full policy. */
@Composable
fun PrivacyDialog(onOpenPolicy: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                privacySummary.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { TextButton(onClick = onOpenPolicy) { Text("Full policy") } },
    )
}

@Preview
@Composable
private fun PrivacyDialogPreview() {
    MandiBhavTheme { PrivacyDialog(onOpenPolicy = {}, onDismiss = {}) }
}
