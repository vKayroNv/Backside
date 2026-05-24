package ru.kayron.dew.input

import ru.kayron.dew.math.Point

data class MouseState(
    val x: Int = 0,
    val y: Int = 0,
    val leftButton: ButtonState = ButtonState.Released,
    val middleButton: ButtonState = ButtonState.Released,
    val rightButton: ButtonState = ButtonState.Released,
    val scrollWheelValue: Int = 0,
    val horizontalScrollWheelValue: Int = 0,
) {
    val position: Point get() = Point(x, y)

    enum class ButtonState {
        Pressed,
        Released,
    }
}
