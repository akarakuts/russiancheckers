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
        val bot = p[KEY_BOT] ?: false
        val humanWhite = p[KEY_HUMAN_WHITE] ?: true
        val diff = AiDifficulty.fromCode(p[KEY_AI_DIFFICULTY])
        return CheckersSettings(bot, humanWhite, diff)
    }

    suspend fun loadGameOrNull(): Triple<Board, Side, Side?>? {
        val p = ds.data.first()
        if (p[KEY_HAS_SAVE] != true) return null
        val enc = p[KEY_BOARD] ?: return null
        val board = Board.decodeBoard(enc) ?: return null
        val turn = p[KEY_TURN]?.toSideOrNull() ?: Side.White
        val winner = p[KEY_WINNER]?.toWinnerOrNull()
        return Triple(board, turn, winner)
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
    }
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
