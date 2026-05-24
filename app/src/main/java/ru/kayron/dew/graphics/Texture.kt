package ru.kayron.dew.graphics

abstract class Texture {
    var glTexture: Int = 0
        protected set
    var levelCount: Int = 1
        protected set
    var format: SurfaceFormat = SurfaceFormat.Color
        protected set

    abstract fun bind()
    abstract fun dispose()

    protected fun generateTexture(): Int {
        val textures = IntArray(1)
        android.opengl.GLES30.glGenTextures(1, textures, 0)
        return textures[0]
    }

    protected fun deleteTexture() {
        if (glTexture != 0) {
            android.opengl.GLES30.glDeleteTextures(1, intArrayOf(glTexture), 0)
            glTexture = 0
        }
    }
}
