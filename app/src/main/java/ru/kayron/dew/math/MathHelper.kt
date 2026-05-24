package ru.kayron.dew.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.PI

object MathHelper {
    const val E = 2.7182817f
    const val Log10E = 0.4342945f
    const val Log2E = 1.442695f
    const val Pi = 3.1415927f
    const val PiOver2 = 1.5707964f
    const val PiOver4 = 0.7853982f
    const val TwoPi = 6.2831855f

    fun clamp(value: Float, min: Float, max: Float): Float =
        when {
            value < min -> min
            value > max -> max
            else -> value
        }

    fun clamp(value: Int, min: Int, max: Int): Int =
        when {
            value < min -> min
            value > max -> max
            else -> value
        }

    fun distance(value1: Float, value2: Float): Float = abs(value1 - value2)

    fun lerp(value1: Float, value2: Float, amount: Float): Float =
        value1 + (value2 - value1) * amount

    fun smoothStep(value1: Float, value2: Float, amount: Float): Float {
        val t = clamp(amount, 0f, 1f)
        val t2 = t * t * (3f - 2f * t)
        return value1 + (value2 - value1) * t2
    }

    fun hermite(value1: Float, tangent1: Float, value2: Float, tangent2: Float, amount: Float): Float {
        val t = amount
        val t2 = t * t
        val t3 = t2 * t
        val a = 2f * t3 - 3f * t2 + 1f
        val b = t3 - 2f * t2 + t
        val c = -2f * t3 + 3f * t2
        val d = t3 - t2
        return value1 * a + tangent1 * b + value2 * c + tangent2 * d
    }

    fun catmullRom(value1: Float, value2: Float, value3: Float, value4: Float, amount: Float): Float {
        val t = amount
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * value2) +
            (-value1 + value3) * t +
            (2f * value1 - 5f * value2 + 4f * value3 - value4) * t2 +
            (-value1 + 3f * value2 - 3f * value3 + value4) * t3
        )
    }

    fun toRadians(degrees: Float): Float = degrees * (Pi / 180f)

    fun toDegrees(radians: Float): Float = radians * (180f / Pi)

    fun wrapAngle(angle: Float): Float {
        var a = angle % TwoPi
        if (a > Pi) a -= TwoPi
        if (a < -Pi) a += TwoPi
        return a
    }

    fun max(a: Float, b: Float): Float = max(a, b)
    fun min(a: Float, b: Float): Float = min(a, b)
    fun sqrt(value: Float): Float = sqrt(value)
}
