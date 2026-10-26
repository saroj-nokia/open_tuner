package com.opentuner.app.metronome

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetronomeUiState(
    val bpm: Int = 120,
    val timeSignature: TimeSignature = TimeSignature(4, 4),
    val accentFirstBeat: Boolean = true,
    val isPlaying: Boolean = false,
    val currentBeat: Int = 0,
    val isAccent: Boolean = false,
)

class MetronomeViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = MetronomeEngine()
    private val tapTimes = mutableListOf<Long>()

    private val _state = MutableStateFlow(MetronomeUiState())
    val state = _state.asStateFlow()

    init {
        engine.onBeatCallback = { beatIndex, isAccent ->
            viewModelScope.launch(Dispatchers.Main) {
                _state.update { it.copy(currentBeat = beatIndex, isAccent = isAccent) }
            }
        }
    }

    fun togglePlay() {
        if (_state.value.isPlaying) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        engine.bpm = _state.value.bpm
        engine.timeSignature = _state.value.timeSignature
        engine.accentFirstBeat = _state.value.accentFirstBeat
        engine.start()
        _state.update { it.copy(isPlaying = true) }
    }

    fun stop() {
        engine.stop()
        _state.update { it.copy(isPlaying = false, currentBeat = 0, isAccent = false) }
    }

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(MetronomeMath.MIN_BPM, MetronomeMath.MAX_BPM)
        engine.bpm = clamped
        _state.update { it.copy(bpm = clamped) }
    }

    fun incrementBpm(delta: Int) {
        setBpm(_state.value.bpm + delta)
    }

    fun setTimeSignature(sig: TimeSignature) {
        engine.timeSignature = sig
        _state.update { it.copy(timeSignature = sig, currentBeat = 0) }
    }

    fun setAccent(enabled: Boolean) {
        engine.accentFirstBeat = enabled
        _state.update { it.copy(accentFirstBeat = enabled) }
    }

    fun tapTempo() {
        val now = SystemClock.elapsedRealtime()
        if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2500) {
            tapTimes.clear()
        }
        tapTimes.add(now)
        if (tapTimes.size > 8) tapTimes.removeAt(0)
        val calculated = MetronomeMath.calculateTapBpm(tapTimes)
        if (calculated != null) {
            setBpm(calculated)
        }
    }

    override fun onCleared() {
        engine.onBeatCallback = null
        engine.stop()
    }
}
