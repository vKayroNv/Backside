package ru.kayron.dew.graphics

import android.opengl.GLES30

data class DepthStencilState(
    var depthBufferEnable: Boolean = true,
    var depthBufferWriteEnable: Boolean = true,
    var depthBufferFunction: CompareFunction = CompareFunction.LessEqual,
    var stencilEnable: Boolean = false,
    var stencilFunction: CompareFunction = CompareFunction.Always,
    var stencilPass: StencilOperation = StencilOperation.Keep,
    var stencilFail: StencilOperation = StencilOperation.Keep,
    var stencilDepthBufferFail: StencilOperation = StencilOperation.Keep,
    var twoSidedStencilMode: Boolean = false,
    var referenceStencil: Int = 0,
    var stencilMask: Int = 0xFF,
    var stencilWriteMask: Int = 0xFF,
) {
    companion object {
        val Default = DepthStencilState()
        val DepthRead = DepthStencilState(
            depthBufferWriteEnable = false
        )
        val None = DepthStencilState(
            depthBufferEnable = false,
            depthBufferWriteEnable = false
        )
    }

    enum class CompareFunction(val glValue: Int) {
        Always(GLES30.GL_ALWAYS),
        Never(GLES30.GL_NEVER),
        Less(GLES30.GL_LESS),
        LessEqual(GLES30.GL_LEQUAL),
        Equal(GLES30.GL_EQUAL),
        GreaterEqual(GLES30.GL_GEQUAL),
        Greater(GLES30.GL_GREATER),
        NotEqual(GLES30.GL_NOTEQUAL),
    }

    enum class StencilOperation(val glValue: Int) {
        Keep(GLES30.GL_KEEP),
        Zero(GLES30.GL_ZERO),
        Replace(GLES30.GL_REPLACE),
        Increment(GLES30.GL_INCR),
        Decrement(GLES30.GL_DECR),
        IncrementSaturation(GLES30.GL_INCR_WRAP),
        DecrementSaturation(GLES30.GL_DECR_WRAP),
        Invert(GLES30.GL_INVERT),
    }

    fun apply() {
        if (depthBufferEnable) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glDepthMask(depthBufferWriteEnable)
            GLES30.glDepthFunc(depthBufferFunction.glValue)
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        }
        if (stencilEnable) {
            GLES30.glEnable(GLES30.GL_STENCIL_TEST)
            GLES30.glStencilFunc(stencilFunction.glValue, referenceStencil, stencilMask)
            GLES30.glStencilOp(
                stencilFail.glValue,
                stencilDepthBufferFail.glValue,
                stencilPass.glValue
            )
            GLES30.glStencilMask(stencilWriteMask)
        } else {
            GLES30.glDisable(GLES30.GL_STENCIL_TEST)
        }
    }
}
