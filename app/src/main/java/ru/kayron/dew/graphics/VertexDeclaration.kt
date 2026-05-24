package ru.kayron.dew.graphics

import android.opengl.GLES30

class VertexDeclaration(vararg elements: VertexElement) {
    val elements: List<VertexElement> = elements.toList()
    val vertexStride: Int = elements.sumOf { it.size }
    var glVao: Int = 0

    fun setup(program: Int) {
        if (glVao == 0) {
            val vaos = IntArray(1)
            GLES30.glGenVertexArrays(1, vaos, 0)
            glVao = vaos[0]
        }
        GLES30.glBindVertexArray(glVao)
        var offset = 0
        for (element in elements) {
            val location = GLES30.glGetAttribLocation(program, element.name)
            if (location >= 0) {
                GLES30.glEnableVertexAttribArray(location)
                GLES30.glVertexAttribPointer(
                    location, element.elementCount,
                    element.type, false, vertexStride, offset
                )
            }
            offset += element.size
        }
    }

    fun dispose() {
        if (glVao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(glVao), 0)
            glVao = 0
        }
    }
}

data class VertexElement(
    val name: String,
    val elementCount: Int,
    val type: Int = GLES30.GL_FLOAT,
    val size: Int = elementCount * when (type) {
        GLES30.GL_FLOAT -> 4
        GLES30.GL_UNSIGNED_BYTE -> 1
        GLES30.GL_UNSIGNED_SHORT -> 2
        else -> 4
    }
)
