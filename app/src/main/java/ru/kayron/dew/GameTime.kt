package ru.kayron.dew

data class GameTime(
    var totalGameTime: Long = 0L,
    var elapsedGameTime: Long = 0L,
    var isRunningSlowly: Boolean = false
) {
    val totalGameTimeSeconds: Float get() = totalGameTime / 1_000_000_000f
    val elapsedGameTimeSeconds: Float get() = elapsedGameTime / 1_000_000_000f
}
