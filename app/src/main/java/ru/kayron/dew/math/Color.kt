package ru.kayron.dew.math

import kotlin.math.roundToInt

data class Color(val packedValue: UInt = 0xFFFFFFFFu) {

    /**
     * Формат:
     * RRGGBBAA
     */
    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        ((r.toUInt() and 0xFFu) shl 24) or
        ((g.toUInt() and 0xFFu) shl 16) or
        ((b.toUInt() and 0xFFu) shl 8) or
        (a.toUInt() and 0xFFu)
    )

    constructor(r: Float, g: Float, b: Float, a: Float = 1f) : this(
        (r * 255f).roundToInt().coerceIn(0, 255),
        (g * 255f).roundToInt().coerceIn(0, 255),
        (b * 255f).roundToInt().coerceIn(0, 255),
        (a * 255f).roundToInt().coerceIn(0, 255)
    )

    constructor(vector3: Vector3) : this(vector3.x, vector3.y, vector3.z)
    constructor(vector4: Vector4) : this(vector4.x, vector4.y, vector4.z, vector4.w)

    val r: Int get() = ((packedValue shr 24) and 0xFFu).toInt()
    val g: Int get() = ((packedValue shr 16) and 0xFFu).toInt()
    val b: Int get() = ((packedValue shr 8) and 0xFFu).toInt()
    val a: Int get() = (packedValue and 0xFFu).toInt()

    val rf: Float get() = r / 255f
    val gf: Float get() = g / 255f
    val bf: Float get() = b / 255f
    val af: Float get() = a / 255f

    fun toVector3(): Vector3 = Vector3(rf, gf, bf)
    fun toVector4(): Vector4 = Vector4(rf, gf, bf, af)

    fun toArray(): FloatArray =
        floatArrayOf(rf, gf, bf, af)

    fun multiply(other: Color): Color = Color(
        (r * other.r / 255f).roundToInt(),
        (g * other.g / 255f).roundToInt(),
        (b * other.b / 255f).roundToInt(),
        (a * other.a / 255f).roundToInt()
    )

    operator fun times(other: Color) = multiply(other)

    operator fun plus(other: Color): Color = Color(
        (r + other.r).coerceAtMost(255),
        (g + other.g).coerceAtMost(255),
        (b + other.b).coerceAtMost(255),
        (a + other.a).coerceAtMost(255)
    )

    override fun toString(): String =
        "Color(r=$r, g=$g, b=$b, a=$a)"

    companion object {

        val Transparent = Color(0x00000000u)

        val Black = Color(0x000000FFu)
        val White = Color(0xFFFFFFFFu)

        val Red = Color(0xFF0000FFu)
        val Green = Color(0x00FF00FFu)
        val Blue = Color(0x0000FFFFu)

        val Yellow = Color(0xFFFF00FFu)
        val Cyan = Color(0x00FFFFFFu)
        val Magenta = Color(0xFF00FFFFu)

        val Orange = Color(0xFFA500FFu)
        val Purple = Color(0x800080FFu)
        val Gray = Color(0x808080FFu)

        val DarkGray = Color(0x444444FFu)
        val LightGray = Color(0xC0C0C0FFu)

        val Pink = Color(0xFFC0CBFFu)
        val CornflowerBlue = Color(0x6495EDFFu)

        val DarkBlue = Color(0x00008BFFu)
        val DarkGreen = Color(0x006400FFu)
        val DarkRed = Color(0x8B0000FFu)

        val DarkCyan = Color(0x008B8BFFu)
        val DarkMagenta = Color(0x8B008BFFu)
        val DarkOrange = Color(0xFF8C00FFu)

        val DarkSalmon = Color(0xE9967AFFu)
        val DarkSeaGreen = Color(0x8FBC8FFFu)
        val DarkSlateBlue = Color(0x483D8BFFu)

        val DarkSlateGray = Color(0x2F4F4FFFu)
        val DarkTurquoise = Color(0x00CED1FFu)
        val DarkViolet = Color(0x9400D3FFu)

        val DeepPink = Color(0xFF1493FFu)
        val DeepSkyBlue = Color(0x00BFFFFFu)
        val DimGray = Color(0x696969FFu)

        val DodgerBlue = Color(0x1E90FFFFu)
        val Firebrick = Color(0xB22222FFu)
        val FloralWhite = Color(0xFFFAF0FFu)

        val ForestGreen = Color(0x228B22FFu)
        val Fuchsia = Color(0xFF00FFFFu)
        val Gainsboro = Color(0xDCDCDCFFu)

        val GhostWhite = Color(0xF8F8FFFFu)
        val Gold = Color(0xFFD700FFu)
        val Goldenrod = Color(0xDAA520FFu)

        val GreenYellow = Color(0xADFF2FFFu)
        val Honeydew = Color(0xF0FFF0FFu)
        val HotPink = Color(0xFF69B4FFu)

        val IndianRed = Color(0xCD5C5CFFu)
        val Indigo = Color(0x4B0082FFu)
        val Ivory = Color(0xFFFFF0FFu)

        val Khaki = Color(0xF0E68CFFu)
        val Lavender = Color(0xE6E6FAFFu)
        val LavenderBlush = Color(0xFFF0F5FFu)

        val LawnGreen = Color(0x7CFC00FFu)
        val LemonChiffon = Color(0xFFFACDFFu)
        val LightBlue = Color(0xADD8E6FFu)

        val LightCoral = Color(0xF08080FFu)
        val LightCyan = Color(0xE0FFFFFFu)
        val LightGoldenrodYellow = Color(0xFAFAD2FFu)

        val LightGreen = Color(0x90EE90FFu)
        val LightPink = Color(0xFFB6C1FFu)
        val LightSalmon = Color(0xFFA07AFFu)

        val LightSeaGreen = Color(0x20B2AAFFu)
        val LightSkyBlue = Color(0x87CEFAFFu)
        val LightSlateGray = Color(0x778899FFu)

        val LightSteelBlue = Color(0xB0C4DEFFu)
        val LightYellow = Color(0xFFFFE0FFu)

        val Lime = Color(0x00FF00FFu)
        val LimeGreen = Color(0x32CD32FFu)

        val Linen = Color(0xFAF0E6FFu)
        val Maroon = Color(0x800000FFu)

        val MediumAquamarine = Color(0x66CDAAFFu)
        val MediumBlue = Color(0x0000CDFFu)
        val MediumOrchid = Color(0xBA55D3FFu)

        val MediumPurple = Color(0x9370DBFFu)
        val MediumSeaGreen = Color(0x3CB371FFu)
        val MediumSlateBlue = Color(0x7B68EEFFu)

        val MediumSpringGreen = Color(0x00FA9AFFu)
        val MediumTurquoise = Color(0x48D1CCFFu)
        val MediumVioletRed = Color(0xC71585FFu)

        val MidnightBlue = Color(0x191970FFu)
        val MintCream = Color(0xF5FFFAFFu)
        val MistyRose = Color(0xFFE4E1FFu)

        val Moccasin = Color(0xFFE4B5FFu)
        val NavajoWhite = Color(0xFFDEADFFu)
        val Navy = Color(0x000080FFu)

        val OldLace = Color(0xFDF5E6FFu)
        val Olive = Color(0x808000FFu)
        val OliveDrab = Color(0x6B8E23FFu)

        val OrangeRed = Color(0xFF4500FFu)
        val Orchid = Color(0xDA70D6FFu)
        val PaleGoldenrod = Color(0xEEE8AAFFu)

        val PaleGreen = Color(0x98FB98FFu)
        val PaleTurquoise = Color(0xAFEEEEFFu)
        val PaleVioletRed = Color(0xDB7093FFu)

        val PapayaWhip = Color(0xFFEFD5FFu)
        val PeachPuff = Color(0xFFDAB9FFu)
        val Peru = Color(0xCD853FFFu)

        val Plum = Color(0xDDA0DDFFu)
        val PowderBlue = Color(0xB0E0E6FFu)
        val RosyBrown = Color(0xBC8F8FFFu)

        val RoyalBlue = Color(0x4169E1FFu)
        val SaddleBrown = Color(0x8B4513FFu)
        val Salmon = Color(0xFA8072FFu)

        val SandyBrown = Color(0xF4A460FFu)
        val SeaGreen = Color(0x2E8B57FFu)
        val SeaShell = Color(0xFFF5EEFFu)

        val Sienna = Color(0xA0522DFFu)
        val Silver = Color(0xC0C0C0FFu)
        val SkyBlue = Color(0x87CEEBFFu)

        val SlateBlue = Color(0x6A5ACDFFu)
        val SlateGray = Color(0x708090FFu)
        val Snow = Color(0xFFFAFAFFu)

        val SpringGreen = Color(0x00FF7FFFu)
        val SteelBlue = Color(0x4682B4FFu)
        val Tan = Color(0xD2B48CFFu)

        val Teal = Color(0x008080FFu)
        val Thistle = Color(0xD8BFD8FFu)
        val Tomato = Color(0xFF6347FFu)

        val Turquoise = Color(0x40E0D0FFu)
        val Violet = Color(0xEE82EEFFu)
        val Wheat = Color(0xF5DEB3FFu)

        val WhiteSmoke = Color(0xF5F5F5FFu)
        val YellowGreen = Color(0x9ACD32FFu)

        fun fromHex(hex: UInt): Color =
            Color(hex)

        fun fromRGB(hex: UInt): Color =
            Color((hex shl 8) or 0xFFu)
    }
}