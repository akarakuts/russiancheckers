package ru.akarakuts.russiancheckers.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import ru.akarakuts.russiancheckers.game.AiDifficulty
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.Side

private val Context.checkersDataStore by preferencesDataStore(name = "russian_checkers_prefs")

/** DataStore-backed game snapshot and settings. */
class GamePreferencesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val ds get() = appContext.checkersDataStore

    suspend fun loadSettings(): CheckersSettings {
        val p = ds.data.first()
        return CheckersSettings(
            botEnabled = p[KEY_BOT] ?: true,
            humanIsWhite = p[KEY_HUMAN_WHITE] ?: true,
            aiDifficulty = AiDifficulty.fromCode(p[KEY_AI_DIFFICULTY]),
            showCoordinates = p[KEY_SHOW_COORDS] ?: true,
        )
    }

    /** Returns saved game or null; [LoadGameResult.Corrupt] if save flag set but data invalid. */
    suspend fun loadGame(): LoadGameResult {
        val p = ds.data.first()
        if (p[KEY_HAS_SAVE] != true) return LoadGameResult.None
        val enc = p[KEY_BOARD] ?: return LoadGameResult.Corrupt
        val board = Board.decodeBoard(enc) ?: return LoadGameResult.Corrupt
        val turn = p[KEY_TURN]?.toSideOrNull() ?: Side.White
        val winner = p[KEY_WINNER]?.toWinnerOrNull()
        return LoadGameResult.Ok(Triple(board, turn, winner))
    }

    suspend fun saveGame(board: Board, turn: Side, winner: Side?) {
        ds.edit { e ->
            e[KEY_HAS_SAVE] = true
            e[KEY_BOARD] = board.encode()
            e[KEY_TURN] = turn.toCode()
            e[KEY_WINNER] = winner.toWinnerCode()
        }
    }

    suspend fun saveSettings(settings: CheckersSettings) {
        ds.edit { e ->
            e[KEY_BOT] = settings.botEnabled
            e[KEY_HUMAN_WHITE] = settings.humanIsWhite
            e[KEY_AI_DIFFICULTY] = settings.aiDifficulty.code
            e[KEY_SHOW_COORDS] = settings.showCoordinates
        }
    }

    suspend fun clearSavedGame() {
        ds.edit { e ->
            e.remove(KEY_HAS_SAVE)
            e.remove(KEY_BOARD)
            e.remove(KEY_TURN)
            e.remove(KEY_WINNER)
        }
    }

    private companion object {
        val KEY_HAS_SAVE = booleanPreferencesKey("has_save")
        val KEY_BOARD = stringPreferencesKey("board")
        val KEY_TURN = stringPreferencesKey("turn")
        val KEY_WINNER = stringPreferencesKey("winner")
        val KEY_BOT = booleanPreferencesKey("bot")
        val KEY_HUMAN_WHITE = booleanPreferencesKey("human_white")
        val KEY_AI_DIFFICULTY = stringPreferencesKey("ai_difficulty")
        val KEY_SHOW_COORDS = booleanPreferencesKey("show_coords")
    }
}

sealed class LoadGameResult {
    data object None : LoadGameResult()
    data object Corrupt : LoadGameResult()
    data class Ok(val game: Triple<Board, Side, Side?>) : LoadGameResult()
}

private fun Side.toCode(): String = when (this) {
    Side.White -> "W"
    Side.Black -> "B"
}

private fun String.toSideOrNull(): Side? = when (this) {
    "W" -> Side.White
    "B" -> Side.Black
    else -> null
}

private fun Side?.toWinnerCode(): String = when (this) {
    null -> "N"
    Side.White -> "W"
    Side.Black -> "B"
}

private fun String.toWinnerOrNull(): Side? = when (this) {
    "W" -> Side.White
    "B" -> Side.Black
    else -> null
}
