package ru.kayron.dew

import android.view.View
import ru.kayron.dew.math.Rectangle
import ru.kayron.dew.graphics.PresentationParameters.DisplayOrientation

open class GameWindow {
    var clientBounds: Rectangle = Rectangle()
        protected set
    var currentOrientation: DisplayOrientation = DisplayOrientation.Default
    var view: View? = null
        internal set
    var allowUserResizing: Boolean = false

    fun setClientBounds(width: Int, height: Int) {
        clientBounds = Rectangle(0, 0, width, height)
    }

    companion object {
        fun create(view: View, width: Int, height: Int): GameWindow {
            val w = GameWindow()
            w.view = view
            w.setClientBounds(width, height)
            return w
        }
    }
}
