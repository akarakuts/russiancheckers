package ru.akarakuts.russiancheckers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.akarakuts.russiancheckers.game.AiDifficulty
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.CheckersAi
import ru.akarakuts.russiancheckers.game.Piece
import ru.akarakuts.russiancheckers.game.Pos
import ru.akarakuts.russiancheckers.game.RussianCheckersEngine
import ru.akarakuts.russiancheckers.game.Side

/** Unit tests for [CheckersAi] move selection with the time-budgeted search. */
class CheckersAiTest {

    @Test
    fun choosesLegalMove_fromInitialPosition() {
        val b = Board.initial()
        val move = CheckersAi.chooseMove(b, Side.White, AiDifficulty.Normal)
        assertNotNull(move)
        assertTrue(RussianCheckersEngine.legalPaths(b, Side.White).contains(move))
    }

    @Test
    fun returnsNull_whenNoMoves() {
        val b = Board.empty().apply {
            this[Pos(0, 1)] = Piece(Side.White, isKing = false)
            this[Pos(5, 4)] = Piece(Side.Black, isKing = false)
        }
        assertNull(CheckersAi.chooseMove(b, Side.White, AiDifficulty.Easy))
    }

    @Test
    fun takesForcedCapture() {
        val b = Board.empty().apply {
            this[Pos(5, 2)] = Piece(Side.White, isKing = false)
            this[Pos(4, 3)] = Piece(Side.Black, isKing = false)
            this[Pos(0, 7)] = Piece(Side.Black, isKing = false)
        }
        val move = CheckersAi.chooseMove(b, Side.White, AiDifficulty.Normal)
        assertNotNull(move)
        assertEquals(Pos(3, 4), move!!.last())
    }

    @Test
    fun expert_respectsTimeBudgetRoughly() {
        val b = Board.initial()
        val start = System.currentTimeMillis()
        CheckersAi.chooseMove(b, Side.White, AiDifficulty.Expert)
        val elapsed = System.currentTimeMillis() - start
        // Бюджет 2500 мс; допускаем накладные расходы, но не многократное превышение.
        assertTrue("took ${elapsed}ms", elapsed < 8000)
    }
}
