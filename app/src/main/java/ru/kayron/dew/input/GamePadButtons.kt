package ru.kayron.dew.input

data class GamePadButtons(
    val a: Boolean = false,
    val b: Boolean = false,
    val x: Boolean = false,
    val y: Boolean = false,
    val start: Boolean = false,
    val back: Boolean = false,
    val leftShoulder: Boolean = false,
    val rightShoulder: Boolean = false,
    val leftStick: Boolean = false,
    val rightStick: Boolean = false,
    val bigButton: Boolean = false,
) {
    companion object {
        val None = GamePadButtons()
    }
}
