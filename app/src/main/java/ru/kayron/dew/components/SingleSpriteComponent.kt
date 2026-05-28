package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Point

class SingleSpriteComponent : Component() {
    private var row = IntArray(INITIAL_CAPACITY)
    private var column = IntArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity, 
        rowVal: Int = 0,
        columnVal: Int = 0
    ) {
        val index = addEntity(entity)
        row[index] = rowVal
        column[index] = columnVal
    }
    
    override fun grow(newSize: Int) {
        row = row.copyOf(newSize)
        column = column.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        row.swap(a, b)
        column.swap(a, b)
    }
    
    fun row(entity: Entity) : Int = row[indexOf(entity)]
    fun column(entity: Entity) : Int = column[indexOf(entity)]
    fun pos(entity: Entity) : Point = Point(row(entity), column(entity))
}