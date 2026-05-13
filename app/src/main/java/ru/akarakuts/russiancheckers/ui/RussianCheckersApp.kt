package ru.akarakuts.russiancheckers.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.akarakuts.russiancheckers.R

/** Bottom navigation route ids. */
object NavRoutes {
    const val PLAY = "play"
    const val RULES = "rules"
    const val SETTINGS = "settings"
}

private fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/** Root scaffold: top bar, bottom tabs, [NavHost] for Play / Rules / Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RussianCheckersApp(vm: CheckersViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: NavRoutes.PLAY
    val activity = LocalContext.current as ComponentActivity
    val accent = MaterialTheme.colorScheme.primary
    val state by vm.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(),
                title = {
                    Text(
                        when (route) {
                            NavRoutes.PLAY -> stringResource(R.string.screen_play)
                            NavRoutes.RULES -> stringResource(R.string.screen_rules)
                            NavRoutes.SETTINGS -> stringResource(R.string.screen_settings)
                            else -> stringResource(R.string.app_name)
                        },
                    )
                },
                actions = {
                    TextButton(onClick = { activity.finish() }) {
                        Text(stringResource(R.string.exit))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = route == NavRoutes.PLAY,
                    onClick = { nav.navigateTab(NavRoutes.PLAY) },
                    icon = { Icon(Icons.Filled.SportsEsports, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_play)) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = accent, selectedTextColor = accent),
                )
                NavigationBarItem(
                    selected = route == NavRoutes.RULES,
                    onClick = { nav.navigateTab(NavRoutes.RULES) },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_rules)) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = accent, selectedTextColor = accent),
                )
                NavigationBarItem(
                    selected = route == NavRoutes.SETTINGS,
                    onClick = { nav.navigateTab(NavRoutes.SETTINGS) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = accent, selectedTextColor = accent),
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = NavRoutes.PLAY,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            composable(NavRoutes.PLAY) {
                PlayScreen(
                    state = state,
                    onCell = vm::onCellClicked,
                    onNewGame = vm::newGame,
                )
            }
            composable(NavRoutes.RULES) {
                RulesScreen()
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(vm = vm)
            }
        }
    }
}
