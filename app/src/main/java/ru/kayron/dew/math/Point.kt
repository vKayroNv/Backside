package ru.kayron.dew.math

data class Point(var x: Int = 0, var y: Int = 0) {

    companion object {
        val Zero = Point(0, 0)
        val One = Point(1, 1)
    }

    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scalar: Int) = Point(x * scalar, y * scalar)
    operator fun div(scalar: Int) = Point(x / scalar, y / scalar)
    operator fun unaryMinus() = Point(-x, -y)

    fun toVector2(): Vector2 = Vector2(x.toFloat(), y.toFloat())

    override fun toString(): String = "($x, $y)"
}
