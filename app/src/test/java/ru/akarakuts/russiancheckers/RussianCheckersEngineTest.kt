package ru.akarakuts.russiancheckers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.Piece
import ru.akarakuts.russiancheckers.game.Pos
import ru.akarakuts.russiancheckers.game.RussianCheckersEngine
import ru.akarakuts.russiancheckers.game.Side

/** Unit tests for [Board] encoding and [RussianCheckersEngine] legality. */
class RussianCheckersEngineTest {

    @Test
    fun initialPosition_whiteHasMoves() {
        val paths = RussianCheckersEngine.legalPaths(Board.initial(), Side.White)
        assertFalse(paths.isEmpty())
        assertTrue(paths.all { it.size == 2 })
    }

    @Test
    fun boardEncodeRoundTrip() {
        val b0 = Board.initial()
        val enc = b0.encode()
        assertEquals(32, enc.length)
        val b1 = Board.fromEncoded(enc)
        assertNotNull(b1)
        assertEquals(enc, b1!!.encode())
    }

    @Test
    fun decodeBoard_rejectsInvalid() {
        assertEquals(null, Board.fromEncoded("x".repeat(32)))
        assertEquals(null, Board.fromEncoded("short"))
    }

    @Test
    fun initialPosition_applySimpleMove_updatesBoard() {
        val b0 = Board.initial()
        val path = RussianCheckersEngine.legalPaths(b0, Side.White).first()
        val b1 = RussianCheckersEngine.applyPath(b0, path)
        assertEquals(null, b1[path.first()])
        assertNotNull(b1[path.last()])
    }

    @Test
    fun forcedCapture_blocksSimpleMoves() {
        val b = Board.empty().apply {
            this[Pos(5, 2)] = Piece(Side.White, isKing = false)
            this[Pos(4, 3)] = Piece(Side.Black, isKing = false)
        }
        val paths = RussianCheckersEngine.legalPaths(b, Side.White)
        assertTrue(paths.isNotEmpty())
        assertTrue(paths.all { it.size >= 2 })
        assertTrue(RussianCheckersEngine.hasForcedCapture(b, Side.White))
    }

    @Test
    fun maxCaptureFilter_keepsLongestOnly() {
        val b = Board.empty().apply {
            this[Pos(6, 1)] = Piece(Side.White, isKing = false)
            this[Pos(5, 2)] = Piece(Side.Black, isKing = false)
            this[Pos(3, 4)] = Piece(Side.Black, isKing = false)
            this[Pos(2, 5)] = null
            this[Pos(1, 6)] = null
        }
        val paths = RussianCheckersEngine.legalPaths(b, Side.White)
        assertTrue(paths.isNotEmpty())
        val jumps = paths.map { it.size - 1 }.toSet()
        assertEquals(1, jumps.size)
        assertEquals(paths.maxOf { it.size - 1 }, jumps.first())
    }

    @Test
    fun king_movesMultipleSquares() {
        val b = Board.empty().apply {
            this[Pos(4, 3)] = Piece(Side.White, isKing = true)
        }
        val paths = RussianCheckersEngine.legalPaths(b, Side.White)
        assertTrue(
            paths.any {
                val from = it.first()
                val to = it.last()
                kotlin.math.abs(from.r - to.r) > 1
            },
        )
    }

    @Test
    fun promotionDuringCapture_becomesKingOnLastRank() {
        val b = Board.empty().apply {
            this[Pos(2, 1)] = Piece(Side.White, isKing = false)
            this[Pos(1, 2)] = Piece(Side.Black, isKing = false)
        }
        val paths = RussianCheckersEngine.legalPaths(b, Side.White)
        assertEquals(1, paths.size)
        assertEquals(Pos(0, 3), paths.first().last())
        val applied = RussianCheckersEngine.applyPath(b, paths.first())
        assertTrue(applied[Pos(0, 3)]!!.isKing)
    }

    @Test
    fun capturedAlong_reportsCapturedSquares() {
        val b = Board.empty().apply {
            this[Pos(6, 1)] = Piece(Side.White, isKing = false)
            this[Pos(5, 2)] = Piece(Side.Black, isKing = false)
            this[Pos(3, 4)] = Piece(Side.Black, isKing = false)
        }
        val path = RussianCheckersEngine.legalPaths(b, Side.White).first()
        val captured = RussianCheckersEngine.capturedAlong(b, path)
        assertEquals(listOf(Pos(5, 2), Pos(3, 4)), captured)
        val simple = RussianCheckersEngine.capturedAlong(Board.initial(), listOf(Pos(5, 2), Pos(4, 3)))
        assertTrue(simple.isEmpty())
    }

    @Test
    fun noLegalMove_meansLoss() {
        val b = Board.empty().apply {
            this[Pos(0, 1)] = Piece(Side.White, isKing = false)
            this[Pos(5, 4)] = Piece(Side.Black, isKing = false)
        }
        assertFalse(RussianCheckersEngine.hasLegalMove(b, Side.White))
        assertTrue(RussianCheckersEngine.hasLegalMove(b, Side.Black))
    }
}
