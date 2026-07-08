package ru.akarakuts.russiancheckers.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random
import ru.akarakuts.russiancheckers.R
import ru.akarakuts.russiancheckers.game.Piece
import ru.akarakuts.russiancheckers.game.Pos
import ru.akarakuts.russiancheckers.game.Side
import ru.akarakuts.russiancheckers.ui.theme.BoardPalette
import ru.akarakuts.russiancheckers.ui.theme.CellHighlightHint
import ru.akarakuts.russiancheckers.ui.theme.CellHighlightLastMove
import ru.akarakuts.russiancheckers.ui.theme.CellHighlightMove
import ru.akarakuts.russiancheckers.ui.theme.CellHighlightRingMove
import ru.akarakuts.russiancheckers.ui.theme.CellHighlightRingSelected
import ru.akarakuts.russiancheckers.ui.theme.CellHighlightSelected
import ru.akarakuts.russiancheckers.ui.theme.boardPalette

private val CoordGutter = 22.dp
private const val SEGMENT_ANIM_MS = 170

/** Play tab: status area, animated board, controls (undo/hint), move log, win overlay. */
@Composable
fun PlayScreen(
    state: CheckersUiState,
    onCell: (Pos) -> Unit,
    onNewGameRequest: () -> Unit,
    onDismissSaveError: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = boardPalette()

    GameFeedbackEffects(state)

    var dismissedResultCounter by remember { mutableIntStateOf(-1) }
    val resultCounter = state.lastMove?.counter ?: -1
    val showResult = state.winner != null && resultCounter >= 0 && dismissedResultCounter != resultCounter

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val wide = maxWidth >= 600.dp
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .aspectRatio(1f)
                            .align(Alignment.CenterVertically),
                    ) {
                        BoardWithCoordinates(state = state, palette = palette, onCell = onCell)
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SidePanels(
                            state = state,
                            onNewGameRequest = onNewGameRequest,
                            onDismissSaveError = onDismissSaveError,
                            onUndo = onUndo,
                            onHint = onHint,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.saveLoadFailed) {
                        SaveErrorCard(onDismissSaveError)
                    }
                    GameStatusPanel(state = state)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    ) {
                        BoardWithCoordinates(state = state, palette = palette, onCell = onCell)
                    }
                    MoveLogRow(state.moveLog)
                    ControlButtons(
                        state = state,
                        onUndo = onUndo,
                        onHint = onHint,
                        onNewGameRequest = onNewGameRequest,
                    )
                    Text(
                        text = if (state.hintLoading) {
                            stringResource(R.string.hint_loading)
                        } else {
                            stringResource(R.string.hint_tap_chain)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                    )
                }
            }
        }

        if (showResult) {
            GameResultOverlay(
                state = state,
                palette = palette,
                onPlayAgain = {
                    dismissedResultCounter = resultCounter
                    onNewGameRequest()
                },
                onDismiss = { dismissedResultCounter = resultCounter },
            )
        }
    }
}

/** Panels shown to the right of the board on wide (tablet) screens. */
@Composable
private fun SidePanels(
    state: CheckersUiState,
    onNewGameRequest: () -> Unit,
    onDismissSaveError: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
) {
    if (state.saveLoadFailed) {
        SaveErrorCard(onDismissSaveError)
    }
    GameStatusPanel(state = state)
    MoveLogRow(state.moveLog)
    ControlButtons(
        state = state,
        onUndo = onUndo,
        onHint = onHint,
        onNewGameRequest = onNewGameRequest,
    )
    Text(
        text = if (state.hintLoading) {
            stringResource(R.string.hint_loading)
        } else {
            stringResource(R.string.hint_tap_chain)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Plays sounds and haptics when a new ply lands (skips restored/undone states). */
@Composable
private fun GameFeedbackEffects(state: CheckersUiState) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }
    val haptics = LocalHapticFeedback.current
    var seenCounter by remember { mutableIntStateOf(state.lastMove?.counter ?: 0) }

    LaunchedEffect(state.lastMove?.counter, state.winner) {
        val mv = state.lastMove ?: return@LaunchedEffect
        if (mv.counter <= seenCounter) return@LaunchedEffect
        seenCounter = mv.counter
        if (state.soundEnabled) {
            val humanWon = !state.botEnabled || state.winner == state.humanSide()
            when {
                state.winner != null -> soundManager.play(if (humanWon) GameSound.Win else GameSound.Lose)
                mv.becameKing -> soundManager.play(GameSound.Crown)
                mv.captured.isNotEmpty() -> soundManager.play(GameSound.Capture)
                else -> soundManager.play(GameSound.Move)
            }
        }
        if (state.hapticsEnabled) {
            haptics.performHapticFeedback(
                if (mv.captured.isNotEmpty()) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
            )
        }
    }
}

@Composable
private fun SaveErrorCard(onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.save_load_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_confirm))
            }
        }
    }
}

@Composable
private fun ControlButtons(
    state: CheckersUiState,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onNewGameRequest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = state.undoAvailable && !state.aiThinking,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.undo_move), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onHint,
                enabled = state.isHumanTurn && !state.hintLoading && !state.aiThinking,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.hint_button), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Button(
            onClick = onNewGameRequest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.new_game))
        }
    }
}

/** Horizontally scrolling move list in draughts notation; auto-scrolls to the latest ply. */
@Composable
private fun MoveLogRow(moveLog: List<String>) {
    if (moveLog.isEmpty()) return
    val scroll = rememberScrollState()
    LaunchedEffect(moveLog.size) {
        scroll.animateScrollTo(scroll.maxValue)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.move_log_title) + ":",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        moveLog.forEachIndexed { i, m ->
            val label = if (i % 2 == 0) "${i / 2 + 1}. $m" else m
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (i == moveLog.lastIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GameStatusPanel(state: CheckersUiState) {
    val title = when (val w = state.winner) {
        null -> when {
            state.aiThinking -> stringResource(R.string.ai_thinking_short)
            !state.isHumanTurn && state.botEnabled -> stringResource(R.string.status_opponent_turn)
            state.isHumanTurn && state.botEnabled -> stringResource(R.string.status_your_turn)
            state.turn == Side.White -> stringResource(R.string.turn_white)
            else -> stringResource(R.string.turn_black)
        }
        Side.White -> stringResource(R.string.winner_white)
        Side.Black -> stringResource(R.string.winner_black)
    }
    val subtitle = if (state.botEnabled && state.winner == null) {
        stringResource(
            if (state.humanIsWhite) R.string.status_you_are_white else R.string.status_you_are_black,
        )
    } else {
        "\u00A0"
    }
    val captureShow =
        state.captureRequired && state.winner == null && state.isHumanTurn && !state.aiThinking
    val aiProgressDescription = stringResource(R.string.ai_thinking)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 86.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (captureShow) {
            Text(
                text = stringResource(R.string.hint_mandatory_capture),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.aiThinking) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier.semantics { contentDescription = aiProgressDescription },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.ai_thinking),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BoardWithCoordinates(
    state: CheckersUiState,
    palette: BoardPalette,
    onCell: (Pos) -> Unit,
) {
    val showCoordinates = state.showCoordinates
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (showCoordinates) {
                Column(
                    modifier = Modifier
                        .width(CoordGutter)
                        .fillMaxHeight(),
                ) {
                    for (r in 0..7) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${8 - r}",
                                style = labelStyle,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            BoardGrid(
                state = state,
                palette = palette,
                onCell = onCell,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        if (showCoordinates) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CoordGutter),
            ) {
                Spacer(Modifier.width(CoordGutter))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    for (c in 0..7) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${'a' + c}",
                                style = labelStyle,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 8×8 grid plus an overlay layer that slides the moved piece and fades out captures. */
@Composable
private fun BoardGrid(
    state: CheckersUiState,
    palette: BoardPalette,
    onCell: (Pos) -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlights = state.nextOptions()
    val prefixLast = state.pathPrefix.lastOrNull()
    val canInteract = state.isHumanTurn && !state.aiThinking
    val lastMoveCells = state.lastMove?.path?.let { setOf(it.first(), it.last()) } ?: emptySet()
    val hintCells = state.hintPath?.toSet() ?: emptySet()

    var animating by remember { mutableStateOf<LastMove?>(null) }
    val progress = remember { Animatable(0f) }
    var animSeen by remember { mutableIntStateOf(state.lastMove?.counter ?: 0) }

    LaunchedEffect(state.lastMove?.counter) {
        val mv = state.lastMove ?: return@LaunchedEffect
        if (mv.counter <= animSeen) return@LaunchedEffect
        animSeen = mv.counter
        animating = mv
        progress.snapTo(0f)
        try {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SEGMENT_ANIM_MS * (mv.path.size - 1),
                    easing = FastOutSlowInEasing,
                ),
            )
        } finally {
            animating = null
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val cellSize = maxWidth / 8
        val anim = animating
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0..7) {
                Row(modifier = Modifier.weight(1f)) {
                    for (c in 0..7) {
                        val p = Pos(r, c)
                        val dark = p.isPlayable()
                        val hidePiece = anim != null && p == anim.path.last()
                        BoardCell(
                            pos = p,
                            dark = dark,
                            piece = if (hidePiece) null else state.board[p],
                            palette = palette,
                            highlightMove = p in highlights,
                            highlightSelected = prefixLast == p,
                            highlightLastMove = p in lastMoveCells,
                            highlightHint = p in hintCells,
                            enabled = dark && canInteract,
                            onClick = { onCell(p) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (anim != null) {
            val t = progress.value
            // Снятые шашки растворяются, пока бьющая скользит по цепочке.
            for ((pos, piece) in anim.captured) {
                Box(
                    modifier = Modifier
                        .offset(x = cellSize * pos.c, y = cellSize * pos.r)
                        .size(cellSize)
                        .alpha(1f - t),
                    contentAlignment = Alignment.Center,
                ) {
                    PieceView(piece, palette, Modifier.fillMaxSize(0.68f))
                }
            }
            val (fr, fc) = pathPoint(anim.path, t)
            Box(
                modifier = Modifier
                    .offset(x = cellSize * fc, y = cellSize * fr)
                    .size(cellSize),
                contentAlignment = Alignment.Center,
            ) {
                PieceView(anim.piece, palette, Modifier.fillMaxSize(0.68f))
            }
        }
    }
}

/** Fractional (row, col) along [path] for animation progress [t] in 0..1. */
private fun pathPoint(path: List<Pos>, t: Float): Pair<Float, Float> {
    val segments = path.size - 1
    if (segments <= 0) return path.first().r.toFloat() to path.first().c.toFloat()
    val f = (t * segments).coerceIn(0f, segments.toFloat())
    val i = f.toInt().coerceAtMost(segments - 1)
    val local = f - i
    val r = path[i].r + (path[i + 1].r - path[i].r) * local
    val c = path[i].c + (path[i + 1].c - path[i].c) * local
    return r to c
}

private fun posLabel(pos: Pos): String = "${'a' + pos.c}${8 - pos.r}"

@Composable
private fun BoardCell(
    pos: Pos,
    dark: Boolean,
    piece: Piece?,
    palette: BoardPalette,
    highlightMove: Boolean,
    highlightSelected: Boolean,
    highlightLastMove: Boolean,
    highlightHint: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = if (dark) palette.darkSquare else palette.lightSquare
    val label = posLabel(pos)
    val pieceName = when {
        piece == null -> stringResource(R.string.cell_empty, label)
        piece.side == Side.White && piece.isKing -> stringResource(R.string.piece_white_king)
        piece.side == Side.White -> stringResource(R.string.piece_white_man)
        piece.isKing -> stringResource(R.string.piece_black_king)
        else -> stringResource(R.string.piece_black_man)
    }
    val status = when {
        highlightSelected -> stringResource(R.string.cell_selected)
        highlightMove -> stringResource(R.string.cell_legal_move)
        highlightHint -> stringResource(R.string.cell_hint)
        highlightLastMove -> stringResource(R.string.cell_last_move)
        else -> null
    }
    val description = if (status != null) {
        stringResource(R.string.cell_piece, pieceName, "$label, $status")
    } else {
        if (piece == null) stringResource(R.string.cell_empty, label) else "$pieceName, $label"
    }

    val highlightBg = when {
        highlightSelected -> CellHighlightSelected.copy(alpha = 0.42f)
        highlightMove && piece == null -> CellHighlightMove.copy(alpha = 0.42f)
        highlightHint -> CellHighlightHint.copy(alpha = 0.30f)
        highlightLastMove -> CellHighlightLastMove.copy(alpha = 0.32f)
        else -> base
    }
    val ringColor = when {
        highlightSelected -> CellHighlightRingSelected
        highlightMove -> CellHighlightRingMove
        highlightHint -> CellHighlightHint
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) { contentDescription = description }
            .background(highlightBg)
            .then(
                if (ringColor != Color.Transparent) {
                    Modifier.border(2.dp, ringColor)
                } else {
                    Modifier
                },
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (piece != null) {
            PieceView(
                piece = piece,
                palette = palette,
                modifier = Modifier.fillMaxSize(if (piece.isKing) 0.74f else 0.68f),
            )
        }
    }
}

/** Checker disc with a rim; kings get a painted crown. */
@Composable
private fun PieceView(piece: Piece, palette: BoardPalette, modifier: Modifier = Modifier) {
    val white = piece.side == Side.White
    val fill = if (white) palette.pieceWhite else palette.pieceBlack
    val rim = if (white) palette.pieceWhiteRim else palette.pieceBlackRim
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, rim, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (piece.isKing) {
            KingCrown(
                color = if (white) palette.crownOnWhite else palette.crownOnBlack,
                modifier = Modifier.fillMaxSize(0.58f),
            )
        }
    }
}

/** Three-point crown drawn with a Path (replaces the old ♔ glyph). */
@Composable
private fun KingCrown(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val w = size.width
        val h = size.height
        val crown = Path().apply {
            moveTo(w * 0.10f, h * 0.62f)
            lineTo(w * 0.10f, h * 0.28f)
            lineTo(w * 0.32f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.68f, h * 0.46f)
            lineTo(w * 0.90f, h * 0.28f)
            lineTo(w * 0.90f, h * 0.62f)
            close()
        }
        drawPath(crown, color)
        drawRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.68f),
            size = Size(w * 0.80f, h * 0.12f),
        )
    }
}

/** Full-screen scrim with the game result, stats of the finished game and confetti. */
@Composable
private fun GameResultOverlay(
    state: CheckersUiState,
    palette: BoardPalette,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    val winner = state.winner ?: return
    val humanWon = !state.botEnabled || winner == state.humanSide()
    val title = when {
        !state.botEnabled && winner == Side.White -> stringResource(R.string.winner_white)
        !state.botEnabled -> stringResource(R.string.winner_black)
        humanWon -> stringResource(R.string.result_you_win)
        else -> stringResource(R.string.result_you_lose)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (humanWon) {
            ConfettiOverlay(
                seed = state.lastMove?.counter ?: 1,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PieceView(
                    piece = Piece(winner, isKing = true),
                    palette = palette,
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.result_moves, state.moveLog.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.result_pieces_left,
                        state.board.count(Side.White),
                        state.board.count(Side.Black),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.result_play_again))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.result_close))
                }
            }
        }
    }
}

private class ConfettiParticle(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val phase: Float,
    val rotSpeed: Float,
    val w: Float,
    val h: Float,
    val color: Color,
)

/** Simple falling-confetti burst (~2.6 s), deterministic for a given [seed]. */
@Composable
private fun ConfettiOverlay(seed: Int, modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFFFFC107), Color(0xFF66BB6A), Color(0xFF42A5F5),
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFFFFA726),
    )
    val particles = remember(seed) {
        val rnd = Random(seed)
        List(56) {
            ConfettiParticle(
                x = rnd.nextFloat(),
                startY = -rnd.nextFloat() * 0.4f,
                speed = 0.8f + rnd.nextFloat() * 0.7f,
                phase = rnd.nextFloat() * 6.28f,
                rotSpeed = (rnd.nextFloat() - 0.5f) * 720f,
                w = 12f + rnd.nextFloat() * 10f,
                h = 6f + rnd.nextFloat() * 6f,
                color = colors[rnd.nextInt(colors.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(seed) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 2600, easing = LinearEasing))
    }
    val t = progress.value
    if (t >= 1f) return
    Canvas(modifier = modifier) {
        for (p in particles) {
            val y = (p.startY + t * p.speed * 1.4f) * size.height
            if (y < -30f || y > size.height + 30f) continue
            val x = (p.x + sin(t * 6f + p.phase) * 0.03f) * size.width
            rotate(degrees = p.phase * 57f + t * p.rotSpeed, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x - p.w / 2, y - p.h / 2),
                    size = Size(p.w, p.h),
                    alpha = (1f - t).coerceIn(0f, 1f),
                )
            }
        }
    }
}
