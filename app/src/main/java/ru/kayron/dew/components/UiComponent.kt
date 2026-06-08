package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.math.Color
import ru.kayron.dew.ui.UiRenderMode
import ru.kayron.dew.utils.swap

class UiComponent : Component() {
    private var parent = IntArray(INITIAL_CAPACITY) { NO_PARENT }
    private var renderMode = IntArray(INITIAL_CAPACITY)
    private var visible = BooleanArray(INITIAL_CAPACITY)
    private var enabled = BooleanArray(INITIAL_CAPACITY)
    private var hovered = BooleanArray(INITIAL_CAPACITY)
    private var pressed = BooleanArray(INITIAL_CAPACITY)
    private var backgroundColor = arrayOfNulls<Color>(INITIAL_CAPACITY)
    private var pressedTint = arrayOfNulls<Color>(INITIAL_CAPACITY)
    private var clipLeft = FloatArray(INITIAL_CAPACITY)
    private var clipTop = FloatArray(INITIAL_CAPACITY)
    private var clipRight = FloatArray(INITIAL_CAPACITY)
    private var clipBottom = FloatArray(INITIAL_CAPACITY)
    private var styleTag = arrayOfNulls<String>(INITIAL_CAPACITY)

    fun add(
        entity: Entity,
        parentVal: Entity = NO_PARENT,
        renderModeVal: UiRenderMode = UiRenderMode.Static,
        visibleVal: Boolean = true,
        enabledVal: Boolean = true,
        backgroundColorVal: Color = Color.Transparent,
        pressedTintVal: Color = Color(0.65f, 0.65f, 0.65f, 1f),
        styleTagVal: String? = null
    ) {
        val index = addEntity(entity)
        parent[index] = parentVal
        renderMode[index] = renderModeVal.ordinal
        visible[index] = visibleVal
        enabled[index] = enabledVal
        hovered[index] = false
        pressed[index] = false
        backgroundColor[index] = backgroundColorVal
        pressedTint[index] = pressedTintVal
        clipLeft[index] = Float.NEGATIVE_INFINITY
        clipTop[index] = Float.NEGATIVE_INFINITY
        clipRight[index] = Float.POSITIVE_INFINITY
        clipBottom[index] = Float.POSITIVE_INFINITY
        styleTag[index] = styleTagVal
    }

    fun update(
        entity: Entity,
        parentVal: Entity? = null,
        renderModeVal: UiRenderMode? = null,
        visibleVal: Boolean? = null,
        enabledVal: Boolean? = null,
        hoveredVal: Boolean? = null,
        pressedVal: Boolean? = null,
        backgroundColorVal: Color? = null,
        pressedTintVal: Color? = null,
        clipLeftVal: Float? = null,
        clipTopVal: Float? = null,
        clipRightVal: Float? = null,
        clipBottomVal: Float? = null,
        styleTagVal: String? = null
    ) {
        val index = indexOf(entity)
        parentVal?.let { parent[index] = it }
        renderModeVal?.let { renderMode[index] = it.ordinal }
        visibleVal?.let { visible[index] = it }
        enabledVal?.let { enabled[index] = it }
        hoveredVal?.let { hovered[index] = it }
        pressedVal?.let { pressed[index] = it }
        backgroundColorVal?.let { backgroundColor[index] = it }
        pressedTintVal?.let { pressedTint[index] = it }
        clipLeftVal?.let { clipLeft[index] = it }
        clipTopVal?.let { clipTop[index] = it }
        clipRightVal?.let { clipRight[index] = it }
        clipBottomVal?.let { clipBottom[index] = it }
        styleTagVal?.let { styleTag[index] = it }
    }

    override fun grow(newSize: Int) {
        parent = parent.copyOf(newSize)
        renderMode = renderMode.copyOf(newSize)
        visible = visible.copyOf(newSize)
        enabled = enabled.copyOf(newSize)
        hovered = hovered.copyOf(newSize)
        pressed = pressed.copyOf(newSize)
        backgroundColor = backgroundColor.copyOf(newSize)
        pressedTint = pressedTint.copyOf(newSize)
        clipLeft = clipLeft.copyOf(newSize)
        clipTop = clipTop.copyOf(newSize)
        clipRight = clipRight.copyOf(newSize)
        clipBottom = clipBottom.copyOf(newSize)
        styleTag = styleTag.copyOf(newSize)
    }

    override fun swap(a: Int, b: Int) {
        parent.swap(a, b)
        renderMode.swap(a, b)
        visible.swap(a, b)
        enabled.swap(a, b)
        hovered.swap(a, b)
        pressed.swap(a, b)
        backgroundColor.swap(a, b)
        pressedTint.swap(a, b)
        clipLeft.swap(a, b)
        clipTop.swap(a, b)
        clipRight.swap(a, b)
        clipBottom.swap(a, b)
        styleTag.swap(a, b)
    }

    fun parent(entity: Entity): Entity = parent[indexOf(entity)]
    fun renderMode(entity: Entity): UiRenderMode = UiRenderMode.entries[renderMode[indexOf(entity)]]
    fun visible(entity: Entity): Boolean = visible[indexOf(entity)]
    fun enabled(entity: Entity): Boolean = enabled[indexOf(entity)]
    fun hovered(entity: Entity): Boolean = hovered[indexOf(entity)]
    fun pressed(entity: Entity): Boolean = pressed[indexOf(entity)]
    fun backgroundColor(entity: Entity): Color = backgroundColor[indexOf(entity)] ?: Color.Transparent
    fun pressedTint(entity: Entity): Color = pressedTint[indexOf(entity)] ?: Color.White
    fun clipLeft(entity: Entity): Float = clipLeft[indexOf(entity)]
    fun clipTop(entity: Entity): Float = clipTop[indexOf(entity)]
    fun clipRight(entity: Entity): Float = clipRight[indexOf(entity)]
    fun clipBottom(entity: Entity): Float = clipBottom[indexOf(entity)]
    fun styleTag(entity: Entity): String? = styleTag[indexOf(entity)]

    companion object {
        const val NO_PARENT: Entity = -1
    }
}
