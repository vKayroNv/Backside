package ru.kayron.dew.systems

import androidx.core.graphics.createBitmap
import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.AnimatedSpriteComponent
import ru.kayron.dew.components.SingleSpriteComponent
import ru.kayron.dew.components.SpriteComponent
import ru.kayron.dew.components.TextComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.components.UiComponent
import ru.kayron.dew.ecs.DrawableGameSystem
import ru.kayron.dew.ecs.World
import ru.kayron.dew.graphics.DepthStencilState
import ru.kayron.dew.graphics.RasterizerState
import ru.kayron.dew.graphics.SamplerState
import ru.kayron.dew.graphics.SpriteBatch
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.graphics.SpriteSortMode
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.math.Color
import ru.kayron.dew.math.Matrix
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.math.Vector2
import ru.kayron.dew.math.Vector3
import ru.kayron.dew.ui.UiElement
import ru.kayron.dew.ui.UiManager
import ru.kayron.dew.ui.UiRenderMode
import ru.kayron.dew.ui.UiTheme
import ru.kayron.dew.ui.ScrollView
import ru.kayron.dew.ui.Slider

class RenderSystem(
    game: Game,
    private val world: World,
    private val uiManager: UiManager
) : DrawableGameSystem(game) {

    private lateinit var batch: SpriteBatch
    private lateinit var defaultFont: SpriteFont
    private lateinit var fallbackTexture: Texture2D
    private var transformMatrix = Matrix.Identity
    
    private val textures = hashMapOf<String, Texture2D>()

    private val clipStack = mutableListOf<Rectangle>()

    companion object {
        private val themedControlTags = setOf(
            "checkbox_unchecked", "checkbox_checked",
            "radio_unselected", "radio_selected",
            "slider",
            "dropdown_item"
        )
    }

    override fun initialize() {
        batch = SpriteBatch(game.graphicsDevice)
        defaultFont = SpriteFont.fromSystemFont("monospace", 32f)
        fallbackTexture = createFallbackTexture()
        
        val filenames = world.componentManager.get<SpriteComponent>().filenames()
        filenames.forEach {
            texture(it)
        }
        resolveSpriteSizes()
    }

    override fun reloadGraphicsResources() {
        textures.clear()
        batch.dispose()
        defaultFont.texture.dispose()
        fallbackTexture.dispose()

        batch = SpriteBatch(game.graphicsDevice)
        defaultFont = SpriteFont.fromSystemFont("monospace", 32f)
        fallbackTexture = createFallbackTexture()

        val filenames = world.componentManager.get<SpriteComponent>().filenames()
        filenames.forEach {
            texture(it)
        }
        resolveSpriteSizes()
    }

    override fun update(gameTime: GameTime) {
        val cameraComponent = world.componentManager.get<CameraComponent>()
        val transformComponent = world.componentManager.get<TransformComponent>()
        val animatedSpriteComponent = world.componentManager.get<AnimatedSpriteComponent>()

        val camera = cameraComponent.getActive()
        if (camera == -1) {
            transformMatrix = Matrix.Identity
        } else {
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
        }

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
        val textComponent = world.componentManager.get<TextComponent>()
        val uiComponent = world.componentManager.get<UiComponent>()
        
        val single = world.componentManager.getEntitiesWith<SpriteComponent, SingleSpriteComponent, TransformComponent>()
        val animated = world.componentManager.getEntitiesWith<SpriteComponent, AnimatedSpriteComponent, TransformComponent>()

        beginWorld()
        
        single.forEach {
            if (shouldDraw(it, UiRenderMode.World, uiComponent)) {
                drawSprite(
                    spriteComponent = spriteComponent,
                    transformComponent = transformComponent,
                    uiComponent = uiComponent,
                    entity = it,
                    row = singleSpriteComponent.row(it),
                    column = singleSpriteComponent.column(it)
                )
            }
        }

        animated.forEach {
            if (shouldDraw(it, UiRenderMode.World, uiComponent)) {
                drawSprite(
                    spriteComponent = spriteComponent,
                    transformComponent = transformComponent,
                    uiComponent = uiComponent,
                    entity = it,
                    row = animatedSpriteComponent.row(it),
                    column = animatedSpriteComponent.currentIndex(it)
                )
            }
        }

        drawUiFallbacks(UiRenderMode.World, transformComponent, uiComponent)
        drawText(UiRenderMode.World, transformComponent, textComponent, uiComponent)
        batch.end()

        beginStatic()

        uiManager.roots.forEach {
            drawUiTree(it, UiRenderMode.Static)
        }

        batch.end()
        endStatic()
    }

    private fun drawUiTree(
        element: UiElement,
        renderMode: UiRenderMode
    ) {
        if (!element.visible) return
        if (element.renderMode != renderMode) return

        if (element is ScrollView) {
            pushClip(
                Rectangle(
                    element.x.toInt(),
                    element.y.toInt(),
                    element.width.toInt(),
                    element.height.toInt()
                )
            )
        }

        drawElement(element)

        element.children.forEach {
            drawUiTree(it, renderMode)
        }

        if (element is ScrollView) {
            popClip()
        }
    }

    private fun drawElement(element: UiElement) {
        val entity = element.entity
        val transform = world.componentManager.get<TransformComponent>()
        val ui = world.componentManager.get<UiComponent>()
        val sprite = world.componentManager.get<SpriteComponent>()
        val text = world.componentManager.get<TextComponent>()

        if (sprite.hasEntity(entity)) {
            val single = world.componentManager.get<SingleSpriteComponent>()
            val animated = world.componentManager.get<AnimatedSpriteComponent>()
            if (single.hasEntity(entity)) {
                drawSprite(sprite, transform, ui, entity, single.row(entity), single.column(entity))
            } else if (animated.hasEntity(entity)) {
                drawSprite(sprite, transform, ui, entity, animated.row(entity), animated.currentIndex(entity))
            }
        } else {
            val color = ui.backgroundColor(entity)
            if (color.a > 0 && transform.width(entity) > 0f && transform.height(entity) > 0f) {
                batch.draw(fallbackTexture, rectangle(transform, entity), color)
            }
        }

        drawControlDecoration(entity, transform, ui)

        if (text.hasEntity(entity)) {
            val value = text.text(entity)
            if (value.isNotEmpty()) {
                val font = text.font(entity) ?: defaultFont
                batch.drawString(
                    font,
                    value,
                    transform.pos(entity) + textOffset(entity, transform, text, font, value),
                    text.color(entity)
                )
            }
        }
    }

    private fun drawControlDecoration(
        entity: Int,
        transform: TransformComponent,
        ui: UiComponent
    ) {
        val tag = ui.styleTag(entity) ?: return
        val ex = transform.x(entity)
        val ey = transform.y(entity)
        val ew = transform.width(entity)
        val eh = transform.height(entity)
        if (ew <= 0f || eh <= 0f) return

        when (tag) {
            "checkbox_unchecked", "checkbox_checked" -> {
                val tex = UiTheme.texture(tag)
                val cy = ey + (eh - 22f) * 0.5f
                batch.draw(tex, Rectangle(ex.toInt() + 2, cy.toInt(), 22, 22), color = Color.White)
            }
            "radio_unselected", "radio_selected" -> {
                val tex = UiTheme.texture(tag)
                val cy = ey + (eh - 22f) * 0.5f
                batch.draw(tex, Rectangle(ex.toInt() + 2, cy.toInt(), 22, 22), color = Color.White)
            }
            "slider" -> {
                val trackTex = UiTheme.texture("slider_track")
                val thumbTex = UiTheme.texture("slider_thumb")
                val trackY = ey + eh * 0.5f - 4f
                batch.draw(trackTex, Rectangle(ex.toInt(), trackY.toInt(), ew.toInt(), 8), color = Color(100, 100, 100))
                val normalized = Slider.getNormalized(entity)
                val thumbX = ex + 10f + (ew - 28f) * normalized
                batch.draw(thumbTex, Rectangle((thumbX - 9f).toInt(), (ey + eh * 0.5f - 9f).toInt(), 18, 18), color = Color.White)
            }
            "dropdown" -> {
                val arrowTex = UiTheme.texture("dropdown_arrow")
                val ax = ex + ew - 24f
                val ay = ey + (eh - 12f) * 0.5f
                batch.draw(arrowTex, Rectangle(ax.toInt(), ay.toInt(), 12, 12), color = Color.White)
            }
        }
    }

    private fun drawSprite(
        spriteComponent: SpriteComponent,
        transformComponent: TransformComponent,
        uiComponent: UiComponent,
        entity: Int,
        row: Int,
        column: Int
    ) {
        resolveSpriteSize(spriteComponent, entity)

        val cellWidth = spriteComponent.cellWidth(entity)
        val cellHeight = spriteComponent.cellHeight(entity)
        val sourceRectangle = Rectangle(
            column * cellWidth,
            row * cellHeight,
            cellWidth,
            cellHeight
        )
        val isUi = uiComponent.hasEntity(entity)
        val color = if (isUi) {
            val base = uiComponent.backgroundColor(entity).takeIf { it.a > 0 } ?: Color.White
            if (uiComponent.pressed(entity)) base * uiComponent.pressedTint(entity) else base
        } else {
            Color.White
        }

        if (isUi && transformComponent.width(entity) > 0f && transformComponent.height(entity) > 0f) {
            batch.draw(
                texture = texture(spriteComponent.filename(entity)),
                destinationRectangle = rectangle(transformComponent, entity),
                sourceRectangle = sourceRectangle,
                color = color,
                rotation = transformComponent.rotation(entity),
                origin = spriteComponent.pivot(entity)
            )
        } else {
            val scale = transformComponent.scale(entity) * spriteComponent.scale(entity)
            batch.draw(
                texture = texture(spriteComponent.filename(entity)),
                position = transformComponent.pos(entity),
                sourceRectangle = sourceRectangle,
                color = color,
                rotation = transformComponent.rotation(entity),
                origin = spriteComponent.pivot(entity),
                scale = scale
            )
        }
    }

    private fun drawUiFallbacks(
        renderMode: UiRenderMode,
        transformComponent: TransformComponent,
        uiComponent: UiComponent
    ) {
        val spriteComponent = world.componentManager.get<SpriteComponent>()
        world.componentManager.getEntitiesWith<UiComponent, TransformComponent>().forEach {
            if (spriteComponent.hasEntity(it)) return@forEach
            if (!shouldDraw(it, renderMode, uiComponent)) return@forEach
            if (uiComponent.styleTag(it) in themedControlTags) return@forEach
            val color = uiComponent.backgroundColor(it)
            if (color.a == 0 || transformComponent.width(it) <= 0f || transformComponent.height(it) <= 0f) {
                return@forEach
            }
            val drawColor = if (uiComponent.pressed(it)) color * uiComponent.pressedTint(it) else color

            val rect = clipRectFor(uiComponent, it, transformComponent)
            if (rect != null) pushClip(rect)

            batch.draw(
                texture = fallbackTexture,
                destinationRectangle = rectangle(transformComponent, it),
                color = drawColor
            )

            if (rect != null) popClip()
        }
    }

    private fun drawText(
        renderMode: UiRenderMode,
        transformComponent: TransformComponent,
        textComponent: TextComponent,
        uiComponent: UiComponent
    ) {
        world.componentManager.getEntitiesWith<TextComponent, TransformComponent>().forEach {
            if (!shouldDraw(it, renderMode, uiComponent)) return@forEach
            if (uiComponent.styleTag(it) == "dropdown_item") return@forEach
            val text = textComponent.text(it)
            if (text.isEmpty()) return@forEach

            val font = textComponent.font(it) ?: defaultFont
            val offset = textOffset(it, transformComponent, textComponent, font, text)
            val pos = transformComponent.pos(it) + offset

            val rect = clipRectFor(uiComponent, it, transformComponent)
            if (rect != null) pushClip(rect)

            batch.drawString(
                font,
                text,
                pos,
                textComponent.color(it)
            )

            if (rect != null) popClip()
        }
    }

    private fun clipRectFor(
        uiComponent: UiComponent,
        entity: Int,
        transformComponent: TransformComponent
    ): Rectangle? {
        val left = uiComponent.clipLeft(entity)
        val top = uiComponent.clipTop(entity)
        val right = uiComponent.clipRight(entity)
        val bottom = uiComponent.clipBottom(entity)
        if (left.isInfinite()) return null

        val clipRect = Rectangle(left.toInt(), top.toInt(), (right - left).toInt(), (bottom - top).toInt())
        return clipRect.takeIf { !it.isEmpty }
    }

    private fun pushClip(rect: Rectangle) {
        val vp = game.graphicsDevice.viewport
        val current = if (clipStack.isEmpty()) {
            Rectangle(0, 0, vp.width, vp.height)
        } else {
            clipStack.last()
        }
        val clipped = current.intersect(rect)
        clipStack.add(clipped)
        val glY = vp.height - (clipped.y + clipped.height)
        game.graphicsDevice.setScissorRect(Rectangle(
            clipped.x.coerceAtLeast(0),
            glY.coerceAtLeast(0),
            clipped.width.coerceAtMost(vp.width),
            clipped.height.coerceAtMost(vp.height)
        ))
    }

    private fun popClip() {
        clipStack.removeLastOrNull()
        val vp = game.graphicsDevice.viewport
        if (clipStack.isEmpty()) {
            game.graphicsDevice.setScissorRect(Rectangle(0, 0, vp.width, vp.height))
        } else {
            val clipped = clipStack.last()
            val glY = vp.height - (clipped.y + clipped.height)
            game.graphicsDevice.setScissorRect(Rectangle(
                clipped.x.coerceAtLeast(0),
                glY.coerceAtLeast(0),
                clipped.width.coerceAtMost(vp.width),
                clipped.height.coerceAtMost(vp.height)
            ))
        }
    }

    private fun textOffset(
        entity: Int,
        transformComponent: TransformComponent,
        textComponent: TextComponent,
        font: SpriteFont,
        text: String
    ): Vector2 {
        val explicit = textComponent.offset(entity)
        if (explicit.x != 0f || explicit.y != 0f) return explicit
        if (transformComponent.width(entity) <= 0f || transformComponent.height(entity) <= 0f) {
            return Vector2.Zero
        }
        val measured = font.measureString(text)
        return Vector2(
            (transformComponent.width(entity) - measured.x) * 0.5f,
            (transformComponent.height(entity) - measured.y) * 0.5f
        )
    }

    private fun shouldDraw(
        entity: Int,
        renderMode: UiRenderMode,
        uiComponent: UiComponent
    ): Boolean {
        if (!uiComponent.hasEntity(entity)) return renderMode == UiRenderMode.World
        return uiComponent.visible(entity) && uiComponent.renderMode(entity) == renderMode
    }

    private fun rectangle(
        transformComponent: TransformComponent,
        entity: Int
    ): Rectangle = Rectangle(
        transformComponent.x(entity).toInt(),
        transformComponent.y(entity).toInt(),
        transformComponent.width(entity).toInt(),
        transformComponent.height(entity).toInt()
    )

    private fun texture(filename: String): Texture2D {
        if (filename.startsWith("theme:")) {
            val themeKey = filename.removePrefix("theme:")
            return UiTheme.texture(themeKey)
        }
        return textures.getOrPut(filename) {
            game.content.load<Texture2D>(filename)
        }
    }

    private fun beginWorld() {
        batch.begin(
            samplerState = SamplerState.PointClamp,
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone,
            transformMatrix = transformMatrix
        )
    }

    private fun beginStatic() {
        game.graphicsDevice.setScissorRect(Rectangle(
            game.graphicsDevice.viewport.x,
            game.graphicsDevice.viewport.y,
            game.graphicsDevice.viewport.width,
            game.graphicsDevice.viewport.height
        ))
        GLScissor.enable()
        clipStack.clear()
        batch.begin(
            sortMode = SpriteSortMode.Immediate,
            samplerState = SamplerState.PointClamp,
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone.copy(scissorTestEnable = true)
        )
    }

    private fun endStatic() {
        GLScissor.disable()
    }

    private fun createFallbackTexture(): Texture2D {
        val bitmap = createBitmap(1, 1)
        bitmap.setPixel(0, 0, android.graphics.Color.WHITE)
        return Texture2D.fromBitmap(bitmap, linear = false)
    }

    private fun resolveSpriteSizes() {
        val spriteComponent = world.componentManager.get<SpriteComponent>()
        world.componentManager.getEntitiesWith<SpriteComponent>().forEach {
            resolveSpriteSize(spriteComponent, it)
        }
    }

    private fun resolveSpriteSize(
        spriteComponent: SpriteComponent,
        entity: Int
    ) {
        if (spriteComponent.width(entity) > 0 && spriteComponent.height(entity) > 0) return

        val texture = texture(spriteComponent.filename(entity))
        spriteComponent.setSize(entity, texture.width, texture.height)
    }
}

private object GLScissor {
    private var scissorEnabled = false

    fun enable() {
        if (!scissorEnabled) {
            android.opengl.GLES30.glEnable(android.opengl.GLES30.GL_SCISSOR_TEST)
            scissorEnabled = true
        }
    }

    fun disable() {
        if (scissorEnabled) {
            android.opengl.GLES30.glDisable(android.opengl.GLES30.GL_SCISSOR_TEST)
            scissorEnabled = false
        }
    }
}
