package ru.kayron.dew.ecs

import ru.kayron.dew.GameTime

interface IDrawable {
    val drawOrder: Int
    var visible: Boolean
    fun draw(gameTime: GameTime)
}
