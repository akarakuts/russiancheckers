package ru.akarakuts.russiancheckers

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.akarakuts.russiancheckers.data.CheckersSettings
import ru.akarakuts.russiancheckers.data.GamePreferencesRepository
import ru.akarakuts.russiancheckers.data.GameStats
import ru.akarakuts.russiancheckers.data.LoadGameResult
import ru.akarakuts.russiancheckers.game.AiDifficulty
import ru.akarakuts.russiancheckers.game.Board
import ru.akarakuts.russiancheckers.game.Side

/** Tests for [GamePreferencesRepository] on an isolated per-test DataStore file. */
@RunWith(RobolectricTestRunner::class)
class GamePreferencesRepositoryTest {

    private lateinit var scope: CoroutineScope
    private lateinit var repo: GamePreferencesRepository

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dir = Files.createTempDirectory("checkers-ds").toFile()
        val ds = PreferenceDataStoreFactory.create(scope = scope) {
            File(dir, "test.preferences_pb")
        }
        repo = GamePreferencesRepository(ds)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun settings_roundTrip() = runBlocking {
        val s = CheckersSettings(
            botEnabled = false,
            humanIsWhite = false,
            aiDifficulty = AiDifficulty.Expert,
            showCoordinates = false,
            soundEnabled = false,
            hapticsEnabled = false,
        )
        repo.saveSettings(s)
        assertEquals(s, repo.loadSettings())
    }

    @Test
    fun game_saveAndLoad() = runBlocking {
        val b = Board.initial()
        repo.saveGame(b, Side.Black, null)
        val loaded = repo.loadGame()
        assertTrue(loaded is LoadGameResult.Ok)
        val (board, turn, winner) = (loaded as LoadGameResult.Ok).game
        assertEquals(b.encode(), board.encode())
        assertEquals(Side.Black, turn)
        assertEquals(null, winner)

        repo.clearSavedGame()
        assertEquals(LoadGameResult.None, repo.loadGame())
    }

    @Test
    fun stats_recordAndReset() = runBlocking {
        val s1 = repo.recordResult(humanWon = true)
        assertEquals(GameStats(wins = 1, losses = 0, winStreak = 1, bestStreak = 1), s1)

        val s2 = repo.recordResult(humanWon = true)
        assertEquals(2, s2.winStreak)
        assertEquals(2, s2.bestStreak)

        val s3 = repo.recordResult(humanWon = false)
        assertEquals(1, s3.losses)
        assertEquals(0, s3.winStreak)
        assertEquals(2, s3.bestStreak)
        assertEquals(3, s3.total)

        assertEquals(s3, repo.loadStats())
        repo.resetStats()
        assertEquals(GameStats(), repo.loadStats())
    }
}
