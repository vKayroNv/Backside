package ru.kayron.dew.audio

import ru.kayron.dew.math.Vector3

data class AudioEmitter(
    var position: Vector3 = Vector3.Zero,
    var forward: Vector3 = Vector3.Forward,
    var up: Vector3 = Vector3.Up,
    var velocity: Vector3 = Vector3.Zero,
    var dopplerScale: Float = 1f,
)
