package com.opentuner.app.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioSignalMathTest {

    @Test
    fun calculateRmsHandlesEmptyAndZeroBuffers() {
        assertEquals(0f, AudioSignalMath.calculateRms(floatArrayOf()), 0.0001f)
        assertEquals(0f, AudioSignalMath.calculateRms(FloatArray(1024)), 0.0001f)
    }

    @Test
    fun calculateRmsMatchesKnownWaveforms() {
        // Full-scale DC signal: RMS of all 1.0 is 1.0
        val dc = FloatArray(100) { 1.0f }
        assertEquals(1.0f, AudioSignalMath.calculateRms(dc), 0.0001f)

        // Pure sine wave: RMS is 1 / sqrt(2) ~= 0.7071
        val sine = FloatArray(44100) { i ->
            sin(2.0 * PI * 440.0 * i / 44100.0).toFloat()
        }
        val rms = AudioSignalMath.calculateRms(sine)
        assertEquals(0.7071f, rms, 0.01f)
    }

    @Test
    fun rmsToDbCalculatesCorrectValues() {
        // RMS = 1.0 -> 0 dBFS
        assertEquals(0f, AudioSignalMath.rmsToDb(1.0f), 0.01f)

        // RMS = 0.1 -> -20 dBFS
        assertEquals(-20f, AudioSignalMath.rmsToDb(0.1f), 0.01f)

        // RMS = 0.01 -> -40 dBFS
        assertEquals(-40f, AudioSignalMath.rmsToDb(0.01f), 0.01f)

        // Zero or tiny RMS clamps to minDb
        assertEquals(AudioSignalMath.MIN_DB, AudioSignalMath.rmsToDb(0.0f), 0.01f)
    }

    @Test
    fun normalizeDbScalesAppropriately() {
        // Minimum dB maps to 0.0
        assertEquals(0f, AudioSignalMath.normalizeDb(-60f, meterFloorDb = -60f, meterCeilDb = 0f), 0.001f)
        assertEquals(0f, AudioSignalMath.normalizeDb(-80f, meterFloorDb = -60f, meterCeilDb = 0f), 0.001f)

        // Maximum dB maps to 1.0
        assertEquals(1f, AudioSignalMath.normalizeDb(0f, meterFloorDb = -60f, meterCeilDb = 0f), 0.001f)
        assertEquals(1f, AudioSignalMath.normalizeDb(5f, meterFloorDb = -60f, meterCeilDb = 0f), 0.001f)

        // Halfway point: -30 dB maps to 0.5
        assertEquals(0.5f, AudioSignalMath.normalizeDb(-30f, meterFloorDb = -60f, meterCeilDb = 0f), 0.001f)
    }

    @Test
    fun isAboveGateThreshold() {
        // -30 dB is above -44 dB gate
        assertTrue(AudioSignalMath.isAboveGate(0.0316f, gateThresholdDb = -44f))

        // -50 dB is below -44 dB gate
        assertFalse(AudioSignalMath.isAboveGate(0.00316f, gateThresholdDb = -44f))
    }
}
