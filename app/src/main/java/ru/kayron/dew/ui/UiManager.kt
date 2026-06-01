package ru.kayron.dew.ui

import ru.kayron.dew.graphics.SpriteBatch
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.input.Mouse
import ru.kayron.dew.input.MouseState
import ru.kayron.dew.math.Matrix
import ru.kayron.dew.math.Vector2

class UiManager {
    private val labels = mutableListOf<Label>()
    private val buttons = mutableListOf<Button>()

    fun add(label: Label): Label {
        labels += label
        return label
    }

    fun add(button: Button): Button {
        buttons += button
        return button
    }

    fun remove(label: Label) {
        labels -= label
    }

    fun remove(button: Button) {
        buttons -= button
    }

    fun clear() {
        labels.clear()
        buttons.clear()
    }

    fun update(worldToScreen: Matrix) {
        val mouse = Mouse.getState()
        val screenPosition = Vector2(mouse.x.toFloat(), mouse.y.toFloat())
        val worldPosition = Vector2.transform(
            screenPosition,
            Matrix.invert(worldToScreen)
        )
        val pressed = mouse.leftButton == MouseState.ButtonState.Pressed

        buttons.forEach {
            it.update(
                screenPosition = screenPosition,
                worldPosition = worldPosition,
                pressed = pressed
            )
        }
    }

    fun draw(
        batch: SpriteBatch,
        renderMode: UiRenderMode,
        defaultFont: SpriteFont,
        texture: (String) -> Texture2D,
        fallbackTexture: Texture2D
    ) {
        buttons.forEach {
            if (it.renderMode == renderMode) {
                it.draw(batch, defaultFont, texture, fallbackTexture)
            }
        }

        labels.forEach {
            if (it.renderMode == renderMode) {
                it.draw(batch, defaultFont)
            }
        }
    }
}
