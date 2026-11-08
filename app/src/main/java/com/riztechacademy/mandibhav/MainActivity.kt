package com.riztechacademy.mandibhav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.riztechacademy.mandibhav.ui.MandiBhavApp
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MandiBhavTheme {
                MandiBhavApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
