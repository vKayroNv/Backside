package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Vector2

class TransformComponent : Component() {
    private var x = FloatArray(INITIAL_CAPACITY)
    private var y = FloatArray(INITIAL_CAPACITY)
    private var rotation = FloatArray(INITIAL_CAPACITY)
    private var scaleX = FloatArray(INITIAL_CAPACITY)
    private var scaleY = FloatArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity, 
        xVal: Float = 0f, 
        yVal: Float = 0f,
        rotationVal: Float = 0f,
        scaleXVal: Float = 1f,
        scaleYVal: Float = 1f
    ) {
        val index = addEntity(entity)
        x[index] = xVal
        y[index] = yVal
        rotation[index] = rotationVal
        scaleX[index] = scaleXVal
        scaleY[index] = scaleYVal
    }
    
    fun update(
        entity: Entity, 
        xVal: Float? = null, 
        yVal: Float? = null,
        rotationVal: Float? = null,
        scaleXVal: Float? = null,
        scaleYVal: Float? = null
    ) {
        val index = indexOf(entity)
        xVal?.let { x[index] = it }
        yVal?.let { y[index] = it }
        rotationVal?.let { rotation[index] = it }
        scaleXVal?.let { scaleX[index] = it }
        scaleYVal?.let { scaleY[index] = it }
    }
    
    override fun grow(newSize: Int) {
        x = x.copyOf(newSize)
        y = y.copyOf(newSize)
        rotation = rotation.copyOf(newSize)
        scaleX = scaleX.copyOf(newSize)
        scaleY = scaleY.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        x.swap(a, b)
        y.swap(a, b)
        rotation.swap(a, b)
        scaleX.swap(a, b)
        scaleY.swap(a, b)
    }
    
    fun x(entity: Entity) : Float = x[indexOf(entity)]
    fun y(entity: Entity) : Float = y[indexOf(entity)]
    fun pos(entity: Entity) : Vector2 = Vector2(x(entity), y(entity))
    fun rotation(entity: Entity) : Float = rotation[indexOf(entity)]
    fun scaleX(entity: Entity) : Float = scaleX[indexOf(entity)]
    fun scaleY(entity: Entity) : Float = scaleY[indexOf(entity)]
    fun scale(entity: Entity) : Vector2 = Vector2(scaleX(entity), scaleY(entity))
}