package ru.kayron.dew.ui

import ru.kayron.dew.components.SingleSpriteComponent
import ru.kayron.dew.components.SpriteComponent
import ru.kayron.dew.components.TextComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.components.UiComponent
import ru.kayron.dew.ecs.World
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.math.Color

class UiManager(
    private val world: World
) {
    private val elementsByEntity = linkedMapOf<Int, UiElement>()
    val elements: Collection<UiElement> get() = elementsByEntity.values
    val roots: List<UiElement> get() = elements.filter { it.parent == null }

    fun add(element: UiElement): UiElement = element

    fun register(element: UiElement) {
        elementsByEntity[element.entity] = element
    }

    fun remove(element: UiElement) {
        element.parent?.remove(element)
        element.children.toList().forEach(::remove)
        elementsByEntity.remove(element.entity)
        world.componentManager.removeEntity(element.entity)
        world.entityManager.remove(element.entity)
    }

    fun clear() {
        elements.toList().forEach(::remove)
    }

    fun element(entity: Int): UiElement? = elementsByEntity[entity]

    fun label(
        text: String,
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 0f,
        height: Float = 0f,
        color: Color = Color.White,
        renderMode: UiRenderMode = UiRenderMode.Static,
        font: SpriteFont? = null,
        parent: UiElement? = null
    ): Label = create(parent, renderMode, x, y, width, height) {
        Label(world, it, text, color, font)
    }

    fun button(
        text: String = "",
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 160f,
        height: Float = 48f,
        backgroundColor: Color = Color.DarkSlateGray,
        textColor: Color = Color.White,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null,
        onClick: (() -> Unit)? = null
    ): Button = create(parent, renderMode, x, y, width, height) {
        Button(world, it, text, textColor, backgroundColor, onClick)
    }

    fun stackView(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 0f,
        height: Float = 0f,
        orientation: Orientation = Orientation.Vertical,
        spacing: Float = 0f,
        backgroundColor: Color = Color.Transparent,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null
    ): StackView = create(parent, renderMode, x, y, width, height) {
        StackView(world, it, orientation, spacing, backgroundColor)
    }

    fun listView(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 0f,
        height: Float = 0f,
        spacing: Float = 0f,
        backgroundColor: Color = Color.Transparent,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null
    ): ListView = create(parent, renderMode, x, y, width, height) {
        ListView(world, it, spacing, backgroundColor)
    }

    fun scrollView(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 0f,
        height: Float = 0f,
        scrollOffset: Float = 0f,
        backgroundColor: Color = Color.Transparent,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null
    ): ScrollView = create(parent, renderMode, x, y, width, height) {
        ScrollView(world, it, scrollOffset, backgroundColor)
    }

    fun gridView(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 0f,
        height: Float = 0f,
        columns: Int = 1,
        rowSpacing: Float = 0f,
        columnSpacing: Float = 0f,
        backgroundColor: Color = Color.Transparent,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null
    ): GridView = create(parent, renderMode, x, y, width, height) {
        GridView(world, it, columns, rowSpacing, columnSpacing, backgroundColor)
    }

    fun checkBox(
        text: String = "",
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 160f,
        height: Float = 36f,
        checked: Boolean = false,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null,
        onChanged: ((Boolean) -> Unit)? = null
    ): CheckBox = create(parent, renderMode, x, y, width, height) {
        CheckBox(world, it, text, checked, onChanged)
    }

    fun radioButton(
        text: String = "",
        group: String = "",
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 160f,
        height: Float = 36f,
        selected: Boolean = false,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null,
        onSelected: (() -> Unit)? = null
    ): RadioButton = create(parent, renderMode, x, y, width, height) {
        RadioButton(world, it, text, group, selected, onSelected)
    }

    fun dropMenu(
        items: List<String> = emptyList(),
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 160f,
        height: Float = 44f,
        selectedIndex: Int = 0,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null,
        onSelected: ((Int, String) -> Unit)? = null
    ): DropMenu = create(parent, renderMode, x, y, width, height) {
        DropMenu(world, it, items, selectedIndex, onSelected = onSelected)
    }

    fun slider(
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 160f,
        height: Float = 36f,
        min: Float = 0f,
        max: Float = 1f,
        value: Float = min,
        renderMode: UiRenderMode = UiRenderMode.Static,
        parent: UiElement? = null,
        onChanged: ((Float) -> Unit)? = null
    ): Slider = create(parent, renderMode, x, y, width, height) {
        Slider(world, it, min, max, value, onChanged)
    }

    private fun <T : UiElement> create(
        parent: UiElement?,
        renderMode: UiRenderMode,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        factory: (Int) -> T
    ): T {
        val entity = world.entityManager.create()
        world.componentManager.get<TransformComponent>().add(entity, x, y, widthVal = width, heightVal = height)
        world.componentManager.get<UiComponent>().add(
            entity = entity,
            parentVal = parent?.entity ?: UiComponent.NO_PARENT,
            renderModeVal = renderMode
        )
        val element = factory(entity)
        element.uiManager = this
        elementsByEntity[entity] = element
        element.setBounds(x, y, width, height)
        element.attachTo(parent)
        ensureDefaultVisual(element)
        return element
    }

    private fun ensureDefaultVisual(element: UiElement) {
        val sprite = world.componentManager.get<SpriteComponent>()
        val single = world.componentManager.get<SingleSpriteComponent>()
        if (!sprite.hasEntity(element.entity) && element.backgroundColor.a > 0) {
            if (!single.hasEntity(element.entity)) {
                single.add(element.entity)
            }
        }
        world.componentManager.get<TextComponent>()
    }
}
