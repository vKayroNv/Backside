package ru.kayron.dew.graphics

import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import ru.kayron.dew.math.*

class SpriteFont(
    val texture: Texture2D,
    val glyphMap: Map<Char, GlyphInfo>,
    val lineSpacing: Float = 0f,
    val spacing: Float = 0f
) {
    data class GlyphInfo(
        val bounds: Rectangle,
        val width: Float,
        val character: Char
    )

    fun measureString(text: String): Vector2 {
        var width = 0f
        var height = lineSpacing
        for (ch in text) {
            val glyph = glyphMap[ch] ?: continue
            width += glyph.width + spacing
            if (glyph.bounds.height.toFloat() > height) {
                height = glyph.bounds.height.toFloat()
            }
        }
        return Vector2(width, height)
    }

    fun draw(batch: SpriteBatch, text: String, position: Vector2, color: Color) {
        var x = position.x
        for (ch in text) {
            val glyph = glyphMap[ch] ?: continue
            batch.draw(
                texture,
                Vector2(x, position.y),
                glyph.bounds,
                color
            )
            x += glyph.width + spacing
        }
    }

    companion object {
        fun fromSystemFont(
            fontName: String = "monospace",
            fontSize: Float = 24f,
            characters: String = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,!?:;()-+=_/\\\"'@#\$%^&*<>[]{}|~`"
        ): SpriteFont {
            val paint = Paint().apply {
                typeface = Typeface.create(fontName, Typeface.NORMAL)
                textSize = fontSize
                isAntiAlias = true
                isFakeBoldText = false
            }
            val metrics = paint.fontMetrics
            val lineHeight = (-metrics.ascent + metrics.descent).toInt()
            val glyphs = mutableMapOf<Char, GlyphInfo>()
            var totalWidth = 0
            for (ch in characters) {
                val width = paint.measureText(ch.toString()).toInt() + 2
                glyphs[ch] = GlyphInfo(Rectangle(totalWidth, 0, width, lineHeight), width.toFloat(), ch)
                totalWidth += width
            }
            val bitmap = createBitmap(totalWidth.coerceAtLeast(1), lineHeight.coerceAtLeast(1))
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            paint.color = android.graphics.Color.WHITE
            var x = 0f
            for (ch in characters) {
                canvas.drawText(ch.toString(), x, -metrics.ascent, paint)
                x += glyphs[ch]?.width ?: 0f
            }
            val texture = Texture2D.fromBitmap(bitmap)
            return SpriteFont(texture, glyphs, lineHeight.toFloat())
        }
    }
}
