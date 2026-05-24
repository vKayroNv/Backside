package ru.kayron.dew.graphics

import android.opengl.GLES30
import ru.kayron.dew.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpriteBatch(private val graphicsDevice: GraphicsDevice) {

    private inner class SpriteInfo(
        val texture: Texture2D,
        val sourceRect: Rectangle?,
        val destinationRect: Rectangle,
        val color: Color,
        val rotation: Float,
        val origin: Vector2,
        val effects: SpriteEffects,
        val depth: Float
    )

    private val sprites = mutableListOf<SpriteInfo>()
    private var sortMode: SpriteSortMode = SpriteSortMode.Deferred
    private var effect: Effect? = null
    private var isStarted = false

    private val maxBatchSize = 2048
    private var currentBatch = 0
    private val vertexData = FloatArray(maxBatchSize * 6 * 9)
    private val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private val vbo = IntArray(1)

    private val spriteEffect: Effect

    init {
        val vertexSrc = """
            #version 300 es
            in vec3 aPosition;
            in vec2 aTexCoord;
            in vec4 aColor;
            out vec2 vTexCoord;
            out vec4 vColor;
            uniform mat4 uProjection;
            void main() {
                gl_Position = uProjection * vec4(aPosition, 1.0);
                vTexCoord = aTexCoord;
                vColor = aColor;
            }
        """.trimIndent()
        val fragmentSrc = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            in vec4 vColor;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            void main() {
                fragColor = texture(uTexture, vTexCoord) * vColor;
            }
        """.trimIndent()
        spriteEffect = Effect()
        spriteEffect.linkProgram(vertexSrc, fragmentSrc)
        GLES30.glGenBuffers(1, vbo, 0)
    }

    fun begin(
        sortMode: SpriteSortMode = SpriteSortMode.Deferred,
        blendState: BlendState? = BlendState.AlphaBlend,
        samplerState: SamplerState? = SamplerState.LinearClamp,
        depthStencilState: DepthStencilState? = null,
        rasterizerState: RasterizerState? = null,
        effect: Effect? = null
    ) {
        this.sortMode = sortMode
        this.effect = effect
        sprites.clear()
        currentBatch = 0
        isStarted = true

        blendState?.apply()
        samplerState?.apply(0)
        depthStencilState?.apply()
        rasterizerState?.apply()
    }

    fun draw(
        texture: Texture2D,
        position: Vector2,
        sourceRectangle: Rectangle? = null,
        color: Color = Color.White,
        rotation: Float = 0f,
        origin: Vector2 = Vector2.Zero,
        scale: Vector2 = Vector2.One,
        effects: SpriteEffects = SpriteEffects.None,
        layerDepth: Float = 0f
    ) {
        val width = sourceRectangle?.width ?: texture.width
        val height = sourceRectangle?.height ?: texture.height
        val dest = Rectangle(
            (position.x - origin.x * scale.x).toInt(),
            (position.y - origin.y * scale.y).toInt(),
            (width * scale.x).toInt(),
            (height * scale.y).toInt()
        )
        val scaledOrigin = Vector2(origin.x * scale.x, origin.y * scale.y)
        sprites.add(SpriteInfo(texture, sourceRectangle, dest, color, rotation, scaledOrigin, effects, layerDepth))
        afterDraw()
    }

    fun draw(
        texture: Texture2D,
        destinationRectangle: Rectangle,
        sourceRectangle: Rectangle? = null,
        color: Color = Color.White,
        rotation: Float = 0f,
        origin: Vector2 = Vector2.Zero,
        effects: SpriteEffects = SpriteEffects.None,
        layerDepth: Float = 0f
    ) {
        sprites.add(SpriteInfo(texture, sourceRectangle, destinationRectangle, color, rotation, origin, effects, layerDepth))
        afterDraw()
    }

    fun draw(
        texture: Texture2D,
        position: Vector2,
        color: Color = Color.White
    ) {
        draw(texture, position, null, color)
    }

    fun draw(
        texture: Texture2D,
        rectangle: Rectangle,
        color: Color = Color.White
    ) {
        draw(texture, rectangle, null, color)
    }

    fun drawString(spriteFont: SpriteFont, text: String, position: Vector2, color: Color) {
        spriteFont.draw(this, text, position, color)
    }

    fun end() {
        if (!isStarted) return
        flushAll()
        isStarted = false
    }

    private fun flushBatch() {
        if (currentBatch >= sprites.size) return
        val batchSize = minOf(maxBatchSize, sprites.size - currentBatch)
        if (batchSize == 0) return

        val rawSlice = sprites.subList(currentBatch, currentBatch + batchSize)
        val sortedSlice = when (sortMode) {
            SpriteSortMode.BackToFront -> rawSlice.sortedByDescending { it.depth }
            SpriteSortMode.FrontToBack -> rawSlice.sortedBy { it.depth }
            SpriteSortMode.Texture -> rawSlice.sortedBy { it.texture.glTexture }
            else -> rawSlice
        }

        var start = 0
        while (start < sortedSlice.size) {
            val texture = sortedSlice[start].texture
            var end = start + 1
            while (end < sortedSlice.size && sortedSlice[end].texture === texture) {
                end++
            }
            flushRun(texture, sortedSlice, start, end)
            start = end
        }
        currentBatch += batchSize
    }

    private fun afterDraw() {
        if (sortMode == SpriteSortMode.Immediate) {
            flushBatch()
            sprites.clear()
            currentBatch = 0
        } else if (sprites.size - currentBatch >= maxBatchSize) {
            flushBatch()
        }
    }

    private fun flushRun(texture: Texture2D, sortedSlice: List<SpriteInfo>, start: Int, end: Int) {
        var vi = 0

        for (i in start until end) {
            val sprite = sortedSlice[i]
            val x = sprite.destinationRect.x.toFloat()
            val y = sprite.destinationRect.y.toFloat()
            val w = sprite.destinationRect.width.toFloat()
            val h = sprite.destinationRect.height.toFloat()
            val originX = sprite.origin.x
            val originY = sprite.origin.y
            val anchorX = x + originX
            val anchorY = y + originY

            val cosR = kotlin.math.cos(sprite.rotation)
            val sinR = kotlin.math.sin(sprite.rotation)

            val fx = if (sprite.effects.value and SpriteEffects.FlipHorizontally.value != 0) -1f else 1f
            val fy = if (sprite.effects.value and SpriteEffects.FlipVertically.value != 0) -1f else 1f

            val u0 = sprite.sourceRect?.let { it.x.toFloat() / sprite.texture.width } ?: 0f
            val v0 = sprite.sourceRect?.let { it.y.toFloat() / sprite.texture.height } ?: 0f
            val u1 = sprite.sourceRect?.let { (it.x + it.width).toFloat() / sprite.texture.width } ?: 1f
            val v1 = sprite.sourceRect?.let { (it.y + it.height).toFloat() / sprite.texture.height } ?: 1f

            val r = sprite.color.rf
            val g = sprite.color.gf
            val b = sprite.color.bf
            val a = sprite.color.af

            fun tx(lx: Float, ly: Float): Float {
                val dx = lx * fx
                val dy = ly * fy
                return dx * cosR - dy * sinR + anchorX
            }

            fun ty(lx: Float, ly: Float): Float {
                val dx = lx * fx
                val dy = ly * fy
                return dx * sinR + dy * cosR + anchorY
            }

            fun emit(rx: Float, ry: Float, u: Float, v: Float) {
                vertexData[vi++] = rx
                vertexData[vi++] = ry
                vertexData[vi++] = 0f
                vertexData[vi++] = u
                vertexData[vi++] = v
                vertexData[vi++] = r
                vertexData[vi++] = g
                vertexData[vi++] = b
                vertexData[vi++] = a
            }

            val x0 = -originX
            val y0 = -originY
            val x1 = w - originX
            val y1 = h - originY

            emit(tx(x0, y0), ty(x0, y0), u0, v0) // tl
            emit(tx(x1, y0), ty(x1, y0), u1, v0) // tr
            emit(tx(x1, y1), ty(x1, y1), u1, v1) // br

            emit(tx(x0, y0), ty(x0, y0), u0, v0) // tl
            emit(tx(x1, y1), ty(x1, y1), u1, v1) // br
            emit(tx(x0, y1), ty(x0, y1), u0, v1) // bl
        }

        applySpriteEffect(texture, vi, end - start)
    }

    private fun applySpriteEffect(texture: Texture2D, vertexFloatCount: Int, spriteCount: Int) {
        val eff = effect ?: spriteEffect
        eff.apply()

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        texture.bind()
        eff.setUniform("uTexture", 0)

        val w = graphicsDevice.viewport.width.toFloat()
        val h = graphicsDevice.viewport.height.toFloat()
        val projection = Matrix.createOrthographicOffCenter(0f, w, h, 0f, 0f, 1f)
        eff.setUniformMatrix("uProjection", projection)

        vertexBuffer.clear()
        vertexBuffer.put(vertexData, 0, vertexFloatCount)
        vertexBuffer.position(0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexFloatCount * 4, vertexBuffer, GLES30.GL_DYNAMIC_DRAW)

        val posLoc = eff.uniforms["aPosition"] ?: GLES30.glGetAttribLocation(eff.program, "aPosition")
        val texLoc = eff.uniforms["aTexCoord"] ?: GLES30.glGetAttribLocation(eff.program, "aTexCoord")
        val colLoc = eff.uniforms["aColor"] ?: GLES30.glGetAttribLocation(eff.program, "aColor")

        val stride = 9 * 4

        if (posLoc >= 0) {
            GLES30.glEnableVertexAttribArray(posLoc)
            GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, stride, 0)
        }

        if (texLoc >= 0) {
            GLES30.glEnableVertexAttribArray(texLoc)
            GLES30.glVertexAttribPointer(texLoc, 2, GLES30.GL_FLOAT, false, stride, 3 * 4)
        }

        if (colLoc >= 0) {
            GLES30.glEnableVertexAttribArray(colLoc)
            GLES30.glVertexAttribPointer(colLoc, 4, GLES30.GL_FLOAT, false, stride, 5 * 4)
        }

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, spriteCount * 6)

        if (posLoc >= 0) GLES30.glDisableVertexAttribArray(posLoc)
        if (texLoc >= 0) GLES30.glDisableVertexAttribArray(texLoc)
        if (colLoc >= 0) GLES30.glDisableVertexAttribArray(colLoc)

        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            android.util.Log.e("SpriteBatch", "GL error: $error")
        }
    }

    private fun flushAll() {
        while (currentBatch < sprites.size) {
            flushBatch()
        }
    }

    fun dispose() {
        if (vbo[0] != 0) {
            GLES30.glDeleteBuffers(1, vbo, 0)
            vbo[0] = 0
        }
        spriteEffect.dispose()
    }
}
