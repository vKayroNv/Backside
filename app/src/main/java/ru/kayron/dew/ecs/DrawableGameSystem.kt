package ru.kayron.dew.ecs

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime

open class DrawableGameSystem(game: Game) : GameSystem(game), IDrawable {
    override val drawOrder: Int = 0
    override var visible: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    override fun draw(gameTime: GameTime) {}
}
