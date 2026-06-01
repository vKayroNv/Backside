package ru.kayron.dew.content

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ru.kayron.dew.Game
import ru.kayron.dew.graphics.Texture2D
import ru.kayron.dew.audio.SoundEffect
import java.io.InputStream
import java.util.Locale

class ContentManager(private val game: Game) {
    var rootDirectory: String = "Content"
        private set
    private val loadedAssets = mutableMapOf<String, Any>()
    private var assetManager: AssetManager? = null

    fun setAssetManager(am: AssetManager) {
        assetManager = am
        Texture2D.setAssetManager(am)
    }

    fun setRootDirectory(path: String) {
        rootDirectory = path.trim('/').ifEmpty { "Content" }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> load(path: String): T {
        val key = "$rootDirectory/$path"
        loadedAssets[key]?.let { return it as T }
        val asset = loadAsset(key)
        loadedAssets[key] = asset
        return asset as T
    }

    private fun loadAsset(path: String): Any {
        val lowerPath = path.lowercase(Locale.ROOT)
        if (lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".bmp")) {
            val bitmap = openStream(path)?.use { BitmapFactory.decodeStream(it) }
                ?: throw RuntimeException("Texture not found: $path")
            return Texture2D.fromBitmap(bitmap, linear = false)
        }
        if (lowerPath.endsWith(".wav")) {
            val stream = openStream(path) ?: throw RuntimeException("Sound not found: $path")
            return SoundEffect.fromWav(stream)
        }
        if (lowerPath.endsWith(".ogg")) {
            val stream = openStream(path) ?: throw RuntimeException("Sound not found: $path")
            return SoundEffect.fromOgg(stream)
        }
        if (lowerPath.endsWith(".txt") || lowerPath.endsWith(".json") || lowerPath.endsWith(".xml") || lowerPath.endsWith(".glsl")) {
            val stream = openStream(path) ?: throw RuntimeException("File not found: $path")
            return stream.bufferedReader().use { it.readText() }
        }
        throw RuntimeException("Unsupported asset type: $path")
    }

    private fun openStream(path: String): InputStream? {
        val am = assetManager ?: return null
        return try {
            am.open(path)
        } catch (e: Exception) {
            try {
                am.open("$rootDirectory/$path")
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun <T> getLoaded(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return loadedAssets["$rootDirectory/$key"] as? T
    }

    fun unload() {
        for ((_, asset) in loadedAssets) {
            when (asset) {
                is Texture2D -> asset.dispose()
                is SoundEffect -> asset.dispose()
            }
        }
        loadedAssets.clear()
    }

    fun dispose() = unload()
}
