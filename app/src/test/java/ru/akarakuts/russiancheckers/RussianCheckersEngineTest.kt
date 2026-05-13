package ru.akarakuts.russiancheckers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.RussianCheckersEngine
import ru.akarakuts.russiancheckers.game.Side

/** Unit tests for [Board] encoding and [RussianCheckersEngine] legality. */
class RussianCheckersEngineTest {

    @Test
    fun initialPosition_whiteHasMoves() {
        val b = Board.initial()
        val paths = RussianCheckersEngine.legalPaths(b, Side.White)
        assertFalse(paths.isEmpty())
    }

    @Test
    fun boardEncodeRoundTrip() {
        val b0 = Board.initial()
        val enc = b0.encode()
        assertEquals(32, enc.length)
        val b1 = Board.decodeBoard(enc)
        assertNotNull(b1)
        assertEquals(enc, b1!!.encode())
    }

    @Test
    fun initialPosition_applySimpleMove_updatesBoard() {
        val b0 = Board.initial()
        val path = RussianCheckersEngine.legalPaths(b0, Side.White).first()
        val b1 = RussianCheckersEngine.applyPath(b0, path)
        assertTrue(b1[path.first()] == null)
        assertTrue(b1[path.last()] != null)
    }
}
