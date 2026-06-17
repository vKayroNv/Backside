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
            width = 2436f,
            height = 1080f,
            orientation = Orientation.Horizontal,
            backgroundColor = Color(18, 20, 26),
            renderMode = UiRenderMode.Static
        )

        val verticalScroll = uiManager.scrollView(
            x = 16f,
            y = 16f,
            width = 2436f - 32f,
            height = 1080f - 32f,
            backgroundColor = Color(16, 20, 28),
            parent = root
        )
        
        val y = 32f
        
        val cameraMovementTestButton = uiManager.button(
            text = "Camera movement",
            x = 32f,
            y = y,
            width = 800f,
            height = 100f
        ) {
            sceneManager.switchTo("cameraMovement")
        }
    }
}
