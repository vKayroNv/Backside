package ru.kayron.dew.graphics

import android.opengl.GLES30

data class RasterizerState(
    var cullMode: CullMode = CullMode.CullCounterClockwiseFace,
    var fillMode: FillMode = FillMode.Solid,
    var depthBias: Float = 0f,
    var slopeScaleDepthBias: Float = 0f,
    var scissorTestEnable: Boolean = false,
    var multiSampleAntiAlias: Boolean = true,
) {
    companion object {
        val CullNone = RasterizerState(cullMode = CullMode.None)
        val CullClockwise = RasterizerState(cullMode = CullMode.CullClockwiseFace)
        val CullCounterClockwise = RasterizerState(cullMode = CullMode.CullCounterClockwiseFace)
    }

    enum class CullMode(val glValue: Int) {
        None(GLES30.GL_NONE),
        CullClockwiseFace(GLES30.GL_FRONT),
        CullCounterClockwiseFace(GLES30.GL_BACK),
    }

    enum class FillMode(val glValue: Int) {
        Solid(0),
    }

    fun apply() {
        if (cullMode == CullMode.None) {
            GLES30.glDisable(GLES30.GL_CULL_FACE)
        } else {
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            GLES30.glCullFace(cullMode.glValue)
        }
        if (scissorTestEnable) {
            GLES30.glEnable(GLES30.GL_SCISSOR_TEST)
        } else {
            GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
        }
        GLES30.glPolygonOffset(depthBias, slopeScaleDepthBias)
    }
}
