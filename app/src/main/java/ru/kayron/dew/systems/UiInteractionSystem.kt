package ru.kayron.dew.systems

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.components.UiComponent
import ru.kayron.dew.ecs.GameSystem
import ru.kayron.dew.ecs.World
import ru.kayron.dew.input.Mouse
import ru.kayron.dew.input.MouseState
import ru.kayron.dew.math.Matrix
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector2
import ru.kayron.dew.math.Vector3
import ru.kayron.dew.ui.RadioButton
import ru.kayron.dew.ui.ScrollView
import ru.kayron.dew.ui.UiManager
import ru.kayron.dew.ui.UiRenderMode

class UiInteractionSystem(
    game: Game,
    private val world: World,
    private val uiManager: UiManager
) : GameSystem(game) {

    override val updateOrder: Int = -10

    private var worldToScreen = Matrix.Identity
    private var wasPressed = false
    private var activeEntity = -1
    private var dragEntity = -1

    fun setWorldToScreen(matrix: Matrix) {
        worldToScreen = matrix
    }

    override fun update(gameTime: GameTime) {
        if (uiManager.elements.isEmpty()) return

        worldToScreen = calculateWorldToScreen()

        val mouse = Mouse.getState()
        val screenPosition = Vector2(mouse.x.toFloat(), mouse.y.toFloat())
        val worldPosition = Vector2.transform(screenPosition, Matrix.invert(worldToScreen))
        val pressed = mouse.leftButton == MouseState.ButtonState.Pressed

        val hit = uiManager.elements
            .asSequence()
            .filter { it.visible && it.enabled && it.canInteract }
            .lastOrNull {
                val pointer = if (it.renderMode == UiRenderMode.Static) screenPosition else worldPosition
                val b = boundsOf(it.entity)
                if (!b.contains(pointer.x.toInt(), pointer.y.toInt())) return@lastOrNull false
                pointInsideClip(it.entity, pointer)
            }

        val ui = uiComponent
        uiManager.elements.forEach {
            ui.update(it.entity, hoveredVal = it == hit)
        }

        if (pressed && !wasPressed) {
            activeEntity = hit?.entity ?: -1
            dragEntity = activeEntity
            if (activeEntity != -1) {
                ui.update(activeEntity, pressedVal = true)
                val active = uiManager.element(activeEntity)
                if (active is ScrollView) {
                    val pointer = if (active.renderMode == UiRenderMode.Static) screenPosition else worldPosition
                    active.captureDragStart(pointer.x - active.x, pointer.y - active.y)
                }
            }
        }

        if (pressed && dragEntity != -1) {
            val active = uiManager.element(dragEntity)
            if (active != null) {
                val pointer = if (active.renderMode == UiRenderMode.Static) screenPosition else worldPosition
                val b = boundsOf(dragEntity)
                active.onPointerDrag(Vector2(pointer.x - b.x, pointer.y - b.y))
            }
        }

        if (!pressed && wasPressed && activeEntity != -1) {
            val active = uiManager.element(activeEntity)
            if (active != null && active == hit) {
                if (active is RadioButton && active.group.isNotEmpty()) {
                    uiManager.elements.filterIsInstance<RadioButton>()
                        .filter { it.group == active.group && it !== active }
                        .forEach { it.setSelectedFromGroup(false) }
                }
                active.onPointerReleasedInside()
            }
            if (uiComponent.hasEntity(activeEntity)) {
                ui.update(activeEntity, pressedVal = false)
            }
            activeEntity = -1
            dragEntity = -1
        }

        if (!pressed && activeEntity == -1) {
            uiManager.elements.forEach {
                ui.update(it.entity, pressedVal = false)
            }
            dragEntity = -1
        }

        wasPressed = pressed
    }

    private fun pointInsideClip(entity: Int, point: Vector2): Boolean {
        val ui = uiComponent
        val left = ui.clipLeft(entity)
        val top = ui.clipTop(entity)
        val right = ui.clipRight(entity)
        val bottom = ui.clipBottom(entity)
        if (left.isInfinite() || top.isInfinite()) return true
        return point.x >= left && point.x < right && point.y >= top && point.y < bottom
    }

    private val transform: TransformComponent
        get() = world.componentManager.get()

    private val uiComponent: UiComponent
        get() = world.componentManager.get()

    private fun boundsOf(entity: Int): Rectangle {
        val t = transform
        return Rectangle(
            t.x(entity).toInt(),
            t.y(entity).toInt(),
            t.width(entity).toInt(),
            t.height(entity).toInt()
        )
    }

    private fun calculateWorldToScreen(): Matrix {
        val cameraComponent = world.componentManager.get<CameraComponent>()
        val transformComponent = world.componentManager.get<TransformComponent>()
        val camera = cameraComponent.getActive()
        if (camera == -1) return Matrix.Identity

        val viewportWidth = cameraComponent.viewportWidth(camera).takeIf { it > 0f }
            ?: game.graphicsDevice.viewport.width.toFloat()
        val viewportHeight = cameraComponent.viewportHeight(camera).takeIf { it > 0f }
            ?: game.graphicsDevice.viewport.height.toFloat()

        return Matrix.createTranslation(
            Vector3(-transformComponent.x(camera), -transformComponent.y(camera), 0f)
        ) *
            Matrix.createRotationZ(-transformComponent.rotation(camera)) *
            Matrix.createScale(Vector3(cameraComponent.zoom(camera), cameraComponent.zoom(camera), 1f)) *
            Matrix.createTranslation(Vector3(viewportWidth * 0.5f, viewportHeight * 0.5f, 0f))
    }
}