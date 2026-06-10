package ru.akarakuts.russiancheckers.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.akarakuts.russiancheckers.data.CheckersSettings
import ru.akarakuts.russiancheckers.data.GamePreferencesRepository
import ru.akarakuts.russiancheckers.data.LoadGameResult
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
    val candidatePaths: List<Path> = emptyList(),
    val pathPrefix: List<Pos> = emptyList(),
    val winner: Side? = null,
    val botEnabled: Boolean = true,
    val humanIsWhite: Boolean = true,
    val aiDifficulty: AiDifficulty = AiDifficulty.Normal,
    val showCoordinates: Boolean = true,
    val aiThinking: Boolean = false,
    val saveLoadFailed: Boolean = false,
) {
    fun humanSide(): Side = if (humanIsWhite) Side.White else Side.Black

    val isHumanTurn: Boolean
        get() = winner == null && (!botEnabled || turn == humanSide())

    val captureRequired: Boolean
        get() = winner == null && RussianCheckersEngine.hasForcedCapture(board, turn)

    val legalStarts: Set<Pos>
        get() = candidatePaths.map { it.first() }.toSet()

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

    private var aiJob: Job? = null
    private var gameGeneration = 0

    init {
        viewModelScope.launch {
            val settings = repo.loadSettings()
            val loaded = repo.loadGame()
            _state.value = when (loaded) {
                is LoadGameResult.Ok -> {
                    val (b, t, w) = loaded.game
                    rebuildGameState(settings, b, t, w)
                }
                LoadGameResult.Corrupt -> freshGameState(settings).copy(saveLoadFailed = true)
                LoadGameResult.None -> freshGameState(settings)
            }
            maybeAiTurn()
        }
    }

    private fun freshGameState(settings: CheckersSettings): CheckersUiState {
        val b = Board.initial()
        return CheckersUiState(
            board = b,
            turn = Side.White,
            candidatePaths = RussianCheckersEngine.legalPaths(b, Side.White),
            botEnabled = settings.botEnabled,
            humanIsWhite = settings.humanIsWhite,
            aiDifficulty = settings.aiDifficulty,
            showCoordinates = settings.showCoordinates,
        )
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
            showCoordinates = settings.showCoordinates,
            aiThinking = false,
        )
    }

    private fun currentSettings(): CheckersSettings {
        val s = _state.value
        return CheckersSettings(s.botEnabled, s.humanIsWhite, s.aiDifficulty, s.showCoordinates)
    }

    private fun persist() {
        val s = _state.value
        viewModelScope.launch {
            repo.saveSettings(currentSettings())
            repo.saveGame(s.board, s.turn, s.winner)
        }
    }

    private fun cancelAi() {
        aiJob?.cancel()
        aiJob = null
        _state.update { it.copy(aiThinking = false) }
    }

    private fun maybeAiTurn() {
        val s = _state.value
        if (s.winner != null || s.aiThinking || !s.botEnabled || s.turn == s.humanSide()) return
        val generation = gameGeneration
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(aiThinking = true) }
            delay(160)
            if (generation != gameGeneration) return@launch
            val cur = _state.value
            if (cur.winner != null || cur.turn == cur.humanSide()) {
                _state.update { it.copy(aiThinking = false) }
                return@launch
            }
            val path = withContext(Dispatchers.Default) {
                CheckersAi.chooseMove(cur.board, cur.turn, cur.aiDifficulty)
                    ?: RussianCheckersEngine.legalPaths(cur.board, cur.turn).firstOrNull()
            }
            if (generation != gameGeneration) return@launch
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
        val before = _state.value
        if (!before.isHumanTurn || before.aiThinking) return
        var changed = false
        _state.update { s ->
            if (s.winner != null) return@update s
            if (!cell.isPlayable()) return@update s
            val next = s.nextOptions()
            if (cell !in next) {
                if (s.pathPrefix.size == 1 && cell == s.pathPrefix.first()) {
                    changed = true
                    return@update s.copy(pathPrefix = emptyList())
                }
                if (s.pathPrefix.isNotEmpty() && cell in s.legalStarts && cell != s.pathPrefix.first()) {
                    changed = true
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
            if (!finished) {
                changed = true
                return@update s.copy(pathPrefix = newPrefix)
            }
            changed = true
            applyPathToState(s, still.first { it.size == newPrefix.size })
        }
        if (changed) {
            persist()
            maybeAiTurn()
        }
    }

    fun dismissSaveLoadError() {
        _state.update { it.copy(saveLoadFailed = false) }
    }

    fun newGame() {
        cancelAi()
        gameGeneration++
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
            showCoordinates = s0.showCoordinates,
            aiThinking = false,
            saveLoadFailed = false,
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
        if (cur.botEnabled == enabled) return
        val hw = cur.humanIsWhite
        val diff = cur.aiDifficulty
        val coords = cur.showCoordinates
        _state.update { it.copy(botEnabled = enabled) }
        viewModelScope.launch { repo.saveSettings(CheckersSettings(enabled, hw, diff, coords)) }
        newGame()
    }

    fun setHumanIsWhite(white: Boolean) {
        val cur = _state.value
        if (cur.humanIsWhite == white) return
        val bot = cur.botEnabled
        val diff = cur.aiDifficulty
        val coords = cur.showCoordinates
        _state.update { it.copy(humanIsWhite = white) }
        viewModelScope.launch { repo.saveSettings(CheckersSettings(bot, white, diff, coords)) }
        if (bot) newGame()
    }

    fun setAiDifficulty(difficulty: AiDifficulty) {
        val cur = _state.value
        if (cur.aiDifficulty == difficulty) return
        val bot = cur.botEnabled
        val hw = cur.humanIsWhite
        val coords = cur.showCoordinates
        _state.update { it.copy(aiDifficulty = difficulty) }
        viewModelScope.launch { repo.saveSettings(CheckersSettings(bot, hw, difficulty, coords)) }
    }

    fun setShowCoordinates(show: Boolean) {
        val cur = _state.value
        if (cur.showCoordinates == show) return
        _state.update { it.copy(showCoordinates = show) }
        viewModelScope.launch {
            repo.saveSettings(
                CheckersSettings(cur.botEnabled, cur.humanIsWhite, cur.aiDifficulty, show),
            )
        }
    }
}
