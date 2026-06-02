package ru.kayron.dew.managers

import ru.kayron.dew.GameTime
import ru.kayron.dew.utils.ArrayUtils
import ru.kayron.dew.ecs.IGameSystem
import ru.kayron.dew.ecs.IUpdateable
import ru.kayron.dew.ecs.IDrawable

class SystemManager {
    var systems: Array<IGameSystem?> = arrayOfNulls<IGameSystem>(INITIAL_CAPACITY)
    var updateSystems: Array<IUpdateable?> = arrayOfNulls<IUpdateable>(INITIAL_CAPACITY)
    var drawSystems: Array<IDrawable?> = arrayOfNulls<IDrawable>(INITIAL_CAPACITY)
    
    var systemSize = 0
    var updateSize = 0
    var drawSize = 0
    
    fun add(system: IGameSystem) {
        systems = ArrayUtils.ensureCapacity(
            systems,
            systemSize,
            null
        )
        systems[systemSize++] = system
        if (system is IUpdateable) {
            updateSystems = ArrayUtils.ensureCapacity(
                updateSystems,
                updateSize,
                null
            )
            updateSystems[updateSize++] = system
        }
        if (system is IDrawable) {
            drawSystems = ArrayUtils.ensureCapacity(
                drawSystems,
                drawSize,
                null
            )
            drawSystems[drawSize++] = system
        }
    }
    
    fun sort() {
        updateSystems.sortBy { it?.updateOrder }
        drawSystems.sortBy { it?.drawOrder }
    }
    
    fun update(gameTime: GameTime) {
        updateSystems.forEach {
            it?.update(gameTime)
        }
    }
    
    fun draw(gameTime: GameTime) {
        drawSystems.forEach {
            it?.draw(gameTime)
        }
    }
    
    fun initialize() {
        systems.forEach {
            it?.initialize()
        }
    }

    fun reloadGraphicsResources() {
        systems.forEach {
            it?.reloadGraphicsResources()
        }
    }
    
    companion object {
        val INITIAL_CAPACITY = 8
    }
}