package ru.kayron.backside

import ru.kayron.dew.managers.SceneManager
import ru.kayron.dew.math.Color
import ru.kayron.dew.scene.Scene
import ru.kayron.dew.ui.Orientation
import ru.kayron.dew.ui.UiRenderMode

class TestScene(
    sceneManager: SceneManager
) : Scene(sceneManager) {

    override fun onInitialize() {
        val root = uiManager.stackView(
            x = 0f,
            y = 0f,
            width = 1920f,
            height = 1080f,
            orientation = Orientation.Vertical,
            backgroundColor = Color(255, 0, 0),
            renderMode = UiRenderMode.Static
        )

        val header = uiManager.stackView(
            width = 1920f,
            height = 96f,
            orientation = Orientation.Horizontal,
            spacing = 24f,
            backgroundColor = Color(28, 34, 44),
            parent = root
        )
        uiManager.label(
            text = "UI scene: 1920 x 1080",
            x = 32f,
            width = 520f,
            height = 96f,
            color = Color.Yellow,
            parent = header
        )

        val status = uiManager.label(
            text = "Select or drag controls",
            width = 620f,
            height = 96f,
            color = Color.LightGreen,
            parent = header
        )

        val body = uiManager.stackView(
            width = 1920f,
            height = 984f,
            orientation = Orientation.Horizontal,
            spacing = 24f,
            backgroundColor = Color(18, 20, 26),
            parent = root
        )

        val controls = uiManager.stackView(
            x = 32f,
            y = 32f,
            width = 420f,
            height = 900f,
            spacing = 14f,
            backgroundColor = Color(255, 0, 0),
            parent = body
        )
        uiManager.label(
            text = "StackView column",
            x = 20f,
            width = 360f,
            height = 48f,
            color = Color.Cyan,
            parent = controls
        )
        uiManager.button(
            text = "Button",
            x = 20f,
            width = 360f,
            height = 64f,
            backgroundColor = Color.DarkSlateGray,
            parent = controls,
            onClick = {
                status.text = "Button clicked"
            }
        )
        uiManager.checkBox(
            text = "CheckBox",
            x = 20f,
            width = 360f,
            height = 56f,
            parent = controls,
            onChanged = {
                status.text = "CheckBox = $it"
            }
        )
        uiManager.slider(
            x = 20f,
            width = 360f,
            height = 56f,
            min = 0f,
            max = 100f,
            value = 42f,
            parent = controls,
            onChanged = {
                status.text = "Slider = ${it.toInt()}"
            }
        )
        uiManager.dropMenu(
            items = listOf("DropMenu: red", "DropMenu: green", "DropMenu: blue"),
            x = 20f,
            width = 360f,
            height = 58f,
            parent = controls,
            onSelected = { _, value ->
                status.text = value
            }
        )

        val radioList = uiManager.listView(
            x = 20f,
            width = 360f,
            height = 144f,
            spacing = 8f,
            backgroundColor = Color(22, 26, 34),
            parent = controls
        )
        uiManager.radioButton(
            text = "RadioButton A",
            group = "demo",
            width = 340f,
            height = 52f,
            selected = true,
            parent = radioList,
            onSelected = {
                status.text = "RadioButton A"
            }
        )
        uiManager.radioButton(
            text = "RadioButton B",
            group = "demo",
            width = 340f,
            height = 52f,
            parent = radioList,
            onSelected = {
                status.text = "RadioButton B"
            }
        )

        val center = uiManager.stackView(
            y = 32f,
            width = 840f,
            height = 900f,
            spacing = 18f,
            backgroundColor = Color(24, 28, 36),
            parent = body
        )
        uiManager.label(
            text = "GridView",
            x = 24f,
            width = 760f,
            height = 48f,
            color = Color.Yellow,
            parent = center
        )
        val grid = uiManager.gridView(
            x = 24f,
            width = 760f,
            height = 360f,
            columns = 4,
            rowSpacing = 16f,
            columnSpacing = 16f,
            backgroundColor = Color(18, 22, 30),
            parent = center
        )
        repeat(12) { index ->
            uiManager.button(
                text = "Cell ${index + 1}",
                width = 0f,
                height = 104f,
                backgroundColor = when (index % 3) {
                    0 -> Color.SteelBlue
                    1 -> Color.SeaGreen
                    else -> Color.DarkSlateBlue
                },
                parent = grid,
                onClick = {
                    status.text = "Grid cell ${index + 1}"
                }
            )
        }

        uiManager.label(
            text = "ScrollView with local child positions",
            x = 24f,
            width = 760f,
            height = 48f,
            color = Color.LightGreen,
            parent = center
        )
        val scroll = uiManager.scrollView(
            x = 24f,
            width = 760f,
            height = 250f,
            scrollOffset = 0f,
            backgroundColor = Color(18, 22, 30),
            parent = center
        )
        repeat(8) { index ->
            uiManager.label(
                text = "Scroll content row ${index + 1}",
                x = 24f,
                y = index * 48f,
                width = 700f,
                height = 44f,
                color = if (index % 2 == 0) Color.White else Color.LightBlue,
                parent = scroll
            )
        }

        val right = uiManager.stackView(
            y = 32f,
            width = 520f,
            height = 900f,
            spacing = 18f,
            backgroundColor = Color(30, 36, 46),
            parent = body
        )
        uiManager.label(
            text = "Nested views",
            x = 24f,
            width = 460f,
            height = 48f,
            color = Color.Cyan,
            parent = right
        )

        val nested = uiManager.stackView(
            x = 24f,
            width = 460f,
            height = 260f,
            spacing = 12f,
            backgroundColor = Color(20, 24, 32),
            parent = right
        )
        val nestedRow = uiManager.stackView(
            x = 18f,
            width = 420f,
            height = 66f,
            orientation = Orientation.Horizontal,
            spacing = 12f,
            backgroundColor = Color(38, 46, 58),
            parent = nested
        )
        uiManager.button(
            text = "Left",
            width = 198f,
            height = 66f,
            backgroundColor = Color.DarkCyan,
            parent = nestedRow,
            onClick = {
                status.text = "Nested left"
            }
        )
        uiManager.button(
            text = "Right",
            width = 198f,
            height = 66f,
            backgroundColor = Color.DarkMagenta,
            parent = nestedRow,
            onClick = {
                status.text = "Nested right"
            }
        )

        val texturedGrid = uiManager.gridView(
            x = 18f,
            width = 420f,
            height = 150f,
            columns = 2,
            rowSpacing = 12f,
            columnSpacing = 12f,
            backgroundColor = Color(16, 20, 28),
            parent = nested
        )
        repeat(4) { index ->
            uiManager.button(
                text = "Sprite ${index + 1}",
                width = 0f,
                height = 64f,
                backgroundColor = Color.White,
                parent = texturedGrid,
                onClick = {
                    status.text = "Sprite-backed UI ${index + 1}"
                }
            ).setSprite("test1.png", rows = 2, columns = 2)
        }

        uiManager.label(
            text = "All rectangles are UiElement instances. Position and size come from TransformComponent.",
            x = 24f,
            width = 460f,
            height = 120f,
            color = Color.LightGray,
            parent = right
        )
    }
}
