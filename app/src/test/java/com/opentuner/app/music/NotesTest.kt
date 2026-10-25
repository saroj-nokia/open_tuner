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
}
