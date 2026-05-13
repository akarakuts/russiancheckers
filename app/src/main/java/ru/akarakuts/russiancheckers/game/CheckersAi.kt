package ru.akarakuts.russiancheckers.game

import kotlin.math.max
import kotlin.random.Random

/** Negamax + alpha–beta; material + mobility heuristic. Search depth comes from [AiDifficulty]. */
object CheckersAi {

    private const val INF = 40_000

    fun chooseMove(board: Board, side: Side, difficulty: AiDifficulty = AiDifficulty.Normal): Path? {
        val depth = difficulty.searchDepth
        val paths = RussianCheckersEngine.legalPaths(board, side)
        if (paths.isEmpty()) return null
        val ordered = paths.sortedByDescending { pathQuickScore(board, side, it) }
        var bestPath = ordered.first()
        var best = Int.MIN_VALUE / 2
        val jitter = Random.nextInt(-2, 3)
        for (path in ordered) {
            val next = RussianCheckersEngine.applyPath(board, path)
            val score = -negamax(next, side.other(), depth - 1, -INF, INF, 1) + jitter
            if (score > best) {
                best = score
                bestPath = path
            }
        }
        return bestPath
    }

    private fun pathQuickScore(board: Board, side: Side, path: Path): Int {
        val after = RussianCheckersEngine.applyPath(board, path)
        return evaluateMaterial(after, side) * 10 + path.size
    }

    private fun negamax(board: Board, side: Side, depth: Int, alpha: Int, beta: Int, ply: Int): Int {
        val paths = RussianCheckersEngine.legalPaths(board, side)
        if (paths.isEmpty()) return -(INF - ply)
        if (depth == 0) return evaluate(board, side)
        var a = alpha
        var best = Int.MIN_VALUE
        for (path in paths.sortedByDescending { pathQuickScore(board, side, it) }) {
            val child = RussianCheckersEngine.applyPath(board, path)
            val s = -negamax(child, side.other(), depth - 1, -beta, -a, ply + 1)
            best = max(best, s)
            a = max(a, s)
            if (a >= beta) break
        }
        return best
    }

    private fun evaluate(board: Board, forSide: Side): Int {
        var m = evaluateMaterial(board, forSide)
        val mob = RussianCheckersEngine.legalPaths(board, forSide).size -
            RussianCheckersEngine.legalPaths(board, forSide.other()).size
        m += mob
        return m
    }

    private fun evaluateMaterial(board: Board, forSide: Side): Int {
        var s = 0
        for (r in 0..7) for (c in 0..7) {
            val p = Pos(r, c)
            if (!p.isPlayable()) continue
            val pc = board[p] ?: continue
            val mul = if (pc.side == forSide) 1 else -1
            val v = if (pc.isKing) 5 else 3
            s += mul * v
        }
        return s
    }
}
