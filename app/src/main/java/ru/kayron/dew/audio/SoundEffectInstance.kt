package ru.kayron.dew.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.PlaybackParams
import android.media.AudioTrack
import kotlin.math.pow

class SoundEffectInstance(private val soundEffect: SoundEffect) {
    var volume: Float = 1f
    var pitch: Float = 0f
    var pan: Float = 0f
    var isLooped: Boolean = false

    private var audioTrack: AudioTrack? = null
    private var state: SoundState = SoundState.Stopped

    enum class SoundState {
        Playing,
        Paused,
        Stopped,
    }

    fun play() {
        if (state == SoundState.Playing) return
        stop()
        val channelConfig = when (soundEffect.channels) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_STEREO
        }
        val audioFormat = when (soundEffect.bitsPerSample) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }
        val bufferSize = soundEffect.buffer.size.coerceAtLeast(
            AudioTrack.getMinBufferSize(soundEffect.sampleRate, channelConfig, audioFormat)
        )
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(soundEffect.sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack = track
        track.write(soundEffect.buffer, 0, soundEffect.buffer.size)
        val bytesPerFrame = (soundEffect.channels * soundEffect.bitsPerSample / 8).coerceAtLeast(1)
        val frameCount = soundEffect.buffer.size / bytesPerFrame
        if (isLooped) {
            if (frameCount > 0) {
                track.setLoopPoints(0, frameCount, -1)
            }
        } else if (frameCount > 0) {
            track.notificationMarkerPosition = frameCount
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack) {
                    stop()
                }

                override fun onPeriodicNotification(track: AudioTrack) = Unit
            })
        }
        applyPlaybackParameters(track)
        track.play()
        state = SoundState.Playing
    }

    private fun applyPlaybackParameters(track: AudioTrack) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        val clampedPan = pan.coerceIn(-1f, 1f)
        val leftVolume = clampedVolume * (1f - clampedPan).coerceIn(0f, 1f)
        val rightVolume = clampedVolume * (1f + clampedPan).coerceIn(0f, 1f)
        val pitchFactor = 2f.pow(pitch.coerceIn(-1f, 1f))

        @Suppress("DEPRECATION")
        track.setStereoVolume(leftVolume, rightVolume)
        track.playbackParams = PlaybackParams().setSpeed(1f).setPitch(pitchFactor)
    }

    fun pause() {
        audioTrack?.pause()
        state = SoundState.Paused
    }

    fun resume() {
        audioTrack?.play()
        state = SoundState.Playing
    }

    fun stop() {
        audioTrack?.let { track ->
            try {
                track.stop()
            } catch (_: IllegalStateException) {
            }
            track.release()
        }
        audioTrack = null
        state = SoundState.Stopped
        soundEffect.releaseInstance(this)
    }

    fun dispose() {
        stop()
    }
}
