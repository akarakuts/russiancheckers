package ru.akarakuts.russiancheckers.game

/** AI strength: iterative deepening up to [maxDepth] within [timeBudgetMs]. */
enum class AiDifficulty(val code: String, val maxDepth: Int, val timeBudgetMs: Long) {
    Easy("E", 2, 150),
    Normal("M", 5, 500),
    Hard("H", 8, 1200),
    Expert("X", 12, 2500),
    ;

    companion object {
        fun fromCode(code: String?): AiDifficulty = entries.firstOrNull { it.code == code } ?: Normal
    }
}
