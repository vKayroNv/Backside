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
        
        val filenames = world.componentManager.get<SpriteComponent>().filenames()
        filenames.forEach {
            texture(it)
        }
    }

    override fun update(gameTime: GameTime) {
        val cameraComponent = world.componentManager.get<CameraComponent>()
        val transformComponent = world.componentManager.get<TransformComponent>()
        val animatedSpriteComponent = world.componentManager.get<AnimatedSpriteComponent>()

        val camera = cameraComponent.getActive()
        if (camera == -1) {
            transformMatrix = Matrix.Identity
            return
        }

        val x = transformComponent.x(camera)
        val y = transformComponent.y(camera)
        val rotation = transformComponent.rotation(camera)
        val zoom = cameraComponent.zoom(camera)

        val viewportWidth = cameraComponent.viewportWidth(camera).takeIf { it > 0f }
            ?: game.graphicsDevice.viewport.width.toFloat()
        val viewportHeight = cameraComponent.viewportHeight(camera).takeIf { it > 0f }
            ?: game.graphicsDevice.viewport.height.toFloat()

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

        val animated = world.componentManager.getEntitiesWith<SpriteComponent, AnimatedSpriteComponent>()
        animated.forEach {
            val fps = animatedSpriteComponent.fps(it)
            val endIndex = animatedSpriteComponent.endIndex(it)
            if (fps <= 0f || endIndex <= 0) return@forEach

            val frameCount = endIndex + 1
            val frameDuration = 1f / fps
            var elapsed = animatedSpriteComponent.elapsed(it) + gameTime.elapsedGameTimeSeconds
            var currentIndex = animatedSpriteComponent.currentIndex(it).coerceIn(0, endIndex)

            while (elapsed >= frameDuration) {
                elapsed -= frameDuration
                currentIndex = (currentIndex + 1) % frameCount
            }

            animatedSpriteComponent.update(
                it,
                currentIndexVal = currentIndex,
                elapsedVal = elapsed
            )
        }
    }

    override fun draw(gameTime: GameTime) {
        game.graphicsDevice.clear(Color.CornflowerBlue)
        
        val spriteComponent = world.componentManager.get<SpriteComponent>()
        val singleSpriteComponent = world.componentManager.get<SingleSpriteComponent>()
        val animatedSpriteComponent = world.componentManager.get<AnimatedSpriteComponent>()
        val transformComponent = world.componentManager.get<TransformComponent>()
        
        val single = world.componentManager.getEntitiesWith<SpriteComponent, SingleSpriteComponent, TransformComponent>()
        val animated = world.componentManager.getEntitiesWith<SpriteComponent, AnimatedSpriteComponent, TransformComponent>()

        batch.begin(
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone,
            transformMatrix = transformMatrix
        )
        
        single.forEach {
            drawSprite(
                spriteComponent = spriteComponent,
                transformComponent = transformComponent,
                entity = it,
                row = singleSpriteComponent.row(it),
                column = singleSpriteComponent.column(it)
            )
        }

        animated.forEach {
            drawSprite(
                spriteComponent = spriteComponent,
                transformComponent = transformComponent,
                entity = it,
                row = animatedSpriteComponent.row(it),
                column = animatedSpriteComponent.currentIndex(it)
            )
        }

        batch.end()
    }

    private fun drawSprite(
        spriteComponent: SpriteComponent,
        transformComponent: TransformComponent,
        entity: Int,
        row: Int,
        column: Int
    ) {
        val cellWidth = spriteComponent.cellWidth(entity)
        val cellHeight = spriteComponent.cellHeight(entity)
        val sourceRectangle = Rectangle(
            column * cellWidth,
            row * cellHeight,
            cellWidth,
            cellHeight
        )
        val scale = transformComponent.scale(entity) * spriteComponent.scale(entity)

        batch.draw(
            texture = texture(spriteComponent.filename(entity)),
            position = transformComponent.pos(entity),
            sourceRectangle = sourceRectangle,
            color = Color.White,
            rotation = transformComponent.rotation(entity),
            origin = spriteComponent.pivot(entity),
            scale = scale
        )
    }

    private fun texture(filename: String): Texture2D =
        textures.getOrPut(filename) {
            game.content.load<Texture2D>(filename)
        }
}
