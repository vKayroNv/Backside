package ru.kayron.dew.ui

import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.components.UiComponent
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.ecs.World
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Vector2

enum class Orientation {
    Vertical,
    Horizontal
}

open class Label internal constructor(
    world: World,
    entity: Entity,
    text: String = "",
    color: Color = Color.White,
    font: SpriteFont? = null
) : UiElement(world, entity) {
    var text: String
        get() = textComponent.text(entity)
        set(value) = textComponent.update(entity, textVal = value)

    var textColor: Color
        get() = textComponent.color(entity)
        set(value) = textComponent.update(entity, colorVal = value)

    init {
        backgroundColor = Color.Transparent
        setText(text, color, font = font)
    }
}

open class Button internal constructor(
    world: World,
    entity: Entity,
    text: String = "",
    color: Color = Color.White,
    backgroundColor: Color = Color.DarkSlateGray,
    val onClick: (() -> Unit)? = null
) : UiElement(world, entity) {
    override val canInteract: Boolean = true

    var text: String
        get() = textComponent.text(entity)
        set(value) = textComponent.update(entity, textVal = value)

    init {
        this.backgroundColor = backgroundColor
        setText(text, color)
    }

    override fun onPointerReleasedInside() {
        onClick?.invoke()
    }
}

open class StackView internal constructor(
    world: World,
    entity: Entity,
    var orientation: Orientation = Orientation.Vertical,
    var spacing: Float = 0f,
    backgroundColor: Color = Color.Transparent
) : UiElement(world, entity) {
    init {
        this.backgroundColor = backgroundColor
    }
}

class ListView internal constructor(
    world: World,
    entity: Entity,
    spacing: Float = 0f,
    backgroundColor: Color = Color.Transparent
) : StackView(world, entity, Orientation.Vertical, spacing, backgroundColor)

class ScrollView internal constructor(
    world: World,
    entity: Entity,
    var scrollOffset: Float = 0f,
    backgroundColor: Color = Color.Transparent
) : UiElement(world, entity) {
    override val canInteract: Boolean = true
    var contentHeight: Float = 0f
    private var dragStartY: Float = 0f
    private var dragStartOffset: Float = 0f

    init {
        this.backgroundColor = backgroundColor
    }

    override fun onPointerDrag(localPosition: Vector2) {
        val dy = localPosition.y - dragStartY
        scrollOffset = (dragStartOffset - dy).coerceAtLeast(0f)
    }

    override fun onPointerReleasedInside() {
        dragStartY = 0f
        dragStartOffset = 0f
    }

    internal fun captureDragStart(localY: Float) {
        dragStartY = localY
        dragStartOffset = scrollOffset
    }
}

class GridView internal constructor(
    world: World,
    entity: Entity,
    var columns: Int = 1,
    var rowSpacing: Float = 0f,
    var columnSpacing: Float = 0f,
    backgroundColor: Color = Color.Transparent
) : UiElement(world, entity) {
    init {
        this.backgroundColor = backgroundColor
    }
}

class CheckBox internal constructor(
    world: World,
    entity: Entity,
    text: String = "",
    var checked: Boolean = false,
    var onChanged: ((Boolean) -> Unit)? = null
) : UiElement(world, entity) {
    override val canInteract: Boolean = true
    private var caption: String = text

    init {
        backgroundColor = Color.Transparent
        syncStyleTag()
        setText("  $caption", Color.White, Vector2(28f, 0f))
    }

    override fun onPointerReleasedInside() {
        checked = !checked
        syncStyleTag()
        onChanged?.invoke(checked)
    }

    private fun syncStyleTag() {
        styleTag = if (checked) "checkbox_checked" else "checkbox_unchecked"
    }
}

class RadioButton internal constructor(
    world: World,
    entity: Entity,
    text: String = "",
    var group: String = "",
    var selected: Boolean = false,
    var onSelected: (() -> Unit)? = null
) : UiElement(world, entity) {
    override val canInteract: Boolean = true
    private var caption: String = text

    init {
        backgroundColor = Color.Transparent
        syncStyleTag()
        setText("  $caption", Color.White, Vector2(28f, 0f))
    }

    override fun onPointerReleasedInside() {
        selected = true
        syncStyleTag()
        onSelected?.invoke()
    }

    internal fun setSelectedFromGroup(value: Boolean) {
        selected = value
        syncStyleTag()
    }

    private fun syncStyleTag() {
        styleTag = if (selected) "radio_selected" else "radio_unselected"
    }
}

class DropMenu internal constructor(
    world: World,
    entity: Entity,
    items: List<String> = emptyList(),
    var selectedIndex: Int = 0,
    var expanded: Boolean = false,
    var onSelected: ((Int, String) -> Unit)? = null
) : UiElement(world, entity) {
    override val canInteract: Boolean = true
    var items: List<String> = items
        set(value) {
            field = value
            if (selectedIndex >= value.size) selectedIndex = 0
            updateText()
        }
    private var dropdownButtons: MutableList<UiElement> = mutableListOf()

    init {
        backgroundColor = Color.DarkSlateGray
        styleTag = "dropdown"
        updateText()
    }

    override fun onPointerReleasedInside() {
        if (items.isEmpty()) return
        expanded = !expanded
        if (expanded) {
            showDropdown()
        } else {
            hideDropdown()
        }
    }

    private fun showDropdown() {
        val itemHeight = height
        items.forEachIndexed { index, item ->
            val eid = world.entityManager.create()
            world.componentManager.get<TransformComponent>().add(eid)
            world.componentManager.get<UiComponent>().add(
                entity = eid,
                renderModeVal = renderMode,
                backgroundColorVal = Color(40, 46, 56)
            )
            val btn = DropMenuItem(world, eid, item, index, this)
            btn.setBounds(0f, height + index * itemHeight, width, itemHeight)
            btn.visible = true
            btn.styleTag = "dropdown_item"
            btn.uiManager = uiManager
            uiManager?.register(btn)
            add(btn)
            dropdownButtons.add(btn)
        }
    }

    private fun hideDropdown() {
        dropdownButtons.forEach {
            remove(it)
            uiManager?.remove(it)
        }
        dropdownButtons.clear()
    }

    internal fun selectItem(index: Int) {
        selectedIndex = index
        expanded = false
        hideDropdown()
        updateText()
        onSelected?.invoke(index, items[index])
    }

    private fun updateText() {
        val selected = items.getOrNull(selectedIndex) ?: ""
        setText(selected, Color.White, Vector2(8f, 0f))
    }
}

internal class DropMenuItem internal constructor(
    world: World,
    entity: Entity,
    text: String,
    val index: Int,
    private val dropMenu: DropMenu
) : UiElement(world, entity) {
    override val canInteract: Boolean = true
    init {
        styleTag = "dropdown_item"
        setText(text, Color.White, Vector2(8f, 0f))
    }
    override fun onPointerReleasedInside() {
        dropMenu.selectItem(index)
    }
}

class Slider internal constructor(
    world: World,
    entity: Entity,
    var min: Float = 0f,
    var max: Float = 1f,
    value: Float = 0f,
    var onChanged: ((Float) -> Unit)? = null
) : UiElement(world, entity) {
    override val canInteract: Boolean = true
    var value: Float = value.coerceIn(min, max)
        set(v) {
            field = v
            valueCache[entity] = (v - min) / (max - min).coerceAtLeast(0.001f)
        }

    init {
        backgroundColor = Color.Transparent
        styleTag = "slider"
        valueCache[entity] = (value - min) / (max - min).coerceAtLeast(0.001f)
    }

    override fun onPointerDrag(localPosition: Vector2) {
        val trackWidth = (width - thumbSize).takeIf { it > 0f } ?: return
        val amount = ((localPosition.x - thumbSize / 2f) / trackWidth).coerceIn(0f, 1f)
        value = min + (max - min) * amount
        onChanged?.invoke(value)
    }

    override fun onPointerReleasedInside() {
        onChanged?.invoke(value)
    }

    companion object {
        const val thumbSize: Float = 20f
        internal val valueCache = mutableMapOf<Int, Float>()
        fun getNormalized(entity: Int): Float = valueCache[entity] ?: 0f
    }
}