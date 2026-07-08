package ru.akarakuts.russiancheckers.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
fun RussianCheckersApp(
    vm: CheckersViewModel,
    onExit: () -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: NavRoutes.PLAY
    val accent = MaterialTheme.colorScheme.primary
    val state by vm.state.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var showNewGameDialog by remember { mutableStateOf(false) }

    BackHandler {
        when (route) {
            NavRoutes.PLAY -> showExitDialog = true
            else -> nav.navigateTab(NavRoutes.PLAY)
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.dialog_exit_title)) },
            text = { Text(stringResource(R.string.dialog_exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showNewGameDialog) {
        AlertDialog(
            onDismissRequest = { showNewGameDialog = false },
            title = { Text(stringResource(R.string.dialog_new_game_title)) },
            text = { Text(stringResource(R.string.dialog_new_game_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showNewGameDialog = false
                    vm.newGame()
                }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

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
                    TextButton(onClick = { showExitDialog = true }) {
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
                    icon = {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.nav_play_cd),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_play)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                    ),
                )
                NavigationBarItem(
                    selected = route == NavRoutes.RULES,
                    onClick = { nav.navigateTab(NavRoutes.RULES) },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.nav_rules_cd),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_rules)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                    ),
                )
                NavigationBarItem(
                    selected = route == NavRoutes.SETTINGS,
                    onClick = { nav.navigateTab(NavRoutes.SETTINGS) },
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.nav_settings_cd),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                    ),
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = NavRoutes.PLAY,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            composable(NavRoutes.PLAY) {
                PlayScreen(
                    state = state,
                    onCell = vm::onCellClicked,
                    onNewGameRequest = { showNewGameDialog = true },
                    onDismissSaveError = vm::dismissSaveLoadError,
                    onUndo = vm::undo,
                    onHint = vm::requestHint,
                )
            }
            composable(NavRoutes.RULES) {
                RulesScreen()
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onNewGameRequest = { showNewGameDialog = true },
                )
            }
        }
    }
}
