package ru.kayron.dew

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import ru.kayron.dew.graphics.GraphicsDevice
import ru.kayron.dew.input.*
import ru.kayron.dew.content.ContentManager
import ru.kayron.cargo.Cargo
import ru.kayron.cargo.CargoContainer

open class Game : View.OnKeyListener, View.OnTouchListener {
    val graphicsDevice = GraphicsDevice()
    val gameWindow: GameWindow = GameWindow()
    val cargo: CargoContainer = Cargo.root
    
    val graphicsDeviceManager: GraphicsDeviceManager = GraphicsDeviceManager(this)

    var isActive: Boolean = true
    var isMouseVisible: Boolean = true
    var exitRequested: Boolean = false
    private var isInitialized = false
    private var isRunning = false

    private var updateTime = GameTime()
    private var drawTime = GameTime()
    private var lastFrameTimeNanos: Long = 0L
    private var totalGameTimeNanos: Long = 0L

    val content: ContentManager = ContentManager(this)

    open fun initialize() {
        if (isInitialized) return
        graphicsDeviceManager.createDevice()
        graphicsDevice.initialize()
        
        loadContent()
        isInitialized = true
    }

    open fun loadContent() {}
    open fun unloadContent() {}

    open fun update(gameTime: GameTime) {
        
    }

    open fun draw(gameTime: GameTime) {
        
    }

    fun run() {
        isRunning = true
        lastFrameTimeNanos = System.nanoTime()
        initialize()
    }

    fun tick(): Boolean {
        if (exitRequested) return false
        val now = System.nanoTime()
        val elapsed = now - lastFrameTimeNanos
        lastFrameTimeNanos = now
        totalGameTimeNanos += elapsed

        val maxElapsed = 50_000_000L
        val clampedElapsed = minOf(elapsed, maxElapsed)

        Keyboard.update()
        Touch.update()
        GamePad.update()
        Mouse.update()

        updateTime = GameTime(totalGameTimeNanos, clampedElapsed)
        drawTime = GameTime(totalGameTimeNanos, clampedElapsed)

        update(updateTime)
        draw(drawTime)
        graphicsDevice.present()

        return true
    }

    fun exit() {
        exitRequested = true
    }

    fun resetElapsedTime() {
        lastFrameTimeNanos = System.nanoTime()
    }

    open fun reloadGraphicsResources() {
        graphicsDevice.initialize()
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            Keyboard.onKeyDown(keyCode)
            GamePad.onKeyDown(keyCode)
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            Keyboard.onKeyUp(keyCode)
            GamePad.onKeyUp(keyCode)
            return true
        }
        return false
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        Touch.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            Mouse.onTouch(event.x, event.y)
        } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            Mouse.onTouchUp()
            v.performClick()
        }
        return true
    }

    open fun dispose() {
        unloadContent()
        graphicsDevice.dispose()
    }
}
