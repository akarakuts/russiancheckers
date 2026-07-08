package ru.akarakuts.russiancheckers

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.akarakuts.russiancheckers.data.GamePreferencesRepository
import ru.akarakuts.russiancheckers.game.AiDifficulty
import ru.akarakuts.russiancheckers.ui.CheckersViewModel

/**
 * Robolectric tests for [CheckersViewModel]: undo, move log, hint, AI turn-taking.
 * Main dispatcher is a test one; AI/DataStore work on real background threads,
 * so assertions go through [waitFor] polling. DataStore is per-test (temp file).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CheckersViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var dsScope: CoroutineScope

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @After
    fun tearDown() {
        dsScope.cancel()
        Dispatchers.resetMain()
    }

    private fun createVm(): CheckersViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dir = Files.createTempDirectory("checkers-vm-ds").toFile()
        val ds = PreferenceDataStoreFactory.create(scope = dsScope) {
            File(dir, "test.preferences_pb")
        }
        val vm = CheckersViewModel(app, GamePreferencesRepository(ds))
        waitFor("initial load") { vm.state.value.candidatePaths.isNotEmpty() }
        return vm
    }

    private fun waitFor(what: String, timeoutMs: Long = 10_000, cond: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            dispatcher.scheduler.advanceUntilIdle()
            if (cond()) return
            check(System.currentTimeMillis() < deadline) { "timeout: $what" }
            Thread.sleep(10)
        }
    }

    /** Two-player mode so the bot never interferes with the scenario. */
    private fun twoPlayerVm(): CheckersViewModel {
        val vm = createVm()
        if (vm.state.value.botEnabled) {
            vm.setBotEnabled(false)
            waitFor("bot off") { !vm.state.value.botEnabled }
        }
        vm.newGame()
        waitFor("fresh game") { vm.state.value.moveLog.isEmpty() && vm.state.value.candidatePaths.isNotEmpty() }
        return vm
    }

    private fun makeFirstLegalMove(vm: CheckersViewModel) {
        val path = vm.state.value.candidatePaths.first()
        for (cell in path) vm.onCellClicked(cell)
    }

    @Test
    fun freshGame_hasNoUndoAndEmptyLog() {
        val vm = twoPlayerVm()
        assertFalse(vm.state.value.undoAvailable)
        assertTrue(vm.state.value.moveLog.isEmpty())
    }

    @Test
    fun move_updatesLogLastMoveAndUndo() {
        val vm = twoPlayerVm()
        val path = vm.state.value.candidatePaths.first()
        for (cell in path) vm.onCellClicked(cell)
        val s = vm.state.value
        assertEquals(1, s.moveLog.size)
        assertNotNull(s.lastMove)
        assertEquals(path, s.lastMove!!.path)
        assertTrue(s.undoAvailable)
        assertEquals(null, s.board[path.first()])
        assertNotNull(s.board[path.last()])
    }

    @Test
    fun undo_revertsSinglePly_inTwoPlayerMode() {
        val vm = twoPlayerVm()
        val before = vm.state.value.board.encode()
        makeFirstLegalMove(vm)
        assertTrue(vm.state.value.undoAvailable)
        vm.undo()
        val s = vm.state.value
        assertEquals(before, s.board.encode())
        assertTrue(s.moveLog.isEmpty())
        assertFalse(s.undoAvailable)
    }

    @Test
    fun undo_vsBot_revertsToHumanTurn() {
        val vm = createVm()
        if (!vm.state.value.botEnabled) vm.setBotEnabled(true)
        vm.setAiDifficulty(AiDifficulty.Easy)
        if (!vm.state.value.humanIsWhite) vm.setHumanIsWhite(true)
        vm.newGame()
        waitFor("fresh vs bot") { vm.state.value.moveLog.isEmpty() && vm.state.value.candidatePaths.isNotEmpty() }

        val before = vm.state.value.board.encode()
        makeFirstLegalMove(vm)
        waitFor("AI reply") { vm.state.value.moveLog.size >= 2 && vm.state.value.isHumanTurn }

        vm.undo()
        val s = vm.state.value
        assertEquals(before, s.board.encode())
        assertTrue(s.isHumanTurn)
        assertTrue(s.moveLog.isEmpty())
    }

    @Test
    fun newGame_clearsHistoryAndLog() {
        val vm = twoPlayerVm()
        makeFirstLegalMove(vm)
        assertTrue(vm.state.value.moveLog.isNotEmpty())
        vm.newGame()
        waitFor("reset") { vm.state.value.moveLog.isEmpty() }
        assertFalse(vm.state.value.undoAvailable)
        assertEquals(null, vm.state.value.winner)
    }

    @Test
    fun hint_returnsLegalPath() {
        val vm = twoPlayerVm()
        vm.setAiDifficulty(AiDifficulty.Easy)
        vm.requestHint()
        waitFor("hint computed") { vm.state.value.hintPath != null }
        val s = vm.state.value
        assertTrue(s.candidatePaths.contains(s.hintPath))
        // Любой клик по доске сбрасывает подсказку.
        vm.onCellClicked(s.hintPath!!.first())
        assertEquals(null, vm.state.value.hintPath)
    }
}
