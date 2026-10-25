package com.opentuner.app.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InTuneAffirmationTrackerTest {

    @Test
    fun noSignalYieldsZeroProgressAndNoAffirmation() {
        val tracker = InTuneAffirmationTracker(requiredDurationMs = 250L)
        val res = tracker.update(targetMidi = 40, cents = 0f, hasSignal = false, currentTimeMs = 1000L)
        assertFalse(res.shouldAffirm)
        assertEquals(0f, res.holdProgress, 0.001f)
        assertFalse(res.isLocked)
    }

    @Test
    fun outOfTuneYieldsZeroProgress() {
        val tracker = InTuneAffirmationTracker(inTuneThresholdCents = 4.0f, requiredDurationMs = 250L)
        val res = tracker.update(targetMidi = 40, cents = 12f, hasSignal = true, currentTimeMs = 1000L)
        assertFalse(res.shouldAffirm)
        assertEquals(0f, res.holdProgress, 0.001f)
        assertFalse(res.isLocked)
    }

    @Test
    fun inTuneAccumulatesProgressAndFiresAffirmationOnce() {
        val tracker = InTuneAffirmationTracker(inTuneThresholdCents = 4.0f, requiredDurationMs = 200L)

        // First frame in tune at t = 1000ms
        val r1 = tracker.update(targetMidi = 40, cents = 1.5f, hasSignal = true, currentTimeMs = 1000L)
        assertFalse(r1.shouldAffirm)
        assertEquals(0f, r1.holdProgress, 0.001f)
        assertFalse(r1.isLocked)

        // Halfway at t = 1100ms
        val r2 = tracker.update(targetMidi = 40, cents = -0.8f, hasSignal = true, currentTimeMs = 1100L)
        assertFalse(r2.shouldAffirm)
        assertEquals(0.5f, r2.holdProgress, 0.05f)
        assertFalse(r2.isLocked)

        // Full duration reached at t = 1200ms
        val r3 = tracker.update(targetMidi = 40, cents = 0.2f, hasSignal = true, currentTimeMs = 1200L)
        assertTrue("Affirmation must fire when required duration reached", r3.shouldAffirm)
        assertEquals(1.0f, r3.holdProgress, 0.001f)
        assertTrue(r3.isLocked)

        // Continued steady hold at t = 1300ms must NOT re-affirm (debounce / single shot)
        val r4 = tracker.update(targetMidi = 40, cents = 0.1f, hasSignal = true, currentTimeMs = 1300L)
        assertFalse("Must not re-affirm while continuing to hold", r4.shouldAffirm)
        assertEquals(1.0f, r4.holdProgress, 0.001f)
        assertTrue(r4.isLocked)
    }

    @Test
    fun noteChangeResetsAffirmationState() {
        val tracker = InTuneAffirmationTracker(inTuneThresholdCents = 4.0f, requiredDurationMs = 200L)

        // Achieve lock on string MIDI 40 (low E)
        tracker.update(targetMidi = 40, cents = 0f, hasSignal = true, currentTimeMs = 1000L)
        val rLocked = tracker.update(targetMidi = 40, cents = 0f, hasSignal = true, currentTimeMs = 1250L)
        assertTrue(rLocked.shouldAffirm)

        // Pluck string MIDI 45 (A string)
        val rNext = tracker.update(targetMidi = 45, cents = 0f, hasSignal = true, currentTimeMs = 1400L)
        assertFalse(rNext.shouldAffirm)
        assertEquals(0f, rNext.holdProgress, 0.001f)

        // Hold A string for 200ms -> should affirm for A string
        val rNextLocked = tracker.update(targetMidi = 45, cents = 0f, hasSignal = true, currentTimeMs = 1650L)
        assertTrue("Affirmation must fire for new string after holding", rNextLocked.shouldAffirm)
    }

    @Test
    fun detuningPastDeadbandReArmsTracker() {
        val tracker = InTuneAffirmationTracker(
            inTuneThresholdCents = 4.0f,
            requiredDurationMs = 200L,
            detuneDeadbandCents = 7.0f,
        )

        // Lock note
        tracker.update(targetMidi = 40, cents = 0f, hasSignal = true, currentTimeMs = 1000L)
        tracker.update(targetMidi = 40, cents = 0f, hasSignal = true, currentTimeMs = 1250L)

        // Player detunes the peg to +10 cents (exceeds 7.0 cents deadband)
        val rDetuned = tracker.update(targetMidi = 40, cents = 10f, hasSignal = true, currentTimeMs = 1400L)
        assertFalse(rDetuned.isLocked)
        assertFalse(rDetuned.shouldAffirm)

        // Player brings string back in tune
        tracker.update(targetMidi = 40, cents = 0.5f, hasSignal = true, currentTimeMs = 1600L)
        val rReLocked = tracker.update(targetMidi = 40, cents = 0.5f, hasSignal = true, currentTimeMs = 1850L)
        assertTrue("Tracker must re-affirm after tuning back in from deadband", rReLocked.shouldAffirm)
    }
}
