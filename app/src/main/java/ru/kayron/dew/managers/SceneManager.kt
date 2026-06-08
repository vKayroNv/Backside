package ru.kayron.dew.managers

import ru.kayron.cargo.CargoContainer
import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.AnimatedSpriteComponent
import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.SingleSpriteComponent
import ru.kayron.dew.components.SpriteComponent
import ru.kayron.dew.components.TextComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.components.UiComponent
import ru.kayron.dew.scene.Scene
import ru.kayron.dew.systems.RenderSystem
import ru.kayron.dew.systems.UiInteractionSystem
import ru.kayron.dew.systems.UiLayoutSystem

class SceneManager(
    private val game: Game
) {
    val scope: CargoContainer = game.cargo.scope()
    
    private val storage = hashMapOf<String, Scene>()
    private val initializedScenes = hashSetOf<Scene>()
    private var activeScene: Scene? = null
    private var initialized = false

    init {
        scope.addSingleton(game)
        scope.addSingleton(this)
    }
    
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

        val components = scene.componentManager

        components.add(scene.scope.create<CameraComponent>())
        components.add(scene.scope.create<TransformComponent>())
        components.add(scene.scope.create<SpriteComponent>())
        components.add(scene.scope.create<SingleSpriteComponent>())
        components.add(scene.scope.create<AnimatedSpriteComponent>())
        components.add(scene.scope.create<TextComponent>())
        components.add(scene.scope.create<UiComponent>())

        scene.systemManager.add(scene.scope.create<UiLayoutSystem>())
        scene.systemManager.add(scene.scope.create<UiInteractionSystem>())
        scene.systemManager.add(scene.scope.create<RenderSystem>())
        scene.initialize()
    }
}
