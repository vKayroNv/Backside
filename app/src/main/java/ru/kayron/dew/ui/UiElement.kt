package ru.kayron.dew.ui

import ru.kayron.dew.components.SpriteComponent
import ru.kayron.dew.components.SingleSpriteComponent
import ru.kayron.dew.components.TextComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.components.UiComponent
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.ecs.World
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector2

open class UiElement internal constructor(
    internal val world: World,
    val entity: Entity
) {
    var uiManager: UiManager? = null
    internal set
    internal val mutableChildren = mutableListOf<UiElement>()
    val children: List<UiElement> get() = mutableChildren

    internal var localX: Float = 0f
        private set
    internal var localY: Float = 0f
        private set
    internal var localWidth: Float = 0f
        private set
    internal var localHeight: Float = 0f
        private set

    var parent: UiElement? = null
        private set

    open val canInteract: Boolean = false
    
    internal val isClipContainer: Boolean
        get() = this is ScrollView

    var renderMode: UiRenderMode
        get() = ui.renderMode(entity)
        set(value) = ui.update(entity, renderModeVal = value)

    var visible: Boolean
        get() = ui.visible(entity)
        set(value) = ui.update(entity, visibleVal = value)

    var enabled: Boolean
        get() = ui.enabled(entity)
        set(value) = ui.update(entity, enabledVal = value)

    var x: Float
        get() = transform.x(entity)
        set(value) {
            localX = value
            transform.update(entity, xVal = value)
        }

    var y: Float
        get() = transform.y(entity)
        set(value) {
            localY = value
            transform.update(entity, yVal = value)
        }

    var width: Float
        get() = transform.width(entity)
        set(value) {
            localWidth = value
            transform.update(entity, widthVal = value)
        }

    var height: Float
        get() = transform.height(entity)
        set(value) {
            localHeight = value
            transform.update(entity, heightVal = value)
        }

    var backgroundColor: Color
        get() = ui.backgroundColor(entity)
        set(value) = ui.update(entity, backgroundColorVal = value)

    var styleTag: String?
        get() = ui.styleTag(entity)
        set(value) = ui.update(entity, styleTagVal = value)

    var clipLeft: Float
        get() = ui.clipLeft(entity)
        set(value) = ui.update(entity, clipLeftVal = value)
    var clipTop: Float
        get() = ui.clipTop(entity)
        set(value) = ui.update(entity, clipTopVal = value)
    var clipRight: Float
        get() = ui.clipRight(entity)
        set(value) = ui.update(entity, clipRightVal = value)
    var clipBottom: Float
        get() = ui.clipBottom(entity)
        set(value) = ui.update(entity, clipBottomVal = value)

    val bounds: Rectangle
        get() = Rectangle(x.toInt(), y.toInt(), width.toInt(), height.toInt())

    internal val transform: TransformComponent
        get() = world.componentManager.get()

    internal val ui: UiComponent
        get() = world.componentManager.get()

    internal val textComponent: TextComponent
        get() = world.componentManager.get()

    internal fun attachTo(parent: UiElement?) {
        this.parent?.mutableChildren?.remove(this)
        this.parent = parent
        parent?.mutableChildren?.add(this)
        ui.update(entity, parentVal = parent?.entity ?: UiComponent.NO_PARENT)
    }

    fun add(child: UiElement): UiElement {
        child.attachTo(this)
        return child
    }

    fun remove(child: UiElement) {
        if (child.parent == this) {
            child.attachTo(null)
        }
    }

    fun setBounds(x: Float, y: Float, width: Float, height: Float): UiElement {
        localX = x
        localY = y
        localWidth = width
        localHeight = height
        transform.update(
            entity = entity,
            xVal = x,
            yVal = y,
            widthVal = width,
            heightVal = height
        )
        return this
    }

    fun setClipRect(left: Float, top: Float, right: Float, bottom: Float) {
        ui.update(entity, clipLeftVal = left, clipTopVal = top, clipRightVal = right, clipBottomVal = bottom)
    }

    fun resetClipRect() {
        setClipRect(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    }

    internal fun arrange(x: Float, y: Float, width: Float = localWidth, height: Float = localHeight) {
        transform.update(
            entity = entity,
            xVal = x,
            yVal = y,
            widthVal = width,
            heightVal = height
        )
    }

    fun setSprite(
        filename: String,
        rows: Int = 1,
        columns: Int = 1,
        scale: Vector2 = Vector2.One
    ): UiElement {
        val sprites = world.componentManager.get<SpriteComponent>()
        if (!sprites.hasEntity(entity)) {
            sprites.add(
                entity = entity,
                filenameVal = filename,
                rowsVal = rows,
                columnsVal = columns,
                scaleXVal = scale.x,
                scaleYVal = scale.y
            )
        }
        val single = world.componentManager.get<SingleSpriteComponent>()
        if (!single.hasEntity(entity)) {
            single.add(entity)
        }
        return this
    }

    internal fun setText(
        text: String,
        color: Color = Color.White,
        offset: Vector2 = Vector2.Zero,
        font: SpriteFont? = null
    ) {
        if (!textComponent.hasEntity(entity)) {
            textComponent.add(entity, text, color, offset.x, offset.y, font)
        } else {
            textComponent.update(entity, text, color, offset.x, offset.y, font)
        }
    }

    open fun onPointerReleasedInside() {}
    open fun onPointerDrag(localPosition: Vector2) {}
}
