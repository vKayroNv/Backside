package ru.kayron.dew

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.InputDevice
import android.view.MotionEvent
import android.view.KeyEvent
import ru.kayron.dew.input.GamePad
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class DewGameView(
    context: Context,
    private val game: Game
) : GLSurfaceView(context), GLSurfaceView.Renderer {

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var initialized = false
    private var contextRecreated = false

    init {
        setEGLContextClientVersion(3)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY
        setPreserveEGLContextOnPause(true)

        setOnKeyListener(game)
        setOnTouchListener(game)

        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        contextRecreated = initialized
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        game.gameWindow.setClientBounds(width, height)
        game.graphicsDevice.presentationParameters.backBufferWidth = width
        game.graphicsDevice.presentationParameters.backBufferHeight = height
        game.graphicsDevice.setViewport(0, 0, width, height)

        if (!initialized) {
            game.run()
            initialized = true
        } else if (contextRecreated) {
            game.reloadGraphicsResources()
            contextRecreated = false
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        if (initialized) {
            val alive = game.tick()
            if (!alive) {
                (context as? DewActivity)?.finish()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        game.onTouch(this, event)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (isJoystick && event.action == MotionEvent.ACTION_MOVE) {
            GamePad.onJoystickMotion(event)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        game.onKey(this, keyCode, event)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        game.onKey(this, keyCode, event)
        return true
    }

    fun onPauseGame() {
        game.isActive = false
        onPause()
    }

    fun onResumeGame() {
        game.isActive = true
        onResume()
    }
}
