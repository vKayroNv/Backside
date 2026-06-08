package ru.kayron.dew.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.createBitmap
import ru.kayron.dew.graphics.Texture2D

object UiTheme {
    val themeTextures: Map<String, Texture2D> by lazy {
        mapOf(
            "checkbox_unchecked" to createCheckboxUnchecked(),
            "checkbox_checked" to createCheckboxChecked(),
            "radio_unselected" to createRadioUnselected(),
            "radio_selected" to createRadioSelected(),
            "slider_track" to createSliderTrack(),
            "slider_thumb" to createSliderThumb(),
            "dropdown_arrow" to createDropdownArrow()
        )
    }

    fun texture(id: String): Texture2D = themeTextures[id]!!

    private fun createCheckboxUnchecked(): Texture2D {
        val s = 22
        val bmp = createBitmap(s, s)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.argb(200, 200, 200, 200)
            strokeWidth = 2f
        }
        c.drawRoundRect(2f, 2f, s - 2f, s - 2f, 3f, 3f, p)
        return Texture2D.fromBitmap(bmp)
    }

    private fun createCheckboxChecked(): Texture2D {
        val s = 22
        val bmp = createBitmap(s, s)
        val c = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(0, 120, 215)
        }
        c.drawRoundRect(2f, 2f, s - 2f, s - 2f, 3f, 3f, fill)
        val check = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.WHITE
            strokeWidth = 2.5f
        }
        val path = Path().apply {
            moveTo(5f, 11f); lineTo(9f, 15f); lineTo(17f, 6f)
        }
        c.drawPath(path, check)
        return Texture2D.fromBitmap(bmp)
    }

    private fun createRadioUnselected(): Texture2D {
        val s = 22
        val bmp = createBitmap(s, s)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.argb(200, 200, 200, 200)
            strokeWidth = 2f
        }
        c.drawCircle(s / 2f, s / 2f, s / 2f - 2f, p)
        return Texture2D.fromBitmap(bmp)
    }

    private fun createRadioSelected(): Texture2D {
        val s = 22
        val bmp = createBitmap(s, s)
        val c = Canvas(bmp)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.rgb(0, 120, 215)
            strokeWidth = 2f
        }
        c.drawCircle(s / 2f, s / 2f, s / 2f - 2f, stroke)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(0, 120, 215)
        }
        c.drawCircle(s / 2f, s / 2f, s / 2f - 5f, fill)
        return Texture2D.fromBitmap(bmp)
    }

    private fun createSliderTrack(): Texture2D {
        val bmp = createBitmap(1, 8)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(80, 255, 255, 255)
        }
        c.drawRoundRect(0f, 1f, 1f, 7f, 3f, 3f, p)
        return Texture2D.fromBitmap(bmp)
    }

    private fun createSliderThumb(): Texture2D {
        val s = 18
        val bmp = createBitmap(s, s)
        val c = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(200, 200, 200)
        }
        c.drawCircle(s / 2f, s / 2f, s / 2f - 2f, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.rgb(120, 120, 120)
            strokeWidth = 1.5f
        }
        c.drawCircle(s / 2f, s / 2f, s / 2f - 2f, border)
        return Texture2D.fromBitmap(bmp)
    }

    private fun createDropdownArrow(): Texture2D {
        val s = 12
        val bmp = createBitmap(s, s)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
        }
        val path = Path().apply {
            moveTo(1f, 3f); lineTo(s / 2f, s - 3f); lineTo(s - 1f, 3f); close()
        }
        c.drawPath(path, p)
        return Texture2D.fromBitmap(bmp)
    }

    fun dispose() {
        themeTextures.values.forEach { it.dispose() }
    }
}