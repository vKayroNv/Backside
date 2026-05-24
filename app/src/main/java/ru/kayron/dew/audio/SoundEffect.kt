package ru.kayron.dew.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

private fun ByteArray.getShortAt(offset: Int): Short {
    return (((this[offset + 1].toInt() and 0xFF) shl 8) or (this[offset].toInt() and 0xFF)).toShort()
}

private fun ByteArray.getIntAt(offset: Int): Int {
    return ((this[offset + 3].toInt() and 0xFF) shl 24) or
           ((this[offset + 2].toInt() and 0xFF) shl 16) or
           ((this[offset + 1].toInt() and 0xFF) shl 8) or
           (this[offset].toInt() and 0xFF)
}

class SoundEffect(
    val buffer: ByteArray,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val bitsPerSample: Int = 16,
    val duration: Long = 0L
) {
    private var instances = mutableListOf<SoundEffectInstance>()

    fun createInstance(): SoundEffectInstance {
        val instance = SoundEffectInstance(this)
        instances.add(instance)
        return instance
    }

    fun play(volume: Float = 1f, pitch: Float = 0f, pan: Float = 0f) {
        val instance = createInstance()
        instance.volume = volume
        instance.pitch = pitch
        instance.pan = pan
        instance.play()
    }

    fun dispose() {
        for (inst in instances.toList()) {
            inst.dispose()
        }
        instances.clear()
    }

    internal fun releaseInstance(instance: SoundEffectInstance) {
        instances.remove(instance)
    }

    companion object {
        fun fromWav(inputStream: InputStream): SoundEffect {
            val data = inputStream.use { it.readBytes() }
            if (data.size < 12) throw RuntimeException("Invalid WAV file")
            val riff = String(data, 0, 4)
            if (riff != "RIFF") throw RuntimeException("Not a WAV file")
            val wave = String(data, 8, 4)
            if (wave != "WAVE") throw RuntimeException("Not a WAVE file")
            var offset = 12
            var format: Short = 1
            var channels: Short = 1
            var sampleRate = 44100
            var bitsPerSample: Short = 16
            var audioData = ByteArray(0)

            while (offset < data.size - 8) {
                val chunkId = String(data, offset, 4)
                val chunkSize = data.getIntAt(offset + 4)
                offset += 8
                if (chunkSize < 0 || offset + chunkSize > data.size) {
                    throw RuntimeException("Invalid WAV chunk: $chunkId")
                }
                when (chunkId) {
                    "fmt " -> {
                        format = data.getShortAt(offset)
                        channels = data.getShortAt(offset + 2)
                        sampleRate = data.getIntAt(offset + 4)
                        bitsPerSample = data.getShortAt(offset + 14)
                    }
                    "data" -> {
                        audioData = data.copyOfRange(offset, offset + chunkSize)
                    }
                }
                offset += chunkSize + (chunkSize and 1)
            }
            if (format.toInt() != 1) {
                throw RuntimeException("Only PCM WAV is supported, format=$format")
            }
            if (audioData.isEmpty()) {
                throw RuntimeException("WAV data chunk is empty")
            }
            val durationMs = if (sampleRate > 0) audioData.size.toLong() * 1000 / (sampleRate * channels * bitsPerSample / 8) else 0L
            return SoundEffect(audioData, sampleRate, channels.toInt(), bitsPerSample.toInt(), durationMs)
        }

        fun fromOgg(inputStream: InputStream): SoundEffect {
            val data = inputStream.use { it.readBytes() }
            val source = ByteArrayMediaDataSource(data)
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            try {
                extractor.setDataSource(source)
                var trackIndex = -1
                var inputFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        trackIndex = i
                        inputFormat = format
                        break
                    }
                }
                if (trackIndex < 0 || inputFormat == null) {
                    throw RuntimeException("No audio track found in OGG")
                }
                extractor.selectTrack(trackIndex)

                val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                    ?: throw RuntimeException("Missing OGG mime type")
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(inputFormat, null, null, 0)
                codec.start()

                val output = ByteArrayOutputStream(data.size * 2)
                val info = MediaCodec.BufferInfo()
                var sawInputEnd = false
                var sawOutputEnd = false
                var sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else 44100
                var channels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else 2
                val timeoutUs = 10_000L

                while (!sawOutputEnd) {
                    if (!sawInputEnd) {
                        val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                            val sampleSize = if (inputBuffer != null) {
                                extractor.readSampleData(inputBuffer, 0)
                            } else {
                                -1
                            }
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                sawInputEnd = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime.coerceAtLeast(0L),
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(info, timeoutUs)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outputFormat = codec.outputFormat
                            if (outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            }
                            if (outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (outputIndex >= 0) {
                            val outputBuffer: ByteBuffer? = codec.getOutputBuffer(outputIndex)
                            if (outputBuffer != null && info.size > 0) {
                                val bytes = ByteArray(info.size)
                                outputBuffer.position(info.offset)
                                outputBuffer.limit(info.offset + info.size)
                                outputBuffer.get(bytes)
                                output.write(bytes)
                            }
                            sawOutputEnd = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }

                val pcm = output.toByteArray()
                val durationMs = if (sampleRate > 0 && channels > 0) {
                    pcm.size.toLong() * 1000L / (sampleRate * channels * 2L)
                } else 0L
                return SoundEffect(pcm, sampleRate, channels, 16, durationMs)
            } finally {
                try {
                    codec?.stop()
                } catch (_: Exception) {
                }
                codec?.release()
                extractor.release()
                source.close()
            }
        }
    }
}

private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) return -1
        val length = minOf(size, data.size - position.toInt())
        System.arraycopy(data, position.toInt(), buffer, offset, length)
        return length
    }

    override fun getSize(): Long = data.size.toLong()

    override fun close() {}
}
