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
import com.opentuner.app.music.CustomTuningStore
import com.opentuner.app.music.CustomTuningValidator
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

data class TunerUiState(
    val a4: Float,
    val tuning: Tuning,
    val leftHanded: Boolean = false,
    val customTunings: List<Tuning> = emptyList(),
    val lockedString: Int? = null,
    val activeString: Int? = null,
    val hasSignal: Boolean = false,
    val toneActive: Boolean = false,
    val frequency: Float = 0f,
    val note: Note? = null,
    val cents: Float = 0f,
    val inTune: Boolean = false,
)

class TunerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("tuner", Context.MODE_PRIVATE)
    private val engine = AudioEngine()
    private val detector = YinPitchDetector(engine.sampleRate)
    private val loadedCustom = CustomTuningStore.deserialize(prefs.getString("custom_tunings", null))

    private val _state = MutableStateFlow(
        TunerUiState(
            a4 = prefs.getFloat("a4", 440f),
            tuning = Tunings.byId(prefs.getString("tuning", null), loadedCustom),
            leftHanded = prefs.getBoolean("left_handed", false),
            customTunings = loadedCustom,
        ),
    )
    val state = _state.asStateFlow()

    private var listenJob: Job? = null
    private var toneJob: Job? = null
    private var lastSignalAt = 0L
    private var lastTarget: Int? = null
    private var smoothCents = 0f

    fun startListening() {
        if (listenJob?.isActive == true) return
        listenJob = viewModelScope.launch {
            engine.frames()
                .map { detector.detect(it) }
                .flowOn(Dispatchers.Default)
                .collect { if (!_state.value.toneActive) onPitch(it) }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        toneJob?.cancel()
        ToneGenerator.stop()
        _state.update { it.copy(hasSignal = false, toneActive = false, inTune = false) }
    }

    private fun onPitch(r: PitchResult?) {
        val now = SystemClock.elapsedRealtime()
        if (r == null || r.confidence < 0.85f) {
            // Hold the last reading for a moment so the needle doesn't flicker between plucks.
            if (now - lastSignalAt > 600) _state.update { it.copy(hasSignal = false, inTune = false) }
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

        _state.update {
            it.copy(
                hasSignal = true,
                frequency = r.frequency,
                note = Note(targetMidi),
                cents = smoothCents,
                inTune = abs(smoothCents) <= 5f,
                activeString = idx,
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
        _state.update { it.copy(tuning = t, lockedString = null, activeString = null) }
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
