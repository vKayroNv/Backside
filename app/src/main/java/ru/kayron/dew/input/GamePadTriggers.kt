package ru.kayron.dew.input

data class GamePadTriggers(
    val left: Float = 0f,
    val right: Float = 0f,
) {
    companion object {
        val None = GamePadTriggers()
    }
}
