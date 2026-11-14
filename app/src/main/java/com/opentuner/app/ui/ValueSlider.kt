@file:OptIn(ExperimentalMaterial3Api::class)

package com.opentuner.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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

    // The bare slider is only about 24dp tall, so a finger that lands or drifts a little off it misses, or turns into a
    // scroll of the Settings sheet. A taller touch area fixes the first; the box eats the touches the slider has already
    // handled so the sheet and its scroll column don't also react to them (the child sees each event first).
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false).consume()
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Slider(
            state = state,
            onValueChange = { state.value = it },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )
    }
}
