package ru.kayron.dew.input

import android.view.MotionEvent
import ru.kayron.dew.math.Vector2

object Touch {
    private val lock = Any()
    private var currentTouches = TouchCollection()
    private var previousTouches = TouchCollection()

    fun getState(): TouchCollection {
        synchronized(lock) { return TouchCollection().also { it.addAll(currentTouches) } }
    }

    fun getPreviousState(): TouchCollection {
        synchronized(lock) { return TouchCollection().also { it.addAll(previousTouches) } }
    }

    fun isAnyTouch(): Boolean = synchronized(lock) { currentTouches.isNotEmpty() }

    internal fun onTouchEvent(event: MotionEvent) {
        synchronized(lock) {
            val pointerIndex = event.actionIndex
            val pointerId = event.getPointerId(pointerIndex)
            val x = event.getX(pointerIndex)
            val y = event.getY(pointerIndex)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    if (currentTouches.findById(pointerId) == null) {
                        currentTouches.add(
                            TouchLocation(
                                id = pointerId,
                                position = Vector2(x, y),
                                state = TouchLocation.TouchLocationState.Pressed,
                                pressure = event.getPressure(pointerIndex)
                            )
                        )
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        val touch = currentTouches.findById(id)
                        if (touch != null) {
                            val idx = currentTouches.indexOf(touch)
                            currentTouches[idx] = touch.copy(
                                position = Vector2(event.getX(i), event.getY(i)),
                                previousPosition = touch.position,
                                state = TouchLocation.TouchLocationState.Moved
                            )
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val touch = currentTouches.findById(pointerId)
                    if (touch != null) {
                        val idx = currentTouches.indexOf(touch)
                        currentTouches[idx] = touch.copy(state = TouchLocation.TouchLocationState.Released)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    currentTouches.clear()
                }
            }
        }
    }

    internal fun update() {
        synchronized(lock) {
            previousTouches.clear()
            previousTouches.addAll(currentTouches)
            currentTouches.removeAll { it.state == TouchLocation.TouchLocationState.Released }
        }
    }
}
