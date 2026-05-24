package ru.kayron.dew.input

import ru.kayron.dew.math.Vector2

data class GamePadThumbSticks(
    val left: Vector2 = Vector2.Zero,
    val right: Vector2 = Vector2.Zero,
) {
    companion object {
        val None = GamePadThumbSticks()
    }
}
