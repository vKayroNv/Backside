package ru.kayron.dew.graphics

enum class PrimitiveType(val glType: Int) {
    TriangleList(android.opengl.GLES30.GL_TRIANGLES),
    TriangleStrip(android.opengl.GLES30.GL_TRIANGLE_STRIP),
    LineList(android.opengl.GLES30.GL_LINES),
    LineStrip(android.opengl.GLES30.GL_LINE_STRIP),
    PointList(android.opengl.GLES30.GL_POINTS),
}
