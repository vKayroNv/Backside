package ru.kayron.dew.ui

import ru.kayron.dew.graphics.SpriteBatch
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Vector2

class Label(
    var text: String,
    var position: Vector2,
    var color: Color = Color.White,
    var renderMode: UiRenderMode = UiRenderMode.Static,
    var font: SpriteFont? = null,
    var visible: Boolean = true
) {
    fun draw(batch: SpriteBatch, defaultFont: SpriteFont) {
        if (!visible || text.isEmpty()) return

        batch.drawString(
            font ?: defaultFont,
            text,
            position,
            color
        )
    }
}
