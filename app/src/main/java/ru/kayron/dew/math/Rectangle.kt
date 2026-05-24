package ru.kayron.dew.math

data class Rectangle(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0
) {
    val left: Int get() = x
    val right: Int get() = x + width
    val top: Int get() = y
    val bottom: Int get() = y + height

    val center: Point get() = Point(x + width / 2, y + height / 2)

    val location: Point get() = Point(x, y)

    val isZero: Boolean get() = x == 0 && y == 0 && width == 0 && height == 0

    val isEmpty: Boolean get() = width == 0 && height == 0

    fun contains(x: Int, y: Int): Boolean =
        x >= left && x < right && y >= top && y < bottom

    fun contains(point: Point): Boolean = contains(point.x, point.y)

    fun contains(rect: Rectangle): Boolean =
        rect.left >= left && rect.right <= right && rect.top >= top && rect.bottom <= bottom

    fun inflate(horizontalAmount: Int, verticalAmount: Int) {
        x -= horizontalAmount
        y -= verticalAmount
        width += horizontalAmount * 2
        height += verticalAmount * 2
    }

    fun intersects(other: Rectangle): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top

    fun intersect(other: Rectangle): Rectangle {
        if (!intersects(other)) return Rectangle()
        return Rectangle(
            maxOf(left, other.left),
            maxOf(top, other.top),
            minOf(right, other.right) - maxOf(left, other.left),
            minOf(bottom, other.bottom) - maxOf(top, other.top)
        )
    }

    fun union(other: Rectangle): Rectangle {
        val minX = minOf(left, other.left)
        val minY = minOf(top, other.top)
        val maxX = maxOf(right, other.right)
        val maxY = maxOf(bottom, other.bottom)
        return Rectangle(minX, minY, maxX - minX, maxY - minY)
    }

    fun offset(offsetX: Int, offsetY: Int) {
        x += offsetX
        y += offsetY
    }

    fun offset(amount: Point) {
        x += amount.x
        y += amount.y
    }

    fun toFloatArray(): FloatArray = floatArrayOf(
        x.toFloat(), y.toFloat(),
        width.toFloat(), height.toFloat()
    )

    override fun toString(): String = "Rectangle($x, $y, $width, $height)"
}
