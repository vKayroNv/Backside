package ru.kayron.dew.graphics

import android.opengl.GLES30

class BlendState private constructor(
    val name: String,
    val colorSourceBlend: Blend,
    val colorDestinationBlend: Blend,
    val colorBlendFunction: BlendFunction,
    val alphaSourceBlend: Blend,
    val alphaDestinationBlend: Blend,
    val alphaBlendFunction: BlendFunction,
    val colorWriteChannels: ColorWriteChannels = ColorWriteChannels.All
) {
    companion object {
        val Opaque = BlendState("Opaque",
            Blend.One, Blend.Zero, BlendFunction.Add,
            Blend.One, Blend.Zero, BlendFunction.Add
        )
        val AlphaBlend = BlendState("AlphaBlend",
            Blend.SourceAlpha, Blend.InverseSourceAlpha, BlendFunction.Add,
            Blend.One, Blend.InverseSourceAlpha, BlendFunction.Add
        )
        val Additive = BlendState("Additive",
            Blend.SourceAlpha, Blend.One, BlendFunction.Add,
            Blend.One, Blend.One, BlendFunction.Add
        )
        val NonPremultiplied = BlendState("NonPremultiplied",
            Blend.SourceAlpha, Blend.InverseSourceAlpha, BlendFunction.Add,
            Blend.SourceAlpha, Blend.InverseSourceAlpha, BlendFunction.Add
        )
        val Multiply = BlendState("Multiply",
            Blend.DestinationColor, Blend.Zero, BlendFunction.Add,
            Blend.DestinationAlpha, Blend.Zero, BlendFunction.Add
        )
        val Subtract = BlendState("Subtract",
            Blend.SourceAlpha, Blend.One, BlendFunction.ReverseSubtract,
            Blend.One, Blend.One, BlendFunction.ReverseSubtract
        )
    }

    enum class Blend(val glValue: Int) {
        Zero(GLES30.GL_ZERO),
        One(GLES30.GL_ONE),
        SourceColor(GLES30.GL_SRC_COLOR),
        InverseSourceColor(GLES30.GL_ONE_MINUS_SRC_COLOR),
        SourceAlpha(GLES30.GL_SRC_ALPHA),
        InverseSourceAlpha(GLES30.GL_ONE_MINUS_SRC_ALPHA),
        DestinationColor(GLES30.GL_DST_COLOR),
        InverseDestinationColor(GLES30.GL_ONE_MINUS_DST_COLOR),
        DestinationAlpha(GLES30.GL_DST_ALPHA),
        InverseDestinationAlpha(GLES30.GL_ONE_MINUS_DST_ALPHA),
        BlendFactor(GLES30.GL_CONSTANT_COLOR),
        InverseBlendFactor(GLES30.GL_ONE_MINUS_CONSTANT_COLOR),
        SourceAlphaSaturation(GLES30.GL_SRC_ALPHA_SATURATE),
    }

    enum class BlendFunction(val glValue: Int) {
        Add(GLES30.GL_FUNC_ADD),
        Subtract(GLES30.GL_FUNC_SUBTRACT),
        ReverseSubtract(GLES30.GL_FUNC_REVERSE_SUBTRACT),
        Min(GLES30.GL_MIN),
        Max(GLES30.GL_MAX),
    }

    enum class ColorWriteChannels(val value: Int) {
        None(0),
        Red(1),
        Green(2),
        Blue(4),
        Alpha(8),
        All(15),
    }

    fun apply() {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFuncSeparate(
            colorSourceBlend.glValue, colorDestinationBlend.glValue,
            alphaSourceBlend.glValue, alphaDestinationBlend.glValue
        )
        GLES30.glBlendEquationSeparate(colorBlendFunction.glValue, alphaBlendFunction.glValue)
        GLES30.glColorMask(
            colorWriteChannels.value and 1 != 0,
            colorWriteChannels.value and 2 != 0,
            colorWriteChannels.value and 4 != 0,
            colorWriteChannels.value and 8 != 0
        )
    }
}
