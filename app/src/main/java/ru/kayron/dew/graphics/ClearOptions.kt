package ru.kayron.dew.graphics

enum class ClearOptions(val value: Int) {
    Target(1),
    DepthBuffer(2),
    Stencil(4),
}
