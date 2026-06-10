package ru.akarakuts.russiancheckers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.akarakuts.russiancheckers.ui.CheckersUiState
import ru.akarakuts.russiancheckers.ui.PlayScreen
import ru.akarakuts.russiancheckers.ui.theme.RussianCheckersTheme

/** Smoke: [PlayScreen] renders status and new-game control. */
@RunWith(AndroidJUnit4::class)
class PlayScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playScreen_showsNewGameButton() {
        composeRule.setContent {
            RussianCheckersTheme {
                PlayScreen(
                    state = CheckersUiState(),
                    onCell = {},
                    onNewGameRequest = {},
                    onDismissSaveError = {},
                )
            }
        }
        composeRule.onNodeWithText("New game").assertIsDisplayed()
    }
}
