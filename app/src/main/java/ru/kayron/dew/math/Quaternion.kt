package ru.kayron.dew.math

import kotlin.math.sqrt
import kotlin.math.acos

data class Quaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {

    companion object {
        val Identity = Quaternion(0f, 0f, 0f, 1f)

        fun createFromAxisAngle(axis: Vector3, angle: Float): Quaternion {
            val half = angle * 0.5f
            val s = kotlin.math.sin(half.toDouble()).toFloat()
            return Quaternion(axis.x * s, axis.y * s, axis.z * s, kotlin.math.cos(half.toDouble()).toFloat())
        }

        fun createFromYawPitchRoll(yaw: Float, pitch: Float, roll: Float): Quaternion {
            val halfYaw = yaw * 0.5f
            val halfPitch = pitch * 0.5f
            val halfRoll = roll * 0.5f
            val cosYaw = kotlin.math.cos(halfYaw.toDouble()).toFloat()
            val sinYaw = kotlin.math.sin(halfYaw.toDouble()).toFloat()
            val cosPitch = kotlin.math.cos(halfPitch.toDouble()).toFloat()
            val sinPitch = kotlin.math.sin(halfPitch.toDouble()).toFloat()
            val cosRoll = kotlin.math.cos(halfRoll.toDouble()).toFloat()
            val sinRoll = kotlin.math.sin(halfRoll.toDouble()).toFloat()
            return Quaternion(
                cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw,
                cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw,
                sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw,
                cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw
            )
        }

        fun createFromRotationMatrix(matrix: Matrix): Quaternion {
            val trace = matrix.m11 + matrix.m22 + matrix.m33
            if (trace > 0f) {
                var s = sqrt((trace + 1f).toDouble()).toFloat() * 2f
                val w = 0.25f * s
                val x = (matrix.m32 - matrix.m23) / s
                val y = (matrix.m13 - matrix.m31) / s
                val z = (matrix.m21 - matrix.m12) / s
                return Quaternion(x, y, z, w)
            } else if (matrix.m11 > matrix.m22 && matrix.m11 > matrix.m33) {
                var s = sqrt((1f + matrix.m11 - matrix.m22 - matrix.m33).toDouble()).toFloat() * 2f
                val w = (matrix.m32 - matrix.m23) / s
                val x = 0.25f * s
                val y = (matrix.m12 + matrix.m21) / s
                val z = (matrix.m13 + matrix.m31) / s
                return Quaternion(x, y, z, w)
            } else if (matrix.m22 > matrix.m33) {
                var s = sqrt((1f + matrix.m22 - matrix.m11 - matrix.m33).toDouble()).toFloat() * 2f
                val w = (matrix.m13 - matrix.m31) / s
                val x = (matrix.m12 + matrix.m21) / s
                val y = 0.25f * s
                val z = (matrix.m23 + matrix.m32) / s
                return Quaternion(x, y, z, w)
            } else {
                var s = sqrt((1f + matrix.m33 - matrix.m11 - matrix.m22).toDouble()).toFloat() * 2f
                val w = (matrix.m21 - matrix.m12) / s
                val x = (matrix.m13 + matrix.m31) / s
                val y = (matrix.m23 + matrix.m32) / s
                val z = 0.25f * s
                return Quaternion(x, y, z, w)
            }
        }

        fun conjugate(q: Quaternion): Quaternion = Quaternion(-q.x, -q.y, -q.z, q.w)

        fun inverse(q: Quaternion): Quaternion {
            val lenSq = q.lengthSquared()
            if (lenSq == 0f) return Quaternion.Identity
            val inv = 1f / lenSq
            return Quaternion(-q.x * inv, -q.y * inv, -q.z * inv, q.w * inv)
        }

        fun dot(q1: Quaternion, q2: Quaternion): Float =
            q1.x * q2.x + q1.y * q2.y + q1.z * q2.z + q1.w * q2.w

        fun lerp(q1: Quaternion, q2: Quaternion, amount: Float): Quaternion {
            val t = MathHelper.clamp(amount, 0f, 1f)
            return Quaternion(
                MathHelper.lerp(q1.x, q2.x, t),
                MathHelper.lerp(q1.y, q2.y, t),
                MathHelper.lerp(q1.z, q2.z, t),
                MathHelper.lerp(q1.w, q2.w, t)
            ).let { it.normalize(); it }
        }

        fun slerp(q1: Quaternion, q2: Quaternion, amount: Float): Quaternion {
            var cosOmega = Quaternion.dot(q1, q2)
            var q2Copy = q2
            if (cosOmega < 0f) {
                cosOmega = -cosOmega
                q2Copy = -q2
            }
            val k0: Float
            val k1: Float
            if (cosOmega > 0.9999f) {
                k0 = 1f - amount
                k1 = amount
            } else {
                val sinOmega = sqrt((1f - cosOmega * cosOmega).toDouble()).toFloat()
                val omega = acos(cosOmega.toDouble()).toFloat()
                val invSinOmega = 1f / sinOmega
                k0 = kotlin.math.sin(((1f - amount) * omega).toDouble()).toFloat() * invSinOmega
                k1 = kotlin.math.sin((amount * omega).toDouble()).toFloat() * invSinOmega
            }
            return Quaternion(
                q1.x * k0 + q2Copy.x * k1,
                q1.y * k0 + q2Copy.y * k1,
                q1.z * k0 + q2Copy.z * k1,
                q1.w * k0 + q2Copy.w * k1
            )
        }

        fun normalize(q: Quaternion): Quaternion {
            val len = q.length()
            return if (len > 0f) Quaternion(q.x / len, q.y / len, q.z / len, q.w / len) else Quaternion.Identity
        }
    }

    fun length(): Float = sqrt((x * x + y * y + z * z + w * w).toDouble()).toFloat()

    fun lengthSquared(): Float = x * x + y * y + z * z + w * w

    fun normalize() {
        val len = length()
        if (len > 0f) {
            x /= len; y /= len; z /= len; w /= len
        }
    }

    fun conjugate() {
        x = -x; y = -y; z = -z
    }

    fun toMatrix(): Matrix = Matrix.createFromQuaternion(this)

    operator fun plus(other: Quaternion) = Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Quaternion) = Quaternion(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            w * other.x + x * other.w + y * other.z - z * other.y,
            w * other.y - x * other.z + y * other.w + z * other.x,
            w * other.z + x * other.y - y * other.x + z * other.w,
            w * other.w - x * other.x - y * other.y - z * other.z
        )
    }
    operator fun times(scalar: Float) = Quaternion(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun unaryMinus() = Quaternion(-x, -y, -z, -w)

    override fun toString(): String = "($x, $y, $z, $w)"
}
