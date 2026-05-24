package ru.kayron.dew.input

data class GamePadDPad(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
) {
    companion object {
        val None = GamePadDPad()
    }
}
