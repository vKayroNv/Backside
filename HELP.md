Теперь видно главную проблему.

Даже если исправить UiLayoutSystem, clipping всё равно не заработает, потому что RenderSystem рисует UI не по дереву, а несколькими проходами:

drawUiFallbacks(...)
drawThemedControls(...)
drawText(...)
drawDropdownItems(...)

и каждый проход перебирает:

world.componentManager.getEntitiesWith<UiComponent, TransformComponent>()

В результате теряется структура:

ScrollView
 └─ StackView
     ├─ Label
     ├─ Button
     └─ Label

и невозможно понять, когда нужно вызвать:

pushClip(scrollRect)
...
popClip()


---

Минимальный набор изменений

Нужно изменить только:

1. RenderSystem


2. UiLayoutSystem



UiManager, ScrollView, Button, Label, GridView трогать не нужно.


---

Изменение №1

Добавь в UiElement:

internal val isClipContainer: Boolean
    get() = this is ScrollView

или проще проверяй через is ScrollView.


---

Изменение №2

В UiLayoutSystem.layoutScroll() оставляем как есть:

scroll.clipLeft = scrollX
scroll.clipTop = scrollY
scroll.clipRight = scrollX + scrollW
scroll.clipBottom = scrollY + scrollH

Это уже правильно.


---

Изменение №3 (главное)

В RenderSystem нужно отказаться от:

drawUiFallbacks(...)
drawText(...)
drawThemedControls(...)
drawDropdownItems(...)

для UI.

Вместо этого рисовать дерево рекурсивно.

Добавь:

private fun drawUiTree(
    element: UiElement,
    renderMode: UiRenderMode
)


---

Новый drawUiTree

private fun drawUiTree(
    element: UiElement,
    renderMode: UiRenderMode
) {
    if (!element.visible) return
    if (element.renderMode != renderMode) return

    if (element is ScrollView) {
        pushClip(
            Rectangle(
                element.x.toInt(),
                element.y.toInt(),
                element.width.toInt(),
                element.height.toInt()
            )
        )
    }

    drawElement(element)

    element.children.forEach {
        drawUiTree(it, renderMode)
    }

    if (element is ScrollView) {
        popClip()
    }
}


---

Новый drawElement

private fun drawElement(element: UiElement) {
    val entity = element.entity

    val transform = world.componentManager.get<TransformComponent>()
    val ui = world.componentManager.get<UiComponent>()
    val sprite = world.componentManager.get<SpriteComponent>()
    val text = world.componentManager.get<TextComponent>()

    if (sprite.hasEntity(entity)) {
        drawSprite(
            sprite,
            transform,
            ui,
            entity,
            0,
            0
        )
    } else {
        val color = ui.backgroundColor(entity)

        if (
            color.a > 0 &&
            transform.width(entity) > 0f &&
            transform.height(entity) > 0f
        ) {
            batch.draw(
                fallbackTexture,
                rectangle(transform, entity),
                color
            )
        }
    }

    drawControlDecoration(entity)

    if (text.hasEntity(entity)) {
        val value = text.text(entity)

        if (value.isNotEmpty()) {
            val font = text.font(entity) ?: defaultFont

            batch.drawString(
                font,
                value,
                transform.pos(entity) +
                    textOffset(
                        entity,
                        transform,
                        text,
                        font,
                        value
                    ),
                text.color(entity)
            )
        }
    }
}


---

Выделить код из drawThemedControls

Вместо большого метода сделай:

private fun drawControlDecoration(entity: Int)

и перенеси туда содержимое:

when(tag)

из drawThemedControls.


---

Новый draw()

После world-рендера:

beginStatic()

uiManager.roots.forEach {
    drawUiTree(it, UiRenderMode.Static)
}

batch.end()
endStatic()


---

Что получится

При дереве:

ScrollView
 └─ StackView
     ├─ Label
     ├─ Button
     └─ Label

рендер станет:

pushClip(scroll)

draw(stack)
draw(label)
draw(button)
draw(label)

popClip()

и весь контент за пределами ScrollView начнёт корректно отсекаться текущим glScissor() без изменения SpriteBatch. Это самый чистый вариант для твоей архитектуры.