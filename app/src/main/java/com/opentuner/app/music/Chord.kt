package com.opentuner.app.music

/**
 * Standard chord types supported by the chord library.
 */
enum class ChordType(
    val symbol: String,
    val title: String,
    val intervals: List<Int>, // Semitones above root
) {
    MAJOR("", "Major", listOf(0, 4, 7)),
    MINOR("m", "Minor", listOf(0, 3, 7)),
    POWER("5", "Power chord", listOf(0, 7)),
    SEVENTH("7", "Dominant 7th", listOf(0, 4, 7, 10)),
    MAJOR_7("maj7", "Major 7th", listOf(0, 4, 7, 11)),
    MINOR_7("m7", "Minor 7th", listOf(0, 3, 7, 10)),
    SUS2("sus2", "Suspended 2nd", listOf(0, 2, 7)),
    SUS4("sus4", "Suspended 4th", listOf(0, 5, 7)),
    DIMINISHED("dim", "Diminished", listOf(0, 3, 6)),
    AUGMENTED("aug", "Augmented", listOf(0, 4, 8)),
    SIXTH("6", "Major 6th", listOf(0, 4, 7, 9)),
    MINOR_6("m6", "Minor 6th", listOf(0, 3, 7, 9)),
    ADD9("add9", "Add 9", listOf(0, 4, 7, 14)),
    SEVENTH_SUS4("7sus4", "Dominant 7th sus4", listOf(0, 5, 7, 10)),
    DIMINISHED_7("dim7", "Diminished 7th", listOf(0, 3, 6, 9)),
    HALF_DIMINISHED("m7♭5", "Half-diminished", listOf(0, 3, 6, 10)),
    NINTH("9", "Dominant 9th", listOf(0, 4, 7, 10, 14));

    val label: String get() = if (symbol.isEmpty()) "maj" else symbol
}

/**
 * A guitar chord voicing on a 6-string guitar in standard tuning (E2, A2, D3, G3, B3, E4).
 *
 * @param frets 6 elements for strings 6 down to 1 (index 0 is Low E, index 5 is High E).
 *              -1 = muted (X), 0 = open (O), 1..15 = fret number.
 * @param fingers Optional finger numbers (1=index, 2=middle, 3=ring, 4=pinky, 0=open, -1=mute).
 * @param baseFret Lowest fret depicted in diagram (1 shows nut; >1 indicates neck position).
 * @param barreFret Fret number of an index finger barre (if any).
 * @param barreFrom Lowest string index of the barre (0..5).
 * @param barreTo Highest string index of the barre (0..5).
 */
data class ChordVoicing(
    val root: String,
    val type: ChordType,
    val frets: List<Int>,
    val fingers: List<Int> = emptyList(),
    val baseFret: Int = 1,
    val barreFret: Int? = null,
    val barreFrom: Int = 0,
    val barreTo: Int = 5,
) {
    val chordName: String get() = "$root${type.symbol}"
    val displayName: String get() = "$root ${type.title}"

    /** Returns the MIDI note for each string (null if muted). */
    fun midiNotes(): List<Int?> = frets.mapIndexed { idx, fret ->
        if (fret < 0) null else Chords.GUITAR_OPEN_MIDI[idx] + fret
    }

    /** Returns audio frequencies for sounded strings (low to high). */
    fun soundedFrequencies(a4: Double = 440.0): List<Double> {
        return midiNotes().filterNotNull().map { Pitch.freqOf(it, a4) }
    }

    /** Reverses the string order for left-handed view (High E on left, Low E on right). */
    fun flipped(): ChordVoicing {
        return copy(
            frets = frets.reversed(),
            fingers = if (fingers.isNotEmpty()) fingers.reversed() else emptyList(),
            barreFrom = 5 - barreTo,
            barreTo = 5 - barreFrom,
        )
    }
}

object Chords {
    val ROOTS = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

    val GUITAR_OPEN_MIDI = listOf(40, 45, 50, 55, 59, 64) // E2, A2, D3, G3, B3, E4

    private val ROOT_SEMITONES = mapOf(
        "C" to 0, "C♯" to 1, "D" to 2, "D♯" to 3, "E" to 4, "F" to 5,
        "F♯" to 6, "G" to 7, "G♯" to 8, "A" to 9, "A♯" to 10, "B" to 11,
    )

    fun rootSemitone(root: String): Int = ROOT_SEMITONES[root] ?: 0

    /** Returns note names (e.g. ["C", "E", "G"]) for a chord. */
    fun notesFor(root: String, type: ChordType): List<String> {
        val rootSemi = rootSemitone(root)
        return type.intervals.map { interval ->
            val semi = (rootSemi + interval) % 12
            ROOTS[semi]
        }
    }

    /** Primary hand-checked open and first-position chord voicings. */
    private val OPEN_VOICINGS = mapOf(
        // C Chords
        ("C" to ChordType.MAJOR) to ChordVoicing("C", ChordType.MAJOR, listOf(-1, 3, 2, 0, 1, 0), listOf(-1, 3, 2, 0, 1, 0)),
        ("C" to ChordType.SEVENTH) to ChordVoicing("C", ChordType.SEVENTH, listOf(-1, 3, 2, 3, 1, 0), listOf(-1, 3, 2, 4, 1, 0)),
        ("C" to ChordType.MAJOR_7) to ChordVoicing("C", ChordType.MAJOR_7, listOf(-1, 3, 2, 0, 0, 0), listOf(-1, 3, 2, 0, 0, 0)),
        ("C" to ChordType.SUS2) to ChordVoicing("C", ChordType.SUS2, listOf(-1, 3, 0, 0, 1, 3), listOf(-1, 3, 0, 0, 1, 4)),
        ("C" to ChordType.SUS4) to ChordVoicing("C", ChordType.SUS4, listOf(-1, 3, 3, 0, 1, 1), listOf(-1, 3, 4, 0, 1, 1)),
        ("C" to ChordType.AUGMENTED) to ChordVoicing("C", ChordType.AUGMENTED, listOf(-1, 3, 2, 1, 1, -1), listOf(-1, 4, 3, 1, 2, -1)),

        // D Chords
        ("D" to ChordType.MAJOR) to ChordVoicing("D", ChordType.MAJOR, listOf(-1, -1, 0, 2, 3, 2), listOf(-1, -1, 0, 1, 3, 2)),
        ("D" to ChordType.MINOR) to ChordVoicing("D", ChordType.MINOR, listOf(-1, -1, 0, 2, 3, 1), listOf(-1, -1, 0, 2, 3, 1)),
        ("D" to ChordType.SEVENTH) to ChordVoicing("D", ChordType.SEVENTH, listOf(-1, -1, 0, 2, 1, 2), listOf(-1, -1, 0, 2, 1, 3)),
        ("D" to ChordType.MAJOR_7) to ChordVoicing("D", ChordType.MAJOR_7, listOf(-1, -1, 0, 2, 2, 2), listOf(-1, -1, 0, 1, 2, 3)),
        ("D" to ChordType.MINOR_7) to ChordVoicing("D", ChordType.MINOR_7, listOf(-1, -1, 0, 2, 1, 1), listOf(-1, -1, 0, 2, 1, 1)),
        ("D" to ChordType.SUS2) to ChordVoicing("D", ChordType.SUS2, listOf(-1, -1, 0, 2, 3, 0), listOf(-1, -1, 0, 1, 2, 0)),
        ("D" to ChordType.SUS4) to ChordVoicing("D", ChordType.SUS4, listOf(-1, -1, 0, 2, 3, 3), listOf(-1, -1, 0, 1, 2, 4)),
        ("D" to ChordType.DIMINISHED) to ChordVoicing("D", ChordType.DIMINISHED, listOf(-1, -1, 0, 1, 3, 1), listOf(-1, -1, 0, 1, 3, 2)),
        ("D" to ChordType.AUGMENTED) to ChordVoicing("D", ChordType.AUGMENTED, listOf(-1, -1, 0, 3, 3, 2), listOf(-1, -1, 0, 2, 3, 1)),

        // E Chords
        ("E" to ChordType.MAJOR) to ChordVoicing("E", ChordType.MAJOR, listOf(0, 2, 2, 1, 0, 0), listOf(0, 2, 3, 1, 0, 0)),
        ("E" to ChordType.MINOR) to ChordVoicing("E", ChordType.MINOR, listOf(0, 2, 2, 0, 0, 0), listOf(0, 2, 3, 0, 0, 0)),
        ("E" to ChordType.SEVENTH) to ChordVoicing("E", ChordType.SEVENTH, listOf(0, 2, 0, 1, 0, 0), listOf(0, 2, 0, 1, 0, 0)),
        ("E" to ChordType.MAJOR_7) to ChordVoicing("E", ChordType.MAJOR_7, listOf(0, 2, 1, 1, 0, 0), listOf(0, 3, 1, 2, 0, 0)),
        ("E" to ChordType.MINOR_7) to ChordVoicing("E", ChordType.MINOR_7, listOf(0, 2, 0, 0, 0, 0), listOf(0, 2, 0, 0, 0, 0)),
        ("E" to ChordType.SUS2) to ChordVoicing("E", ChordType.SUS2, listOf(0, 2, 4, 4, 0, 0), listOf(0, 1, 3, 4, 0, 0)),
        ("E" to ChordType.SUS4) to ChordVoicing("E", ChordType.SUS4, listOf(0, 2, 2, 2, 0, 0), listOf(0, 2, 3, 4, 0, 0)),
        ("E" to ChordType.AUGMENTED) to ChordVoicing("E", ChordType.AUGMENTED, listOf(0, 3, 2, 1, 1, 0), listOf(0, 4, 3, 1, 2, 0)),

        // G Chords
        ("G" to ChordType.MAJOR) to ChordVoicing("G", ChordType.MAJOR, listOf(3, 2, 0, 0, 0, 3), listOf(2, 1, 0, 0, 0, 3)),
        ("G" to ChordType.SEVENTH) to ChordVoicing("G", ChordType.SEVENTH, listOf(3, 2, 0, 0, 0, 1), listOf(3, 2, 0, 0, 0, 1)),
        ("G" to ChordType.MAJOR_7) to ChordVoicing("G", ChordType.MAJOR_7, listOf(3, 2, 0, 0, 0, 2), listOf(2, 1, 0, 0, 0, 3)),
        ("G" to ChordType.SUS2) to ChordVoicing("G", ChordType.SUS2, listOf(3, 0, 0, 0, 3, 3), listOf(1, 0, 0, 0, 3, 4)),
        ("G" to ChordType.SUS4) to ChordVoicing("G", ChordType.SUS4, listOf(3, 3, 0, 0, 1, 3), listOf(2, 3, 0, 0, 1, 4)),
        ("G" to ChordType.AUGMENTED) to ChordVoicing("G", ChordType.AUGMENTED, listOf(3, 2, 1, 0, 0, 3), listOf(3, 2, 1, 0, 0, 4)),

        // A Chords
        ("A" to ChordType.MAJOR) to ChordVoicing("A", ChordType.MAJOR, listOf(-1, 0, 2, 2, 2, 0), listOf(-1, 0, 1, 2, 3, 0)),
        ("A" to ChordType.MINOR) to ChordVoicing("A", ChordType.MINOR, listOf(-1, 0, 2, 2, 1, 0), listOf(-1, 0, 2, 3, 1, 0)),
        ("A" to ChordType.SEVENTH) to ChordVoicing("A", ChordType.SEVENTH, listOf(-1, 0, 2, 0, 2, 0), listOf(-1, 0, 2, 0, 3, 0)),
        ("A" to ChordType.MAJOR_7) to ChordVoicing("A", ChordType.MAJOR_7, listOf(-1, 0, 2, 1, 2, 0), listOf(-1, 0, 2, 1, 3, 0)),
        ("A" to ChordType.MINOR_7) to ChordVoicing("A", ChordType.MINOR_7, listOf(-1, 0, 2, 0, 1, 0), listOf(-1, 0, 2, 0, 1, 0)),
        ("A" to ChordType.SUS2) to ChordVoicing("A", ChordType.SUS2, listOf(-1, 0, 2, 2, 0, 0), listOf(-1, 0, 1, 2, 0, 0)),
        ("A" to ChordType.SUS4) to ChordVoicing("A", ChordType.SUS4, listOf(-1, 0, 2, 2, 3, 0), listOf(-1, 0, 1, 2, 4, 0)),
        ("A" to ChordType.DIMINISHED) to ChordVoicing("A", ChordType.DIMINISHED, listOf(-1, 0, 1, 2, 1, -1), listOf(-1, 0, 1, 3, 2, -1)),
        ("A" to ChordType.AUGMENTED) to ChordVoicing("A", ChordType.AUGMENTED, listOf(-1, 0, 3, 2, 2, 1), listOf(-1, 0, 4, 2, 3, 1)),
    )

    /**
     * Resolves the primary chord voicing for any given [root] and [type].
     * Prefers hand-checked open chords, falling back to clean CAGED moveable barre chords.
     */
    fun getVoicing(root: String, type: ChordType): ChordVoicing {
        OPEN_VOICINGS[root to type]?.let { return it }

        val rootSemi = rootSemitone(root)

        // E-shape (string 6 root): fret = (rootSemi - 4 + 12) % 12
        val fret6 = (rootSemi - 4 + 12) % 12
        // A-shape (string 5 root): fret = (rootSemi - 9 + 12) % 12
        val fret5 = (rootSemi - 9 + 12) % 12

        // Power chords need no barre, so they may sit on open strings and use whichever root string is lower.
        if (type == ChordType.POWER) return if (fret6 <= fret5) generateEShape(root, type, fret6) else generateAShape(root, type, fret5)

        // Prefer whichever position sits lowest on the neck (between frets 1 and 7)
        return if (fret6 in 1..6 && (fret5 !in 1..6 || fret6 <= fret5)) {
            generateEShape(root, type, fret6)
        } else {
            generateAShape(root, type, fret5)
        }
    }

    /** Builds the voicing; the barre is only drawn where a finger really lies across 2+ strings with nothing lower in between. */
    private fun finish(root: String, type: ChordType, frets: List<Int>, fingers: List<Int>, f: Int): ChordVoicing {
        val atF = frets.indices.filter { frets[it] == f }
        val from = atF.firstOrNull()
        val to = atF.lastOrNull()
        val barre = f >= 1 && atF.size >= 2 && from != null && to != null && frets.slice(from..to).all { it >= f }
        return ChordVoicing(
            root = root,
            type = type,
            frets = frets,
            fingers = fingers,
            baseFret = frets.filter { it > 0 }.minOrNull() ?: 1,
            barreFret = if (barre) f else null,
            barreFrom = from ?: 0,
            barreTo = to ?: 5,
        )
    }

    /**
     * Moveable E-shape barre chord rooted on String 6 (Low E).
     */
    private fun generateEShape(root: String, type: ChordType, f: Int): ChordVoicing {
        val (frets, fingers) = when (type) {
            ChordType.MAJOR -> listOf(f, f + 2, f + 2, f + 1, f, f) to listOf(1, 3, 4, 2, 1, 1)
            ChordType.MINOR -> listOf(f, f + 2, f + 2, f, f, f) to listOf(1, 3, 4, 1, 1, 1)
            ChordType.SEVENTH -> listOf(f, f + 2, f, f + 1, f, f) to listOf(1, 3, 1, 2, 1, 1)
            ChordType.MAJOR_7 -> listOf(f, f + 2, f + 1, f + 1, f, f) to listOf(1, 4, 2, 3, 1, 1)
            ChordType.MINOR_7 -> listOf(f, f + 2, f, f, f, f) to listOf(1, 3, 1, 1, 1, 1)
            ChordType.SUS2 -> listOf(f, f + 2, f + 4, f + 4, f, f) to listOf(1, 2, 4, 4, 1, 1)
            ChordType.SUS4 -> listOf(f, f + 2, f + 2, f + 2, f, f) to listOf(1, 3, 4, 2, 1, 1)
            ChordType.DIMINISHED -> listOf(f, f + 1, f + 2, f, -1, -1) to listOf(1, 2, 3, 1, -1, -1)
            ChordType.AUGMENTED -> listOf(f, -1, f + 2, f + 1, f + 1, -1) to listOf(1, -1, 3, 2, 2, -1)
            ChordType.POWER -> listOf(f, f + 2, f + 2, -1, -1, -1) to listOf(1, 3, 4, -1, -1, -1)
            ChordType.SIXTH -> listOf(f, f + 2, f + 2, f + 1, f + 2, f) to listOf(1, 3, 4, 2, 4, 1)
            ChordType.MINOR_6 -> listOf(f, f + 2, f + 2, f, f + 2, f) to listOf(1, 3, 4, 1, 4, 1)
            ChordType.ADD9 -> listOf(f, f + 2, f + 4, f + 1, f, f) to listOf(1, 3, 4, 2, 1, 1)
            ChordType.SEVENTH_SUS4 -> listOf(f, f + 2, f, f + 2, f, f) to listOf(1, 3, 1, 4, 1, 1)
            ChordType.DIMINISHED_7 -> listOf(f, -1, f - 1, f, f - 1, -1) to listOf(3, -1, 1, 4, 2, -1)
            ChordType.HALF_DIMINISHED -> listOf(f, -1, f, f, f - 1, -1) to listOf(2, -1, 3, 4, 1, -1)
            ChordType.NINTH -> listOf(f, f + 2, f, f + 1, f, f + 2) to listOf(1, 3, 1, 2, 1, 4)
        }
        return finish(root, type, frets, fingers, f)
    }

    /**
     * Moveable A-shape barre chord rooted on String 5 (A string).
     */
    private fun generateAShape(root: String, type: ChordType, f: Int): ChordVoicing {
        val (frets, fingers) = when (type) {
            ChordType.MAJOR -> listOf(-1, f, f + 2, f + 2, f + 2, f) to listOf(-1, 1, 2, 3, 4, 1)
            ChordType.MINOR -> listOf(-1, f, f + 2, f + 2, f + 1, f) to listOf(-1, 1, 3, 4, 2, 1)
            ChordType.SEVENTH -> listOf(-1, f, f + 2, f, f + 2, f) to listOf(-1, 1, 3, 1, 4, 1)
            ChordType.MAJOR_7 -> listOf(-1, f, f + 2, f + 1, f + 2, f) to listOf(-1, 1, 3, 2, 4, 1)
            ChordType.MINOR_7 -> listOf(-1, f, f + 2, f, f + 1, f) to listOf(-1, 1, 3, 1, 2, 1)
            ChordType.SUS2 -> listOf(-1, f, f + 2, f + 2, f, f) to listOf(-1, 1, 3, 4, 1, 1)
            ChordType.SUS4 -> listOf(-1, f, f + 2, f + 2, f + 3, f) to listOf(-1, 1, 2, 3, 4, 1)
            ChordType.DIMINISHED -> listOf(-1, f, f + 1, f + 2, f + 1, -1) to listOf(-1, 1, 2, 4, 3, -1)
            ChordType.AUGMENTED -> listOf(-1, f, f + 3, f + 2, f + 2, -1) to listOf(-1, 1, 4, 2, 3, -1)
            ChordType.POWER -> listOf(-1, f, f + 2, f + 2, -1, -1) to listOf(-1, 1, 3, 4, -1, -1)
            ChordType.SIXTH -> listOf(-1, f, f + 2, f + 2, f + 2, f + 2) to listOf(-1, 1, 3, 3, 3, 3)
            ChordType.MINOR_6 -> listOf(-1, f, f + 2, f + 2, f + 1, f + 2) to listOf(-1, 1, 3, 4, 2, 4)
            ChordType.ADD9 -> listOf(-1, f, f + 2, f + 4, f + 2, f) to listOf(-1, 1, 2, 4, 3, 1)
            ChordType.SEVENTH_SUS4 -> listOf(-1, f, f + 2, f, f + 3, f) to listOf(-1, 1, 3, 1, 4, 1)
            ChordType.DIMINISHED_7 -> listOf(-1, f, f + 1, f - 1, f + 1, f - 1) to listOf(-1, 2, 3, 1, 4, 1)
            ChordType.HALF_DIMINISHED -> listOf(-1, f, f + 1, f, f + 1, -1) to listOf(-1, 1, 3, 1, 4, -1)
            ChordType.NINTH -> listOf(-1, f, f - 1, f, f, f) to listOf(-1, 2, 1, 3, 3, 3)
        }
        return finish(root, type, frets, fingers, f)
    }
}
