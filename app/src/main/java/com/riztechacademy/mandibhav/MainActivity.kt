package com.riztechacademy.mandibhav

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.riztechacademy.mandibhav.navigation.parseDeepLink
import com.riztechacademy.mandibhav.ui.MandiBhavApp
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Where a deep link asked to go, until the app has gone there. */
    private var linkTarget by mutableStateOf<NavKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start. After rotation or process death the intent
        // is the same one, and the back stack already has the screen on it.
        if (savedInstanceState == null) linkTarget = intent.linkTarget()
        // The app was already open (singleTop): the link arrives here.
        addOnNewIntentListener { newIntent -> newIntent.linkTarget()?.let { linkTarget = it } }
        setContent {
            MandiBhavTheme {
                MandiBhavApp(
                    modifier = Modifier.fillMaxSize(),
                    linkTarget = linkTarget,
                    onLinkTargetOpened = { linkTarget = null },
                )
            }
        }
    }

    /**
     * The screen a mandibhav:// link names, from a notification, another app,
     * a web page or adb; null for an ordinary launch or a link that is not
     * one of ours. Everything in [Intent.getData] is untrusted: parseDeepLink checks it.
     */
    private fun Intent.linkTarget(): NavKey? =
        if (action == Intent.ACTION_VIEW) data?.toString()?.let(::parseDeepLink) else null
}
