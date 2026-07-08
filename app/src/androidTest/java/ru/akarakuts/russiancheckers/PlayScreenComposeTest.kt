package ru.akarakuts.russiancheckers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.Piece
import ru.akarakuts.russiancheckers.game.Pos
import ru.akarakuts.russiancheckers.game.Side
import ru.akarakuts.russiancheckers.ui.CheckersUiState
import ru.akarakuts.russiancheckers.ui.LastMove
import ru.akarakuts.russiancheckers.ui.PlayScreen
import ru.akarakuts.russiancheckers.ui.theme.RussianCheckersTheme

/** Smoke: [PlayScreen] renders controls and the game-over overlay. */
@RunWith(AndroidJUnit4::class)
class PlayScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setPlayScreen(state: CheckersUiState) {
        composeRule.setContent {
            RussianCheckersTheme {
                PlayScreen(
                    state = state,
                    onCell = {},
                    onNewGameRequest = {},
                    onDismissSaveError = {},
                    onUndo = {},
                    onHint = {},
                )
            }
        }
    }

    @Test
    fun playScreen_showsControls() {
        setPlayScreen(CheckersUiState())
        composeRule.onNodeWithText("New game").assertIsDisplayed()
        composeRule.onNodeWithText("Hint").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").assertIsDisplayed()
        // Без истории отмена недоступна.
        composeRule.onNodeWithText("Undo").assertIsNotEnabled()
    }

    @Test
    fun playScreen_showsResultOverlay_whenGameEnds() {
        val board = Board.empty().apply {
            this[Pos(0, 1)] = Piece(Side.White, isKing = true)
        }
        val state = CheckersUiState(
            board = board,
            winner = Side.White,
            botEnabled = true,
            humanIsWhite = true,
            lastMove = LastMove(
                path = listOf(Pos(2, 3), Pos(0, 1)),
                captured = listOf(Pos(1, 2) to Piece(Side.Black, isKing = false)),
                piece = Piece(Side.White, isKing = true),
                becameKing = false,
                counter = 5,
            ),
            moveLog = listOf("c3-d4", "f6-e5", "d4:f6"),
            soundEnabled = false,
            hapticsEnabled = false,
        )
        setPlayScreen(state)
        composeRule.onNodeWithText("You win!").assertIsDisplayed()
        composeRule.onNodeWithText("Play again").assertIsDisplayed()
    }
}
