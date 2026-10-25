package com.opentuner.app.metronome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetronomeMathTest {

    private val sampleRate = 44100

    @Test
    fun samplesPerBeatCalculatesExactMathematicalIntervals() {
        val fourFour = TimeSignature(4, 4)

        // 60 BPM = 1 beat per second = 44100 samples
        assertEquals(44100, MetronomeMath.samplesPerBeat(60, fourFour, sampleRate))

        // 120 BPM = 2 beats per second = 22050 samples
        assertEquals(22050, MetronomeMath.samplesPerBeat(120, fourFour, sampleRate))

        // 240 BPM = 4 beats per second = 11025 samples
        assertEquals(11025, MetronomeMath.samplesPerBeat(240, fourFour, sampleRate))
    }

    @Test
    fun eighthNoteTimeSignaturesDoublePulseRate() {
        val sixEight = TimeSignature(6, 8)
        val sevenEight = TimeSignature(7, 8)

        // In compound/eighth-note meters, eighth note is half a quarter note
        // At 120 BPM, quarter = 22050 samples, eighth = 11025 samples
        assertEquals(11025, MetronomeMath.samplesPerBeat(120, sixEight, sampleRate))
        assertEquals(11025, MetronomeMath.samplesPerBeat(120, sevenEight, sampleRate))
    }

    @Test
    fun bpmIsClampedToSafeBounds() {
        val sig = TimeSignature(4, 4)
        // 10 BPM clamps to 30 BPM -> 44100 * 2 = 88200 samples
        assertEquals(88200, MetronomeMath.samplesPerBeat(10, sig, sampleRate))

        // 500 BPM clamps to 300 BPM -> 44100 / 5 = 8820 samples
        assertEquals(8820, MetronomeMath.samplesPerBeat(500, sig, sampleRate))
    }

    @Test
    fun tapTempoCalculatesCorrectBpm() {
        // Empty or 1 tap returns null
        assertNull(MetronomeMath.calculateTapBpm(emptyList()))
        assertNull(MetronomeMath.calculateTapBpm(listOf(1000L)))

        // 500ms intervals = 120 BPM
        val taps120 = listOf(1000L, 1500L, 2000L, 2500L)
        assertEquals(120, MetronomeMath.calculateTapBpm(taps120))

        // 1000ms intervals = 60 BPM
        val taps60 = listOf(0L, 1000L, 2000L, 3000L)
        assertEquals(60, MetronomeMath.calculateTapBpm(taps60))

        // 400ms intervals = 150 BPM
        val taps150 = listOf(5000L, 5400L, 5800L, 6200L)
        assertEquals(150, MetronomeMath.calculateTapBpm(taps150))
    }

    @Test
    fun timeSignaturesContainAllStandardOptions() {
        val labels = TimeSignature.all.map { it.label }
        val expected = listOf("2/4", "3/4", "4/4", "5/4", "6/8", "7/8")
        assertEquals(expected, labels)
    }
}
