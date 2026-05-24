package ru.kayron.dew

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
