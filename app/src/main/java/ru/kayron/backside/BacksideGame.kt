package ru.kayron.backside

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
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

    private lateinit var batch: SpriteBatch

    override fun loadContent() {
        batch = SpriteBatch(graphicsDevice)
    }
    
    override fun initialize() {
        super.initialize()
    }

    override fun update(gameTime: GameTime) {
        if (Keyboard.getState().isKeyDown(Keys.Escape)) {
            exit()
        }
        
        super.update(gameTime)
    }

    override fun draw(gameTime: GameTime) {
        graphicsDevice.clear(Color.CornflowerBlue)

        batch.begin(
            depthStencilState = DepthStencilState.None,
            rasterizerState = RasterizerState.CullNone
        )

        onDraw(batch)
        batch.end()
        
        super.draw(gameTime)
    }

    protected open fun onTouch(position: Vector2, state: TouchLocation.TouchLocationState) {}
    protected open fun onDraw(batch: SpriteBatch) {}

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}
