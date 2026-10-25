package com.opentuner.app.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordsTest {

    @Test
    fun testAll108ChordsHaveValidVoicingsAndTones() {
        for (root in Chords.ROOTS) {
            val rootSemi = Chords.rootSemitone(root)
            for (type in ChordType.entries) {
                val voicing = Chords.getVoicing(root, type)
                assertNotNull("Voicing for $root ${type.name} should not be null", voicing)
                assertEquals("Frets must have 6 entries", 6, voicing.frets.size)

                val expectedPitchClasses = type.intervals.map { (rootSemi + it) % 12 }.toSet()
                val soundedMidis = voicing.midiNotes().filterNotNull()

                assertTrue("Chord $root ${type.name} must have at least 3 sounded strings", soundedMidis.size >= 3)

                val soundedPitchClasses = soundedMidis.map { it % 12 }.toSet()

                // Verify every sounded note belongs to the chord
                for (midi in soundedMidis) {
                    val pitchClass = midi % 12
                    assertTrue(
                        "Sounded note PC $pitchClass ($midi) in $root ${type.name} with frets ${voicing.frets} must belong to $expectedPitchClasses",
                        expectedPitchClasses.contains(pitchClass),
                    )
                }

                // Verify the root note is present in the chord
                assertTrue(
                    "Chord $root ${type.name} must contain its root pitch class ($rootSemi)",
                    soundedPitchClasses.contains(rootSemi),
                )
            }
        }
    }

    @Test
    fun testCommonOpenChords() {
        val cMaj = Chords.getVoicing("C", ChordType.MAJOR)
        assertEquals(listOf(-1, 3, 2, 0, 1, 0), cMaj.frets)

        val eMaj = Chords.getVoicing("E", ChordType.MAJOR)
        assertEquals(listOf(0, 2, 2, 1, 0, 0), eMaj.frets)

        val aMin = Chords.getVoicing("A", ChordType.MINOR)
        assertEquals(listOf(-1, 0, 2, 2, 1, 0), aMin.frets)

        val gMaj = Chords.getVoicing("G", ChordType.MAJOR)
        assertEquals(listOf(3, 2, 0, 0, 0, 3), gMaj.frets)

        val dMaj = Chords.getVoicing("D", ChordType.MAJOR)
        assertEquals(listOf(-1, -1, 0, 2, 3, 2), dMaj.frets)
    }

    @Test
    fun testLeftHandedFlipping() {
        val cMaj = Chords.getVoicing("C", ChordType.MAJOR)
        val flipped = cMaj.flipped()

        assertEquals(listOf(0, 1, 0, 2, 3, -1), flipped.frets)
    }

    @Test
    fun testChordToneNames() {
        val cMajNotes = Chords.notesFor("C", ChordType.MAJOR)
        assertEquals(listOf("C", "E", "G"), cMajNotes)

        val aMinNotes = Chords.notesFor("A", ChordType.MINOR)
        assertEquals(listOf("A", "C", "E"), aMinNotes)

        val g7Notes = Chords.notesFor("G", ChordType.SEVENTH)
        assertEquals(listOf("G", "B", "D", "F"), g7Notes)
    }
}
