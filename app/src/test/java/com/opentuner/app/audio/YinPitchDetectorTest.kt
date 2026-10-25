package com.opentuner.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {
    private val sr = 44100

    // Fundamental plus two harmonics, like a plucked string.
    private fun tone(f: Double) = FloatArray(4096) { i ->
        val t = i.toDouble() / sr
        ((sin(2 * PI * f * t) + 0.5 * sin(4 * PI * f * t) + 0.25 * sin(6 * PI * f * t)) * 0.3).toFloat()
    }

    @Test
    fun detectsStringsFromBassLowEToHighNotes() {
        val yin = YinPitchDetector(sr)
        for (f in listOf(41.20, 82.41, 110.0, 146.83, 196.0, 246.94, 329.63, 880.0)) {
            val r = assertNotNull("no pitch for $f Hz", yin.detect(tone(f)))
            assertEquals(f, r!!.frequency.toDouble(), f * 0.002) // within ~3 cents
        }
    }

    @Test
    fun silenceGivesNoPitch() {
        assertNull(YinPitchDetector(sr).detect(FloatArray(4096)))
    }
}
