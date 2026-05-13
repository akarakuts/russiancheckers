package ru.akarakuts.russiancheckers.game

/** Player side; White moves first (Russian draughts). */
enum class Side {
    White,
    Black,
    ;

    fun other(): Side = if (this == White) Black else White
}

data class Piece(val side: Side, val isKing: Boolean)

/** Board square on 8×8; play is only on dark cells ((r + c) is odd). */
data class Pos(val r: Int, val c: Int) {
    fun isOnBoard(): Boolean = r in 0..7 && c in 0..7

    fun isPlayable(): Boolean = isOnBoard() && (r + c) % 2 == 1
}

/** Full ply: cells from start to end, including intermediate landings in a capture chain. */
typealias Path = List<Pos>
