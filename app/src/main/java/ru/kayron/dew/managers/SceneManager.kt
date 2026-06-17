package ru.kayron.dew.managers

import ru.kayron.cargo.CargoContainer
import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.scene.Scene

class SceneManager(
    private val game: Game
) {
    val scope: CargoContainer = game.cargo.scope()
    
    private val storage = hashMapOf<String, Scene>()
    private val initializedScenes = hashSetOf<Scene>()
    private var activeScene: Scene? = null
    private var initialized = false
    
    fun initialize() {
        storage.values.forEach {
            initializeScene(it)
        }

        initialized = true
    }
    
    fun add(name: String, scene: Scene): Scene {
        val stored = storage.getOrPut(name) { scene }
        if (initialized) {
            initializeScene(stored)
        }
        return stored
    }
    
    fun switchTo(name: String) {
        val previous = activeScene
        activeScene?.onExit()
        
        val next = storage[name] ?: error("Scene not found: $name")
        activeScene = next
        next.onEnter(previous)
    }
    
    fun update(gameTime: GameTime) = activeScene?.update(gameTime)
    
    fun draw(gameTime: GameTime) = activeScene?.draw(gameTime)

    fun reloadGraphicsResources() {
        activeScene?.reloadGraphicsResources()
    }

    private fun initializeScene(scene: Scene) {
        if (!initializedScenes.add(scene)) return
        scene.initialize()
    }
}
