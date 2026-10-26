package com.opentuner.app.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

data class TimeSignature(
    val numerator: Int,
    val denominator: Int,
) {
    val label: String get() = "$numerator/$denominator"

    companion object {
        val all = listOf(
            TimeSignature(2, 4),
            TimeSignature(3, 4),
            TimeSignature(4, 4),
            TimeSignature(5, 4),
            TimeSignature(6, 8),
            TimeSignature(7, 8),
        )
    }
}

object MetronomeMath {
    const val MIN_BPM = 30
    const val MAX_BPM = 300

    /**
     * Calculates the exact number of audio samples between metronome clicks.
     * Guaranteed sample-accurate: for quarter-note meters (denom=4), 1 click = 1 beat;
     * for eighth-note meters (denom=8), 1 click = 1 eighth-note pulse.
     */
    fun samplesPerBeat(bpm: Int, timeSignature: TimeSignature, sampleRate: Int = 44100): Int {
        val clampedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
        val beatsPerSecond = clampedBpm / 60.0
        val pulseRate = if (timeSignature.denominator == 8) beatsPerSecond * 2.0 else beatsPerSecond
        return (sampleRate / pulseRate).roundToInt().coerceAtLeast(1)
    }

    /**
     * Calculates BPM from a sequence of millisecond tap timestamps.
     * Returns null if not enough valid taps are available.
     */
    fun calculateTapBpm(tapTimesMs: List<Long>): Int? {
        if (tapTimesMs.size < 2) return null
        val intervals = mutableListOf<Long>()
        for (i in 1 until tapTimesMs.size) {
            val diff = tapTimesMs[i] - tapTimesMs[i - 1]
            // Accept intervals corresponding to 24 - 400 BPM (150ms to 2500ms)
            if (diff in 150..2500) {
                intervals.add(diff)
            }
        }
        if (intervals.isEmpty()) return null
        val avgMs = intervals.average()
        val calculated = (60000.0 / avgMs).roundToInt()
        return calculated.coerceIn(MIN_BPM, MAX_BPM)
    }
}

/**
 * Sample-accurate audio engine for the metronome.
 * Generates PCM click samples directly into a streaming AudioTrack on a real-time audio thread,
 * eliminating all timer drift and OS thread scheduling jitter.
 */
class MetronomeEngine(
    private val sampleRate: Int = 44100,
) {
    @Volatile private var isRunning = false

    // Bumped on every start/stop so a stale audio thread can never keep playing after a quick stop-then-start.
    @Volatile private var runId = 0

    @Volatile var bpm: Int = 120
    @Volatile var timeSignature: TimeSignature = TimeSignature(4, 4)
    @Volatile var accentFirstBeat: Boolean = true

    var onBeatCallback: ((beatIndex: Int, isAccent: Boolean) -> Unit)? = null

    // Pre-synthesized crisp percussive clicks
    private val accentClick: FloatArray = generateClick(freq = 2500.0, durationMs = 18.0)
    private val regularClick: FloatArray = generateClick(freq = 1500.0, durationMs = 15.0)

    private fun generateClick(freq: Double, durationMs: Double): FloatArray {
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        return FloatArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val envelope = exp(-320.0 * t) // Sharp exponential decay
            val wave = sin(2.0 * PI * freq * t)
            (wave * envelope * 0.95).toFloat()
        }
    }

    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = max(minBufSize, 4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.play()
        val id = ++runId

        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                runAudioLoop(track, id)
            } finally {
                // This thread owns the track, so stop() never makes the UI thread wait on the audio driver.
                runCatching {
                    track.pause()
                    track.flush()
                }
                track.release()
            }
        }, "MetronomeStreamThread").start()
    }

    private fun runAudioLoop(track: AudioTrack, id: Int) {
        val chunkSize = 512
        val pcm = ShortArray(chunkSize)
        var sampleInBeat = 0
        var currentBeat = 0

        while (isRunning && runId == id) {
            val interval = MetronomeMath.samplesPerBeat(bpm, timeSignature, sampleRate)
            val sig = timeSignature
            val accent = accentFirstBeat

            for (i in 0 until chunkSize) {
                if (sampleInBeat == 0) {
                    val isAccent = accent && (currentBeat == 0)
                    onBeatCallback?.invoke(currentBeat, isAccent)
                }

                val isAccent = accent && (currentBeat == 0)
                val clickSamples = if (isAccent) accentClick else regularClick

                val sampleVal = if (sampleInBeat < clickSamples.size) {
                    clickSamples[sampleInBeat]
                } else {
                    0f
                }

                pcm[i] = (sampleVal * Short.MAX_VALUE).toInt().toShort()

                sampleInBeat++
                if (sampleInBeat >= interval) {
                    sampleInBeat = 0
                    currentBeat = (currentBeat + 1) % sig.numerator
                }
            }

            val written = track.write(pcm, 0, chunkSize)
            if (written < 0) break
        }
    }

    @Synchronized
    fun stop() {
        if (!isRunning) return
        isRunning = false
        runId++
    }

    fun isPlaying(): Boolean = isRunning
}
