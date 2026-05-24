package ru.kayron.dew

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import ru.kayron.dew.input.GamePad

abstract class DewActivity : AppCompatActivity() {
    protected abstract fun createGame(): Game

    private var gameView: DewGameView? = null
    private var game: Game? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        hideSystemUi()
        GamePad.initialize(this)
        val createdGame = createGame()
        game = createdGame
        createdGame.content.setAssetManager(assets)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                game?.exit()
            }
        })
        gameView = DewGameView(this, createdGame)
        setContentView(gameView)
        gameView?.requestFocus()
    }

    private fun hideSystemUi() {
        window.insetsController?.let { c ->
            c.hide(WindowInsets.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onPause() {
        gameView?.onPauseGame()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        gameView?.onResumeGame()
    }

    override fun onDestroy() {
        game?.dispose()
        super.onDestroy()
    }
}
