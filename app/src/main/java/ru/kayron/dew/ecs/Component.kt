package ru.kayron.dew.ecs

import ru.kayron.dew.utils.ArrayUtils

abstract class Component() {
    protected var entityIds = IntArray(INITIAL_CAPACITY)
    protected var entityToIndex = IntArray(INITIAL_CAPACITY) { -1 }

    protected var size = 0

    val entitiesCount: Int
        get() = size

    fun hasEntity(entity: Entity): Boolean {
        return entity >= 0 &&
            entity < entityToIndex.size &&
            entityToIndex[entity] != -1
    }

    fun entities(): List<Entity> = entityIds
        .copyOfRange(0, size)
        .toList()

    protected fun indexOf(entity: Entity): Int {
        require(entity < entityToIndex.size) { "Entity out of bounds: $entity" }
        val idx = entityToIndex[entity]
        require(idx != -1) { "Entity $entity not found" }
        return idx
    }

    protected fun addEntity(entity: Entity): Int {
        entityIds = ArrayUtils.ensureCapacity(
            entityIds,
            size,
            ::grow
        )
        entityToIndex = ArrayUtils.ensureCapacity(
            entityToIndex,
            entity,
            ::grow
        )

        val index = size
        entityIds[index] = entity
        entityToIndex[entity] = index

        size++
        return index
    }

    fun remove(entity: Entity) {
        val index = indexOf(entity)
        val lastIndex = size - 1
        val lastEntity = entityIds[lastIndex]

        entityIds[index] = lastEntity
        entityToIndex[lastEntity] = index

        swap(index, lastIndex)

        entityToIndex[entity] = -1
        entityIds[lastIndex] = -1

        size--
    }

    fun clear() {
        for (i in 0 until size) {
            entityToIndex[entityIds[i]] = -1
            entityIds[i] = -1
        }
        size = 0
    }

    protected abstract fun grow(newSize: Int)
    protected abstract fun swap(a: Int, b: Int)
    
    companion object {
        val INITIAL_CAPACITY = 128
    }
}
