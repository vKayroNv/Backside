package ru.kayron.dew.math

import kotlin.math.sqrt

data class Vector4(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 0f) {

    companion object {
        val Zero = Vector4(0f, 0f, 0f, 0f)
        val One = Vector4(1f, 1f, 1f, 1f)

        fun dot(v1: Vector4, v2: Vector4): Float =
            v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1.w * v2.w

        fun lerp(v1: Vector4, v2: Vector4, amount: Float): Vector4 = Vector4(
            MathHelper.lerp(v1.x, v2.x, amount),
            MathHelper.lerp(v1.y, v2.y, amount),
            MathHelper.lerp(v1.z, v2.z, amount),
            MathHelper.lerp(v1.w, v2.w, amount)
        )

        fun transform(v: Vector4, matrix: Matrix): Vector4 {
            val x = v.x * matrix.m11 + v.y * matrix.m21 + v.z * matrix.m31 + v.w * matrix.m41
            val y = v.x * matrix.m12 + v.y * matrix.m22 + v.z * matrix.m32 + v.w * matrix.m42
            val z = v.x * matrix.m13 + v.y * matrix.m23 + v.z * matrix.m33 + v.w * matrix.m43
            val w = v.x * matrix.m14 + v.y * matrix.m24 + v.z * matrix.m34 + v.w * matrix.m44
            return Vector4(x, y, z, w)
        }
    }

    fun length(): Float = sqrt(x * x + y * y + z * z + w * w)

    fun lengthSquared(): Float = x * x + y * y + z * z + w * w

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len; y /= len; z /= len; w /= len
        }
    }

    operator fun plus(other: Vector4) = Vector4(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Vector4) = Vector4(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(other: Vector4) = Vector4(x * other.x, y * other.y, z * other.z, w * other.w)
    operator fun times(scalar: Float) = Vector4(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun div(scalar: Float) = Vector4(x / scalar, y / scalar, z / scalar, w / scalar)
    operator fun unaryMinus() = Vector4(-x, -y, -z, -w)

    override fun toString(): String = "($x, $y, $z, $w)"
}
