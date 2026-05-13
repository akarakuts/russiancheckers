package ru.akarakuts.russiancheckers.data

import ru.akarakuts.russiancheckers.game.AiDifficulty

/** User toggles and AI options persisted outside the saved position. */
data class CheckersSettings(
    val botEnabled: Boolean,
    val humanIsWhite: Boolean,
    val aiDifficulty: AiDifficulty,
)
