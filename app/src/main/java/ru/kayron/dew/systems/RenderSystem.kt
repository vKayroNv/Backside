package ru.kayron.dew.systems

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.AnimatedSpriteComponent
import ru.kayron.dew.components.SingleSpriteComponent
import ru.kayron.dew.components.SpriteComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.ecs.DrawableGameSystem
import ru.kayron.dew.ecs.World
import ru.kayron.dew.graphics.DepthStencilState
import ru.kayron.dew.graphics.RasterizerState
import ru.kayron.dew.graphics.SpriteBatch
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Matrix
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector3

class RenderSystem(
    game: Game,
    private val world: World
) : DrawableGameSystem(game) {

    private lateinit var batch: SpriteBatch
    private var transformMatrix = Matrix.Identity
    
    private val textures = hashMapOf<String, Texture2D>()

    override fun initialize() {
        batch = SpriteBatch(game.graphicsDevice)
        
        var filenames = world.componentManager.get<SpriteComponent>().filenames()
        filenames.forEach {
            val texture = game.content.load<Texture2D>(it)
            textures.getOrPut(it) { texture }
        }
    }

    override fun update(gameTime: GameTime) {
        val cameraComponent = world.componentManager.get<CameraComponent>()
        val transformComponent = world.componentManager.get<TransformComponent>()

        val camera = cameraComponent.getActive()
        if (camera == -1) {
            transformMatrix = Matrix.Identity
            return
        }

        val x = transformComponent.x(camera)
        val y = transformComponent.y(camera)
        val rotation = transformComponent.rotation(camera)
        val zoom = cameraComponent.zoom(camera)

        val viewportWidth = cameraComponent.viewportWidth(camera)
        val viewportHeight = cameraComponent.viewportHeight(camera)

        transformMatrix =
            Matrix.createTranslation(
                Vector3(-x, -y, 0f)
            ) *
            Matrix.createRotationZ(-rotation) *
            Matrix.createScale(
                Vector3(zoom, zoom, 1f)
            ) *
            Matrix.createTranslation(
                Vector3(
                    viewportWidth * 0.5f,
                    viewportHeight * 0.5f,
                    0f
                )
            )
    }

    override fun draw(gameTime: GameTime) {
        game.graphicsDevice.clear(Color.CornflowerBlue)
        
        val spriteComponent = world.componentManager.get<SpriteComponent>()
        val singleSpriteComponent = world.componentManager.get<SingleSpriteComponent>()
        val animatedSpriteComponent = world.componentManager.get<AnimatedSpriteComponent>()
        val transformComponent = world.componentManager.get<TransformComponent>()
        
        val single = world.componentManager.getEntitiesWith<SpriteComponent, SingleSpriteComponent>()
        val animated = world.componentManager.getEntitiesWith<SpriteComponent, SingleSpriteComponent>()

        batch.begin(
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone,
            transformMatrix = transformMatrix
        )
        
        single.forEach {
            /*batch.draw(
                textures[spriteComponent.filename(it)],
                transformComponent.pos(it) + spriteComponent.cellSize(it) * spriteComponent.pivot(it),
                
            )*/
        }

        batch.end()
    }
}