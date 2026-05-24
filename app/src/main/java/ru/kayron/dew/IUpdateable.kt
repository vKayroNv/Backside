package ru.kayron.dew

interface IUpdateable {
    val updateOrder: Int
    var enabled: Boolean
    fun update(gameTime: GameTime)
}
