package ru.kayron.dew.scene

import ru.kayron.cargo.CargoContainer
import ru.kayron.cargo.module
import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.*
import ru.kayron.dew.ecs.World
import ru.kayron.dew.managers.ComponentManager
import ru.kayron.dew.managers.EntityManager
import ru.kayron.dew.managers.SceneManager
import ru.kayron.dew.managers.SystemManager
import ru.kayron.dew.systems.RenderSystem
import ru.kayron.dew.systems.UiInteractionSystem
import ru.kayron.dew.systems.UiLayoutSystem
import ru.kayron.dew.ui.UiManager

open class Scene(
    protected val sceneManager: SceneManager
) {
    val game: Game get() = scope.get()
    val scope: CargoContainer = sceneManager.scope.scope()
    val entityManager: EntityManager
    val componentManager: ComponentManager
    val systemManager: SystemManager
    val uiManager: UiManager
    val world: World

    private var initialized = false

    init {
        scope.load(module {
            scoped { EntityManager() }
            scoped { ComponentManager() }
            scoped { SystemManager() }
            scoped {
                World(
                    get(),
                    get(),
                    get()
                )
            }
            scoped { UiManager(get()) }
        })

        entityManager = scope.get()
        componentManager = scope.get()
        systemManager = scope.get()
        uiManager = scope.get()
        world = scope.get()

        componentManager.add(scope.get<CameraComponent>())
        componentManager.add(scope.get<TransformComponent>())
        componentManager.add(scope.get<SpriteComponent>())
        componentManager.add(scope.get<SingleSpriteComponent>())
        componentManager.add(scope.get<AnimatedSpriteComponent>())
        componentManager.add(scope.get<TextComponent>())
        componentManager.add(scope.get<UiComponent>())
        componentManager.add(scope.get<VelocityComponent>())

        systemManager.add(scope.get<UiLayoutSystem>())
        systemManager.add(scope.get<UiInteractionSystem>())
        systemManager.add(scope.get<RenderSystem>())
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
