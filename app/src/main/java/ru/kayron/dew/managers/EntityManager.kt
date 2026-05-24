package ru.kayron.dew.managers

import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.ArrayUtils

class EntityManager {
    var nextId: Entity = 1
    var storage = IntArray(INITIAL_CAPACITY)
    var size = 0
    
    fun create() : Entity {
        if (size == 0) return nextId++
        return storage[--size]
    }
    
    fun remove(entity: Entity) {
        storage = ArrayUtils.ensureCapacity(
            storage,
            size,
            null
        )
        
        storage[size++] = entity
    }
    
    fun reset() {
        nextId = 1
        storage = IntArray(INITIAL_CAPACITY)
        size = 0
    }
    
    companion object {
        val INITIAL_CAPACITY = 128
    }
}