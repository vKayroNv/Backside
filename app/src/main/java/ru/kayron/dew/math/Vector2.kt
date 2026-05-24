package ru.kayron.dew.math

import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

data class Vector2(var x: Float = 0f, var y: Float = 0f) {

    companion object {
        val Zero = Vector2(0f, 0f)
        val One = Vector2(1f, 1f)
        val UnitX = Vector2(1f, 0f)
        val UnitY = Vector2(0f, 1f)
    }

    fun length(): Float = sqrt(x * x + y * y)

    fun lengthSquared(): Float = x * x + y * y

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len
            y /= len
        }
    }

    fun toArray(): FloatArray = floatArrayOf(x, y)

    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(other: Vector2) = Vector2(x * other.x, y * other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    operator fun div(other: Vector2) = Vector2(x / other.x, y / other.y)
    operator fun div(scalar: Float) = Vector2(x / scalar, y / scalar)
    operator fun unaryMinus() = Vector2(-x, -y)

    // ---------- Static methods ----------

    fun add(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x + v2.x, v1.y + v2.y)

    fun subtract(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x - v2.x, v1.y - v2.y)

    fun multiply(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x * v2.x, v1.y * v2.y)

    fun multiply(v: Vector2, scalar: Float): Vector2 = Vector2(v.x * scalar, v.y * scalar)

    fun divide(v1: Vector2, v2: Vector2): Vector2 = Vector2(v1.x / v2.x, v1.y / v2.y)

    fun divide(v: Vector2, scalar: Float): Vector2 = Vector2(v.x / scalar, v.y / scalar)

    fun negate(v: Vector2): Vector2 = Vector2(-v.x, -v.y)

    fun dot(v1: Vector2, v2: Vector2): Float = v1.x * v2.x + v1.y * v2.y

    fun cross(v1: Vector2, v2: Vector2): Float = v1.x * v2.y - v1.y * v2.x

    fun distance(v1: Vector2, v2: Vector2): Float = (v1 - v2).length()

    fun distanceSquared(v1: Vector2, v2: Vector2): Float = (v1 - v2).lengthSquared()

    fun lerp(v1: Vector2, v2: Vector2, amount: Float): Vector2 =
        Vector2(
            MathHelper.lerp(v1.x, v2.x, amount),
            MathHelper.lerp(v1.y, v2.y, amount)
        )

    fun normalize(v: Vector2): Vector2 {
        val len = v.length()
        return if (len > 0f) Vector2(v.x / len, v.y / len) else Vector2.Zero
    }

    fun reflect(vector: Vector2, normal: Vector2): Vector2 {
        val d = dot(vector, normal)
        return vector - (normal * (2f * d))
    }

    fun min(v1: Vector2, v2: Vector2): Vector2 =
        Vector2(minOf(v1.x, v2.x), minOf(v1.y, v2.y))

    fun max(v1: Vector2, v2: Vector2): Vector2 =
        Vector2(maxOf(v1.x, v2.x), maxOf(v1.y, v2.y))

    fun clamp(v: Vector2, min: Vector2, max: Vector2): Vector2 =
        Vector2(
            MathHelper.clamp(v.x, min.x, max.x),
            MathHelper.clamp(v.y, min.y, max.y)
        )

    fun transform(v: Vector2, matrix: Matrix): Vector2 {
        val x = v.x * matrix.m11 + v.y * matrix.m21 + matrix.m41
        val y = v.x * matrix.m12 + v.y * matrix.m22 + matrix.m42
        return Vector2(x, y)
    }

    fun transformNormal(v: Vector2, matrix: Matrix): Vector2 {
        val x = v.x * matrix.m11 + v.y * matrix.m21
        val y = v.x * matrix.m12 + v.y * matrix.m22
        return Vector2(x, y)
    }

    fun angle(v1: Vector2, v2: Vector2): Float =
        atan2(v2.y - v1.y, v2.x - v1.x)

    override fun toString(): String = "($x, $y)"
}
