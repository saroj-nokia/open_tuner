package com.opentuner.app.music

import kotlin.math.log2
import kotlin.math.pow

private val NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

data class Note(val midi: Int) {
    val name: String get() = NAMES[Math.floorMod(midi, 12)]
    val octave: Int get() = Math.floorDiv(midi, 12) - 1
    val label: String get() = "$name$octave"
}

object Pitch {
    fun midiOf(freq: Double, a4: Double): Double = 69.0 + 12.0 * log2(freq / a4)
    fun freqOf(midi: Int, a4: Double): Double = a4 * 2.0.pow((midi - 69) / 12.0)
    fun centsBetween(freq: Double, target: Double): Double = 1200.0 * log2(freq / target)
}

/** [strings] are MIDI notes, lowest string first. Empty means chromatic. */
data class Tuning(val id: String, val title: String, val strings: List<Int>) {
    val isChromatic: Boolean get() = strings.isEmpty()
}

object Tunings {
    val all = listOf(
        Tuning("chromatic", "Chromatic", emptyList()),
        Tuning("guitar_std", "Guitar · Standard", listOf(40, 45, 50, 55, 59, 64)),
        Tuning("guitar_half", "Guitar · Half step down", listOf(39, 44, 49, 54, 58, 63)),
        Tuning("guitar_dropd", "Guitar · Drop D", listOf(38, 45, 50, 55, 59, 64)),
        Tuning("guitar_dadgad", "Guitar · DADGAD", listOf(38, 45, 50, 55, 57, 62)),
        Tuning("guitar_openg", "Guitar · Open G", listOf(38, 43, 50, 55, 59, 62)),
        Tuning("guitar_opend", "Guitar · Open D", listOf(38, 45, 50, 54, 57, 62)),
        Tuning("bass4", "Bass · Standard", listOf(28, 33, 38, 43)),
        Tuning("ukulele", "Ukulele · GCEA", listOf(67, 60, 64, 69)),
        Tuning("violin", "Violin · GDAE", listOf(55, 62, 69, 76)),
    )

    fun byId(id: String?): Tuning = all.firstOrNull { it.id == id } ?: all[1]
}
