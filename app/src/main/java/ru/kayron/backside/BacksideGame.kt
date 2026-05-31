package ru.kayron.backside

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import ru.kayron.dew.systems.*
import ru.kayron.dew.ecs.*
import ru.kayron.dew.components.*
import ru.kayron.dew.managers.*
import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.graphics.DepthStencilState
import ru.kayron.dew.graphics.RasterizerState
import ru.kayron.dew.graphics.SpriteBatch
import ru.kayron.dew.graphics.SpriteFont
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.input.Touch
import ru.kayron.dew.input.TouchLocation
import ru.kayron.dew.input.Keyboard
import ru.kayron.dew.input.Keys
import ru.kayron.dew.math.*

open class BacksideGame : Game() {
    lateinit var w: World
    lateinit var em: EntityManager
    lateinit var cm: ComponentManager
    lateinit var sm: SystemManager
    
    override fun loadContent() {
        em = EntityManager()
        cm = ComponentManager()
        sm = SystemManager()
        w = World(em, cm, sm)
        w.componentManager.add(CameraComponent())
        w.componentManager.add(TransformComponent())
        w.componentManager.add(SpriteComponent())
        w.componentManager.add(SingleSpriteComponent())
        w.systemManager.add(RenderSystem(this, w))
    }
    
    override fun initialize() {
        super.initialize()
    }

    override fun update(gameTime: GameTime) {
        if (Keyboard.getState().isKeyDown(Keys.Escape)) {
            exit()
        }
        
        w.systemManager.update(gameTime)
        
        super.update(gameTime)
    }

    override fun draw(gameTime: GameTime) {
        w.systemManager.draw(gameTime)
        
        super.draw(gameTime)
    }

    protected open fun onTouch(position: Vector2, state: TouchLocation.TouchLocationState) {}
    protected open fun onDraw(batch: SpriteBatch) {}

    override fun dispose() {
        super.dispose()
    }
}
