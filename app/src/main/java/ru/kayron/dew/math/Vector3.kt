package ru.kayron.dew.math

import kotlin.math.sqrt

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {

    companion object {
        val Zero = Vector3(0f, 0f, 0f)
        val One = Vector3(1f, 1f, 1f)
        val UnitX = Vector3(1f, 0f, 0f)
        val UnitY = Vector3(0f, 1f, 0f)
        val UnitZ = Vector3(0f, 0f, 1f)
        val Up = Vector3(0f, 1f, 0f)
        val Down = Vector3(0f, -1f, 0f)
        val Right = Vector3(1f, 0f, 0f)
        val Left = Vector3(-1f, 0f, 0f)
        val Forward = Vector3(0f, 0f, -1f)
        val Backward = Vector3(0f, 0f, 1f)

        fun add(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z)

        fun subtract(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z)

        fun multiply(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x * v2.x, v1.y * v2.y, v1.z * v2.z)

        fun multiply(v: Vector3, scalar: Float): Vector3 = Vector3(v.x * scalar, v.y * scalar, v.z * scalar)

        fun divide(v1: Vector3, v2: Vector3): Vector3 = Vector3(v1.x / v2.x, v1.y / v2.y, v1.z / v2.z)

        fun divide(v: Vector3, scalar: Float): Vector3 = Vector3(v.x / scalar, v.y / scalar, v.z / scalar)

        fun negate(v: Vector3): Vector3 = Vector3(-v.x, -v.y, -v.z)

        fun dot(v1: Vector3, v2: Vector3): Float = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z

        fun cross(v1: Vector3, v2: Vector3): Vector3 = Vector3(
            v1.y * v2.z - v1.z * v2.y,
            v1.z * v2.x - v1.x * v2.z,
            v1.x * v2.y - v1.y * v2.x
        )

        fun distance(v1: Vector3, v2: Vector3): Float = (v1 - v2).length()

        fun distanceSquared(v1: Vector3, v2: Vector3): Float = (v1 - v2).lengthSquared()

        fun lerp(v1: Vector3, v2: Vector3, amount: Float): Vector3 = Vector3(
            MathHelper.lerp(v1.x, v2.x, amount),
            MathHelper.lerp(v1.y, v2.y, amount),
            MathHelper.lerp(v1.z, v2.z, amount)
        )

        fun normalize(v: Vector3): Vector3 {
            val len = v.length()
            return if (len > 0f) Vector3(v.x / len, v.y / len, v.z / len) else Vector3.Zero
        }

        fun reflect(vector: Vector3, normal: Vector3): Vector3 {
            val d = dot(vector, normal)
            return vector - (normal * (2f * d))
        }

        fun min(v1: Vector3, v2: Vector3): Vector3 = Vector3(
            minOf(v1.x, v2.x), minOf(v1.y, v2.y), minOf(v1.z, v2.z)
        )

        fun max(v1: Vector3, v2: Vector3): Vector3 = Vector3(
            maxOf(v1.x, v2.x), maxOf(v1.y, v2.y), maxOf(v1.z, v2.z)
        )

        fun clamp(v: Vector3, min: Vector3, max: Vector3): Vector3 = Vector3(
            MathHelper.clamp(v.x, min.x, max.x),
            MathHelper.clamp(v.y, min.y, max.y),
            MathHelper.clamp(v.z, min.z, max.z)
        )

        fun transform(v: Vector3, matrix: Matrix): Vector3 {
            val x = v.x * matrix.m11 + v.y * matrix.m21 + v.z * matrix.m31 + matrix.m41
            val y = v.x * matrix.m12 + v.y * matrix.m22 + v.z * matrix.m32 + matrix.m42
            val z = v.x * matrix.m13 + v.y * matrix.m23 + v.z * matrix.m33 + matrix.m43
            return Vector3(x, y, z)
        }

        fun transformNormal(v: Vector3, matrix: Matrix): Vector3 {
            val x = v.x * matrix.m11 + v.y * matrix.m21 + v.z * matrix.m31
            val y = v.x * matrix.m12 + v.y * matrix.m22 + v.z * matrix.m32
            val z = v.x * matrix.m13 + v.y * matrix.m23 + v.z * matrix.m33
            return Vector3(x, y, z)
        }
    }

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len
            y /= len
            z /= len
        }
    }

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(other: Vector3) = Vector3(x * other.x, y * other.y, z * other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(other: Vector3) = Vector3(x / other.x, y / other.y, z / other.z)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus() = Vector3(-x, -y, -z)

    override fun toString(): String = "($x, $y, $z)"
}
