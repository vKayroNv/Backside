package ru.kayron.dew.systems

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.ecs.GameSystem
import ru.kayron.dew.ui.GridView
import ru.kayron.dew.ui.Orientation
import ru.kayron.dew.ui.ScrollView
import ru.kayron.dew.ui.StackView
import ru.kayron.dew.ui.UiElement
import ru.kayron.dew.ui.UiManager

class UiLayoutSystem(
    game: Game,
    private val uiManager: UiManager
) : GameSystem(game) {

    override val updateOrder: Int = -20

    override fun update(gameTime: GameTime) {
        uiManager.roots.forEach {
            it.arrange(it.localX, it.localY, it.localWidth, it.localHeight)
            it.resetClipRect()
            layout(it)
        }
    }

    private fun layout(element: UiElement) {
        when (element) {
            is StackView -> layoutStack(element)
            is GridView -> layoutGrid(element)
            is ScrollView -> layoutScroll(element)
            else -> layoutChildren(element)
        }
    }

    private fun layoutChildren(parent: UiElement) {
        parent.children.forEach { child ->
            child.arrange(
                parent.x + child.localX,
                parent.y + child.localY,
                child.localWidth,
                child.localHeight
            )
            child.renderMode = parent.renderMode
            if (child.styleTag != "dropdown_item") {
                child.clipLeft = parent.clipLeft
                child.clipTop = parent.clipTop
                child.clipRight = parent.clipRight
                child.clipBottom = parent.clipBottom
            }
            layout(child)
        }
    }

    private fun layoutStack(stack: StackView) {
        var offset = 0f
        stack.children.forEach { child ->
            when (stack.orientation) {
                Orientation.Vertical -> {
                    val childWidth = child.localWidth.takeIf { it > 0f } ?: stack.width
                    child.arrange(stack.x + child.localX, stack.y + offset + child.localY, childWidth, child.localHeight)
                    offset += child.height + stack.spacing
                }
                Orientation.Horizontal -> {
                    val childHeight = child.localHeight.takeIf { it > 0f } ?: stack.height
                    child.arrange(stack.x + offset + child.localX, stack.y + child.localY, child.localWidth, childHeight)
                    offset += child.width + stack.spacing
                }
            }
            child.renderMode = stack.renderMode
            child.clipLeft = stack.clipLeft
            child.clipTop = stack.clipTop
            child.clipRight = stack.clipRight
            child.clipBottom = stack.clipBottom
            layout(child)
        }
    }

    private fun layoutGrid(grid: GridView) {
        val columns = grid.columns.coerceAtLeast(1)
        val availableWidth = grid.width.takeIf { it > 0f } ?: return
        val cellWidth = (availableWidth - grid.columnSpacing * (columns - 1)) / columns

        grid.children.forEachIndexed { index, child ->
            val column = index % columns
            val row = index / columns
            val childWidth = child.localWidth.takeIf { it > 0f } ?: cellWidth
            child.arrange(
                grid.x + column * (cellWidth + grid.columnSpacing) + child.localX,
                grid.y + row * (child.localHeight + grid.rowSpacing) + child.localY,
                childWidth,
                child.localHeight
            )
            child.renderMode = grid.renderMode
            child.clipLeft = grid.clipLeft
            child.clipTop = grid.clipTop
            child.clipRight = grid.clipRight
            child.clipBottom = grid.clipBottom
            layout(child)
        }
    }

    private fun layoutScroll(scroll: ScrollView) {
        val scrollX = scroll.x
        val scrollY = scroll.y
        val scrollW = scroll.width
        val scrollH = scroll.height

        var maxContentX = scrollW
        var maxContentY = scrollH
        scroll.children.forEach { child ->
            val childRight = child.localX + (child.localWidth.takeIf { it > 0f } ?: scrollW)
            val childBottom = child.localY + child.localHeight
            if (childRight > maxContentX) maxContentX = childRight
            if (childBottom > maxContentY) maxContentY = childBottom
        }
        scroll.contentWidth = maxContentX
        scroll.contentHeight = maxContentY

        scroll.clipLeft = scrollX
        scroll.clipTop = scrollY
        scroll.clipRight = scrollX + scrollW
        scroll.clipBottom = scrollY + scrollH

        scroll.children.forEach { child ->
            val childWidth = child.localWidth.takeIf { it > 0f } ?: scrollW
            child.arrange(
                scrollX + child.localX - scroll.scrollOffsetX,
                scrollY + child.localY - scroll.scrollOffset,
                childWidth,
                child.localHeight
            )
            child.renderMode = scroll.renderMode
            child.clipLeft = scroll.clipLeft
            child.clipTop = scroll.clipTop
            child.clipRight = scroll.clipRight
            child.clipBottom = scroll.clipBottom
            layout(child)
        }
    }
}
