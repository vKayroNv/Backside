package ru.kayron.dew.input

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice
import android.view.MotionEvent
import android.view.KeyEvent

object GamePad {
    private val states = Array(4) { GamePadState() }
    private var vibrator: Vibrator? = null

    internal fun initialize(context: Context) {
        vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }

    fun getState(playerIndex: Int): GamePadState {
        return if (playerIndex in 0..3) states[playerIndex] else GamePadState()
    }

    fun setVibration(playerIndex: Int, leftMotor: Float, rightMotor: Float): Boolean {
        if (playerIndex !in 0..3) return false
        val vib = vibrator ?: return false
        if (!vib.hasVibrator()) return false
        val amplitude = (maxOf(leftMotor, rightMotor).coerceIn(0f, 1f) * 255f).toInt()
        if (amplitude <= 0) {
            vib.cancel()
            return true
        }
        vib.vibrate(VibrationEffect.createOneShot(100L, amplitude.coerceIn(1, 255)))
        return true
    }

    internal fun onKeyDown(keyCode: Int, playerIndex: Int = 0) {
        val state = states[playerIndex]
        val buttons = state.buttons
        val dPad = state.dPad
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> states[playerIndex] = state.copy(
                dPad = dPad.copy(up = true)
            )
            KeyEvent.KEYCODE_DPAD_DOWN -> states[playerIndex] = state.copy(
                dPad = dPad.copy(down = true)
            )
            KeyEvent.KEYCODE_DPAD_LEFT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(left = true)
            )
            KeyEvent.KEYCODE_DPAD_RIGHT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(right = true)
            )
            KeyEvent.KEYCODE_BUTTON_A -> states[playerIndex] = state.copy(
                buttons = buttons.copy(a = true)
            )
            KeyEvent.KEYCODE_BUTTON_B -> states[playerIndex] = state.copy(
                buttons = buttons.copy(b = true)
            )
            KeyEvent.KEYCODE_BUTTON_X -> states[playerIndex] = state.copy(
                buttons = buttons.copy(x = true)
            )
            KeyEvent.KEYCODE_BUTTON_Y -> states[playerIndex] = state.copy(
                buttons = buttons.copy(y = true)
            )
            KeyEvent.KEYCODE_BUTTON_L1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftShoulder = true)
            )
            KeyEvent.KEYCODE_BUTTON_R1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightShoulder = true)
            )
            KeyEvent.KEYCODE_BUTTON_L2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(left = 1f)
            )
            KeyEvent.KEYCODE_BUTTON_R2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(right = 1f)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBL -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftStick = true)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBR -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightStick = true)
            )
            KeyEvent.KEYCODE_BUTTON_START -> states[playerIndex] = state.copy(
                buttons = buttons.copy(start = true)
            )
            KeyEvent.KEYCODE_BUTTON_SELECT -> states[playerIndex] = state.copy(
                buttons = buttons.copy(back = true)
            )
        }
    }

    internal fun onKeyUp(keyCode: Int, playerIndex: Int = 0) {
        val state = states[playerIndex]
        val buttons = state.buttons
        val dPad = state.dPad
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> states[playerIndex] = state.copy(
                dPad = dPad.copy(up = false)
            )
            KeyEvent.KEYCODE_DPAD_DOWN -> states[playerIndex] = state.copy(
                dPad = dPad.copy(down = false)
            )
            KeyEvent.KEYCODE_DPAD_LEFT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(left = false)
            )
            KeyEvent.KEYCODE_DPAD_RIGHT -> states[playerIndex] = state.copy(
                dPad = dPad.copy(right = false)
            )
            KeyEvent.KEYCODE_BUTTON_A -> states[playerIndex] = state.copy(
                buttons = buttons.copy(a = false)
            )
            KeyEvent.KEYCODE_BUTTON_B -> states[playerIndex] = state.copy(
                buttons = buttons.copy(b = false)
            )
            KeyEvent.KEYCODE_BUTTON_X -> states[playerIndex] = state.copy(
                buttons = buttons.copy(x = false)
            )
            KeyEvent.KEYCODE_BUTTON_Y -> states[playerIndex] = state.copy(
                buttons = buttons.copy(y = false)
            )
            KeyEvent.KEYCODE_BUTTON_L1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftShoulder = false)
            )
            KeyEvent.KEYCODE_BUTTON_R1 -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightShoulder = false)
            )
            KeyEvent.KEYCODE_BUTTON_L2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(left = 0f)
            )
            KeyEvent.KEYCODE_BUTTON_R2 -> states[playerIndex] = state.copy(
                triggers = state.triggers.copy(right = 0f)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBL -> states[playerIndex] = state.copy(
                buttons = buttons.copy(leftStick = false)
            )
            KeyEvent.KEYCODE_BUTTON_THUMBR -> states[playerIndex] = state.copy(
                buttons = buttons.copy(rightStick = false)
            )
            KeyEvent.KEYCODE_BUTTON_START -> states[playerIndex] = state.copy(
                buttons = buttons.copy(start = false)
            )
            KeyEvent.KEYCODE_BUTTON_SELECT -> states[playerIndex] = state.copy(
                buttons = buttons.copy(back = false)
            )
        }
    }

    internal fun onJoystickMotion(event: MotionEvent) {
        val playerIndex = 0
        val state = states[playerIndex]
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        val lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        states[playerIndex] = state.copy(
            thumbSticks = ru.kayron.dew.input.GamePadThumbSticks(
                left = ru.kayron.dew.math.Vector2(lx, ly),
                right = ru.kayron.dew.math.Vector2(rx, ry)
            ),
            triggers = ru.kayron.dew.input.GamePadTriggers(lt, rt),
            isConnected = true
        )
    }

    internal fun update() {
        for (i in states.indices) {
            states[i] = states[i].copy(packetNumber = states[i].packetNumber + 1)
        }
    }
}
