@file:OptIn(ExperimentalMaterial3Api::class)

package com.opentuner.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlin.math.abs

/**
 * Slider on the non-deprecated SliderState API.
 *
 * That API never moves the thumb by itself: the caller must write the new value into the state from
 * onValueChange, otherwise the thumb stays put. Changes that come from outside (the +/- buttons, tap tempo)
 * are copied into the state too.
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

    // Only resync real jumps, so a drag isn't pulled back toward the rounded value it just reported.
    LaunchedEffect(value) {
        if (abs(state.value - value) > 0.5f) state.value = value
    }

    Slider(
        state = state,
        onValueChange = {
            state.value = it
            onValueChange(it)
        },
        modifier = modifier,
    )
}
