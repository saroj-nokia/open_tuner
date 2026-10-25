package com.opentuner.app.music

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure mathematical functions for audio signal level analysis,
 * RMS calculation, decibel conversion, and noise gate threshold evaluation.
 */
object AudioSignalMath {

    const val MIN_DB = -80f
    const val MAX_DB = 0f

    // Standard preset thresholds for noise gate
    const val GATE_PRESET_QUIET = -54f     // Sensitive: quiet rooms, acoustic nylon/bass harmonics
    const val GATE_PRESET_NORMAL = -44f    // Balanced: filters computer fans and light room noise
    const val GATE_PRESET_REHEARSAL = -34f // Aggressive: band rehearsal rooms, stage bleed

    /**
     * Calculates the Root Mean Square (RMS) energy of a normalized audio buffer [-1.0, 1.0].
     * Returns 0.0f if buffer is empty.
     */
    fun calculateRms(buffer: FloatArray): Float {
        if (buffer.isEmpty()) return 0f
        var sumSquares = 0.0
        for (sample in buffer) {
            sumSquares += sample * sample
        }
        return sqrt(sumSquares / buffer.size).toFloat()
    }

    /**
     * Converts a linear RMS amplitude value to decibels relative to full scale (dBFS).
     * Returns [minDb] for near-zero or zero values.
     */
    fun rmsToDb(rms: Float, minDb: Float = MIN_DB): Float {
        if (rms <= 0.00001f) return minDb
        val db = 20f * log10(rms)
        return max(minDb, min(MAX_DB, db))
    }

    /**
     * Normalizes a decibel value into a [0.0, 1.0] range for UI meters.
     * Maps [meterFloorDb] to 0.0, and [meterCeilDb] to 1.0.
     */
    fun normalizeDb(
        db: Float,
        meterFloorDb: Float = -60f,
        meterCeilDb: Float = MAX_DB,
    ): Float {
        if (meterCeilDb <= meterFloorDb) return 0f
        val clamped = max(meterFloorDb, min(meterCeilDb, db))
        return (clamped - meterFloorDb) / (meterCeilDb - meterFloorDb)
    }

    /**
     * Determines whether the given signal RMS exceeds the noise gate threshold.
     */
    fun isAboveGate(rms: Float, gateThresholdDb: Float): Boolean {
        val db = rmsToDb(rms)
        return db >= gateThresholdDb
    }
}
