package ru.kayron.backside.scenes

import ru.kayron.dew.managers.SceneManager
import ru.kayron.dew.math.Color
import ru.kayron.dew.scene.Scene
import ru.kayron.dew.ui.Orientation
import ru.kayron.dew.ui.UiRenderMode

class TestMainScene(
    sceneManager: SceneManager
) : Scene(sceneManager) {

    override fun onInitialize() {
        val root = uiManager.stackView(
            x = 0f,
            y = 0f,
            width = game.graphicsDevice.viewport.width.toFloat(),
            height = game.graphicsDevice.viewport.height.toFloat(),
            orientation = Orientation.Horizontal,
            backgroundColor = Color(18, 20, 26),
            renderMode = UiRenderMode.Static
        )

        val verticalScroll = uiManager.scrollView(
            x = 16f,
            y = 16f,
            width = root.width - 232f,
            height = root.height - 32f,
            backgroundColor = Color(16, 20, 28),
            parent = root
        )
        
        var y = 32f
        
        val cameraMovementTestButton = uiManager.button(
            text = "Camera movement",
            x = 32f,
            y = y,
            width = 800f,
            height = 100f,
            parent = verticalScroll
        ) {
            sceneManager.switchTo("cameraMovement")
        }

        y += cameraMovementTestButton.height + 16f

        val exitButton = uiManager.button(
            text = "Exit",
            x = 32f,
            y = y,
            width = 800f,
            height = 100f,
            parent = verticalScroll
        ) {
            Runtime.getRuntime().exit(0)
        }
    }
}
