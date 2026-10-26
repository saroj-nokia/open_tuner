@file:OptIn(ExperimentalMaterial3Api::class)

package com.opentuner.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlin.math.abs

/**
 * Slider on the non-deprecated SliderState API.
 *
 * The slider's own state is the source of truth while it is touched; its value is reported to the caller
 * by watching the state. Changes that come from outside (the +/- buttons, tap tempo, presets) are copied into the state.
 *
 * shortcut: the 0.5 resync threshold assumes whole-number sliders (every slider here steps by 1), upgrade it to
 * half a step if a fractional slider is ever added.
 */
@Composable
fun ValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
) {
    val state = rememberSliderState(value, steps, trackRange = valueRange)
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)

    // Only resync real jumps, so a drag isn't pulled back toward the rounded value it just reported.
    LaunchedEffect(value) {
        if (abs(state.value - value) > 0.5f) state.value = value
    }

    // Report changes by watching the state itself, so a tap, a drag or a fling all reach the caller
    // whatever path the slider took to write them.
    LaunchedEffect(state) {
        snapshotFlow { state.value }.collect { if (abs(it - latestValue) > 0.001f) latestOnChange(it) }
    }

    Slider(state = state, onValueChange = { state.value = it }, modifier = modifier)
}
