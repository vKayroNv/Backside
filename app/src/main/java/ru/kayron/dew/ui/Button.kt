package ru.kayron.dew.ui

import ru.kayron.dew.graphics.SpriteBatch
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector2

class Button(
    var bounds: Rectangle,
    var text: String = "",
    var textureFilename: String? = null,
    var backgroundColor: Color = Color.DarkSlateGray,
    var pressedTint: Color = Color(0.65f, 0.65f, 0.65f, 1f),
    var textColor: Color = Color.White,
    var renderMode: UiRenderMode = UiRenderMode.Static,
    var font: SpriteFont? = null,
    var visible: Boolean = true,
    var onClick: (() -> Unit)? = null
) {
    var isPressed: Boolean = false
        private set

    fun update(
        screenPosition: Vector2,
        worldPosition: Vector2,
        pressed: Boolean
    ) {
        if (!visible) {
            isPressed = false
            return
        }

        val pointerPosition = when (renderMode) {
            UiRenderMode.Static -> screenPosition
            UiRenderMode.World -> worldPosition
        }
        val contains = bounds.contains(
            pointerPosition.x.toInt(),
            pointerPosition.y.toInt()
        )

        if (pressed) {
            if (contains) {
                isPressed = true
            }
            return
        }

        if (isPressed && contains) {
            onClick?.invoke()
        }
        isPressed = false
    }

    fun draw(
        batch: SpriteBatch,
        defaultFont: SpriteFont,
        texture: (String) -> Texture2D,
        fallbackTexture: Texture2D
    ) {
        if (!visible) return

        val buttonTexture = textureFilename?.let(texture) ?: fallbackTexture
        val drawColor = if (isPressed) backgroundColor * pressedTint else backgroundColor
        batch.draw(
            texture = buttonTexture,
            destinationRectangle = bounds,
            color = drawColor
        )

        if (text.isEmpty()) return

        val textFont = font ?: defaultFont
        val textSize = textFont.measureString(text)
        val textPosition = Vector2(
            bounds.x + (bounds.width - textSize.x) * 0.5f,
            bounds.y + (bounds.height - textSize.y) * 0.5f
        )

        batch.drawString(
            textFont,
            text,
            textPosition,
            textColor
        )
    }
}
