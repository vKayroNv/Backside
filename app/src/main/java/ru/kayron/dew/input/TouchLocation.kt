package ru.kayron.dew.input

import ru.kayron.dew.math.Vector2

data class TouchLocation(
    val id: Int = 0,
    val position: Vector2 = Vector2.Zero,
    val previousPosition: Vector2 = Vector2.Zero,
    val state: TouchLocationState = TouchLocationState.Invalid,
    val pressure: Float = 1f
) {
    enum class TouchLocationState {
        Invalid,
        Pressed,
        Moved,
        Released,
    }

    fun tryGetPreviousLocation(): TouchLocation {
        return if (previousPosition != position) {
            copy(position = previousPosition, state = TouchLocationState.Moved)
        } else this
    }
}
