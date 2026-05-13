package ru.akarakuts.russiancheckers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.akarakuts.russiancheckers.R
import ru.akarakuts.russiancheckers.game.Piece
import ru.akarakuts.russiancheckers.game.Pos
import ru.akarakuts.russiancheckers.game.Side
import ru.akarakuts.russiancheckers.ui.theme.BoardDark
import ru.akarakuts.russiancheckers.ui.theme.BoardLight
import ru.akarakuts.russiancheckers.ui.theme.PieceBlack
import ru.akarakuts.russiancheckers.ui.theme.PieceWhite

private val CoordGutter = 22.dp
private val StatusPanelHeight = 168.dp

/** Play tab: fixed-height status area, board with rank/file labels, controls. */
@Composable
fun PlayScreen(
    state: CheckersUiState,
    onCell: (Pos) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlights = state.nextOptions()
    val prefixLast = state.pathPrefix.lastOrNull()
    val canInteract = state.isHumanTurn && !state.aiThinking

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GameStatusPanel(state = state)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            BoardWithCoordinates(
                state = state,
                highlights = highlights,
                prefixLast = prefixLast,
                canInteract = canInteract,
                onCell = onCell,
            )
        }

        Text(
            text = stringResource(R.string.hint_tap_chain),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(onClick = onNewGame) {
                Text(stringResource(R.string.new_game))
            }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(StatusPanelHeight),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (captureShow) {
                Text(
                    text = stringResource(R.string.hint_mandatory_capture),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
        ) {
            if (state.aiThinking) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}

@Composable
private fun BoardWithCoordinates(
    state: CheckersUiState,
    highlights: Set<Pos>,
    prefixLast: Pos?,
    canInteract: Boolean,
    onCell: (Pos) -> Unit,
) {
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                for (r in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0..7) {
                            val p = Pos(r, c)
                            val dark = p.isPlayable()
                            val piece = state.board[p]
                            val highlightMove = p in highlights
                            val highlightSelected = prefixLast == p
                            BoardCell(
                                dark = dark,
                                piece = piece,
                                highlightMove = highlightMove,
                                highlightSelected = highlightSelected,
                                enabled = dark && canInteract,
                                onClick = { onCell(p) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
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

@Composable
private fun BoardCell(
    dark: Boolean,
    piece: Piece?,
    highlightMove: Boolean,
    highlightSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = if (dark) BoardDark else BoardLight
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                when {
                    highlightSelected -> Color(0xFFFFC107).copy(alpha = 0.42f)
                    highlightMove && piece == null -> Color(0xFF66BB6A).copy(alpha = 0.42f)
                    else -> base
                },
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (piece != null) {
            val fill = when (piece.side) {
                Side.White -> PieceWhite
                Side.Black -> PieceBlack
            }
            val ring = when {
                highlightSelected -> Color(0xFFFFEB3B)
                highlightMove -> Color(0xFF81C784).copy(alpha = 0.92f)
                else -> Color.Transparent
            }
            if (ring != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .clip(CircleShape)
                        .background(ring),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(if (piece.isKing) 0.74f else 0.64f)
                    .clip(CircleShape)
                    .background(fill),
                contentAlignment = Alignment.Center,
            ) {
                if (piece.isKing) {
                    Text(
                        text = "♔",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (piece.side == Side.White) Color(0xFF222222) else Color(0xFFDDDDDD),
                    )
                }
            }
        }
    }
}
