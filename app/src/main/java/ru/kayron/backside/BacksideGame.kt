package ru.kayron.backside

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.input.Keyboard
import ru.kayron.dew.input.Keys
import ru.kayron.dew.managers.SceneManager

open class BacksideGame : Game() {
    private lateinit var sceneManager: SceneManager

    override fun reloadGraphicsResources() {
        super.reloadGraphicsResources()
        content.unload()
        sceneManager.reloadGraphicsResources()
    }
    
    override fun loadContent() {
        sceneManager = SceneManager(this)
        sceneManager.add("test", TestScene(sceneManager))
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
