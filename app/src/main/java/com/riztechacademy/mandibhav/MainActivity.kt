package com.riztechacademy.mandibhav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.riztechacademy.mandibhav.ui.PricesRoute
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MandiBhavTheme {
                PricesRoute(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
