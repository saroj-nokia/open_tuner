package com.opentuner.app.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opentuner.app.audio.AudioEngine
import com.opentuner.app.audio.PitchResult
import com.opentuner.app.audio.ToneGenerator
import com.opentuner.app.audio.YinPitchDetector
import com.opentuner.app.music.AudioSignalMath
import com.opentuner.app.music.CustomTuningStore
import com.opentuner.app.music.CustomTuningValidator
import com.opentuner.app.music.InTuneAffirmationTracker
import com.opentuner.app.music.Note
import com.opentuner.app.music.Pitch
import com.opentuner.app.music.Tuning
import com.opentuner.app.music.Tunings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TunerDisplayMode {
    NEEDLE,
    STROBE,
}

data class TunerUiState(
    val a4: Float,
    val tuning: Tuning,
    val leftHanded: Boolean = false,
    val displayMode: TunerDisplayMode = TunerDisplayMode.NEEDLE,
    val customTunings: List<Tuning> = emptyList(),
    val lockedString: Int? = null,
    val activeString: Int? = null,
    val hasSignal: Boolean = false,
    val toneActive: Boolean = false,
    val frequency: Float = 0f,
    val note: Note? = null,
    val cents: Float = 0f,
    val inTune: Boolean = false,
    val inputLevelNormalized: Float = 0f,
    val inputLevelDb: Float = -80f,
    val inputAboveGate: Boolean = false,
    val noiseGateDb: Float = AudioSignalMath.GATE_PRESET_NORMAL,
    val inTuneChimeEnabled: Boolean = true,
    val inTuneHapticEnabled: Boolean = true,
    val inTuneHoldProgress: Float = 0f,
    val affirmationTrigger: Long = 0L,
)

private data class AudioAnalysis(
    val pitch: PitchResult?,
    val rms: Float,
    val db: Float,
    val aboveGate: Boolean,
)

class TunerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("tuner", Context.MODE_PRIVATE)
    private val engine = AudioEngine()
    private val detector = YinPitchDetector(engine.sampleRate)
    private val loadedCustom = CustomTuningStore.deserialize(prefs.getString("custom_tunings", null))
    private val loadedDisplayMode = try {
        prefs.getString("display_mode", null)?.let { TunerDisplayMode.valueOf(it.uppercase()) } ?: TunerDisplayMode.NEEDLE
    } catch (_: Exception) {
        TunerDisplayMode.NEEDLE
    }
    private val loadedNoiseGate = prefs.getFloat("noise_gate_db", AudioSignalMath.GATE_PRESET_NORMAL)
    private val loadedInTuneChime = prefs.getBoolean("in_tune_chime", true)
    private val loadedInTuneHaptic = prefs.getBoolean("in_tune_haptic", true)
    private val affirmationTracker = InTuneAffirmationTracker()

    private val _state = MutableStateFlow(
        TunerUiState(
            a4 = prefs.getFloat("a4", 440f),
            tuning = Tunings.byId(prefs.getString("tuning", null), loadedCustom),
            leftHanded = prefs.getBoolean("left_handed", false),
            displayMode = loadedDisplayMode,
            customTunings = loadedCustom,
            noiseGateDb = loadedNoiseGate,
            inTuneChimeEnabled = loadedInTuneChime,
            inTuneHapticEnabled = loadedInTuneHaptic,
        ),
    )
    val state = _state.asStateFlow()

    private var listenJob: Job? = null
    private var toneJob: Job? = null
    private var lastSignalAt = 0L
    private var lastTarget: Int? = null
    private var smoothCents = 0f

    // The speaker chime is loud enough for the mic to hear (C6 is inside the detector's range), so skip frames while it rings.
    @Volatile private var ignoreAudioUntil = 0L

    fun startListening() {
        if (listenJob?.isActive == true) return
        listenJob = viewModelScope.launch {
            engine.frames()
                .map { frame ->
                    val rms = AudioSignalMath.calculateRms(frame)
                    val db = AudioSignalMath.rmsToDb(rms)
                    val aboveGate = db >= _state.value.noiseGateDb
                    val pitch = if (aboveGate) detector.detect(frame) else null
                    AudioAnalysis(pitch, rms, db, aboveGate)
                }
                .flowOn(Dispatchers.Default)
                .collect {
                    if (!_state.value.toneActive && SystemClock.elapsedRealtime() >= ignoreAudioUntil) onAudioAnalysis(it)
                }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        toneJob?.cancel()
        ToneGenerator.stop()
        affirmationTracker.reset()
        _state.update {
            it.copy(
                hasSignal = false,
                toneActive = false,
                inTune = false,
                inputLevelNormalized = 0f,
                inputLevelDb = -80f,
                inputAboveGate = false,
                inTuneHoldProgress = 0f,
            )
        }
    }

    private fun onAudioAnalysis(analysis: AudioAnalysis) {
        val now = SystemClock.elapsedRealtime()
        val normLevel = AudioSignalMath.normalizeDb(analysis.db)
        val r = analysis.pitch

        if (r == null || r.confidence < 0.85f || !analysis.aboveGate) {
            val trigger = affirmationTracker.update(null, 0f, hasSignal = false, currentTimeMs = now)
            // Hold the last reading for a moment so the needle doesn't flicker between plucks.
            if (now - lastSignalAt > 600) {
                _state.update {
                    it.copy(
                        hasSignal = false,
                        inTune = false,
                        inputLevelNormalized = normLevel,
                        inputLevelDb = analysis.db,
                        inputAboveGate = analysis.aboveGate,
                        inTuneHoldProgress = 0f,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        inputLevelNormalized = normLevel,
                        inputLevelDb = analysis.db,
                        inputAboveGate = analysis.aboveGate,
                        inTuneHoldProgress = trigger.holdProgress,
                    )
                }
            }
            return
        }
        lastSignalAt = now

        val s = _state.value
        val f = r.frequency.toDouble()
        val a4 = s.a4.toDouble()
        val strings = s.tuning.strings
        val idx = s.lockedString
            ?: if (strings.isEmpty()) null else strings.indices.minBy { abs(Pitch.midiOf(f, a4) - strings[it]) }
        val targetMidi = idx?.let { strings[it] } ?: Pitch.midiOf(f, a4).roundToInt()

        val raw = Pitch.centsBetween(f, Pitch.freqOf(targetMidi, a4)).toFloat()
        smoothCents = if (targetMidi != lastTarget) raw else smoothCents + 0.4f * (raw - smoothCents)
        lastTarget = targetMidi

        val trigger = affirmationTracker.update(targetMidi, smoothCents, hasSignal = true, currentTimeMs = now)

        if (trigger.shouldAffirm && s.inTuneChimeEnabled) {
            // 0.22 s chime + room tail + the 93 ms analysis window that still holds chime samples.
            ignoreAudioUntil = now + 500
            viewModelScope.launch(Dispatchers.Default) {
                ToneGenerator.playInTuneChime()
            }
        }

        _state.update {
            it.copy(
                hasSignal = true,
                frequency = r.frequency,
                note = Note(targetMidi),
                cents = smoothCents,
                inTune = abs(smoothCents) <= 5f,
                activeString = idx,
                inputLevelNormalized = normLevel,
                inputLevelDb = analysis.db,
                inputAboveGate = analysis.aboveGate,
                inTuneHoldProgress = trigger.holdProgress,
                affirmationTrigger = if (trigger.shouldAffirm) now else it.affirmationTrigger,
            )
        }
    }

    /** Tap a string to lock onto it and hear it; tap again to go back to auto-detect. */
    fun selectString(i: Int) {
        val s = _state.value
        if (s.lockedString == i) {
            _state.update { it.copy(lockedString = null) }
            return
        }
        _state.update { it.copy(lockedString = i) }
        playTone(Pitch.freqOf(s.tuning.strings[i], s.a4.toDouble()))
    }

    // Detection pauses while the reference tone plays, otherwise the mic would tune the speaker.
    private fun playTone(freq: Double) {
        toneJob?.cancel()
        toneJob = viewModelScope.launch {
            _state.update { it.copy(toneActive = true, hasSignal = false, inTune = false) }
            withContext(Dispatchers.Default) { ToneGenerator.play(freq) }
            delay(2500)
            _state.update { it.copy(toneActive = false) }
        }
    }

    fun setTuning(t: Tuning) {
        lastTarget = null
        affirmationTracker.reset()
        _state.update { it.copy(tuning = t, lockedString = null, activeString = null, inTuneHoldProgress = 0f) }
        prefs.edit { putString("tuning", t.id) }
    }

    fun setA4(hz: Float) {
        _state.update { it.copy(a4 = hz) }
        prefs.edit { putFloat("a4", hz) }
    }

    fun setLeftHanded(enabled: Boolean) {
        _state.update { it.copy(leftHanded = enabled) }
        prefs.edit { putBoolean("left_handed", enabled) }
    }

    fun setDisplayMode(mode: TunerDisplayMode) {
        _state.update { it.copy(displayMode = mode) }
        prefs.edit { putString("display_mode", mode.name.lowercase()) }
    }

    fun setNoiseGateDb(db: Float) {
        _state.update { it.copy(noiseGateDb = db) }
        prefs.edit { putFloat("noise_gate_db", db) }
    }

    fun setInTuneChimeEnabled(enabled: Boolean) {
        _state.update { it.copy(inTuneChimeEnabled = enabled) }
        prefs.edit { putBoolean("in_tune_chime", enabled) }
    }

    fun setInTuneHapticEnabled(enabled: Boolean) {
        _state.update { it.copy(inTuneHapticEnabled = enabled) }
        prefs.edit { putBoolean("in_tune_haptic", enabled) }
    }

    fun addCustomTuning(name: String, strings: List<Int>): Tuning? {
        val error = CustomTuningValidator.validate(name, strings)
        if (error != null) return null
        val tuning = Tuning(
            id = "custom_${System.currentTimeMillis()}",
            instrument = "Custom",
            name = name.trim(),
            strings = strings,
        )
        val updated = _state.value.customTunings + tuning
        _state.update { it.copy(customTunings = updated, tuning = tuning, lockedString = null, activeString = null) }
        prefs.edit {
            putString("custom_tunings", CustomTuningStore.serialize(updated))
            putString("tuning", tuning.id)
        }
        return tuning
    }

    fun deleteCustomTuning(id: String) {
        val updated = _state.value.customTunings.filterNot { it.id == id }
        val current = _state.value.tuning
        val nextTuning = if (current.id == id) Tunings.all[0] else current
        _state.update { it.copy(customTunings = updated, tuning = nextTuning, lockedString = null, activeString = null) }
        prefs.edit {
            putString("custom_tunings", CustomTuningStore.serialize(updated))
            putString("tuning", nextTuning.id)
        }
    }

    fun playPreview(midi: Int) {
        playTone(Pitch.freqOf(midi, _state.value.a4.toDouble()))
    }

    override fun onCleared() {
        ToneGenerator.stop()
    }
}
