package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Vector2

class SpriteComponent : Component() {
    private var vx = FloatArray(INITIAL_CAPACITY)
    private var vy = FloatArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity, 
        vxVal: Float = 0f, 
        vyVal: Float = 0f
    ) {
        val index = addEntity(entity)
        vx[index] = vxVal
        vy[index] = vyVal
    }
    
    fun update(
        entity: Entity, 
        vxVal: Float? = null, 
        vyVal: Float? = null
    ) {
        val index = indexOf(entity)
        vxVal?.let { vx[index] = it }
        vyVal?.let { vy[index] = it }
    }
    
    override fun grow(newSize: Int) {
        vx = vx.copyOf(newSize)
        vy = vy.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        vx.swap(a, b)
        vy.swap(a, b)
    }
    
    fun vx(entity: Entity) : Float = vx[indexOf(entity)]
    fun vy(entity: Entity) : Float = vy[indexOf(entity)]
    fun vel(entity: Entity) : Vector2 = Vector2(vx(entity), vy(entity))
}