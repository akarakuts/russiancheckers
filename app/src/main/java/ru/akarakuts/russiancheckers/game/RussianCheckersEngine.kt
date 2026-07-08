package ru.akarakuts.russiancheckers.game

/**
 * Russian draughts on 8×8: mandatory capture, among captures pick a chain that removes the most pieces;
 * men move forward only without capture, capture in any diagonal; kings move and capture long-range.
 */
object RussianCheckersEngine {

    fun legalPaths(board: Board, side: Side): List<Path> {
        val captures = collectAllCapturePaths(board, side)
        if (captures.isNotEmpty()) {
            val maxJumps = captures.maxOf { it.size - 1 }
            return captures.filter { it.size - 1 == maxJumps }
        }
        return collectSimplePaths(board, side)
    }

    fun applyPath(board: Board, path: Path): Board {
        require(path.size >= 2)
        val b = board.copy()
        var cur = path.first()
        var piece = b[cur] ?: error("empty start")
        b[cur] = null
        for (i in 0 until path.lastIndex) {
            val to = path[i + 1]
            val cap = findSingleCaptured(b, cur, to, piece.side)
            if (cap != null) {
                require(b[cap]?.side == piece.side.other()) { "capture target" }
                b[cap] = null
            }
            cur = to
            piece = promoteIfLastRank(piece, cur)
        }
        b[path.last()] = promoteIfLastRank(piece, path.last())
        return b
    }

    /** Positions captured while walking [path] on [board] (empty for a simple move). */
    fun capturedAlong(board: Board, path: Path): List<Pos> {
        if (path.size < 2) return emptyList()
        val b = board.copy()
        val side = b[path.first()]?.side ?: return emptyList()
        b[path.first()] = null
        val out = mutableListOf<Pos>()
        var cur = path.first()
        for (i in 0 until path.lastIndex) {
            val to = path[i + 1]
            val cap = findSingleCaptured(b, cur, to, side)
            if (cap != null) {
                out.add(cap)
                b[cap] = null
            }
            cur = to
        }
        return out
    }

    /** True if [side] has at least one capture (before the “max captures” filter). */
    fun hasForcedCapture(board: Board, side: Side): Boolean =
        collectAllCapturePaths(board, side).isNotEmpty()

    /** False when [toMove] has no legal move (loss condition). */
    fun hasLegalMove(board: Board, toMove: Side): Boolean = legalPaths(board, toMove).isNotEmpty()

    // Captures

    private fun collectAllCapturePaths(board: Board, side: Side): List<Path> {
        val acc = mutableListOf<Path>()
        for (start in board.allPieces(side)) {
            dfsCaptures(board.copy(), start, side, listOf(start), acc)
        }
        return acc
    }

    private fun dfsCaptures(board: Board, at: Pos, side: Side, path: Path, acc: MutableList<Path>) {
        val piece = board[at] ?: return
        require(piece.side == side)
        val leaps = singleCaptureDestinations(at, board, piece)
        if (leaps.isEmpty()) {
            if (path.size > 1) acc.add(path)
            return
        }
        for (to in leaps) {
            val cap = findSingleCaptured(board, at, to, side) ?: continue
            val landed = promoteIfLastRank(piece, to)
            val b2 = board.copy()
            b2[cap] = null
            b2[to] = landed
            b2[at] = null
            dfsCaptures(b2, to, side, path + to, acc)
        }
    }

    private fun singleCaptureDestinations(from: Pos, board: Board, piece: Piece): List<Pos> {
        val out = mutableListOf<Pos>()
        if (!piece.isKing) {
            for (dr in intArrayOf(-1, 1)) for (dc in intArrayOf(-1, 1)) {
                val mid = Pos(from.r + dr, from.c + dc)
                val to = Pos(from.r + 2 * dr, from.c + 2 * dc)
                if (!to.isPlayable() || board[to] != null) continue
                val m = board[mid] ?: continue
                if (m.side != piece.side.other()) continue
                out.add(to)
            }
            return out
        }
        // King
        for (dr in intArrayOf(-1, 1)) for (dc in intArrayOf(-1, 1)) {
            var r = from.r + dr
            var c = from.c + dc
            while (Pos(r, c).isPlayable() && board[Pos(r, c)] == null) {
                r += dr; c += dc
            }
            if (!Pos(r, c).isPlayable()) continue
            val victim = board[Pos(r, c)] ?: continue
            if (victim.side != piece.side.other()) continue
            var er = r + dr
            var ec = c + dc
            while (Pos(er, ec).isPlayable()) {
                when (val cell = board[Pos(er, ec)]) {
                    null -> out.add(Pos(er, ec))
                    else -> break
                }
                er += dr; ec += dc
            }
        }
        return out
    }

    private fun findSingleCaptured(board: Board, from: Pos, to: Pos, side: Side): Pos? {
        val dr = (to.r - from.r).sign()
        val dc = (to.c - from.c).sign()
        if (dr == 0 || dc == 0) return null
        val steps = kotlin.math.abs(to.r - from.r)
        if (steps != kotlin.math.abs(to.c - from.c)) return null
        var enemy: Pos? = null
        var r = from.r + dr
        var c = from.c + dc
        repeat(steps - 1) {
            val p = Pos(r, c)
            if (!p.isPlayable()) return null
            when (val pc = board[p]) {
                null -> {}
                else -> {
                    if (pc.side != side.other()) return null
                    if (enemy != null) return null
                    enemy = p
                }
            }
            r += dr; c += dc
        }
        return enemy
    }

    /** Man becomes king immediately on the promotion rank (including mid-capture). */
    private fun promoteIfLastRank(piece: Piece, at: Pos): Piece {
        if (piece.isKing) return piece
        return when (piece.side) {
            Side.White -> if (at.r == 0) piece.copy(isKing = true) else piece
            Side.Black -> if (at.r == 7) piece.copy(isKing = true) else piece
        }
    }

    // Simple (non-capture) moves

    private fun collectSimplePaths(board: Board, side: Side): List<Path> = buildList {
        for (p in board.allPieces(side)) {
            val piece = board[p] ?: continue
            if (!piece.isKing) {
                val forward = if (piece.side == Side.White) -1 else 1
                for (dc in intArrayOf(-1, 1)) {
                    val to = Pos(p.r + forward, p.c + dc)
                    if (to.isPlayable() && board[to] == null) add(listOf(p, to))
                }
            } else {
                for (dr in intArrayOf(-1, 1)) for (dc in intArrayOf(-1, 1)) {
                    var r = p.r + dr
                    var c = p.c + dc
                    while (Pos(r, c).isPlayable() && board[Pos(r, c)] == null) {
                        add(listOf(p, Pos(r, c)))
                        r += dr; c += dc
                    }
                }
            }
        }
    }
}

private fun Int.sign(): Int = when {
    this > 0 -> 1
    this < 0 -> -1
    else -> 0
}
