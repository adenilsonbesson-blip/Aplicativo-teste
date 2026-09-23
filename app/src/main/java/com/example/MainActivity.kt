package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.MainScreen
import com.example.ui.theme.AquaAlertaTheme
import com.example.ui.viewmodel.HydrationViewModel
import com.example.ui.viewmodel.HydrationViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: HydrationViewModel by viewModels {
        HydrationViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            AquaAlertaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.example.ACTION_OPEN_ALARM_RINGING") {
            // Intent received from clicking on the persistent alarm notification
            // The overlay will automatically react to viewModel.isAlarmRinging
        }
    }
}
