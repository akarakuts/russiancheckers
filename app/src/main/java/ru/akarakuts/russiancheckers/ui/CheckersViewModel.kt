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
import ru.akarakuts.russiancheckers.data.GameStats
import ru.akarakuts.russiancheckers.data.LoadGameResult
import ru.akarakuts.russiancheckers.game.AiDifficulty
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.CheckersAi
import ru.akarakuts.russiancheckers.game.Path
import ru.akarakuts.russiancheckers.game.Piece
import ru.akarakuts.russiancheckers.game.Pos
import ru.akarakuts.russiancheckers.game.RussianCheckersEngine
import ru.akarakuts.russiancheckers.game.Side

/** Applied ply details for animations, sounds and the last-move highlight. */
data class LastMove(
    val path: Path,
    val captured: List<Pair<Pos, Piece>>,
    val piece: Piece,
    val becameKing: Boolean,
    val counter: Int,
)

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
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val aiThinking: Boolean = false,
    val saveLoadFailed: Boolean = false,
    val lastMove: LastMove? = null,
    val hintPath: Path? = null,
    val hintLoading: Boolean = false,
    val moveLog: List<String> = emptyList(),
    val undoAvailable: Boolean = false,
    val stats: GameStats = GameStats(),
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

/** [AndroidViewModel] holding [CheckersUiState], persistence, undo history, and AI moves. */
class CheckersViewModel internal constructor(
    app: Application,
    private val repo: GamePreferencesRepository,
) : AndroidViewModel(app) {

    constructor(app: Application) : this(app, GamePreferencesRepository(app))
    private val _state = MutableStateFlow(CheckersUiState())
    val state: StateFlow<CheckersUiState> = _state.asStateFlow()

    private var aiJob: Job? = null
    private var hintJob: Job? = null
    private var gameGeneration = 0
    private var moveCounter = 0

    /** Pre-move snapshots for undo (board/turn/log; transient flags dropped on restore). */
    private data class Snapshot(
        val board: Board,
        val turn: Side,
        val moveLog: List<String>,
        val lastMove: LastMove?,
    )

    private val history = ArrayDeque<Snapshot>()

    init {
        viewModelScope.launch {
            val settings = repo.loadSettings()
            val stats = repo.loadStats()
            val loaded = repo.loadGame()
            _state.value = when (loaded) {
                is LoadGameResult.Ok -> {
                    val (b, t, w) = loaded.game
                    rebuildGameState(settings, b, t, w)
                }
                LoadGameResult.Corrupt -> freshGameState(settings).copy(saveLoadFailed = true)
                LoadGameResult.None -> freshGameState(settings)
            }.copy(stats = stats)
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
            soundEnabled = settings.soundEnabled,
            hapticsEnabled = settings.hapticsEnabled,
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
            soundEnabled = settings.soundEnabled,
            hapticsEnabled = settings.hapticsEnabled,
            aiThinking = false,
        )
    }

    private fun currentSettings(): CheckersSettings {
        val s = _state.value
        return CheckersSettings(
            botEnabled = s.botEnabled,
            humanIsWhite = s.humanIsWhite,
            aiDifficulty = s.aiDifficulty,
            showCoordinates = s.showCoordinates,
            soundEnabled = s.soundEnabled,
            hapticsEnabled = s.hapticsEnabled,
        )
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

    private fun cancelHint() {
        hintJob?.cancel()
        hintJob = null
        if (_state.value.hintPath != null || _state.value.hintLoading) {
            _state.update { it.copy(hintPath = null, hintLoading = false) }
        }
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
            _state.value = applyPathToState(_state.value, path).copy(aiThinking = false)
            afterMoveApplied()
        }
    }

    private fun posLabel(p: Pos): String = "${'a' + p.c}${8 - p.r}"

    private fun applyPathToState(s: CheckersUiState, path: Path): CheckersUiState {
        val movedPiece = s.board[path.first()]
        val capturedPos = RussianCheckersEngine.capturedAlong(s.board, path)
        val captured = capturedPos.mapNotNull { pos -> s.board[pos]?.let { pos to it } }
        history.addLast(Snapshot(s.board, s.turn, s.moveLog, s.lastMove))

        val newBoard = RussianCheckersEngine.applyPath(s.board, path)
        val becameKing = movedPiece?.isKing == false && newBoard[path.last()]?.isKing == true
        moveCounter++
        val sep = if (captured.isEmpty()) "-" else ":"
        val notation = path.joinToString(sep) { posLabel(it) }

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
            hintPath = null,
            hintLoading = false,
            lastMove = LastMove(
                path = path,
                captured = captured,
                piece = movedPiece ?: Piece(s.turn, isKing = false),
                becameKing = becameKing,
                counter = moveCounter,
            ),
            moveLog = s.moveLog + notation,
            undoAvailable = undoAvailable(s.botEnabled, s.humanSide()),
        )
    }

    private fun undoAvailable(botEnabled: Boolean, humanSide: Side): Boolean =
        if (botEnabled) history.any { it.turn == humanSide } else history.isNotEmpty()

    private fun afterMoveApplied() {
        val s = _state.value
        persist()
        if (s.winner != null && s.botEnabled) {
            val humanWon = s.winner == s.humanSide()
            viewModelScope.launch {
                val stats = repo.recordResult(humanWon)
                _state.update { it.copy(stats = stats) }
            }
        }
        maybeAiTurn()
    }

    // Все мутации состояния идут с main-потока, поэтому read-compute-assign безопасен
    // (а applyPathToState трогает history и не должен перезапускаться внутри update {}).
    fun onCellClicked(cell: Pos) {
        val s = _state.value
        if (!s.isHumanTurn || s.aiThinking) return
        if (s.winner != null || !cell.isPlayable()) return
        cancelHint()
        val next = s.nextOptions()
        if (cell !in next) {
            if (s.pathPrefix.size == 1 && cell == s.pathPrefix.first()) {
                _state.value = s.copy(pathPrefix = emptyList(), hintPath = null)
            } else if (s.pathPrefix.isNotEmpty() && cell in s.legalStarts && cell != s.pathPrefix.first()) {
                _state.value = s.copy(pathPrefix = listOf(cell), hintPath = null)
            }
            return
        }
        val newPrefix = s.pathPrefix + cell
        val still = s.candidatePaths.filter { p ->
            p.size >= newPrefix.size && p.take(newPrefix.size) == newPrefix
        }
        if (still.isEmpty()) return
        val finished = still.any { it.size == newPrefix.size }
        if (!finished) {
            _state.value = s.copy(pathPrefix = newPrefix, hintPath = null)
            return
        }
        _state.value = applyPathToState(s, still.first { it.size == newPrefix.size })
        afterMoveApplied()
    }

    /** Reverts to the last position where it was the human's turn (or one ply without a bot). */
    fun undo() {
        val s = _state.value
        if (!s.undoAvailable) return
        cancelAi()
        cancelHint()
        gameGeneration++
        var snap = history.removeLastOrNull() ?: return
        if (s.botEnabled) {
            while (snap.turn != s.humanSide()) {
                snap = history.removeLastOrNull() ?: break
            }
        }
        val paths = RussianCheckersEngine.legalPaths(snap.board, snap.turn)
        _state.update {
            it.copy(
                board = snap.board,
                turn = snap.turn,
                candidatePaths = paths,
                pathPrefix = emptyList(),
                winner = null,
                aiThinking = false,
                hintPath = null,
                hintLoading = false,
                lastMove = snap.lastMove,
                moveLog = snap.moveLog,
                undoAvailable = undoAvailable(it.botEnabled, it.humanSide()),
            )
        }
        persist()
        maybeAiTurn()
    }

    /** Computes the best move for the human at the current difficulty and shows it. */
    fun requestHint() {
        val s = _state.value
        if (!s.isHumanTurn || s.winner != null || s.hintLoading) return
        hintJob?.cancel()
        val generation = gameGeneration
        hintJob = viewModelScope.launch {
            _state.update { it.copy(hintLoading = true, hintPath = null) }
            val path = withContext(Dispatchers.Default) {
                CheckersAi.chooseMove(s.board, s.turn, s.aiDifficulty)
            }
            if (generation != gameGeneration) return@launch
            _state.update { it.copy(hintPath = path, hintLoading = false) }
        }
    }

    fun dismissSaveLoadError() {
        _state.update { it.copy(saveLoadFailed = false) }
    }

    fun newGame() {
        cancelAi()
        cancelHint()
        gameGeneration++
        history.clear()
        val s0 = _state.value
        val b = Board.initial()
        _state.value = CheckersUiState(
            board = b,
            turn = Side.White,
            candidatePaths = RussianCheckersEngine.legalPaths(b, Side.White),
            botEnabled = s0.botEnabled,
            humanIsWhite = s0.humanIsWhite,
            aiDifficulty = s0.aiDifficulty,
            showCoordinates = s0.showCoordinates,
            soundEnabled = s0.soundEnabled,
            hapticsEnabled = s0.hapticsEnabled,
            stats = s0.stats,
        )
        viewModelScope.launch {
            repo.clearSavedGame()
            repo.saveGame(b, Side.White, null)
            repo.saveSettings(currentSettings())
        }
        maybeAiTurn()
    }

    fun resetStats() {
        viewModelScope.launch {
            repo.resetStats()
            _state.update { it.copy(stats = GameStats()) }
        }
    }

    fun setBotEnabled(enabled: Boolean) {
        if (_state.value.botEnabled == enabled) return
        _state.update { it.copy(botEnabled = enabled) }
        viewModelScope.launch { repo.saveSettings(currentSettings()) }
        newGame()
    }

    fun setHumanIsWhite(white: Boolean) {
        val cur = _state.value
        if (cur.humanIsWhite == white) return
        _state.update { it.copy(humanIsWhite = white) }
        viewModelScope.launch { repo.saveSettings(currentSettings()) }
        if (cur.botEnabled) newGame()
    }

    fun setAiDifficulty(difficulty: AiDifficulty) {
        if (_state.value.aiDifficulty == difficulty) return
        _state.update { it.copy(aiDifficulty = difficulty) }
        viewModelScope.launch { repo.saveSettings(currentSettings()) }
    }

    fun setShowCoordinates(show: Boolean) {
        if (_state.value.showCoordinates == show) return
        _state.update { it.copy(showCoordinates = show) }
        viewModelScope.launch { repo.saveSettings(currentSettings()) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        if (_state.value.soundEnabled == enabled) return
        _state.update { it.copy(soundEnabled = enabled) }
        viewModelScope.launch { repo.saveSettings(currentSettings()) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        if (_state.value.hapticsEnabled == enabled) return
        _state.update { it.copy(hapticsEnabled = enabled) }
        viewModelScope.launch { repo.saveSettings(currentSettings()) }
    }
}
