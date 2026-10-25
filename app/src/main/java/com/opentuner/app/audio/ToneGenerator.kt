package com.opentuner.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Plucked-string style reference tone. Harmonics are mixed in so low notes are audible on phone speakers. */
object ToneGenerator {
    private const val SAMPLE_RATE = 44100
    private var track: AudioTrack? = null

    @Synchronized
    fun play(freq: Double, seconds: Double = 2.4) {
        stop()
        val n = (SAMPLE_RATE * seconds).toInt()
        val data = ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-1.6 * t) * min(1.0, t / 0.01)
            val s = (sin(2 * PI * freq * t) + 0.6 * sin(4 * PI * freq * t) + 0.35 * sin(6 * PI * freq * t)) / 1.95
            (s * envelope * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(n * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(data, 0, n)
        t.play()
        track = t
    }

    @Synchronized
    fun stop() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }
}
