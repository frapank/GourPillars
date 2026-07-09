package org.gourmet.gourPillars.managers

enum class XpSource(
    val configKey: String,
) {
    KILL("kill"),
    VOID_KILL("void-kill"),
    WIN("win"),
    GAME_PLAYED("game-played"),
}
