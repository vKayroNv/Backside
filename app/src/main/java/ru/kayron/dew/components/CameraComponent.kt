package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Vector2

class CameraComponent : Component() {
    private var zoom = FloatArray(INITIAL_CAPACITY)
    private var viewportWidth = FloatArray(INITIAL_CAPACITY)
    private var viewportHeight = FloatArray(INITIAL_CAPACITY)
    private var active = -1
    
    fun add(
        entity: Entity, 
        zoomVal: Float = 1f,
        viewportWidthVal: Float = 0f, 
        viewportHeightVal: Float = 0f
    ) {
        val index = addEntity(entity)
        zoom[index] = zoomVal
        viewportWidth[index] = viewportWidthVal
        viewportHeight[index] = viewportHeightVal
    }
    
    fun update(
        entity: Entity, 
        zoomVal: Float? = null, 
        viewportWidthVal: Float? = null,
        viewportHeightVal: Float? = null
    ) {
        val index = indexOf(entity)
        zoomVal?.let { zoom[index] = it }
        viewportWidthVal?.let { viewportWidth[index] = it }
        viewportHeightVal?.let { viewportHeight[index] = it }
    }
    
    override fun grow(newSize: Int) {
        zoom = zoom.copyOf(newSize)
        viewportWidth = viewportWidth.copyOf(newSize)
        viewportHeight = viewportHeight.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        zoom.swap(a, b)
        viewportWidth.swap(a, b)
        viewportHeight.swap(a, b)
    }
    
    fun zoom(entity: Entity) : Float = zoom[indexOf(entity)]
    fun viewportWidth(entity: Entity) : Float = viewportWidth[indexOf(entity)]
    fun viewportHeight(entity: Entity) : Float = viewportHeight[indexOf(entity)]
    fun viewport(entity: Entity) : Vector2 = Vector2(viewportWidth(entity), viewportHeight(entity))
    fun getActive(): Entity = if (active != -1) entityIds[active] else -1
    
    fun setActive(entity: Entity) {
        val index = indexOf(entity)
        active = index
    }
}