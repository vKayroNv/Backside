package ru.kayron.dew

open class GameSystem(protected val game: Game) : IGameSystem, IUpdateable {
    override var enabled: Boolean = true
    override val updateOrder: Int = 0

    override fun initialize() {}

    override fun update(gameTime: GameTime) {}
}
