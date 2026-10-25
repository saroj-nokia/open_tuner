package com.opentuner.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneGeneratorTest {

    private val sampleRate = 44100

    @Test
    fun synthProducesCorrectSampleLengthAndNonZeroEnergy() {
        val synth = KarplusStrongSynth()
        val seconds = 1.5
        val audio = synth.getAudio(440.0, seconds, sampleRate)
        assertEquals((sampleRate * seconds).toInt(), audio.size)

        var energy = 0.0
        var maxAmp = 0
        for (s in audio) {
            val v = s.toInt()
            energy += v * v
            if (abs(v) > maxAmp) maxAmp = abs(v)
        }
        assertTrue("Synthesized audio must have non-zero energy", energy > 0.0)
        assertTrue("Synthesized audio must have strong peak amplitude", maxAmp > 5000)
    }

    @Test
    fun synthPitchMatchesFundamentalFrequency() {
        val synth = KarplusStrongSynth()
        val detector = YinPitchDetector(sampleRate)

        // Test representative guitar and bass notes
        val testPitches = listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63, 440.0)

        for (targetFreq in testPitches) {
            val audio = synth.getAudio(targetFreq, 1.0, sampleRate)
            // Convert first 4096 samples to FloatArray normalized to [-1, 1] for YIN detector
            val floatBuf = FloatArray(4096) { i -> audio[i] / 32768f }
            val result = detector.detect(floatBuf)
            assertNotNull("Pitch must be detected for $targetFreq Hz", result)
            // Allow small acoustic inharmonicity tolerance (< 2.5% frequency)
            assertEquals(
                targetFreq,
                result!!.frequency.toDouble(),
                targetFreq * 0.025,
            )
        }
    }

    @Test
    fun synthHandlesLowBassNotesWithoutInstability() {
        val synth = KarplusStrongSynth()
        // Low B0 on 5-string bass (~30.87 Hz)
        val bassB0 = synth.getAudio(30.87, 2.0, sampleRate)
        assertEquals(sampleRate * 2, bassB0.size)

        var nonZeroCount = 0
        for (s in bassB0) {
            if (s != 0.toShort()) nonZeroCount++
        }
        assertTrue("Low bass notes must synthesize rich acoustic vibrations", nonZeroCount > 1000)
    }

    @Test
    fun audioSourceCanBePluggedAndExtracted() {
        val original = ToneGenerator.audioSource
        try {
            val dummySource = object : StringAudioSource {
                override fun getAudio(freq: Double, seconds: Double, sampleRate: Int): ShortArray {
                    return ShortArray(100) { 42 }
                }
            }
            ToneGenerator.audioSource = dummySource
            val data = ToneGenerator.audioSource.getAudio(440.0, 1.0, sampleRate)
            assertEquals(100, data.size)
            assertEquals(42.toShort(), data[0])

            val sampledSource = SampledStringAudioSource(fallback = dummySource)
            val sampledData = sampledSource.getAudio(440.0, 1.0, sampleRate)
            assertEquals(100, sampledData.size)
        } finally {
            ToneGenerator.audioSource = original
        }
    }

    @Test
    fun inTuneChimeSynthesizesCleanDecayingWaveform() {
        val chime = ToneGenerator.generateInTuneChime(sampleRate = sampleRate, seconds = 0.22)
        assertEquals((sampleRate * 0.22).toInt(), chime.size)

        var peakAmp = 0
        var tailAmp = 0
        val tailStart = chime.size - 2000

        for ((idx, sample) in chime.withIndex()) {
            val a = abs(sample.toInt())
            if (a > peakAmp) peakAmp = a
            if (idx >= tailStart && a > tailAmp) tailAmp = a
        }

        assertTrue("Chime must have clear audible peak amplitude", peakAmp > 4000)
        assertTrue("Chime tail must decay toward silence to prevent clicks", tailAmp < peakAmp / 4)
    }
}
