package ru.akarakuts.russiancheckers.data

import ru.akarakuts.russiancheckers.game.AiDifficulty

/** User toggles and AI options persisted outside the saved position. */
data class CheckersSettings(
    val botEnabled: Boolean,
    val humanIsWhite: Boolean,
    val aiDifficulty: AiDifficulty,
    val showCoordinates: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

/** Lifetime results vs the bot, persisted in DataStore. */
data class GameStats(
    val wins: Int = 0,
    val losses: Int = 0,
    val winStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    val total: Int get() = wins + losses
}
