package ru.kayron.dew.graphics

import android.opengl.GLES30
import android.opengl.GLES32
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector4

class GraphicsDevice {
    val presentationParameters = PresentationParameters()
    var viewport: Viewport = Viewport()
        private set

    var blendState: BlendState = BlendState.Opaque
    var rasterizerState: RasterizerState = RasterizerState.CullCounterClockwise
    var depthStencilState: DepthStencilState = DepthStencilState.Default
    var samplerStates: Array<SamplerState?> = arrayOfNulls(16)
    var textures: Array<Texture?> = arrayOfNulls(16)

    var vertexBuffer: VertexBuffer? = null
    var indexBuffer: IndexBuffer? = null

    private var defaultFramebuffer: Int = 0
    private var currentRenderTarget: RenderTarget2D? = null

    val displayMode: DisplayMode
        get() = DisplayMode(viewport.width, viewport.height)

    fun initialize() {
        val fb = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, fb, 0)
        defaultFramebuffer = fb[0]

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        val extVersion = GLES30.glGetString(GLES30.GL_EXTENSIONS) ?: ""
        if (extVersion.contains("GL_OES_EGL_image_external")) {
            android.util.Log.i("GraphicsDevice", "EGL image external supported")
        }
        android.util.Log.i("GraphicsDevice", "OpenGL ES ${GLES30.glGetString(GLES30.GL_VERSION)}")
        android.util.Log.i("GraphicsDevice", "GL Renderer: ${GLES30.glGetString(GLES30.GL_RENDERER)}")
    }

    fun clear(color: Color) {
        GLES30.glClearColor(color.rf, color.gf, color.bf, color.af)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_STENCIL_BUFFER_BIT)
    }

    fun clear(options: ClearOptions, color: Color, depth: Float = 1f, stencil: Int = 0) {
        var mask = 0
        if (options == ClearOptions.Target || options.value and 1 != 0) {
            GLES30.glClearColor(color.rf, color.gf, color.bf, color.af)
            mask = mask or GLES30.GL_COLOR_BUFFER_BIT
        }
        if (options == ClearOptions.DepthBuffer || options.value and 2 != 0) {
            GLES30.glClearDepthf(depth)
            mask = mask or GLES30.GL_DEPTH_BUFFER_BIT
        }
        if (options == ClearOptions.Stencil || options.value and 4 != 0) {
            GLES30.glClearStencil(stencil)
            mask = mask or GLES30.GL_STENCIL_BUFFER_BIT
        }
        GLES30.glClear(mask)
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        viewport = Viewport(x, y, width, height)
        GLES30.glViewport(x, y, width, height)
        GLES30.glScissor(x, y, width, height)
    }

    @JvmName("applyViewport")
    fun setViewport(vp: Viewport) {
        setViewport(vp.x, vp.y, vp.width, vp.height)
    }

    fun setRenderTarget(renderTarget: RenderTarget2D?) {
        if (renderTarget == null) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, defaultFramebuffer)
            currentRenderTarget = null
            setViewport(0, 0, presentationParameters.backBufferWidth, presentationParameters.backBufferHeight)
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, renderTarget.glFramebuffer)
            currentRenderTarget = renderTarget
            setViewport(0, 0, renderTarget.width, renderTarget.height)
        }
    }

    fun getRenderTarget(): RenderTarget2D? = currentRenderTarget

    @JvmName("bindVertexBuffer")
    fun setVertexBuffer(buffer: VertexBuffer) {
        vertexBuffer = buffer
        buffer.bind()
    }

    @JvmName("bindIndexBuffer")
    fun setIndexBuffer(buffer: IndexBuffer) {
        indexBuffer = buffer
        buffer.bind()
    }

    fun drawPrimitives(primitiveType: PrimitiveType, startVertex: Int, vertexCount: Int) {
        vertexBuffer?.bind()
        vertexBuffer?.declaration?.setup(0)
        GLES30.glDrawArrays(primitiveType.glType, startVertex, vertexCount)
    }

    fun drawIndexedPrimitives(primitiveType: PrimitiveType, baseVertex: Int, startIndex: Int, primitiveCount: Int) {
        vertexBuffer?.bind()
        indexBuffer?.bind()
        vertexBuffer?.declaration?.setup(0)
        val indexCount = when (primitiveType) {
            PrimitiveType.TriangleList -> primitiveCount * 3
            PrimitiveType.TriangleStrip -> primitiveCount + 2
            PrimitiveType.LineList -> primitiveCount * 2
            PrimitiveType.LineStrip -> primitiveCount + 1
            PrimitiveType.PointList -> primitiveCount
        }
        GLES30.glDrawElements(primitiveType.glType, indexCount, GLES30.GL_UNSIGNED_SHORT, startIndex * 2)
    }

    fun setScissorRect(rect: Rectangle) {
        GLES30.glScissor(rect.x, rect.y, rect.width, rect.height)
    }

    fun present() {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            android.util.Log.w("GraphicsDevice", "GL error after present: $error")
        }
        GLES30.glFinish()
    }

    fun dispose() {
        vertexBuffer?.dispose()
        indexBuffer?.dispose()
    }
}
