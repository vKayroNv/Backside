package ru.kayron.dew.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.sqrt

class Matrix {
    var m11: Float = 1f; var m12: Float = 0f; var m13: Float = 0f; var m14: Float = 0f
    var m21: Float = 0f; var m22: Float = 1f; var m23: Float = 0f; var m24: Float = 0f
    var m31: Float = 0f; var m32: Float = 0f; var m33: Float = 1f; var m34: Float = 0f
    var m41: Float = 0f; var m42: Float = 0f; var m43: Float = 0f; var m44: Float = 1f

    constructor()
    constructor(
        m11: Float, m12: Float, m13: Float, m14: Float,
        m21: Float, m22: Float, m23: Float, m24: Float,
        m31: Float, m32: Float, m33: Float, m34: Float,
        m41: Float, m42: Float, m43: Float, m44: Float
    ) {
        this.m11 = m11; this.m12 = m12; this.m13 = m13; this.m14 = m14
        this.m21 = m21; this.m22 = m22; this.m23 = m23; this.m24 = m24
        this.m31 = m31; this.m32 = m32; this.m33 = m33; this.m34 = m34
        this.m41 = m41; this.m42 = m42; this.m43 = m43; this.m44 = m44
    }

    companion object {
        val Identity = Matrix()

        fun createTranslation(x: Float, y: Float, z: Float): Matrix {
            val m = Matrix()
            m.m41 = x; m.m42 = y; m.m43 = z
            return m
        }

        fun createTranslation(position: Vector3): Matrix = createTranslation(position.x, position.y, position.z)

        fun createScale(x: Float, y: Float, z: Float): Matrix {
            val m = Matrix()
            m.m11 = x; m.m22 = y; m.m33 = z
            return m
        }

        fun createScale(scale: Vector3): Matrix = createScale(scale.x, scale.y, scale.z)

        fun createScale(scale: Float): Matrix = createScale(scale, scale, scale)

        fun createRotationX(radians: Float): Matrix {
            val c = cos(radians); val s = sin(radians)
            val m = Matrix()
            m.m22 = c; m.m23 = s
            m.m32 = -s; m.m33 = c
            return m
        }

        fun createRotationY(radians: Float): Matrix {
            val c = cos(radians); val s = sin(radians)
            val m = Matrix()
            m.m11 = c; m.m13 = -s
            m.m31 = s; m.m33 = c
            return m
        }

        fun createRotationZ(radians: Float): Matrix {
            val c = cos(radians); val s = sin(radians)
            val m = Matrix()
            m.m11 = c; m.m12 = s
            m.m21 = -s; m.m22 = c
            return m
        }

        fun createOrthographic(width: Float, height: Float, zNearPlane: Float, zFarPlane: Float): Matrix {
            val m = Matrix()
            m.m11 = 2f / width
            m.m22 = 2f / height
            m.m33 = 1f / (zNearPlane - zFarPlane)
            m.m41 = -1f
            m.m42 = -1f
            m.m43 = zNearPlane / (zNearPlane - zFarPlane)
            return m
        }

        fun createOrthographicOffCenter(
            left: Float, right: Float,
            bottom: Float, top: Float,
            zNearPlane: Float, zFarPlane: Float
        ): Matrix {
            val m = Matrix()
            m.m11 = 2f / (right - left)
            m.m22 = 2f / (top - bottom)
            m.m33 = 1f / (zNearPlane - zFarPlane)
            m.m41 = (left + right) / (left - right)
            m.m42 = (top + bottom) / (bottom - top)
            m.m43 = zNearPlane / (zNearPlane - zFarPlane)
            return m
        }

        fun createPerspectiveFieldOfView(fieldOfView: Float, aspectRatio: Float, nearPlane: Float, farPlane: Float): Matrix {
            val yScale = 1f / tan(fieldOfView / 2f)
            val xScale = yScale / aspectRatio
            val m = Matrix()
            m.m11 = xScale
            m.m22 = yScale
            m.m33 = farPlane / (nearPlane - farPlane)
            m.m34 = -1f
            m.m43 = nearPlane * farPlane / (nearPlane - farPlane)
            m.m44 = 0f
            return m
        }

        fun createLookAt(cameraPosition: Vector3, cameraTarget: Vector3, cameraUpVector: Vector3): Matrix {
            val zAxis = Vector3.normalize(cameraPosition - cameraTarget)
            val xAxis = Vector3.normalize(Vector3.cross(cameraUpVector, zAxis))
            val yAxis = Vector3.cross(zAxis, xAxis)
            val m = Matrix()
            m.m11 = xAxis.x; m.m12 = yAxis.x; m.m13 = zAxis.x; m.m14 = 0f
            m.m21 = xAxis.y; m.m22 = yAxis.y; m.m23 = zAxis.y; m.m24 = 0f
            m.m31 = xAxis.z; m.m32 = yAxis.z; m.m33 = zAxis.z; m.m34 = 0f
            m.m41 = -Vector3.dot(xAxis, cameraPosition)
            m.m42 = -Vector3.dot(yAxis, cameraPosition)
            m.m43 = -Vector3.dot(zAxis, cameraPosition)
            m.m44 = 1f
            return m
        }

        fun createFromQuaternion(quaternion: Quaternion): Matrix {
            val xx = quaternion.x * quaternion.x * 2f
            val yy = quaternion.y * quaternion.y * 2f
            val zz = quaternion.z * quaternion.z * 2f
            val xy = quaternion.x * quaternion.y * 2f
            val xz = quaternion.x * quaternion.z * 2f
            val yz = quaternion.y * quaternion.z * 2f
            val wx = quaternion.w * quaternion.x * 2f
            val wy = quaternion.w * quaternion.y * 2f
            val wz = quaternion.w * quaternion.z * 2f
            val m = Matrix()
            m.m11 = 1f - (yy + zz); m.m12 = xy + wz;          m.m13 = xz - wy;          m.m14 = 0f
            m.m21 = xy - wz;          m.m22 = 1f - (xx + zz); m.m23 = yz + wx;          m.m24 = 0f
            m.m31 = xz + wy;          m.m32 = yz - wx;          m.m33 = 1f - (xx + yy); m.m34 = 0f
            m.m41 = 0f;               m.m42 = 0f;               m.m43 = 0f;               m.m44 = 1f
            return m
        }

        fun createWorld(position: Vector3, forward: Vector3, up: Vector3): Matrix {
            val zAxis = Vector3.normalize(-forward)
            val xAxis = Vector3.normalize(Vector3.cross(up, zAxis))
            val yAxis = Vector3.cross(zAxis, xAxis)
            val m = Matrix()
            m.m11 = xAxis.x; m.m12 = xAxis.y; m.m13 = xAxis.z; m.m14 = 0f
            m.m21 = yAxis.x; m.m22 = yAxis.y; m.m23 = yAxis.z; m.m24 = 0f
            m.m31 = zAxis.x; m.m32 = zAxis.y; m.m33 = zAxis.z; m.m34 = 0f
            m.m41 = position.x; m.m42 = position.y; m.m43 = position.z; m.m44 = 1f
            return m
        }

        fun transpose(matrix: Matrix): Matrix {
            val m = Matrix()
            m.m11 = matrix.m11; m.m12 = matrix.m21; m.m13 = matrix.m31; m.m14 = matrix.m41
            m.m21 = matrix.m12; m.m22 = matrix.m22; m.m23 = matrix.m32; m.m24 = matrix.m42
            m.m31 = matrix.m13; m.m32 = matrix.m23; m.m33 = matrix.m33; m.m34 = matrix.m43
            m.m41 = matrix.m14; m.m42 = matrix.m24; m.m43 = matrix.m34; m.m44 = matrix.m44
            return m
        }

        fun multiply(matrix1: Matrix, matrix2: Matrix): Matrix {
            val m = Matrix()
            m.m11 = matrix1.m11 * matrix2.m11 + matrix1.m12 * matrix2.m21 + matrix1.m13 * matrix2.m31 + matrix1.m14 * matrix2.m41
            m.m12 = matrix1.m11 * matrix2.m12 + matrix1.m12 * matrix2.m22 + matrix1.m13 * matrix2.m32 + matrix1.m14 * matrix2.m42
            m.m13 = matrix1.m11 * matrix2.m13 + matrix1.m12 * matrix2.m23 + matrix1.m13 * matrix2.m33 + matrix1.m14 * matrix2.m43
            m.m14 = matrix1.m11 * matrix2.m14 + matrix1.m12 * matrix2.m24 + matrix1.m13 * matrix2.m34 + matrix1.m14 * matrix2.m44
            m.m21 = matrix1.m21 * matrix2.m11 + matrix1.m22 * matrix2.m21 + matrix1.m23 * matrix2.m31 + matrix1.m24 * matrix2.m41
            m.m22 = matrix1.m21 * matrix2.m12 + matrix1.m22 * matrix2.m22 + matrix1.m23 * matrix2.m32 + matrix1.m24 * matrix2.m42
            m.m23 = matrix1.m21 * matrix2.m13 + matrix1.m22 * matrix2.m23 + matrix1.m23 * matrix2.m33 + matrix1.m24 * matrix2.m43
            m.m24 = matrix1.m21 * matrix2.m14 + matrix1.m22 * matrix2.m24 + matrix1.m23 * matrix2.m34 + matrix1.m24 * matrix2.m44
            m.m31 = matrix1.m31 * matrix2.m11 + matrix1.m32 * matrix2.m21 + matrix1.m33 * matrix2.m31 + matrix1.m34 * matrix2.m41
            m.m32 = matrix1.m31 * matrix2.m12 + matrix1.m32 * matrix2.m22 + matrix1.m33 * matrix2.m32 + matrix1.m34 * matrix2.m42
            m.m33 = matrix1.m31 * matrix2.m13 + matrix1.m32 * matrix2.m23 + matrix1.m33 * matrix2.m33 + matrix1.m34 * matrix2.m43
            m.m34 = matrix1.m31 * matrix2.m14 + matrix1.m32 * matrix2.m24 + matrix1.m33 * matrix2.m34 + matrix1.m34 * matrix2.m44
            m.m41 = matrix1.m41 * matrix2.m11 + matrix1.m42 * matrix2.m21 + matrix1.m43 * matrix2.m31 + matrix1.m44 * matrix2.m41
            m.m42 = matrix1.m41 * matrix2.m12 + matrix1.m42 * matrix2.m22 + matrix1.m43 * matrix2.m32 + matrix1.m44 * matrix2.m42
            m.m43 = matrix1.m41 * matrix2.m13 + matrix1.m42 * matrix2.m23 + matrix1.m43 * matrix2.m33 + matrix1.m44 * matrix2.m43
            m.m44 = matrix1.m41 * matrix2.m14 + matrix1.m42 * matrix2.m24 + matrix1.m43 * matrix2.m34 + matrix1.m44 * matrix2.m44
            return m
        }

        fun invert(matrix: Matrix): Matrix {
            val det = matrix.determinant()
            if (det == 0f) return Matrix()
            val invDet = 1f / det
            val m = Matrix()
            m.m11 = (matrix.m22 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                     matrix.m23 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) +
                     matrix.m24 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42)) * invDet
            m.m12 = -(matrix.m12 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                      matrix.m13 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) +
                      matrix.m14 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42)) * invDet
            m.m13 = (matrix.m12 * (matrix.m23 * matrix.m44 - matrix.m24 * matrix.m43) -
                     matrix.m13 * (matrix.m22 * matrix.m44 - matrix.m24 * matrix.m42) +
                     matrix.m14 * (matrix.m22 * matrix.m43 - matrix.m23 * matrix.m42)) * invDet
            m.m14 = -(matrix.m12 * (matrix.m23 * matrix.m34 - matrix.m24 * matrix.m33) -
                      matrix.m13 * (matrix.m22 * matrix.m34 - matrix.m24 * matrix.m32) +
                      matrix.m14 * (matrix.m22 * matrix.m33 - matrix.m23 * matrix.m32)) * invDet
            m.m21 = -(matrix.m21 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                      matrix.m23 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                      matrix.m24 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41)) * invDet
            m.m22 = (matrix.m11 * (matrix.m33 * matrix.m44 - matrix.m34 * matrix.m43) -
                     matrix.m13 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                     matrix.m14 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41)) * invDet
            m.m23 = -(matrix.m11 * (matrix.m23 * matrix.m44 - matrix.m24 * matrix.m43) -
                      matrix.m13 * (matrix.m21 * matrix.m44 - matrix.m24 * matrix.m41) +
                      matrix.m14 * (matrix.m21 * matrix.m43 - matrix.m23 * matrix.m41)) * invDet
            m.m24 = (matrix.m11 * (matrix.m23 * matrix.m34 - matrix.m24 * matrix.m33) -
                     matrix.m13 * (matrix.m21 * matrix.m34 - matrix.m24 * matrix.m31) +
                     matrix.m14 * (matrix.m21 * matrix.m33 - matrix.m23 * matrix.m31)) * invDet
            m.m31 = (matrix.m21 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) -
                     matrix.m22 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                     matrix.m24 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m32 = -(matrix.m11 * (matrix.m32 * matrix.m44 - matrix.m34 * matrix.m42) -
                      matrix.m12 * (matrix.m31 * matrix.m44 - matrix.m34 * matrix.m41) +
                      matrix.m14 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m33 = (matrix.m11 * (matrix.m22 * matrix.m44 - matrix.m24 * matrix.m42) -
                     matrix.m12 * (matrix.m21 * matrix.m44 - matrix.m24 * matrix.m41) +
                     matrix.m14 * (matrix.m21 * matrix.m42 - matrix.m22 * matrix.m41)) * invDet
            m.m34 = -(matrix.m11 * (matrix.m22 * matrix.m34 - matrix.m24 * matrix.m32) -
                      matrix.m12 * (matrix.m21 * matrix.m34 - matrix.m24 * matrix.m31) +
                      matrix.m14 * (matrix.m21 * matrix.m32 - matrix.m22 * matrix.m31)) * invDet
            m.m41 = -(matrix.m21 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42) -
                      matrix.m22 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41) +
                      matrix.m23 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m42 = (matrix.m11 * (matrix.m32 * matrix.m43 - matrix.m33 * matrix.m42) -
                     matrix.m12 * (matrix.m31 * matrix.m43 - matrix.m33 * matrix.m41) +
                     matrix.m13 * (matrix.m31 * matrix.m42 - matrix.m32 * matrix.m41)) * invDet
            m.m43 = -(matrix.m11 * (matrix.m22 * matrix.m43 - matrix.m23 * matrix.m42) -
                      matrix.m12 * (matrix.m21 * matrix.m43 - matrix.m23 * matrix.m41) +
                      matrix.m13 * (matrix.m21 * matrix.m42 - matrix.m22 * matrix.m41)) * invDet
            m.m44 = (matrix.m11 * (matrix.m22 * matrix.m33 - matrix.m23 * matrix.m32) -
                     matrix.m12 * (matrix.m21 * matrix.m33 - matrix.m23 * matrix.m31) +
                     matrix.m13 * (matrix.m21 * matrix.m32 - matrix.m22 * matrix.m31)) * invDet
            return m
        }
    }

    fun determinant(): Float {
        return m11 * (m22 * (m33 * m44 - m34 * m43) - m23 * (m32 * m44 - m34 * m42) + m24 * (m32 * m43 - m33 * m42))
             - m12 * (m21 * (m33 * m44 - m34 * m43) - m23 * (m31 * m44 - m34 * m41) + m24 * (m31 * m43 - m33 * m41))
             + m13 * (m21 * (m32 * m44 - m34 * m42) - m22 * (m31 * m44 - m34 * m41) + m24 * (m31 * m42 - m32 * m41))
             - m14 * (m21 * (m32 * m43 - m33 * m42) - m22 * (m31 * m43 - m33 * m41) + m23 * (m31 * m42 - m32 * m41))
    }

    fun toArray(): FloatArray = floatArrayOf(
        m11, m12, m13, m14,
        m21, m22, m23, m24,
        m31, m32, m33, m34,
        m41, m42, m43, m44
    )

    fun translation(): Vector3 = Vector3(m41, m42, m43)

    fun scale(): Vector3 {
        val sx = Vector3(m11, m12, m13).length()
        val sy = Vector3(m21, m22, m23).length()
        val sz = Vector3(m31, m32, m33).length()
        return Vector3(sx, sy, sz)
    }

    fun forward(): Vector3 = Vector3(-m31, -m32, -m33)
    fun backward(): Vector3 = Vector3(m31, m32, m33)
    fun up(): Vector3 = Vector3(m21, m22, m23)
    fun down(): Vector3 = Vector3(-m21, -m22, -m23)
    fun right(): Vector3 = Vector3(m11, m12, m13)
    fun left(): Vector3 = Vector3(-m11, -m12, -m13)

    fun decomposeScale(): Vector3 {
        val sx = Vector3(m11, m12, m13).length()
        val sy = Vector3(m21, m22, m23).length()
        val sz = Vector3(m31, m32, m33).length()
        return Vector3(sx, sy, sz)
    }

    operator fun times(other: Matrix): Matrix = multiply(this, other)

    operator fun times(vector: Vector3): Vector3 = Vector3.transform(vector, this)

    override fun toString(): String {
        return "[$m11,$m12,$m13,$m14]\n[$m21,$m22,$m23,$m24]\n[$m31,$m32,$m33,$m34]\n[$m41,$m42,$m43,$m44]"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Matrix) return false
        return m11 == other.m11 && m12 == other.m12 && m13 == other.m13 && m14 == other.m14 &&
               m21 == other.m21 && m22 == other.m22 && m23 == other.m23 && m24 == other.m24 &&
               m31 == other.m31 && m32 == other.m32 && m33 == other.m33 && m34 == other.m34 &&
               m41 == other.m41 && m42 == other.m42 && m43 == other.m43 && m44 == other.m44
    }

    override fun hashCode(): Int {
        var hash = m11.hashCode()
        hash = 31 * hash + m22.hashCode()
        hash = 31 * hash + m33.hashCode()
        hash = 31 * hash + m44.hashCode()
        return hash
    }
}
