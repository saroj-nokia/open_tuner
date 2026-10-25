package com.opentuner.app.music

import kotlin.math.abs
import kotlin.math.sign

/**
 * Pure calculation logic for stroboscopic tuner display drift velocity and phase wrapping.
 */
object StrobeMath {
    /**
     * In-tune deadband tolerance in cents where the strobe comes to a complete standstill.
     * When pitch deviation is within this band, the pattern is stationary.
     */
    const val IN_TUNE_TOLERANCE_CENTS = 1.0f

    /**
     * Maximum drift speed in dp/second to keep the visual pattern clear without motion blur.
     */
    const val MAX_DRIFT_SPEED_DPS = 240f

    /**
     * Calculates the horizontal drift speed in dp per second.
     *
     * @param cents Deviation in cents from the target note (-50..+50).
     * @param hasSignal Whether a valid audio pitch is currently detected.
     * @param inTune Whether the tuner state has locked in tune.
     * @return Drift speed in dp/second:
     *   > 0 = sharp (drifts right),
     *   < 0 = flat (drifts left),
     *   0.0 = stationary / locked in tune.
     */
    fun calculateDriftSpeed(
        cents: Float,
        hasSignal: Boolean,
        inTune: Boolean = false,
    ): Float {
        if (!hasSignal) return 0f
        val absCents = abs(cents)
        // If system marked inTune and within fine threshold, immediately freeze pattern
        if (inTune && absCents <= 2.5f) return 0f
        if (absCents <= IN_TUNE_TOLERANCE_CENTS) return 0f

        val effectiveCents = absCents - IN_TUNE_TOLERANCE_CENTS
        val rawSpeed = effectiveCents * 6.0f
        val clampedSpeed = rawSpeed.coerceAtMost(MAX_DRIFT_SPEED_DPS)
        return sign(cents) * clampedSpeed
    }

    /**
     * Wraps a continuous phase offset into [0, period) for cyclic canvas rendering.
     */
    fun wrapPhase(phase: Float, period: Float): Float {
        if (period <= 0f) return 0f
        val rem = phase % period
        return if (rem < 0f) rem + period else rem
    }
}
