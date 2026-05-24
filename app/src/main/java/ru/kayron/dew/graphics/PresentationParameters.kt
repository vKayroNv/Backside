package ru.kayron.dew.graphics

import android.opengl.GLES30
import ru.kayron.dew.math.Rectangle

class PresentationParameters {
    var backBufferWidth: Int = 0
    var backBufferHeight: Int = 0
    var backBufferFormat: SurfaceFormat = SurfaceFormat.Color
    var depthStencilFormat: DepthFormat = DepthFormat.Depth24Stencil8
    var multiSampleCount: Int = 0
    var presentationInterval: PresentInterval = PresentInterval.Default
    var displayOrientation: DisplayOrientation = DisplayOrientation.Default
    var autoDepthStencil: Boolean = true
    var isFullScreen: Boolean = true
    var bounds: Rectangle = Rectangle()
    var renderTargetUsage: RenderTargetUsage = RenderTargetUsage.DiscardContents

    enum class DepthFormat {
        None,
        Depth16,
        Depth24,
        Depth24Stencil8,
    }

    enum class PresentInterval {
        Default,
        One,
        Two,
        Immediate,
    }

    enum class DisplayOrientation(val value: Int) {
        Default(0),
        LandscapeLeft(1),
        LandscapeRight(2),
        Portrait(3),
        PortraitDown(4),
    }

    enum class RenderTargetUsage {
        DiscardContents,
        PreserveContents,
        PlatformContents,
    }

    val glDepthFormat: Int
        get() = when (depthStencilFormat) {
            DepthFormat.Depth16 -> GLES30.GL_DEPTH_COMPONENT16
            DepthFormat.Depth24 -> GLES30.GL_DEPTH_COMPONENT24
            DepthFormat.Depth24Stencil8 -> GLES30.GL_DEPTH24_STENCIL8
            DepthFormat.None -> GLES30.GL_NONE
        }

    val glStencilFormat: Int
        get() = when (depthStencilFormat) {
            DepthFormat.Depth24Stencil8 -> GLES30.GL_STENCIL_INDEX8
            else -> GLES30.GL_NONE
        }

    fun clone(): PresentationParameters {
        val pp = PresentationParameters()
        pp.backBufferWidth = backBufferWidth
        pp.backBufferHeight = backBufferHeight
        pp.backBufferFormat = backBufferFormat
        pp.depthStencilFormat = depthStencilFormat
        pp.multiSampleCount = multiSampleCount
        pp.presentationInterval = presentationInterval
        pp.displayOrientation = displayOrientation
        pp.autoDepthStencil = autoDepthStencil
        pp.isFullScreen = isFullScreen
        pp.bounds = bounds
        pp.renderTargetUsage = renderTargetUsage
        return pp
    }
}
