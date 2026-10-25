package com.opentuner.app.music

import kotlin.math.abs

/**
 * Result of evaluating pitch stability against the in-tune confirmation window.
 *
 * @param shouldAffirm True exactly once when the pitch has been held in-tune for the required duration.
 * @param holdProgress Value between 0.0f and 1.0f indicating how long the pitch has been steadily held in-tune.
 * @param isLocked True whenever the pitch has locked into tune and is being maintained.
 */
data class InTuneTriggerResult(
    val shouldAffirm: Boolean,
    val holdProgress: Float,
    val isLocked: Boolean,
)

/**
 * Tracks pitch stability over time to trigger hands-free audible and haptic
 * in-tune affirmations. Prevents repetitive audio trigger chatter through
 * deadband hysteresis and debounce timeouts.
 */
class InTuneAffirmationTracker(
    val inTuneThresholdCents: Float = 4.0f,
    val requiredDurationMs: Long = 260L,
    val detuneDeadbandCents: Float = 6.5f,
    val silenceTimeoutMs: Long = 500L,
) {
    private var inTuneStartTime: Long? = null
    private var isAffirmed: Boolean = false
    private var lastTargetMidi: Int? = null
    private var lastSignalTime: Long = 0L

    /**
     * Updates the tracker with the latest pitch analysis frame.
     */
    fun update(
        targetMidi: Int?,
        cents: Float,
        hasSignal: Boolean,
        currentTimeMs: Long,
    ): InTuneTriggerResult {
        if (!hasSignal || targetMidi == null) {
            inTuneStartTime = null
            if (currentTimeMs - lastSignalTime > silenceTimeoutMs) {
                isAffirmed = false
                lastTargetMidi = null
            }
            return InTuneTriggerResult(shouldAffirm = false, holdProgress = 0f, isLocked = false)
        }

        lastSignalTime = currentTimeMs

        // If target string/note changed, reset affirmation state
        if (targetMidi != lastTargetMidi) {
            lastTargetMidi = targetMidi
            isAffirmed = false
            inTuneStartTime = null
        }

        val absCents = abs(cents)
        if (absCents <= inTuneThresholdCents) {
            if (inTuneStartTime == null) {
                inTuneStartTime = currentTimeMs
            }
            val elapsed = currentTimeMs - inTuneStartTime!!
            val progress = (elapsed.toFloat() / requiredDurationMs.toFloat()).coerceIn(0f, 1f)

            var fireAffirmation = false
            val locked = progress >= 1.0f

            if (locked && !isAffirmed) {
                isAffirmed = true
                fireAffirmation = true
            }

            return InTuneTriggerResult(
                shouldAffirm = fireAffirmation,
                holdProgress = progress,
                isLocked = locked,
            )
        } else {
            inTuneStartTime = null
            // Only re-arm for the same note if the user detunes beyond the deadband
            if (absCents > detuneDeadbandCents) {
                isAffirmed = false
            }
            return InTuneTriggerResult(
                shouldAffirm = false,
                holdProgress = 0f,
                isLocked = false,
            )
        }
    }

    /**
     * Explicitly resets tracker state (e.g. when tuning selection changes or listening stops).
     */
    fun reset() {
        inTuneStartTime = null
        isAffirmed = false
        lastTargetMidi = null
        lastSignalTime = 0L
    }
}
