package ru.kayron.dew.graphics

import android.opengl.GLES30

class SamplerState(
    var filter: TextureFilter = TextureFilter.Linear,
    var addressU: TextureAddressMode = TextureAddressMode.Clamp,
    var addressV: TextureAddressMode = TextureAddressMode.Clamp,
    var addressW: TextureAddressMode = TextureAddressMode.Clamp,
    var maxAnisotropy: Int = 4,
    var maxMipLevel: Int = 0,
    var mipMapLevelOfDetailBias: Float = 0f,
) {
    companion object {
        val LinearClamp = SamplerState()
        val PointClamp = SamplerState(filter = TextureFilter.Point)
        val LinearWrap = SamplerState(addressU = TextureAddressMode.Wrap, addressV = TextureAddressMode.Wrap)
        val PointWrap = SamplerState(filter = TextureFilter.Point, addressU = TextureAddressMode.Wrap, addressV = TextureAddressMode.Wrap)
        val AnisotropicClamp = SamplerState(filter = TextureFilter.Anisotropic)
    }

    enum class TextureFilter {
        Linear,
        Point,
        Anisotropic,
        LinearMipPoint,
        PointMipLinear,
        MinLinearMagPointMipLinear,
        MinLinearMagPointMipPoint,
        MinPointMagLinearMipLinear,
        MinPointMagLinearMipPoint,
    }

    enum class TextureAddressMode(val glValue: Int) {
        Wrap(GLES30.GL_REPEAT),
        Clamp(GLES30.GL_CLAMP_TO_EDGE),
        Mirror(GLES30.GL_MIRRORED_REPEAT),
    }

    fun apply(unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        val glFilter = when (filter) {
            TextureFilter.Linear -> GLES30.GL_LINEAR
            TextureFilter.Point -> GLES30.GL_NEAREST
            TextureFilter.Anisotropic -> GLES30.GL_LINEAR_MIPMAP_LINEAR
            else -> GLES30.GL_LINEAR
        }
        val glMinFilter = when (filter) {
            TextureFilter.Anisotropic -> GLES30.GL_LINEAR_MIPMAP_LINEAR
            TextureFilter.LinearMipPoint -> GLES30.GL_LINEAR_MIPMAP_NEAREST
            TextureFilter.PointMipLinear -> GLES30.GL_NEAREST_MIPMAP_LINEAR
            else -> glFilter
        }
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, glMinFilter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, glFilter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, addressU.glValue)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, addressV.glValue)
    }
}
