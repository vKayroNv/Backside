package ru.kayron.dew.ecs

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime

open class GameSystem(protected val game: Game) : IGameSystem, IUpdateable {
    override var enabled: Boolean = true
    override val updateOrder: Int = 0

    override fun initialize() {}

    override fun update(gameTime: GameTime) {}
}
