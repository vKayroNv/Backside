package ru.kayron.dew.components

import ru.kayron.dew.ecs.Component
import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.utils.swap
import ru.kayron.dew.math.Point
import ru.kayron.dew.math.Vector2

class SpriteComponent : Component() {
    private var filename = arrayOfNulls<String>(INITIAL_CAPACITY)
    private var width = IntArray(INITIAL_CAPACITY)
    private var height = IntArray(INITIAL_CAPACITY)
    private var rows = IntArray(INITIAL_CAPACITY)
    private var columns = IntArray(INITIAL_CAPACITY)
    private var cellWidth = IntArray(INITIAL_CAPACITY)
    private var cellHeight = IntArray(INITIAL_CAPACITY)
    private var scaleX = FloatArray(INITIAL_CAPACITY)
    private var scaleY = FloatArray(INITIAL_CAPACITY)
    private var pivotX = FloatArray(INITIAL_CAPACITY)
    private var pivotY = FloatArray(INITIAL_CAPACITY)
    
    fun add(
        entity: Entity,
        filenameVal: String,
        widthVal: Int,
        heightVal: Int,
        rowsVal: Int = 1,
        columnsVal: Int = 1,
        scaleXVal: Float = 1f,
        scaleYVal: Float = 1f,
        pivotXVal: Float = 0f,
        pivotYVal: Float = 0f
    ) {
        val index = addEntity(entity)
        filename[index] = filenameVal
        width[index] = widthVal
        height[index] = heightVal
        rows[index] = rowsVal
        columns[index] = columnsVal
        cellWidth[index] = widthVal / columnsVal
        cellHeight[index] = heightVal / rowsVal
        scaleX[index] = scaleXVal
        scaleY[index] = scaleYVal
        pivotX[index] = pivotXVal
        pivotY[index] = pivotYVal
    }
    
    override fun grow(newSize: Int) {
        filename = filename.copyOf(newSize)
        width = width.copyOf(newSize)
        height = height.copyOf(newSize)
        rows = rows.copyOf(newSize)
        columns = columns.copyOf(newSize)
        cellWidth = cellWidth.copyOf(newSize)
        cellHeight = cellHeight.copyOf(newSize)
        scaleX = scaleX.copyOf(newSize)
        scaleY = scaleY.copyOf(newSize)
        pivotX = pivotX.copyOf(newSize)
        pivotY = pivotY.copyOf(newSize)
    }
    
    override fun swap(a: Int, b: Int) {
        filename.swap(a, b)
        width.swap(a, b)
        height.swap(a, b)
        rows.swap(a, b)
        columns.swap(a, b)
        cellWidth.swap(a, b)
        cellHeight.swap(a, b)
        scaleX.swap(a, b)
        scaleY.swap(a, b)
        pivotX.swap(a, b)
        pivotY.swap(a, b)
    }
    
    fun filename(entity: Entity) : String = filename[indexOf(entity)] as String
    fun filenames() : List<String> = filename.filterNotNull()
    fun width(entity: Entity) : Int = width[indexOf(entity)]
    fun height(entity: Entity) : Int = height[indexOf(entity)]
    fun size(entity: Entity) : Point = Point(width(entity), height(entity))
    fun rows(entity: Entity) : Int = rows[indexOf(entity)]
    fun columns(entity: Entity) : Int = columns[indexOf(entity)]
    fun grid(entity: Entity) : Point = Point(rows(entity), columns(entity))
    fun cellWidth(entity: Entity) : Int = cellWidth[indexOf(entity)]
    fun cellHeight(entity: Entity) : Int = cellHeight[indexOf(entity)]
    fun cellSize(entity: Entity) : Point = Point(cellWidth(entity), cellHeight(entity))
    fun scaleX(entity: Entity) : Float = scaleX[indexOf(entity)]
    fun scaleY(entity: Entity) : Float = scaleY[indexOf(entity)]
    fun scale(entity: Entity) : Vector2 = Vector2(scaleX(entity), scaleY(entity))
    fun pivotX(entity: Entity) : Float = pivotX[indexOf(entity)]
    fun pivotY(entity: Entity) : Float = pivotY[indexOf(entity)]
    fun pivot(entity: Entity) : Vector2 = Vector2(pivotX(entity), pivotY(entity))
}