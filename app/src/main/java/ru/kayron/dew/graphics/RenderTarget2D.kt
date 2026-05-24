package ru.kayron.dew.graphics

import android.opengl.GLES30

class RenderTarget2D(
    width: Int,
    height: Int,
    mipMap: Boolean = false,
    format: SurfaceFormat = SurfaceFormat.Color,
    val depthStencilFormat: PresentationParameters.DepthFormat = PresentationParameters.DepthFormat.Depth24Stencil8,
    val multiSampleCount: Int = 0,
    val renderTargetUsage: PresentationParameters.RenderTargetUsage = PresentationParameters.RenderTargetUsage.DiscardContents
) : Texture2D(width, height, mipMap, format) {

    var glFramebuffer: Int = 0
        private set
    var glDepthStencil: Int = 0
        private set

    init {
        createFramebuffer()
    }

    private fun createFramebuffer() {
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        glFramebuffer = fbos[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, glFramebuffer)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, glTexture, 0
        )
        if (depthStencilFormat != PresentationParameters.DepthFormat.None) {
            val rbos = IntArray(1)
            GLES30.glGenRenderbuffers(1, rbos, 0)
            glDepthStencil = rbos[0]
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, glDepthStencil)
            val depthStencilGlFormat = when (depthStencilFormat) {
                PresentationParameters.DepthFormat.Depth16 -> GLES30.GL_DEPTH_COMPONENT16
                PresentationParameters.DepthFormat.Depth24 -> GLES30.GL_DEPTH_COMPONENT24
                PresentationParameters.DepthFormat.Depth24Stencil8 -> GLES30.GL_DEPTH24_STENCIL8
                PresentationParameters.DepthFormat.None -> GLES30.GL_NONE
            }
            GLES30.glRenderbufferStorage(
                GLES30.GL_RENDERBUFFER,
                depthStencilGlFormat,
                width, height
            )
            val attachment = when (depthStencilFormat) {
                PresentationParameters.DepthFormat.Depth24Stencil8 -> GLES30.GL_DEPTH_STENCIL_ATTACHMENT
                else -> GLES30.GL_DEPTH_ATTACHMENT
            }
            GLES30.glFramebufferRenderbuffer(
                GLES30.GL_FRAMEBUFFER, attachment,
                GLES30.GL_RENDERBUFFER, glDepthStencil
            )
        }
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            android.util.Log.e("RenderTarget2D", "Framebuffer not complete: $status")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    override fun dispose() {
        if (glFramebuffer != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(glFramebuffer), 0)
            glFramebuffer = 0
        }
        if (glDepthStencil != 0) {
            GLES30.glDeleteRenderbuffers(1, intArrayOf(glDepthStencil), 0)
            glDepthStencil = 0
        }
        super.dispose()
    }
}
