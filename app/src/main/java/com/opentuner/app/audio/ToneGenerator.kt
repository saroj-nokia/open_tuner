package com.opentuner.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pluggable audio source for reference tones.
 * Allows seamless drop-in of recorded audio samples from res/raw in the future.
 */
interface StringAudioSource {
    fun getAudio(freq: Double, seconds: Double, sampleRate: Int): ShortArray
}

/**
 * Karplus-Strong physical modeling plucked-string synthesis.
 * Simulates a plucked acoustic string with pick comb-filtering,
 * fractional delay tuning, and natural frequency-dependent decay.
 */
class KarplusStrongSynth(
    private val randomSeed: Long = 1337L,
) : StringAudioSource {

    override fun getAudio(freq: Double, seconds: Double, sampleRate: Int): ShortArray {
        val totalSamples = (sampleRate * seconds).toInt()
        val delay = (sampleRate / freq) - 0.5
        val delayInt = delay.toInt().coerceAtLeast(2)
        val frac = (delay - delayInt).coerceIn(0.0, 1.0)

        val bufSize = delayInt + 2
        val ring = DoubleArray(bufSize)

        // Seed initial pluck excitation with deterministic random noise
        val rng = Random(randomSeed xor java.lang.Double.doubleToRawLongBits(freq))
        for (i in 0 until delayInt) {
            ring[i] = rng.nextDouble() * 2.0 - 1.0
        }

        // Pick comb filter (simulates pick distance from bridge, approx 1/6th string length)
        val pickDist = (delayInt / 6).coerceIn(1, delayInt - 1)
        for (i in delayInt - 1 downTo pickDist) {
            ring[i] = 0.5 * (ring[i] - ring[i - pickDist])
        }

        // Decay constant tuned for acoustic resonance
        val decay = 0.996.coerceAtMost(1.0 - 0.0008 * (freq / 440.0).coerceIn(0.2, 3.0))

        val output = ShortArray(totalSamples)
        var writeIdx = 0
        var prevSample = 0.0

        for (n in 0 until totalSamples) {
            // Read fractional delay from the past
            val readIdx1 = (writeIdx - delayInt + bufSize) % bufSize
            val readIdx2 = (writeIdx - delayInt - 1 + bufSize) % bufSize
            val delayed = (1.0 - frac) * ring[readIdx1] + frac * ring[readIdx2]

            // Two-point lowpass filter (averaging adjacent samples)
            val filtered = 0.5 * (delayed + prevSample)
            prevSample = delayed

            val feedback = filtered * decay
            ring[writeIdx] = feedback
            writeIdx = (writeIdx + 1) % bufSize

            // Fade out smoothly over last 1000 samples to prevent click
            val fade = if (n > totalSamples - 1000) (totalSamples - n) / 1000.0 else 1.0

            // Low frequency harmonic enhancement for mobile phone speakers (< 90 Hz)
            val bassHarmonic = if (freq < 90.0) {
                0.25 * sin(4.0 * PI * freq * n / sampleRate) * exp(-1.8 * n / sampleRate)
            } else 0.0

            // Soft saturation to add warm acoustic tone and prevent clipping
            val s = (feedback + bassHarmonic) * fade
            val saturated = s / (1.0 + 0.25 * abs(s))
            output[n] = (saturated.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.85).toInt().toShort()
        }

        return output
    }
}

/**
 * Future-proof audio source that can load recorded audio files from res/raw,
 * adjusting playback pitch to match target frequency, falling back to Karplus-Strong.
 */
class SampledStringAudioSource(
    private val context: Context? = null,
    private val fallback: StringAudioSource = KarplusStrongSynth(),
) : StringAudioSource {
    override fun getAudio(freq: Double, seconds: Double, sampleRate: Int): ShortArray {
        // When real audio samples are dropped into res/raw, they can be unpacked and pitch-adjusted here.
        // Gracefully defaults to KarplusStrongSynth.
        return fallback.getAudio(freq, seconds, sampleRate)
    }
}

/** Plucked-string audio generator playing through an AudioTrack. */
object ToneGenerator {
    private const val SAMPLE_RATE = 44100
    private var track: AudioTrack? = null

    /** Active audio source; can be swapped with SampledStringAudioSource or custom provider. */
    var audioSource: StringAudioSource = KarplusStrongSynth()

    @Synchronized
    fun play(freq: Double, seconds: Double = 2.4) {
        stop()
        val data = audioSource.getAudio(freq, seconds, SAMPLE_RATE)
        val n = data.size
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

    /** Generates a crystalline dual-harmonic bell chime for in-tune feedback. */
    fun generateInTuneChime(
        sampleRate: Int = SAMPLE_RATE,
        seconds: Double = 0.22,
    ): ShortArray {
        val totalSamples = (sampleRate * seconds).toInt()
        val data = ShortArray(totalSamples)
        val f1 = 1046.50 // C6 fundamental bell
        val f2 = 2093.00 // C7 upper harmonic
        val f3 = 3135.96 // G7 sparkle

        val attackSamples = (sampleRate * 0.005).toInt().coerceAtLeast(1)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env1 = exp(-14.0 * t)
            val env2 = exp(-20.0 * t)
            val env3 = exp(-30.0 * t)
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0

            val sample = attack * (
                0.55 * sin(2.0 * PI * f1 * t) * env1 +
                0.32 * sin(2.0 * PI * f2 * t) * env2 +
                0.13 * sin(2.0 * PI * f3 * t) * env3
            )
            data[i] = (sample * 0.65 * Short.MAX_VALUE).toInt().toShort()
        }
        return data
    }

    @Synchronized
    fun playInTuneChime() {
        stop()
        val data = generateInTuneChime(SAMPLE_RATE)
        val n = data.size
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
    fun playStrum(
        frequencies: List<Double>,
        seconds: Double = 2.4,
        strumIntervalMs: Double = 32.0,
    ) {
        stop()
        if (frequencies.isEmpty()) return

        val offsetSamplesPerString = (SAMPLE_RATE * (strumIntervalMs / 1000.0)).toInt()
        val totalStrumDelaySamples = (frequencies.size - 1) * offsetSamplesPerString
        val singleStringSamples = (SAMPLE_RATE * seconds).toInt()
        val totalSamples = singleStringSamples + totalStrumDelaySamples

        val mixBuffer = FloatArray(totalSamples)

        for ((idx, freq) in frequencies.withIndex()) {
            if (freq <= 0.0) continue
            val stringAudio = audioSource.getAudio(freq, seconds, SAMPLE_RATE)
            val startOffset = idx * offsetSamplesPerString
            for (i in stringAudio.indices) {
                if (startOffset + i < totalSamples) {
                    mixBuffer[startOffset + i] += stringAudio[i].toFloat() / Short.MAX_VALUE
                }
            }
        }

        val count = frequencies.size.coerceAtLeast(1)
        val headroom = 0.85f / kotlin.math.sqrt(count.toFloat())
        val output = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val sample = mixBuffer[i] * headroom
            val saturated = sample / (1.0f + kotlin.math.abs(sample) * 0.2f)
            output[i] = (saturated.coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
        }

        val n = output.size
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
        t.write(output, 0, n)
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
