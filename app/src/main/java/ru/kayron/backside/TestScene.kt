package ru.kayron.backside

import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.AnimatedSpriteComponent
import ru.kayron.dew.components.SingleSpriteComponent
import ru.kayron.dew.components.SpriteComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.managers.SceneManager
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector2
import ru.kayron.dew.scene.Scene
import ru.kayron.dew.ui.Button
import ru.kayron.dew.ui.Label
import ru.kayron.dew.ui.UiRenderMode

class TestScene(
    sceneManager: SceneManager
) : Scene(sceneManager) {

    override fun onInitialize() {
        val camera = entityManager.create()
        componentManager.get<CameraComponent>().add(camera)
        componentManager.get<CameraComponent>().setActive(camera)
        componentManager.get<TransformComponent>().add(camera)

        val x = intArrayOf(0, 0, 1, 1)
        val y = intArrayOf(0, 1, 0, 1)
        
        for (i in 0..3) {
            val test = entityManager.create()
            componentManager.get<SpriteComponent>().add(
                entity = test,
                filenameVal = "test1.png",
                rowsVal = 2,
                columnsVal = 2,
                scaleXVal = 3f,
                scaleYVal = 3f
            )
            componentManager.get<SingleSpriteComponent>().add(
                test, x[i], y[i]
            )
            componentManager.get<TransformComponent>().add(
                test, 100f * i, 0f
            )
        }
        
        val test2 = entityManager.create()
        componentManager.get<SpriteComponent>().add(
            entity = test2,
            filenameVal = "test2.png",
            rowsVal = 1,
            columnsVal = 4,
            scaleXVal = 10f,
            scaleYVal = 10f
        )
        componentManager.get<AnimatedSpriteComponent>().add(
            test2, 3, 5f
        )
        componentManager.get<TransformComponent>().add(
            test2, 0f, 100f
        )
        
        val staticLabel = uiManager.add(
            Label(
                text = "Static label",
                position = Vector2(32f, 32f),
                color = Color.Yellow,
                renderMode = UiRenderMode.Static
            )
        )

        uiManager.add(
            Button(
                bounds = Rectangle(32, 88, 260, 72),
                text = "Static button",
                backgroundColor = Color.DarkSlateGray,
                textColor = Color.White,
                renderMode = UiRenderMode.Static,
                onClick = {
                    staticLabel.text = "Button clicked"
                }
            )
        )

        uiManager.add(
            Label(
                text = "World label",
                position = Vector2(-160f, -420f),
                color = Color.Cyan,
                renderMode = UiRenderMode.World
            )
        )
    }
}
