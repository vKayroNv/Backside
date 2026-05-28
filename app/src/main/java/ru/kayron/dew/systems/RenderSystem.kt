package ru.kayron.dew.systems

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.ecs.DrawableGameSystem
import ru.kayron.dew.ecs.World
import ru.kayron.dew.graphics.SpriteBatch

class RenderSystem(
    game: Game, 
    private val world: World
) : DrawableGameSystem(game) {
    private lateinit var batch: SpriteBatch
    
    override fun initialize() {
        batch = SpriteBatch(game.graphicsDevice)
    }

    override fun update(gameTime: GameTime) {}

    override fun draw(gameTime: GameTime) {
        graphicsDevice.clear(Color.CornflowerBlue)
        
        val cc = world.componentManager.get<CameraComponent>()
        

        batch.begin(
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone,
            transformMatrix = null
        )
        
        batch.end()
    }
}