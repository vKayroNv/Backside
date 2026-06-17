package ru.kayron.dew

import ru.kayron.cargo.module
import ru.kayron.dew.graphics.*
import ru.kayron.dew.graphics.PresentationParameters.DepthFormat
import ru.kayron.dew.graphics.PresentationParameters.DisplayOrientation
import ru.kayron.dew.graphics.PresentationParameters.PresentInterval

class GraphicsDeviceManager(private val game: Game) : IGraphicsDeviceManager, IGraphicsDeviceService {

    override val graphicsDevice: GraphicsDevice get() = game.graphicsDevice

    var preferredBackBufferWidth: Int = 0
    var preferredBackBufferHeight: Int = 0
    var preferredBackBufferFormat: SurfaceFormat = SurfaceFormat.Color
    var preferredDepthStencilFormat: DepthFormat = DepthFormat.Depth24Stencil8
    var preferredMultiSampleCount: Int = 0
    var synchronizedWithVerticalRetrace: Boolean = true
    var isFullScreen: Boolean = false

    private var initialized = false

    override fun createDevice() {
        if (preferredBackBufferWidth <= 0) preferredBackBufferWidth = game.gameWindow.clientBounds.width
        if (preferredBackBufferHeight <= 0) preferredBackBufferHeight = game.gameWindow.clientBounds.height

        val pp = graphicsDevice.presentationParameters
        pp.backBufferWidth = preferredBackBufferWidth
        pp.backBufferHeight = preferredBackBufferHeight
        pp.backBufferFormat = preferredBackBufferFormat
        pp.depthStencilFormat = preferredDepthStencilFormat
        pp.multiSampleCount = preferredMultiSampleCount
        pp.presentationInterval = if (synchronizedWithVerticalRetrace) PresentInterval.Default else PresentInterval.Immediate
        pp.isFullScreen = isFullScreen
        pp.displayOrientation = DisplayOrientation.LandscapeLeft

        graphicsDevice.setViewport(0, 0, preferredBackBufferWidth, preferredBackBufferHeight)

        game.cargo.load(module {
            singleton<IGraphicsDeviceService> { this@GraphicsDeviceManager }
        })

        initialized = true
    }

    override fun applyChanges() {
        if (!initialized) createDevice()
        val pp = graphicsDevice.presentationParameters
        pp.backBufferWidth = preferredBackBufferWidth
        pp.backBufferHeight = preferredBackBufferHeight
        pp.isFullScreen = isFullScreen
        graphicsDevice.setViewport(0, 0, preferredBackBufferWidth, preferredBackBufferHeight)
    }

    fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        applyChanges()
    }

    companion object {
        const val DefaultBackBufferWidth = 1920
        const val DefaultBackBufferHeight = 1080
    }
}
