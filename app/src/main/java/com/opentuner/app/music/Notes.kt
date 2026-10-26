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
data class Tuning(
    val id: String,
    val instrument: String,
    val name: String,
    val strings: List<Int>,
) {
    val title: String get() = if (instrument == name || instrument.isEmpty()) name else "$instrument · $name"
    val isChromatic: Boolean get() = strings.isEmpty()

    /** Backward-compatible constructor for Tuning(id, title, strings) */
    constructor(id: String, title: String, strings: List<Int>) : this(
        id = id,
        instrument = if (title.contains(" · ")) title.substringBefore(" · ").trim() else title,
        name = if (title.contains(" · ")) title.substringAfter(" · ").trim() else title,
        strings = strings,
    )
}

object Tunings {
    val all = listOf(
        Tuning("guitar_std", "Guitar", "Standard", listOf(40, 45, 50, 55, 59, 64)),
        Tuning("guitar_half", "Guitar", "Half step down", listOf(39, 44, 49, 54, 58, 63)),
        Tuning("guitar_dropd", "Guitar", "Drop D", listOf(38, 45, 50, 55, 59, 64)),
        Tuning("guitar_dropc", "Guitar", "Drop C", listOf(36, 43, 48, 53, 57, 62)),
        Tuning("guitar_opene", "Guitar", "Open E", listOf(40, 47, 52, 56, 59, 64)),
        Tuning("guitar_openg", "Guitar", "Open G", listOf(38, 43, 50, 55, 59, 62)),
        Tuning("guitar_opend", "Guitar", "Open D", listOf(38, 45, 50, 54, 57, 62)),
        Tuning("guitar_dadgad", "Guitar", "DADGAD", listOf(38, 45, 50, 55, 57, 62)),
        Tuning("guitar_7str", "Guitar", "7-String Standard", listOf(35, 40, 45, 50, 55, 59, 64)),
        Tuning("guitar_12str", "Guitar", "12-String Standard", listOf(40, 45, 50, 55, 59, 64)),

        Tuning("bass_std", "Bass", "Standard", listOf(28, 33, 38, 43)),
        Tuning("bass_5str", "Bass", "5-String", listOf(23, 28, 33, 38, 43)),
        Tuning("bass_dropd", "Bass", "Drop D", listOf(26, 33, 38, 43)),

        Tuning("ukulele_std", "Ukulele", "Standard (GCEA)", listOf(67, 60, 64, 69)),
        Tuning("ukulele_baritone", "Ukulele", "Baritone", listOf(50, 55, 59, 64)),

        Tuning("banjo_openg", "Banjo", "Open G", listOf(50, 55, 59, 62, 67)),

        Tuning("mandolin_std", "Mandolin", "Standard", listOf(55, 62, 69, 76)),

        Tuning("violin_std", "Violin", "Standard", listOf(55, 62, 69, 76)),
        Tuning("viola_std", "Viola", "Standard", listOf(48, 55, 62, 69)),
        Tuning("cello_std", "Cello", "Standard", listOf(36, 43, 50, 57)),
        Tuning("double_bass_std", "Double Bass", "Standard", listOf(28, 33, 38, 43)),

        Tuning("chromatic", "Chromatic", "Chromatic", emptyList()),
    )

    val instruments: List<String> = listOf(
        "Guitar",
        "Bass",
        "Ukulele",
        "Banjo",
        "Mandolin",
        "Violin",
        "Viola",
        "Cello",
        "Double Bass",
        "Chromatic",
        "Custom",
    )

    fun forInstrument(instrument: String, custom: List<Tuning> = emptyList()): List<Tuning> =
        if (instrument == "Custom") custom else all.filter { it.instrument == instrument }

    fun byId(id: String?, custom: List<Tuning> = emptyList()): Tuning {
        val mappedId = when (id) {
            "bass4" -> "bass_std"
            "ukulele" -> "ukulele_std"
            "violin" -> "violin_std"
            else -> id
        }
        return custom.firstOrNull { it.id == mappedId }
            ?: all.firstOrNull { it.id == mappedId }
            ?: all[0]
    }
}

object CustomTuningValidator {
    const val MIN_STRINGS = 4
    const val MAX_STRINGS = 8
    const val MIN_MIDI = 24
    const val MAX_MIDI = 88
    const val MAX_NAME = 24
    const val MAX_TUNINGS = 50

    fun validate(name: String, strings: List<Int>): String? {
        if (name.trim().isEmpty()) return "Tuning name cannot be empty"
        if (name.length > MAX_NAME) return "Tuning name is limited to $MAX_NAME characters"
        if (strings.size !in MIN_STRINGS..MAX_STRINGS) {
            return "Tuning must have between $MIN_STRINGS and $MAX_STRINGS strings"
        }
        if (strings.any { it !in MIN_MIDI..MAX_MIDI }) {
            return "Strings must be between MIDI $MIN_MIDI and $MAX_MIDI"
        }
        return null
    }
}

object CustomTuningStore {
    fun serialize(tunings: List<Tuning>): String =
        tunings.joinToString("\n") { t ->
            // Tabs and line breaks are the file's separators, so a name containing them could forge extra records.
            val cleanName = t.name.replace(Regex("[\\t\\r\\n]"), " ").trim()
            val notesStr = t.strings.joinToString(",")
            "${t.id}\t$cleanName\t$notesStr"
        }

    fun deserialize(data: String?): List<Tuning> {
        if (data.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Tuning>()
        data.lineSequence().take(CustomTuningValidator.MAX_TUNINGS).forEach { line ->
            val parts = line.split("\t")
            if (parts.size >= 3) {
                val id = parts[0]
                val name = parts[1]
                val notes = parts[2].split(",").mapNotNull { it.trim().toIntOrNull() }
                if (CustomTuningValidator.validate(name, notes) == null) {
                    list.add(Tuning(id = id, instrument = "Custom", name = name, strings = notes))
                }
            }
        }
        return list
    }
}
