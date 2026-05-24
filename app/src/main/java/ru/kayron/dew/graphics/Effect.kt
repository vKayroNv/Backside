package ru.kayron.dew.graphics

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class Effect {
    var program: Int = 0
        protected set
    val uniforms = mutableMapOf<String, Int>()
    protected val attributes = mutableMapOf<String, Int>()

    protected fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            android.util.Log.e("Effect", "Failed to create shader")
            return 0
        }
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            android.util.Log.e("Effect", "Shader compile error: $log")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun linkProgram(vertexSource: String, fragmentSource: String) {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) {
            program = 0
            return
        }
        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            android.util.Log.e("Effect", "Program link error: $log")
            GLES30.glDeleteProgram(program)
            program = 0
        }
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        if (program != 0) {
            cacheUniforms()
            cacheAttributes()
        }
    }

    private fun cacheUniforms() {
        val count = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_ACTIVE_UNIFORMS, count, 0)
        for (i in 0 until count[0]) {
            val length = IntArray(1)
            val size = IntArray(1)
            val type = IntArray(1)
            val nameBytes = ByteArray(128)
            GLES30.glGetActiveUniform(program, i, 128, length, 0, size, 0, type, 0, nameBytes, 0)
            val name = if (length[0] > 0) String(nameBytes, 0, length[0]) else null
            if (name != null) {
                uniforms[name] = GLES30.glGetUniformLocation(program, name)
            }
        }
    }

    private fun cacheAttributes() {
        val count = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_ACTIVE_ATTRIBUTES, count, 0)
        for (i in 0 until count[0]) {
            val length = IntArray(1)
            val size = IntArray(1)
            val type = IntArray(1)
            val nameBytes = ByteArray(128)
            GLES30.glGetActiveAttrib(program, i, 128, length, 0, size, 0, type, 0, nameBytes, 0)
            val name = if (length[0] > 0) String(nameBytes, 0, length[0]) else null
            if (name != null) {
                attributes[name] = GLES30.glGetAttribLocation(program, name)
            }
        }
    }

    open fun apply() {
        if (program == 0) {
            android.util.Log.w("Effect", "apply() called with program=0")
            return
        }
        GLES30.glUseProgram(program)
    }

    fun setUniform(name: String, value: Float) {
        uniforms[name]?.let { GLES30.glUniform1f(it, value) }
    }

    fun setUniform(name: String, v0: Float, v1: Float) {
        uniforms[name]?.let { GLES30.glUniform2f(it, v0, v1) }
    }

    fun setUniform(name: String, v0: Float, v1: Float, v2: Float) {
        uniforms[name]?.let { GLES30.glUniform3f(it, v0, v1, v2) }
    }

    fun setUniform(name: String, v0: Float, v1: Float, v2: Float, v3: Float) {
        uniforms[name]?.let { GLES30.glUniform4f(it, v0, v1, v2, v3) }
    }

    fun setUniform(name: String, value: Int) {
        uniforms[name]?.let { GLES30.glUniform1i(it, value) }
    }

    fun setUniform(name: String, value: Boolean) {
        uniforms[name]?.let { GLES30.glUniform1i(it, if (value) 1 else 0) }
    }

    fun setUniformMatrix(name: String, matrix: ru.kayron.dew.math.Matrix) {
        uniforms[name]?.let { loc ->
            val buffer = ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(matrix.toArray())
            buffer.position(0)
            GLES30.glUniformMatrix4fv(loc, 1, false, buffer)
        }
    }

    fun setUniformMatrixArray(name: String, matrices: FloatArray) {
        uniforms[name]?.let { loc ->
            val buffer = ByteBuffer.allocateDirect(matrices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(matrices)
            buffer.position(0)
            GLES30.glUniformMatrix4fv(loc, matrices.size / 16, false, buffer)
        }
    }

    fun dispose() {
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        uniforms.clear()
        attributes.clear()
    }
}
