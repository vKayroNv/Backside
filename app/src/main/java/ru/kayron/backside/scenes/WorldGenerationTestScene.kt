package ru.kayron.backside.scenes

import ru.kayron.backside.systems.CameraMovementSystem
import ru.kayron.backside.systems.WorldGenerationSystem
import ru.kayron.dew.managers.SceneManager
import ru.kayron.dew.scene.Scene
import ru.kayron.dew.components.*

class WorldGenerationTestScene(
    sceneManager: SceneManager
) : Scene(sceneManager) {

    override fun onInitialize() {
        systemManager.add(scope.get<CameraMovementSystem>())
        systemManager.add(scope.get<WorldGenerationSystem>())
        
        uiManager.button(
            text = "Назад",
            x = 50f,
            y = 50f,
            width = 200f,
            height = 100f
        ) {
            sceneManager.switchTo("test")
        }
        
        val camera = entityManager.create()
        componentManager.get<CameraComponent>().add(
            camera,
            1f,
            sceneManager.game.graphicsDevice.viewport.width.toFloat(),
            sceneManager.game.graphicsDevice.viewport.height.toFloat()
        )
        componentManager.get<CameraComponent>().setActive(camera)
        componentManager.get<TransformComponent>().add(camera)
    }
}
