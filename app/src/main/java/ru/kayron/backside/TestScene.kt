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
            orientation = Orientation.Horizontal,
            backgroundColor = Color(18, 20, 26),
            renderMode = UiRenderMode.Static
        )

        // --- Vertical ScrollView ---
        val leftPanel = uiManager.stackView(
            x = 32f,
            y = 32f,
            width = 880f,
            height = 1000f,
            orientation = Orientation.Vertical,
            spacing = 12f,
            backgroundColor = Color(24, 28, 36),
            parent = root
        )

        uiManager.label(
            text = "Vertical ScrollView",
            x = 16f,
            width = 840f,
            height = 48f,
            color = Color.Yellow,
            parent = leftPanel
        )

        val verticalScroll = uiManager.scrollView(
            x = 16f,
            width = 840f,
            height = 860f,
            backgroundColor = Color(16, 20, 28),
            parent = leftPanel
        )

        val itemColors = listOf(
            Color.SteelBlue, Color.SeaGreen, Color.DarkSlateBlue,
            Color.DarkCyan, Color.DarkMagenta, Color.SaddleBrown,
            Color.Teal, Color.MediumVioletRed, Color.RoyalBlue,
            Color.DimGray, Color.OliveDrab, Color.IndianRed
        )

        repeat(24) { index ->
            val bg = itemColors[index % itemColors.size]
            val item = uiManager.label(
                text = "Vertical item ${index + 1}",
                x = 16f,
                y = index * 64f,
                width = 800f,
                height = 56f,
                color = Color.White,
                parent = verticalScroll
            )
            item.backgroundColor = bg
        }

        // --- Horizontal ScrollView ---
        val rightPanel = uiManager.stackView(
            y = 32f,
            width = 920f,
            height = 1000f,
            orientation = Orientation.Vertical,
            spacing = 12f,
            backgroundColor = Color(30, 36, 46),
            parent = root
        )

        uiManager.label(
            text = "Horizontal ScrollView",
            x = 16f,
            width = 880f,
            height = 48f,
            color = Color.Cyan,
            parent = rightPanel
        )

        val horizontalScroll = uiManager.scrollView(
            x = 16f,
            width = 880f,
            height = 200f,
            backgroundColor = Color(16, 20, 28),
            parent = rightPanel
        )

        val hItemColors = listOf(
            Color.DarkRed, Color.DodgerBlue, Color.DarkGreen,
            Color.DarkOrange, Color.DeepPink, Color.DarkCyan
        )

        repeat(16) { index ->
            val bg = hItemColors[index % hItemColors.size]
            val item = uiManager.label(
                text = "H${index + 1}",
                x = index * 120f,
                y = 8f,
                width = 108f,
                height = 180f,
                color = Color.White,
                parent = horizontalScroll
            )
            item.backgroundColor = bg
        }
    }
}
