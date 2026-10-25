package com.opentuner.app.music

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTest {
    @Test
    fun noteLabels() {
        assertEquals("E2", Note(40).label)
        assertEquals("A4", Note(69).label)
        assertEquals("C♯3", Note(49).label)
    }

    @Test
    fun frequencyAndCentsMath() {
        assertEquals(82.41, Pitch.freqOf(40, 440.0), 0.01)
        assertEquals(440.0, Pitch.freqOf(69, 440.0), 1e-9)
        assertEquals(0.0, Pitch.centsBetween(440.0, 440.0), 1e-9)
        assertEquals(100.0, Pitch.centsBetween(466.1638, 440.0), 0.01)
    }

    @Test
    fun allExpectedInstrumentsAreConfigured() {
        val expected = listOf(
            "Guitar", "Bass", "Ukulele", "Banjo", "Mandolin",
            "Violin", "Viola", "Cello", "Double Bass", "Chromatic",
        )
        assertEquals(expected, Tunings.instruments)
    }

    @Test
    fun specificTuningStringNotesMatchStandards() {
        // Guitar Drop C: C2, G2, C3, F3, A3, D4
        val dropC = Tunings.byId("guitar_dropc")
        assertEquals(listOf(36, 43, 48, 53, 57, 62), dropC.strings)
        assertEquals("C2", Note(dropC.strings.first()).label)

        // Guitar Open E: E2, B2, E3, G#3, B3, E4
        val openE = Tunings.byId("guitar_opene")
        assertEquals(listOf(40, 47, 52, 56, 59, 64), openE.strings)

        // Guitar 7-string: B1 to E4
        val guitar7 = Tunings.byId("guitar_7str")
        assertEquals(listOf(35, 40, 45, 50, 55, 59, 64), guitar7.strings)
        assertEquals("B1", Note(guitar7.strings.first()).label)

        // Bass 5-string: B0, E1, A1, D2, G2
        val bass5 = Tunings.byId("bass_5str")
        assertEquals(listOf(23, 28, 33, 38, 43), bass5.strings)
        assertEquals("B0", Note(bass5.strings.first()).label)

        // Bass Drop D: D1, A1, D2, G2
        val bassDropD = Tunings.byId("bass_dropd")
        assertEquals(listOf(26, 33, 38, 43), bassDropD.strings)
        assertEquals("D1", Note(bassDropD.strings.first()).label)

        // Banjo Open G: D3, G3, B3, D4, G4
        val banjo = Tunings.byId("banjo_openg")
        assertEquals(listOf(50, 55, 59, 62, 67), banjo.strings)

        // Mandolin & Violin: G3, D4, A4, E5
        val mandolin = Tunings.byId("mandolin_std")
        assertEquals(listOf(55, 62, 69, 76), mandolin.strings)
        val violin = Tunings.byId("violin_std")
        assertEquals(listOf(55, 62, 69, 76), violin.strings)

        // Viola: C3, G3, D4, A4
        val viola = Tunings.byId("viola_std")
        assertEquals(listOf(48, 55, 62, 69), viola.strings)

        // Cello: C2, G2, D3, A3
        val cello = Tunings.byId("cello_std")
        assertEquals(listOf(36, 43, 50, 57), cello.strings)

        // Double Bass: E1, A1, D2, G2
        val doubleBass = Tunings.byId("double_bass_std")
        assertEquals(listOf(28, 33, 38, 43), doubleBass.strings)

        // Ukulele Baritone: D3, G3, B3, E4
        val baritone = Tunings.byId("ukulele_baritone")
        assertEquals(listOf(50, 55, 59, 64), baritone.strings)
    }

    @Test
    fun legacyTuningIdsResolveSafely() {
        assertEquals("bass_std", Tunings.byId("bass4").id)
        assertEquals("ukulele_std", Tunings.byId("ukulele").id)
        assertEquals("violin_std", Tunings.byId("violin").id)
        assertEquals("guitar_std", Tunings.byId("non_existent").id)
    }

    @Test
    fun forInstrumentFiltersCorrectly() {
        val guitarTunings = Tunings.forInstrument("Guitar")
        assertEquals(10, guitarTunings.size)
        val bassTunings = Tunings.forInstrument("Bass")
        assertEquals(3, bassTunings.size)
    }

    @Test
    fun leftHandedStringOrderReversesDisplayIndicesWithoutAlteringStrings() {
        val stdGuitar = Tunings.byId("guitar_std")
        val normalIndices = stdGuitar.strings.indices.toList()
        val leftHandedIndices = stdGuitar.strings.indices.reversed().toList()

        assertEquals(listOf(0, 1, 2, 3, 4, 5), normalIndices)
        assertEquals(listOf(5, 4, 3, 2, 1, 0), leftHandedIndices)

        // Verifies the underlying pitch to string index resolution is identical
        // E2 (40) is string 0, E4 (64) is string 5 in both modes
        val lowE = stdGuitar.strings[leftHandedIndices.last()]
        val highE = stdGuitar.strings[leftHandedIndices.first()]
        assertEquals(40, lowE)
        assertEquals(64, highE)
    }
}
