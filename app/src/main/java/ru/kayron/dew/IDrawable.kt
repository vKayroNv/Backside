package ru.kayron.dew

interface IDrawable {
    val drawOrder: Int
    var visible: Boolean
    fun draw(gameTime: GameTime)
}
