package ru.akarakuts.russiancheckers.game

/** 8×8 grid; non-playable squares always hold null. */
class Board private constructor(private val cells: Array<Array<Piece?>>) {

    operator fun get(p: Pos): Piece? = if (p.isPlayable()) cells[p.r][p.c] else null

    operator fun set(p: Pos, value: Piece?) {
        if (p.isPlayable()) cells[p.r][p.c] = value
    }

    fun copy(): Board = Board(Array(8) { r -> cells[r].clone() })

    fun count(side: Side): Int {
        var n = 0
        for (r in 0..7) for (c in 0..7) {
            val p = Pos(r, c)
            if (p.isPlayable() && cells[r][c]?.side == side) n++
        }
        return n
    }

    fun allPieces(side: Side): List<Pos> = buildList {
        for (r in 0..7) for (c in 0..7) {
            val p = Pos(r, c)
            if (p.isPlayable() && cells[r][c]?.side == side) add(p)
        }
    }

    companion object {
        fun initial(): Board {
            val g = Array(8) { arrayOfNulls<Piece>(8) }
            for (r in 0..7) for (c in 0..7) {
                val p = Pos(r, c)
                if (!p.isPlayable()) continue
                when (r) {
                    in 0..2 -> g[r][c] = Piece(Side.Black, isKing = false)
                    in 5..7 -> g[r][c] = Piece(Side.White, isKing = false)
                    else -> g[r][c] = null
                }
            }
            return Board(g)
        }

        /** 32 chars: playable squares in row-major scan order. */
        fun decodeBoard(encoded: String): Board? {
            if (encoded.length != 32) return null
            val g = Array(8) { arrayOfNulls<Piece>(8) }
            var i = 0
            for (r in 0..7) for (c in 0..7) {
                val p = Pos(r, c)
                if (!p.isPlayable()) continue
                g[r][c] = when (val ch = encoded[i++]) {
                    '.' -> null
                    'w' -> Piece(Side.White, isKing = false)
                    'W' -> Piece(Side.White, isKing = true)
                    'b' -> Piece(Side.Black, isKing = false)
                    'B' -> Piece(Side.Black, isKing = true)
                    else -> return null
                }
            }
            return Board(g)
        }
    }

    /** Serialises playable squares to 32 characters (inverse of [decodeBoard]). */
    fun encode(): String = buildString(32) {
        for (r in 0..7) for (c in 0..7) {
            val p = Pos(r, c)
            if (!p.isPlayable()) continue
            when (val pc = cells[r][c]) {
                null -> append('.')
                else -> when {
                    pc.side == Side.White && !pc.isKing -> append('w')
                    pc.side == Side.White && pc.isKing -> append('W')
                    pc.side == Side.Black && !pc.isKing -> append('b')
                    else -> append('B')
                }
            }
        }
    }
}
