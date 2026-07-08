package ru.akarakuts.russiancheckers.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Board chrome and piece fills for the draughts UI. */
val BoardLight = Color(0xFFE8D4B8)
val BoardDark = Color(0xFF6B4423)
val PieceWhite = Color(0xFFF5F5F5)
val PieceBlack = Color(0xFF2C1810)
val Accent = Color(0xFFB8860B)
val CellHighlightSelected = Color(0xFFFFC107)
val CellHighlightMove = Color(0xFF66BB6A)
val CellHighlightRingSelected = Color(0xFFFFEB3B)
val CellHighlightRingMove = Color(0xFF2E7D32)
val CellHighlightLastMove = Color(0xFF5C9BD1)
val CellHighlightHint = Color(0xFFAB47BC)

/** Palette for the board itself; light and night variants (see [boardPalette]). */
@Immutable
data class BoardPalette(
    val lightSquare: Color,
    val darkSquare: Color,
    val pieceWhite: Color,
    val pieceBlack: Color,
    val pieceWhiteRim: Color,
    val pieceBlackRim: Color,
    val crownOnWhite: Color,
    val crownOnBlack: Color,
)

val LightBoardPalette = BoardPalette(
    lightSquare = BoardLight,
    darkSquare = BoardDark,
    pieceWhite = PieceWhite,
    pieceBlack = PieceBlack,
    pieceWhiteRim = Color(0xFFBDB4A5),
    pieceBlackRim = Color(0xFF54331F),
    crownOnWhite = Color(0xFFB8860B),
    crownOnBlack = Color(0xFFE8C36A),
)

val NightBoardPalette = BoardPalette(
    lightSquare = Color(0xFF8A7A63),
    darkSquare = Color(0xFF3E2A18),
    pieceWhite = Color(0xFFE6E0D4),
    pieceBlack = Color(0xFF1E120B),
    pieceWhiteRim = Color(0xFF9C917D),
    pieceBlackRim = Color(0xFF4C3420),
    crownOnWhite = Color(0xFFB8860B),
    crownOnBlack = Color(0xFFE8C36A),
)
