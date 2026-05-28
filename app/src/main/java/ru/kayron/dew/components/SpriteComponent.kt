package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Point

class SpriteComponent : Component() {
    private var filename = arrayOfNulls<String>(INITIAL_CAPACITY)
    private var width = IntArray(INITIAL_CAPACITY)
    private var height = IntArray(INITIAL_CAPACITY)
    private var rows = IntArray(INITIAL_CAPACITY)
    private var columns = IntArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity,
        filenameVal: String,
        widthVal: Int,
        heightVal: Int,
        rowsVal: Int = 1,
        columnsVal: Int = 1
    ) {
        val index = addEntity(entity)
        filename[index] = filenameVal
        width[index] = widthVal
        height[index] = heightVal
        rows[index] = rowsVal
        columns[index] = columnsVal
    }
    
    override fun grow(newSize: Int) {
        filename = filename.copyOf(newSize)
        width = width.copyOf(newSize)
        height = height.copyOf(newSize)
        rows = rows.copyOf(newSize)
        columns = columns.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        filename.swap(a, b)
        width.swap(a, b)
        height.swap(a, b)
        rows.swap(a, b)
        columns.swap(a, b)
    }
    
    fun filename(entity: Entity) : String = filename[indexOf(entity)] as String
    fun width(entity: Entity) : Int = width[indexOf(entity)]
    fun height(entity: Entity) : Int = height[indexOf(entity)]
    fun size(entity: Entity) : Point = Point(width(entity), height(entity))
    fun rows(entity: Entity) : Int = rows[indexOf(entity)]
    fun columns(entity: Entity) : Int = columns[indexOf(entity)]
    fun grid(entity: Entity) : Point = Point(rows(entity), columns(entity))
}