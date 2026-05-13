package ru.akarakuts.russiancheckers.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.akarakuts.russiancheckers.data.CheckersSettings
import ru.akarakuts.russiancheckers.data.GamePreferencesRepository
import ru.akarakuts.russiancheckers.game.AiDifficulty
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.CheckersAi
import ru.akarakuts.russiancheckers.game.Path
import ru.akarakuts.russiancheckers.game.Pos
import ru.akarakuts.russiancheckers.game.RussianCheckersEngine
import ru.akarakuts.russiancheckers.game.Side

/** UI-facing game state: board, legal paths for the current ply prefix, bot flags. */
data class CheckersUiState(
    val board: Board = Board.initial(),
    val turn: Side = Side.White,
    val candidatePaths: List<Path> = RussianCheckersEngine.legalPaths(Board.initial(), Side.White),
    val pathPrefix: List<Pos> = emptyList(),
    val winner: Side? = null,
    val botEnabled: Boolean = false,
    val humanIsWhite: Boolean = true,
    val aiDifficulty: AiDifficulty = AiDifficulty.Normal,
    val aiThinking: Boolean = false,
) {
    fun humanSide(): Side = if (humanIsWhite) Side.White else Side.Black

    val isHumanTurn: Boolean
        get() = winner == null && (!botEnabled || turn == humanSide())

    val captureRequired: Boolean
        get() = winner == null && RussianCheckersEngine.hasForcedCapture(board, turn)

    val legalStarts: Set<Pos> = candidatePaths.map { it.first() }.toSet()

    fun nextOptions(): Set<Pos> {
        if (winner != null || aiThinking) return emptySet()
        if (pathPrefix.isEmpty()) return legalStarts
        return candidatePaths
            .filter { p -> p.size >= pathPrefix.size + 1 && p.take(pathPrefix.size) == pathPrefix }
            .map { it[pathPrefix.size] }
            .toSet()
    }
}

/** [AndroidViewModel] holding [CheckersUiState], persistence, and AI moves. */
class CheckersViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GamePreferencesRepository(app)
    private val _state = MutableStateFlow(CheckersUiState())
    val state: StateFlow<CheckersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repo.loadSettings()
            val loaded = repo.loadGameOrNull()
            _state.value = if (loaded != null) {
                val (b, t, w) = loaded
                rebuildGameState(settings, b, t, w)
            } else {
                CheckersUiState(
                    botEnabled = settings.botEnabled,
                    humanIsWhite = settings.humanIsWhite,
                    aiDifficulty = settings.aiDifficulty,
                    candidatePaths = RussianCheckersEngine.legalPaths(Board.initial(), Side.White),
                )
            }
            maybeAiTurn()
        }
    }

    private fun rebuildGameState(
        settings: CheckersSettings,
        board: Board,
        turn: Side,
        winner: Side?,
    ): CheckersUiState {
        val paths = if (winner == null) RussianCheckersEngine.legalPaths(board, turn) else emptyList()
        return CheckersUiState(
            board = board,
            turn = turn,
            winner = winner,
            candidatePaths = paths,
            pathPrefix = emptyList(),
            botEnabled = settings.botEnabled,
            humanIsWhite = settings.humanIsWhite,
            aiDifficulty = settings.aiDifficulty,
            aiThinking = false,
        )
    }

    private fun currentSettings(): CheckersSettings {
        val s = _state.value
        return CheckersSettings(s.botEnabled, s.humanIsWhite, s.aiDifficulty)
    }

    private fun persist() {
        val s = _state.value
        viewModelScope.launch {
            repo.saveSettings(currentSettings())
            repo.saveGame(s.board, s.turn, s.winner)
        }
    }

    private fun maybeAiTurn() {
        val s = _state.value
        if (s.winner != null || s.aiThinking || !s.botEnabled || s.turn == s.humanSide()) return
        viewModelScope.launch {
            _state.update { it.copy(aiThinking = true) }
            delay(160)
            val cur = _state.value
            if (cur.winner != null || cur.turn == cur.humanSide()) {
                _state.update { it.copy(aiThinking = false) }
                return@launch
            }
            val path = CheckersAi.chooseMove(cur.board, cur.turn, cur.aiDifficulty)
                ?: RussianCheckersEngine.legalPaths(cur.board, cur.turn).firstOrNull()
            if (path == null) {
                _state.update { it.copy(aiThinking = false) }
                return@launch
            }
            _state.update { applyPathToState(cur, path).copy(aiThinking = false) }
            persist()
            maybeAiTurn()
        }
    }

    private fun applyPathToState(s: CheckersUiState, path: Path): CheckersUiState {
        val newBoard = RussianCheckersEngine.applyPath(s.board, path)
        val nextTurn = s.turn.other()
        val lostByPieces = when {
            newBoard.count(nextTurn) == 0 -> s.turn
            newBoard.count(s.turn) == 0 -> nextTurn
            else -> null
        }
        val win = lostByPieces
            ?: if (!RussianCheckersEngine.hasLegalMove(newBoard, nextTurn)) s.turn else null
        val candidates = if (win == null) {
            RussianCheckersEngine.legalPaths(newBoard, nextTurn)
        } else {
            emptyList()
        }
        return s.copy(
            board = newBoard,
            turn = if (win == null) nextTurn else s.turn,
            candidatePaths = candidates,
            pathPrefix = emptyList(),
            winner = win,
            aiThinking = false,
        )
    }

    fun onCellClicked(cell: Pos) {
        val s0 = _state.value
        if (!s0.isHumanTurn || s0.aiThinking) return
        _state.update { s ->
            if (s.winner != null) return@update s
            if (!cell.isPlayable()) return@update s
            val next = s.nextOptions()
            if (cell !in next) {
                if (s.pathPrefix.size == 1 && cell == s.pathPrefix.first()) {
                    return@update s.copy(pathPrefix = emptyList())
                }
                if (s.pathPrefix.isNotEmpty() && cell in s.legalStarts && cell != s.pathPrefix.first()) {
                    return@update s.copy(pathPrefix = listOf(cell))
                }
                return@update s
            }
            val newPrefix = s.pathPrefix + cell
            val still = s.candidatePaths.filter { p ->
                p.size >= newPrefix.size && p.take(newPrefix.size) == newPrefix
            }
            if (still.isEmpty()) return@update s
            val finished = still.any { it.size == newPrefix.size }
            if (!finished) return@update s.copy(pathPrefix = newPrefix)
            val path = still.first { it.size == newPrefix.size }
            applyPathToState(s, path)
        }
        persist()
        maybeAiTurn()
    }

    fun newGame() {
        val s0 = _state.value
        val b = Board.initial()
        _state.value = CheckersUiState(
            board = b,
            turn = Side.White,
            candidatePaths = RussianCheckersEngine.legalPaths(b, Side.White),
            pathPrefix = emptyList(),
            winner = null,
            botEnabled = s0.botEnabled,
            humanIsWhite = s0.humanIsWhite,
            aiDifficulty = s0.aiDifficulty,
            aiThinking = false,
        )
        viewModelScope.launch {
            repo.clearSavedGame()
            repo.saveGame(b, Side.White, null)
            repo.saveSettings(currentSettings())
        }
        maybeAiTurn()
    }

    fun setBotEnabled(enabled: Boolean) {
        val cur = _state.value
        val old = cur.botEnabled
        val hw = cur.humanIsWhite
        val diff = cur.aiDifficulty
        _state.update { it.copy(botEnabled = enabled) }
        viewModelScope.launch { repo.saveSettings(CheckersSettings(enabled, hw, diff)) }
        if (enabled != old) newGame()
    }

    fun setHumanIsWhite(white: Boolean) {
        val cur = _state.value
        if (cur.humanIsWhite == white) return
        val bot = cur.botEnabled
        val diff = cur.aiDifficulty
        _state.update { it.copy(humanIsWhite = white) }
        viewModelScope.launch { repo.saveSettings(CheckersSettings(bot, white, diff)) }
        if (bot) newGame()
    }

    fun setAiDifficulty(difficulty: AiDifficulty) {
        val cur = _state.value
        if (cur.aiDifficulty == difficulty) return
        val bot = cur.botEnabled
        val hw = cur.humanIsWhite
        _state.update { it.copy(aiDifficulty = difficulty) }
        viewModelScope.launch { repo.saveSettings(CheckersSettings(bot, hw, difficulty)) }
    }
}
