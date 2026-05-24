package ru.kayron.dew.audio

class AudioEngine {
    var isDisposed: Boolean = false
        private set

    fun update() {}

    fun dispose() {
        isDisposed = true
    }
}
