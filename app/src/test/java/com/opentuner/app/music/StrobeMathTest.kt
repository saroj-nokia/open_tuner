package com.opentuner.app.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StrobeMathTest {

    @Test
    fun testNoSignalIsStationary() {
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = 15f, hasSignal = false), 0.001f)
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = -20f, hasSignal = false), 0.001f)
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = 0f, hasSignal = false), 0.001f)
    }

    @Test
    fun testExactZeroCentsIsStationary() {
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = 0f, hasSignal = true), 0.001f)
    }

    @Test
    fun testWithinDeadbandToleranceIsStationary() {
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = 0.5f, hasSignal = true), 0.001f)
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = -0.8f, hasSignal = true), 0.001f)
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = 1.0f, hasSignal = true), 0.001f)
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = -1.0f, hasSignal = true), 0.001f)
    }

    @Test
    fun testInTuneLockToleranceIsStationary() {
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = 2.0f, hasSignal = true, inTune = true), 0.001f)
        assertEquals(0f, StrobeMath.calculateDriftSpeed(cents = -1.8f, hasSignal = true, inTune = true), 0.001f)
    }

    @Test
    fun testSharpDriftsRight() {
        val speedSmall = StrobeMath.calculateDriftSpeed(cents = 5f, hasSignal = true)
        val speedLarge = StrobeMath.calculateDriftSpeed(cents = 25f, hasSignal = true)

        assertTrue("Sharp speed must be positive (drifts right)", speedSmall > 0f)
        assertTrue("Higher cents deviation must produce faster drift", speedLarge > speedSmall)
    }

    @Test
    fun testFlatDriftsLeft() {
        val speedSmall = StrobeMath.calculateDriftSpeed(cents = -5f, hasSignal = true)
        val speedLarge = StrobeMath.calculateDriftSpeed(cents = -25f, hasSignal = true)

        assertTrue("Flat speed must be negative (drifts left)", speedSmall < 0f)
        assertTrue("Larger flat deviation must produce larger negative drift", speedLarge < speedSmall)
    }

    @Test
    fun testSymmetryBetweenSharpAndFlat() {
        val speedSharp = StrobeMath.calculateDriftSpeed(cents = 12f, hasSignal = true)
        val speedFlat = StrobeMath.calculateDriftSpeed(cents = -12f, hasSignal = true)

        assertEquals("Sharp and flat speeds must have identical magnitude", abs(speedSharp), abs(speedFlat), 0.001f)
        assertEquals("Opposite signs", speedSharp, -speedFlat, 0.001f)
    }

    @Test
    fun testClampedMaxSpeed() {
        val speedExtreme = StrobeMath.calculateDriftSpeed(cents = 100f, hasSignal = true)
        assertEquals(StrobeMath.MAX_DRIFT_SPEED_DPS, speedExtreme, 0.001f)

        val speedExtremeFlat = StrobeMath.calculateDriftSpeed(cents = -100f, hasSignal = true)
        assertEquals(-StrobeMath.MAX_DRIFT_SPEED_DPS, speedExtremeFlat, 0.001f)
    }

    @Test
    fun testPhaseWrapping() {
        val period = 36f
        assertEquals(10f, StrobeMath.wrapPhase(10f, period), 0.001f)
        assertEquals(0f, StrobeMath.wrapPhase(36f, period), 0.001f)
        assertEquals(4f, StrobeMath.wrapPhase(40f, period), 0.001f)
        assertEquals(30f, StrobeMath.wrapPhase(-6f, period), 0.001f)
        assertEquals(0f, StrobeMath.wrapPhase(0f, period), 0.001f)
    }
}
