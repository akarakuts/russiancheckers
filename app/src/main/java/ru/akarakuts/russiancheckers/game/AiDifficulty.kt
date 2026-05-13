package ru.akarakuts.russiancheckers.game

/** AI search depth (higher = stronger, slower on weak devices). */
enum class AiDifficulty(val code: String, val searchDepth: Int) {
    Easy("E", 2),
    Normal("M", 4),
    Hard("H", 6),
    ;

    companion object {
        fun fromCode(code: String?): AiDifficulty = entries.firstOrNull { it.code == code } ?: Normal
    }
}
