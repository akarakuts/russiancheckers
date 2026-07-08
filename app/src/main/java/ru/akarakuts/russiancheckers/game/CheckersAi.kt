package ru.akarakuts.russiancheckers.game

import kotlin.math.max
import kotlin.random.Random

/**
 * Negamax + alpha–beta with iterative deepening, time budget and a transposition table.
 * Depth/time limits come from [AiDifficulty]; evaluation = material + advancement + mobility.
 */
object CheckersAi {

    private const val INF = 1_000_000
    private const val WIN = 900_000

    private class TtEntry(val depth: Int, val score: Int, val flag: Int)

    private const val EXACT = 0
    private const val LOWER = 1
    private const val UPPER = 2

    private class TimeUp : RuntimeException() {
        override fun fillInStackTrace(): Throwable = this
    }

    private class Search(val deadlineNanos: Long) {
        val tt = HashMap<String, TtEntry>()
        var nodes = 0

        fun checkTime() {
            // System.nanoTime() дорог — проверяем раз в 512 узлов.
            if (++nodes and 511 == 0 && System.nanoTime() > deadlineNanos) throw TimeUp()
        }
    }

    fun chooseMove(board: Board, side: Side, difficulty: AiDifficulty = AiDifficulty.Normal): Path? {
        val paths = RussianCheckersEngine.legalPaths(board, side)
        if (paths.isEmpty()) return null
        if (paths.size == 1) return paths.first()

        val search = Search(System.nanoTime() + difficulty.timeBudgetMs * 1_000_000)
        // В дебюте немного рандома, чтобы партии не повторялись.
        val opening = board.count(Side.White) + board.count(Side.Black) >= 22
        var ordered = paths.sortedByDescending { pathQuickScore(board, side, it) }
        var bestPath = ordered.first()

        for (depth in 1..difficulty.maxDepth) {
            try {
                val scored = ordered.map { path ->
                    val child = RussianCheckersEngine.applyPath(board, path)
                    val jitter = if (opening) Random.nextInt(0, 8) else 0
                    path to (-negamax(search, child, side.other(), depth - 1, -INF, INF, 1) + jitter)
                }.sortedByDescending { it.second }
                ordered = scored.map { it.first }
                bestPath = ordered.first()
                // Найден форсированный выигрыш — глубже искать незачем.
                if (scored.first().second >= WIN - 100) break
            } catch (_: TimeUp) {
                break
            }
        }
        return bestPath
    }

    private fun pathQuickScore(board: Board, side: Side, path: Path): Int {
        val after = RussianCheckersEngine.applyPath(board, path)
        return evaluateMaterial(after, side) + path.size
    }

    private fun negamax(
        search: Search,
        board: Board,
        side: Side,
        depth: Int,
        alpha: Int,
        beta: Int,
        ply: Int,
    ): Int {
        search.checkTime()

        val key = board.encode() + side.name[0]
        val cached = search.tt[key]
        if (cached != null && cached.depth >= depth) {
            when (cached.flag) {
                EXACT -> return cached.score
                LOWER -> if (cached.score >= beta) return cached.score
                UPPER -> if (cached.score <= alpha) return cached.score
            }
        }

        val paths = RussianCheckersEngine.legalPaths(board, side)
        if (paths.isEmpty()) return -(WIN - ply)
        if (depth == 0) return evaluate(board, side, paths.size)

        var a = alpha
        var best = Int.MIN_VALUE
        for (path in paths.sortedByDescending { pathQuickScore(board, side, it) }) {
            val child = RussianCheckersEngine.applyPath(board, path)
            val s = -negamax(search, child, side.other(), depth - 1, -beta, -a, ply + 1)
            best = max(best, s)
            a = max(a, s)
            if (a >= beta) break
        }

        val flag = when {
            best <= alpha -> UPPER
            best >= beta -> LOWER
            else -> EXACT
        }
        search.tt[key] = TtEntry(depth, best, flag)
        return best
    }

    private fun evaluate(board: Board, forSide: Side, myMobility: Int): Int {
        val theirMobility = RussianCheckersEngine.legalPaths(board, forSide.other()).size
        return evaluateMaterial(board, forSide) + 2 * (myMobility - theirMobility)
    }

    /** Material (man 100 / king 300) + продвижение простых к дамочному полю. */
    private fun evaluateMaterial(board: Board, forSide: Side): Int {
        var s = 0
        for (r in 0..7) for (c in 0..7) {
            val p = Pos(r, c)
            if (!p.isPlayable()) continue
            val pc = board[p] ?: continue
            val mul = if (pc.side == forSide) 1 else -1
            var v = if (pc.isKing) 300 else 100
            if (!pc.isKing) {
                val advance = if (pc.side == Side.White) 7 - r else r
                v += advance * 4
            }
            s += mul * v
        }
        return s
    }
}
