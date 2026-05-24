package ru.kayron.dew.ecs

import ru.kayron.dew.GameTime

interface IUpdateable {
    val updateOrder: Int
    var enabled: Boolean
    fun update(gameTime: GameTime)
}
