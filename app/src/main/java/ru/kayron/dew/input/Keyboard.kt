package ru.kayron.dew.input

object Keyboard {
    private val currentState = KeyboardState()
    private val previousState = KeyboardState()

    fun getState(): KeyboardState = currentState

    fun getPreviousState(): KeyboardState = previousState

    fun isKeyPressed(key: Keys): Boolean =
        currentState.isKeyDown(key) && previousState.isKeyUp(key)

    fun isKeyReleased(key: Keys): Boolean =
        currentState.isKeyUp(key) && previousState.isKeyDown(key)

    internal fun onKeyDown(keyCode: Int) {
        currentState.onKeyDown(keyCode)
    }

    internal fun onKeyUp(keyCode: Int) {
        currentState.onKeyUp(keyCode)
    }

    internal fun update() {
        previousState.keys.clear()
        previousState.keys.addAll(currentState.keys)
    }

    internal fun clear() {
        previousState.clear()
        currentState.clear()
    }
}
