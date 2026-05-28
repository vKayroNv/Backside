package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap

class AnimatedSpriteComponent : Component() {
    private var currentIndex = IntArray(INITIAL_CAPACITY)
    private var endIndex = IntArray(INITIAL_CAPACITY)
    private var fps = FloatArray(INITIAL_CAPACITY)
    private var row = IntArray(INITIAL_CAPACITY)
    private var elapsed = FloatArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity, 
        endIndexVal: Int = 0, 
        fpsVal: Float = 0f,
        rowVal: Int = 0
    ) {
        val index = addEntity(entity)
        currentIndex[index] = 0
        endIndex[index] = endIndexVal
        fps[index] = fpsVal
        row[index] = rowVal
        elapsed[index] = 0f
    }
    
    fun update(
        entity: Entity, 
        currentIndexVal: Int? = null,
        elapsedVal: Float? = null
    ) {
        val index = indexOf(entity)
        currentIndexVal?.let { currentIndex[index] = it }
        elapsedVal?.let { elapsed[index] = it }
    }
    
    override fun grow(newSize: Int) {
        currentIndex = currentIndex.copyOf(newSize)
        endIndex = endIndex.copyOf(newSize)
        fps = fps.copyOf(newSize)
        row = row.copyOf(newSize)
        elapsed = elapsed.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        currentIndex.swap(a, b)
        endIndex.swap(a, b)
        fps.swap(a, b)
        row.swap(a, b)
        elapsed.swap(a, b)
    }
    
    fun currentIndex(entity: Entity) : Int = currentIndex[indexOf(entity)]
    fun endIndex(entity: Entity) : Int = endIndex[indexOf(entity)]
    fun fps(entity: Entity) : Float = fps[indexOf(entity)]
    fun row(entity: Entity) : Int = row[indexOf(entity)]
    fun elapsed(entity: Entity) : Float = elapsed[indexOf(entity)]
}