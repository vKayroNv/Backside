package ru.kayron.backside

import ru.kayron.cargo.module
import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.input.Keyboard
import ru.kayron.dew.input.Keys
import ru.kayron.dew.managers.SceneManager
import ru.kayron.backside.scenes.TestMainScene
import ru.kayron.backside.scenes.CameraMovementTestScene
import ru.kayron.backside.systems.CameraMovementSystem

open class BacksideGame : Game() {
    private lateinit var sceneManager: SceneManager

    override fun reloadGraphicsResources() {
        super.reloadGraphicsResources()
        content.unload()
        sceneManager.reloadGraphicsResources()
    }
    
    override fun loadContent() {
        cargo.load(module {
            singleton { SceneManager(this@BacksideGame) }
            singleton { TestMainScene(get()) }
            singleton { CameraMovementTestScene(get()) }
            scoped { CameraMovementSystem(get(), get()) }
        })

        sceneManager = cargo.get()
        sceneManager.add("test", cargo.get<TestMainScene>())
        sceneManager.add("cameraMovement", cargo.get<CameraMovementTestScene>())
        sceneManager.initialize()
        sceneManager.switchTo("test")
    }

    override fun update(gameTime: GameTime) {
        if (Keyboard.getState().isKeyDown(Keys.Escape)) {
            exit()
        }
        
        sceneManager.update(gameTime)
        
        super.update(gameTime)
    }

    override fun draw(gameTime: GameTime) {
        sceneManager.draw(gameTime)
        
        super.draw(gameTime)
    }
}
