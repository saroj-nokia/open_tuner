package com.opentuner.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opentuner.app.audio.ToneGenerator
import com.opentuner.app.music.ChordType
import com.opentuner.app.music.ChordVoicing
import com.opentuner.app.music.Chords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChordUiState(
    val selectedRoot: String = "C",
    val selectedType: ChordType = ChordType.MAJOR,
    val leftHanded: Boolean = false,
    val isStrumming: Boolean = false,
    val a4: Double = 440.0,
) {
    val currentVoicing: ChordVoicing
        get() {
            val v = Chords.getVoicing(selectedRoot, selectedType)
            return if (leftHanded) v.flipped() else v
        }

    val chordNotes: List<String>
        get() = Chords.notesFor(selectedRoot, selectedType)
}

class ChordViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("tuner_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        ChordUiState(
            leftHanded = prefs.getBoolean("left_handed", false),
            a4 = prefs.getFloat("a4", 440f).toDouble(),
        ),
    )
    val state: StateFlow<ChordUiState> = _state.asStateFlow()

    private var strumJob: Job? = null

    fun selectRoot(root: String) {
        _state.update { it.copy(selectedRoot = root) }
    }

    fun selectType(type: ChordType) {
        _state.update { it.copy(selectedType = type) }
    }

    fun setLeftHanded(enabled: Boolean) {
        prefs.edit().putBoolean("left_handed", enabled).apply()
        _state.update { it.copy(leftHanded = enabled) }
    }

    fun toggleLeftHanded() {
        setLeftHanded(!_state.value.leftHanded)
    }

    fun strum() {
        val s = _state.value
        val voicing = Chords.getVoicing(s.selectedRoot, s.selectedType)
        val freqs = voicing.soundedFrequencies(s.a4)
        if (freqs.isNotEmpty()) {
            // Synthesising six strings is real CPU work; keep it off the UI thread.
            viewModelScope.launch(Dispatchers.Default) { ToneGenerator.playStrum(freqs) }
            strumJob?.cancel()
            strumJob = viewModelScope.launch {
                _state.update { it.copy(isStrumming = true) }
                delay(600)
                _state.update { it.copy(isStrumming = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ToneGenerator.stop()
    }
}
