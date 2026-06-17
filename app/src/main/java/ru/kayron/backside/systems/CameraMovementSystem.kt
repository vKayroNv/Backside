package ru.kayron.backside.systems

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.ecs.GameSystem
import ru.kayron.dew.ecs.World
import ru.kayron.dew.input.Touch
import ru.kayron.dew.input.TouchCollection
import ru.kayron.dew.math.Vector2
import kotlin.math.hypot

class CameraMovementSystem(
    game: Game,
    private val world: World
) : GameSystem(game) {
    override val updateOrder = 0

    private var lastDistance = 1f
    private var prevTouchPosition: Vector2? = null
    private var previousTouchCount = 0

    override fun update(gameTime: GameTime) {
        val state = Touch.getState()

        when (state.size) {
            1 -> drag(state)
            2 -> zoom(state)
            else -> {
                prevTouchPosition = null
            }
        }

        previousTouchCount = state.size
    }

    private fun drag(state: TouchCollection) {
        if (state.size != 1) return

        val cameraComponent = world.componentManager.get<CameraComponent>()
        val camera = cameraComponent.getActive()
        if (camera == -1) return

        val transformComponent = world.componentManager.get<TransformComponent>()
        val transform = transformComponent.pos(camera) ?: return

        val currentPos = state[0].position

        if (previousTouchCount != 1) {
            prevTouchPosition = currentPos
            return
        }

        val prevPos = prevTouchPosition ?: run {
            prevTouchPosition = currentPos
            return
        }

        val deltaX = currentPos.x - prevPos.x
        val deltaY = currentPos.y - prevPos.y

        val zoom = cameraComponent.zoom(camera)
        val worldDeltaX = deltaX / zoom
        val worldDeltaY = deltaY / zoom

        transform.x -= worldDeltaX
        transform.y -= worldDeltaY

        transformComponent.update(camera, transform.x, transform.y)

        prevTouchPosition = currentPos
    }

    private fun zoom(state: TouchCollection) {
        if (state.size != 2) return

        val cameraComponent = world.componentManager.get<CameraComponent>()
        val camera = cameraComponent.getActive()
        if (camera == -1) return

        val d = hypot(
            state[0].position.x - state[1].position.x,
            state[0].position.y - state[1].position.y
        )
        if (d == 0f) return

        if (previousTouchCount != 2) {
            lastDistance = d
            return
        }

        val delta = (d - lastDistance) / lastDistance

        val currentZoom = cameraComponent.zoom(camera)
        var newZoom = currentZoom * (1 + delta * 0.5f)
        if (newZoom >= 20f) newZoom = 20f

        cameraComponent.update(camera, newZoom.coerceAtLeast(0.00001f))

        lastDistance = d
    }
}