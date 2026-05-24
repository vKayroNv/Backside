package ru.kayron.dew.graphics

data class DisplayMode(
    val width: Int,
    val height: Int,
    val format: SurfaceFormat = SurfaceFormat.Color,
    val refreshRate: Int = 60
)
