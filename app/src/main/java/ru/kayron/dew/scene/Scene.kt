package ru.kayron.dew.scene

import ru.kayron.cargo.CargoContainer
import ru.kayron.dew.ecs.World
import ru.kayron.dew.GameTime
import ru.kayron.dew.managers.ComponentManager
import ru.kayron.dew.managers.EntityManager
import ru.kayron.dew.managers.SceneManager
import ru.kayron.dew.managers.SystemManager
import ru.kayron.dew.ui.UiManager

open class Scene(
    protected val sceneManager: SceneManager
) {
    val scope: CargoContainer = sceneManager.scope.scope()
    val entityManager: EntityManager
    val componentManager: ComponentManager
    val systemManager: SystemManager
    val uiManager: UiManager
    val world: World

    private var initialized = false

    init {
        scope.addScoped { EntityManager() }
        scope.addScoped { ComponentManager() }
        scope.addScoped { SystemManager() }
        scope.addScoped { UiManager() }
        scope.addScoped {
            World(
                get(),
                get(),
                get()
            )
        }

        entityManager = scope.get()
        componentManager = scope.get()
        systemManager = scope.get()
        uiManager = scope.get()
        world = scope.get()
    }

    fun initialize() {
        if (initialized) return

        onInitialize()

        systemManager.sort()
        systemManager.initialize()

        initialized = true
    }

    protected open fun onInitialize() {}
    
    open fun onEnter(previousScene: Scene?) {}
    
    open fun onExit() {}
    
    open fun update(gameTime: GameTime) {
        systemManager.update(gameTime)
    }
    
    open fun draw(gameTime: GameTime) {
        systemManager.draw(gameTime)
    }

    open fun reloadGraphicsResources() {
        systemManager.reloadGraphicsResources()
    }
}
