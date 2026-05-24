package ru.kayron.dew.graphics

import ru.kayron.dew.math.Matrix
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector3

data class Viewport(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var minDepth: Float = 0f,
    var maxDepth: Float = 1f
) {
    val aspectRatio: Float get() = if (height != 0) width.toFloat() / height.toFloat() else 0f

    val bounds: Rectangle get() = Rectangle(x, y, width, height)

    val titleSafeArea: Rectangle get() = Rectangle(x, y, width, height)

    fun project(source: Vector3, projection: Matrix, view: Matrix, world: Matrix): Vector3 {
        val matrix = world * view * projection
        var vector = Vector3.transform(source, matrix)
        val w = source.x * matrix.m14 + source.y * matrix.m24 + source.z * matrix.m34 + matrix.m44
        if (w != 0f) {
            vector /= w
        }
        vector.x = (vector.x + 1f) * 0.5f * width.toFloat() + x.toFloat()
        vector.y = (-vector.y + 1f) * 0.5f * height.toFloat() + y.toFloat()
        vector.z = vector.z * (maxDepth - minDepth) + minDepth
        return vector
    }

    fun unproject(source: Vector3, projection: Matrix, view: Matrix, world: Matrix): Vector3 {
        val matrix = Matrix.invert(world * view * projection)
        var vector = source
        vector.x = ((vector.x - x.toFloat()) / width.toFloat()) * 2f - 1f
        vector.y = -(((vector.y - y.toFloat()) / height.toFloat()) * 2f - 1f)
        vector.z = (vector.z - minDepth) / (maxDepth - minDepth)
        vector = Vector3.transform(vector, matrix)
        val w = source.x * matrix.m14 + source.y * matrix.m24 + source.z * matrix.m34 + matrix.m44
        if (w != 0f) {
            vector /= w
        }
        return vector
    }

    override fun toString(): String = "Viewport($x, $y, $width, $height)"
}
