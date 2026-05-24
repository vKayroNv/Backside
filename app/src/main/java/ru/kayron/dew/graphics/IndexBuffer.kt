package ru.kayron.dew.graphics

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

class IndexBuffer(
    val indexCount: Int,
    val usage: VertexBuffer.BufferUsage = VertexBuffer.BufferUsage.None
) {
    var glBuffer: Int = 0
        private set
    var isDynamic: Boolean = usage == VertexBuffer.BufferUsage.WriteOnly

    init {
        val bufs = IntArray(1)
        GLES30.glGenBuffers(1, bufs, 0)
        glBuffer = bufs[0]
    }

    fun setData(indices: ShortArray) {
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, glBuffer)
        val buffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        buffer.put(indices)
        buffer.position(0)
        val usageGl = if (isDynamic) GLES30.GL_DYNAMIC_DRAW else GLES30.GL_STATIC_DRAW
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, buffer, usageGl)
    }

    fun bind() {
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, glBuffer)
    }

    fun dispose() {
        if (glBuffer != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(glBuffer), 0)
            glBuffer = 0
        }
    }
}
