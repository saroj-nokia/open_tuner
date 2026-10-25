package com.opentuner.app.audio

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class PitchResult(val frequency: Float, val confidence: Float)

/** YIN pitch detector (de Cheveigné & Kawahara, 2002). Reuses scratch buffers, so one thread only. */
class YinPitchDetector(
    private val sampleRate: Int,
    private val threshold: Float = 0.15f,
    private val minFreq: Float = 30f,
    private val maxFreq: Float = 1500f,
    private val silenceRms: Float = 0.008f,
) {
    private var diff = FloatArray(0)
    private var cmnd = FloatArray(0)

    fun detect(buf: FloatArray): PitchResult? {
        var energy = 0f
        for (x in buf) energy += x * x
        if (sqrt(energy / buf.size) < silenceRms) return null

        val half = buf.size / 2
        val tauMax = min(half, (sampleRate / minFreq).toInt())
        val tauMin = max(2, (sampleRate / maxFreq).toInt())
        if (diff.size != tauMax + 1) {
            diff = FloatArray(tauMax + 1)
            cmnd = FloatArray(tauMax + 1)
        }

        // Difference function, then cumulative mean normalisation.
        for (tau in 1..tauMax) {
            var sum = 0f
            for (i in 0 until half) {
                val d = buf[i] - buf[i + tau]
                sum += d * d
            }
            diff[tau] = sum
        }
        cmnd[0] = 1f
        var running = 0f
        for (tau in 1..tauMax) {
            running += diff[tau]
            cmnd[tau] = if (running == 0f) 1f else diff[tau] * tau / running
        }

        // First dip under the threshold, walked down to its local minimum, refined parabolically.
        var tau = tauMin
        while (tau < tauMax) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 < tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                val s0 = cmnd[tau - 1]
                val s1 = cmnd[tau]
                val s2 = cmnd[tau + 1]
                val denom = 2f * (2f * s1 - s2 - s0)
                val better = if (denom == 0f) tau.toFloat() else tau + (s2 - s0) / denom
                return PitchResult(sampleRate / better, 1f - s1)
            }
            tau++
        }
        return null
    }
}
