package ru.kayron.dew.input

object Mouse {
    private var currentState = MouseState()
    private var previousState = MouseState()

    fun getState(): MouseState = currentState

    fun setPosition(x: Int, y: Int) {
        currentState = currentState.copy(x = x, y = y)
    }

    fun isButtonPressed(button: MouseState.ButtonState): Boolean =
        button == MouseState.ButtonState.Pressed

    internal fun onTouch(x: Float, y: Float) {
        currentState = currentState.copy(
            x = x.toInt(),
            y = y.toInt(),
            leftButton = MouseState.ButtonState.Pressed
        )
    }

    internal fun onTouchUp() {
        currentState = currentState.copy(
            leftButton = MouseState.ButtonState.Released
        )
    }

    internal fun update() {
        previousState = currentState
    }
}
