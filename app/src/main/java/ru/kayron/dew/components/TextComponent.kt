package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Vector2
import ru.kayron.dew.utils.swap

class TextComponent : Component() {
    private var text = arrayOfNulls<String>(INITIAL_CAPACITY)
    private var color = arrayOfNulls<Color>(INITIAL_CAPACITY)
    private var offsetX = FloatArray(INITIAL_CAPACITY)
    private var offsetY = FloatArray(INITIAL_CAPACITY)
    private var font = arrayOfNulls<SpriteFont>(INITIAL_CAPACITY)

    fun add(
        entity: Entity,
        textVal: String = "",
        colorVal: Color = Color.White,
        offsetXVal: Float = 0f,
        offsetYVal: Float = 0f,
        fontVal: SpriteFont? = null
    ) {
        val index = addEntity(entity)
        text[index] = textVal
        color[index] = colorVal
        offsetX[index] = offsetXVal
        offsetY[index] = offsetYVal
        font[index] = fontVal
    }

    fun update(
        entity: Entity,
        textVal: String? = null,
        colorVal: Color? = null,
        offsetXVal: Float? = null,
        offsetYVal: Float? = null,
        fontVal: SpriteFont? = null
    ) {
        val index = indexOf(entity)
        textVal?.let { text[index] = it }
        colorVal?.let { color[index] = it }
        offsetXVal?.let { offsetX[index] = it }
        offsetYVal?.let { offsetY[index] = it }
        if (fontVal != null) font[index] = fontVal
    }

    override fun grow(newSize: Int) {
        text = text.copyOf(newSize)
        color = color.copyOf(newSize)
        offsetX = offsetX.copyOf(newSize)
        offsetY = offsetY.copyOf(newSize)
        font = font.copyOf(newSize)
    }

    override fun swap(a: Int, b: Int) {
        text.swap(a, b)
        color.swap(a, b)
        offsetX.swap(a, b)
        offsetY.swap(a, b)
        font.swap(a, b)
    }

    fun text(entity: Entity): String = text[indexOf(entity)] ?: ""
    fun color(entity: Entity): Color = color[indexOf(entity)] ?: Color.White
    fun offset(entity: Entity): Vector2 = Vector2(offsetX(entity), offsetY(entity))
    fun offsetX(entity: Entity): Float = offsetX[indexOf(entity)]
    fun offsetY(entity: Entity): Float = offsetY[indexOf(entity)]
    fun font(entity: Entity): SpriteFont? = font[indexOf(entity)]
}
