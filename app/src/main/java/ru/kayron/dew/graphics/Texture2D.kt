package ru.kayron.dew.graphics

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class Texture2D(
    val width: Int,
    val height: Int,
    mipMap: Boolean = false,
    format: SurfaceFormat = SurfaceFormat.Color
) : Texture() {
    var mipMap: Boolean = mipMap
        private set

    init {
        this.format = format
        this.glTexture = generateTexture()
        bind()
        val internalFormat = when (format) {
            SurfaceFormat.Color -> GLES30.GL_RGBA
            SurfaceFormat.Bgr565 -> GLES30.GL_RGB
            SurfaceFormat.Bgra5551 -> GLES30.GL_RGB5_A1
            SurfaceFormat.Bgra4444 -> GLES30.GL_RGBA4
            SurfaceFormat.Alpha8 -> GLES30.GL_ALPHA
            SurfaceFormat.Single -> GLES30.GL_LUMINANCE
            else -> GLES30.GL_RGBA
        }
        val border = 0
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, internalFormat,
            width, height, border, internalFormat,
            GLES30.GL_UNSIGNED_BYTE, null
        )
        if (mipMap) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            levelCount = (32 - Integer.numberOfLeadingZeros(maxOf(width, height))).coerceAtLeast(1)
        }
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    }

    fun setData(color: IntArray) {
        require(color.size >= width * height) {
            "Expected at least ${width * height} pixels, got ${color.size}"
        }
        bind()
        val buffer = ByteBuffer.allocateDirect(color.size * 4)
            .order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(color)
        buffer.position(0)
        GLES30.glTexSubImage2D(
            GLES30.GL_TEXTURE_2D, 0, 0, 0, width, height,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer
        )
        if (mipMap) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        }
    }

    fun setData(color: IntArray, startX: Int, startY: Int, width: Int, height: Int) {
        require(color.size >= width * height) {
            "Expected at least ${width * height} pixels, got ${color.size}"
        }
        bind()
        val buffer = ByteBuffer.allocateDirect(color.size * 4)
            .order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(color)
        buffer.position(0)
        GLES30.glTexSubImage2D(
            GLES30.GL_TEXTURE_2D, 0, startX, startY, width, height,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer
        )
    }

    fun setData(bitmap: Bitmap) {
        bind()
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        if (mipMap) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        }
    }

    fun getData(): IntArray {
        val pixels = IntArray(width * height)
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        buffer.asIntBuffer().get(pixels)
        return pixels
    }

    override fun bind() {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture)
    }

    override fun dispose() {
        deleteTexture()
    }

    companion object {
        private var assetManager: AssetManager? = null

        internal fun setAssetManager(am: AssetManager) {
            assetManager = am
        }

        fun fromBitmap(bitmap: Bitmap, mipMap: Boolean = false, linear: Boolean = true): Texture2D {
            val tex = Texture2D(bitmap.width, bitmap.height, mipMap)
            tex.setData(bitmap)
            tex.bind()
            val filter = if (linear) GLES30.GL_LINEAR else GLES30.GL_NEAREST
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, filter)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, filter)
            return tex
        }

        fun fromAsset(path: String, mipMap: Boolean = false): Texture2D? {
            val am = assetManager ?: return null
            val candidates = if (path.startsWith("Content/")) {
                listOf(path)
            } else {
                listOf(path, "Content/$path")
            }
            for (candidate in candidates) {
                try {
                    am.open(candidate).use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream) ?: return null
                        return fromBitmap(bitmap, mipMap)
                    }
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}
