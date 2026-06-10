package ru.akarakuts.russiancheckers

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.akarakuts.russiancheckers.ui.CheckersViewModel
import ru.akarakuts.russiancheckers.ui.RussianCheckersApp
import ru.akarakuts.russiancheckers.ui.theme.RussianCheckersTheme

/** Single-activity entry: edge-to-edge Compose, [CheckersViewModel], root [RussianCheckersApp]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Портрет без датчика; дублирует манифест на API < 36 и после смены конфигурации.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RussianCheckersTheme {
                val vm: CheckersViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
                )
                Surface(modifier = Modifier.fillMaxSize()) {
                    RussianCheckersApp(vm, onExit = { finish() })
                }
            }
        }
    }
}
