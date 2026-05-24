package ru.kayron.dew.input

import android.view.KeyEvent

class KeyboardState {
    internal val keys = mutableSetOf<Keys>()

    fun isKeyDown(key: Keys): Boolean = keys.contains(key)

    fun isKeyUp(key: Keys): Boolean = !keys.contains(key)

    fun getPressedKeys(): Set<Keys> = keys.toSet()

    internal fun onKeyDown(keyCode: Int) {
        val key = keyCodeToKeys(keyCode) ?: return
        keys.add(key)
    }

    internal fun onKeyUp(keyCode: Int) {
        val key = keyCodeToKeys(keyCode) ?: return
        keys.remove(key)
    }

    internal fun clear() {
        keys.clear()
    }

    companion object {
        fun keyCodeToKeys(keyCode: Int): Keys? {
            return Keys.entries.find { it.value == keyCode }
        }
    }
}
