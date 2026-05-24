package ru.kayron.dew.graphics

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VertexBuffer(
    val declaration: VertexDeclaration,
    val vertexCount: Int,
    val usage: BufferUsage = BufferUsage.None
) {
    var glBuffer: Int = 0
        private set
    var isDynamic: Boolean = usage == BufferUsage.WriteOnly

    enum class BufferUsage {
        None,
        WriteOnly,
    }

    init {
        val bufs = IntArray(1)
        GLES30.glGenBuffers(1, bufs, 0)
        glBuffer = bufs[0]
    }

    fun setData(vertices: FloatArray) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glBuffer)
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(vertices)
        buffer.position(0)
        val usageGl = if (isDynamic) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, usageGl)
    }

    fun setData(buffer: FloatBuffer) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glBuffer)
        buffer.position(0)
        val usageGl = if (isDynamic) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, usageGl)
    }

    fun bind() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glBuffer)
    }

    fun dispose() {
        if (glBuffer != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(glBuffer), 0)
            glBuffer = 0
        }
    }
}
