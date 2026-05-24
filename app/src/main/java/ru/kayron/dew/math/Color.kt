package ru.kayron.dew.math

import kotlin.math.pow
import kotlin.math.roundToInt

data class Color(val packedValue: UInt = 0xFFFFFFFFu) {

    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        ((a.toUInt() and 0xFFu) shl 24) or
        ((b.toUInt() and 0xFFu) shl 16) or
        ((g.toUInt() and 0xFFu) shl 8) or
        (r.toUInt() and 0xFFu)
    )

    constructor(r: Float, g: Float, b: Float, a: Float = 1f) : this(
        ((a * 255f).roundToInt().coerceIn(0, 255)),
        ((b * 255f).roundToInt().coerceIn(0, 255)),
        ((g * 255f).roundToInt().coerceIn(0, 255)),
        ((r * 255f).roundToInt().coerceIn(0, 255))
    )

    constructor(vector3: Vector3) : this(vector3.x, vector3.y, vector3.z)
    constructor(vector4: Vector4) : this(vector4.x, vector4.y, vector4.z, vector4.w)

    val r: Int get() = (packedValue and 0xFFu).toInt()
    val g: Int get() = ((packedValue shr 8) and 0xFFu).toInt()
    val b: Int get() = ((packedValue shr 16) and 0xFFu).toInt()
    val a: Int get() = ((packedValue shr 24) and 0xFFu).toInt()

    val rf: Float get() = r / 255f
    val gf: Float get() = g / 255f
    val bf: Float get() = b / 255f
    val af: Float get() = a / 255f

    fun toVector3(): Vector3 = Vector3(rf, gf, bf)
    fun toVector4(): Vector4 = Vector4(rf, gf, bf, af)

    fun toArray(): FloatArray = floatArrayOf(rf, gf, bf, af)

    companion object {
        val Transparent = Color(0u)
        val Black = Color(0x000000FFu)
        val White = Color(0xFFFFFFFFu)
        val Red = Color(0x0000FFFFu)
        val Green = Color(0x00FF00FFu)
        val Blue = Color(0xFF0000FFu)
        val Yellow = Color(0x00FFFFFFu)
        val Cyan = Color(0xFFFF00FFu)
        val Magenta = Color(0xFF00FFFFu)
        val Orange = Color(0x0080FFFFu)
        val Purple = Color(0x800080FFu)
        val Gray = Color(0x808080FFu)
        val DarkGray = Color(0x444444FFu)
        val LightGray = Color(0xC0C0C0FFu)
        val Pink = Color(0xC8A3ECFFu)
        val CornflowerBlue = Color(0xED9564FFu)
        val DarkBlue = Color(0xA00000FFu)
        val DarkGreen = Color(0x006400FFu)
        val DarkRed = Color(0x00008BFFu)
        val DarkCyan = Color(0x8B8B00FFu)
        val DarkMagenta = Color(0x8B008BFFu)
        val DarkOrange = Color(0x008CFF00u)
        val DarkSalmon = Color(0x7A96E9FFu)
        val DarkSeaGreen = Color(0x8FBC8FFFu)
        val DarkSlateBlue = Color(0x8B3D48FFu)
        val DarkSlateGray = Color(0x4F4F2Fu)
        val DarkTurquoise = Color(0xD1CE00FFu)
        val DarkViolet = Color(0xD30094FFu)
        val DeepPink = Color(0x9314FFu)
        val DeepSkyBlue = Color(0xFFBF00FFu)
        val DimGray = Color(0x696969FFu)
        val DodgerBlue = Color(0xFF901EFFu)
        val Firebrick = Color(0x2222B2FFu)
        val FloralWhite = Color(0xF0FAFFF0u)
        val ForestGreen = Color(0x228B22FFu)
        val Fuchsia = Color(0xFF00FFFFu)
        val Gainsboro = Color(0xDCDCDCFFu)
        val GhostWhite = Color(0xFFF8F8FFu)
        val Gold = Color(0x00D7FFu)
        val Goldenrod = Color(0x20A5DAFFu)
        val GreenYellow = Color(0x2FFFADFFu)
        val Honeydew = Color(0xF0FFF0FFu)
        val HotPink = Color(0xB469FFu)
        val IndianRed = Color(0x5C5CCDFFu)
        val Indigo = Color(0x82004BFFu)
        val Ivory = Color(0xF0FFFFF0u)
        val Khaki = Color(0x8CE6F0FFu)
        val Lavender = Color(0xFAE6E6FFu)
        val LavenderBlush = Color(0xF5F0FFu)
        val LawnGreen = Color(0x00FC7CFu)
        val LemonChiffon = Color(0xCDFAFFF0u)
        val LightBlue = Color(0xE6D8ADFFu)
        val LightCoral = Color(0x8080F0FFu)
        val LightCyan = Color(0xFFFFE0FFu)
        val LightGoldenrodYellow = Color(0xD2FAFAFFu)
        val LightGreen = Color(0x90EE90FFu)
        val LightPink = Color(0xC1B6FFFFu)
        val LightSalmon = Color(0x7AA0FFFFu)
        val LightSeaGreen = Color(0xAAB220FFu)
        val LightSkyBlue = Color(0xFACE87FFu)
        val LightSlateGray = Color(0x998877FFu)
        val LightSteelBlue = Color(0xDEC4B0FFu)
        val LightYellow = Color(0xE0FFFFF0u)
        val Lime = Color(0x00FF00FFu)
        val LimeGreen = Color(0x32CD32FFu)
        val Linen = Color(0xE6F0FAFFu)
        val Maroon = Color(0x000080FFu)
        val MediumAquamarine = Color(0xAACD66FFu)
        val MediumBlue = Color(0xCD0000FFu)
        val MediumOrchid = Color(0xD355BAFFu)
        val MediumPurple = Color(0xDB7093FFu)
        val MediumSeaGreen = Color(0x71B33CFFu)
        val MediumSlateBlue = Color(0xEE687BFFu)
        val MediumSpringGreen = Color(0x9AFA00FFu)
        val MediumTurquoise = Color(0xCCD148FFu)
        val MediumVioletRed = Color(0x8515C7FFu)
        val MidnightBlue = Color(0x701919FFu)
        val MintCream = Color(0xFAFFF5FFu)
        val MistyRose = Color(0xE1E4FFFFu)
        val Moccasin = Color(0xB5E4FFFFu)
        val NavajoWhite = Color(0xADDEFFFFu)
        val Navy = Color(0x800000FFu)
        val OldLace = Color(0xE6F5FDFFu)
        val Olive = Color(0x008080FFu)
        val OliveDrab = Color(0x238E6BFFu)
        val OrangeRed = Color(0x0045FFFFu)
        val Orchid = Color(0xD670DAFFu)
        val PaleGoldenrod = Color(0xAAE8EEFFu)
        val PaleGreen = Color(0x98FB98FFu)
        val PaleTurquoise = Color(0xEEEEAFFFu)
        val PaleVioletRed = Color(0x9370DBFFu)
        val PapayaWhip = Color(0xD5EFFFFFu)
        val PeachPuff = Color(0xB9DAFFFFu)
        val Peru = Color(0x3F85CDFFu)
        val Plum = Color(0xDDA0DDFFu)
        val PowderBlue = Color(0xE6E0B0FFu)
        val RosyBrown = Color(0x8F8FBCFFu)
        val RoyalBlue = Color(0xE16941FFu)
        val SaddleBrown = Color(0x13458BFFu)
        val Salmon = Color(0x7280FAFFu)
        val SandyBrown = Color(0x60A4F4FFu)
        val SeaGreen = Color(0x578B2EFFu)
        val SeaShell = Color(0xEEF5FFFFu)
        val Sienna = Color(0x2D52A0FFu)
        val Silver = Color(0xC0C0C0FFu)
        val SkyBlue = Color(0xEBCE87FFu)
        val SlateBlue = Color(0xCD5A6AFFu)
        val SlateGray = Color(0x908070FFu)
        val Snow = Color(0xFAFAFFFFu)
        val SpringGreen = Color(0x7FFF00FFu)
        val SteelBlue = Color(0xB48246FFu)
        val Tan = Color(0x8CB4D2FFu)
        val Teal = Color(0x808000FFu)
        val Thistle = Color(0xD8BFD8FFu)
        val Tomato = Color(0x4763FFFFu)
        val Turquoise = Color(0xD0E040FFu)
        val Violet = Color(0xEE82EEFFu)
        val Wheat = Color(0xB3DEF5FFu)
        val WhiteSmoke = Color(0xF5F5F5FFu)
        val YellowGreen = Color(0x50FF9ACDu)
    }

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

    override fun toString(): String = "Color(r=$r, g=$g, b=$b, a=$a)"
}
