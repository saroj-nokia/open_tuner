@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.opentuner.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opentuner.app.metronome.MetronomeUiState
import com.opentuner.app.metronome.TimeSignature
import kotlin.math.roundToInt

@Composable
fun MetronomeScreen(
    state: MetronomeUiState,
    onTogglePlay: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onIncrementBpm: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
    onAccentChange: (Boolean) -> Unit,
    onTapTempo: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val cs = MaterialTheme.colorScheme

    val tempoName = when (state.bpm) {
        in 30..59 -> "Largo · Very slow"
        in 60..75 -> "Adagio · Slow & graceful"
        in 76..107 -> "Andante · Walking pace"
        in 108..119 -> "Moderato · Moderate"
        in 120..159 -> "Allegro · Fast & bright"
        in 160..199 -> "Vivace · Lively"
        else -> "Presto · Very fast"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Visual Beat Indicator
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val beatsCount = state.timeSignature.numerator
            for (i in 0 until beatsCount) {
                val isActive = state.isPlaying && state.currentBeat == i
                val isFirst = i == 0 && state.accentFirstBeat

                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.35f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
                    label = "beatScale_$i",
                )

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isActive && isFirst -> cs.tertiary
                        isActive -> cs.primary
                        isFirst -> cs.tertiaryContainer.copy(alpha = 0.6f)
                        else -> cs.surfaceContainerHighest
                    },
                    label = "beatColor_$i",
                )

                val height = if (isFirst) 32.dp else 24.dp
                val width = if (isFirst) 32.dp else 24.dp

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .scale(scale)
                        .size(width = width, height = height)
                        .clip(if (isFirst) RoundedCornerShape(8.dp) else CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = if (isActive) cs.onPrimary else cs.onSurfaceVariant,
                    )
                }
            }
        }

        // BPM Counter & Tempo Name
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${state.bpm}",
                    style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = 72.sp),
                    color = cs.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = cs.primary,
                )
            }
            Text(
                tempoName,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }

        // Fine Adjustment Controls (-5, -1, +1, +5)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = { onIncrementBpm(-5) },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(44.dp),
            ) { Text("-5", style = MaterialTheme.typography.labelLarge) }

            FilledTonalIconButton(
                onClick = { onIncrementBpm(-1) },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(44.dp),
            ) { Text("-1", style = MaterialTheme.typography.labelLarge) }

            FilledTonalButton(
                onClick = onTapTempo,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Tap Tempo", style = MaterialTheme.typography.labelLarge)
            }

            FilledTonalIconButton(
                onClick = { onIncrementBpm(+1) },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(44.dp),
            ) { Text("+1", style = MaterialTheme.typography.labelLarge) }

            FilledTonalIconButton(
                onClick = { onIncrementBpm(+5) },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(44.dp),
            ) { Text("+5", style = MaterialTheme.typography.labelLarge) }
        }

        // Tempo Slider
        Slider(
            value = state.bpm.toFloat(),
            onValueChange = { onBpmChange(it.roundToInt()) },
            valueRange = 30f..300f,
            steps = 270,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )

        HorizontalDivider(color = cs.outlineVariant)

        // Time Signature Selector
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Time signature", style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(TimeSignature.all) { sig ->
                    FilterChip(
                        selected = state.timeSignature == sig,
                        onClick = { onTimeSignatureChange(sig) },
                        label = { Text(sig.label) },
                    )
                }
            }
        }

        // Accent First Beat Toggle
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Accent first beat", style = MaterialTheme.typography.titleMedium)
                Text(
                    "High chime on beat 1 to mark the start of each bar",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.accentFirstBeat,
                onCheckedChange = onAccentChange,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Large Start / Stop Button
        Button(
            onClick = onTogglePlay,
            colors = if (state.isPlaying) {
                ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError)
            } else {
                ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(
                if (state.isPlaying) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Stop Metronome" else "Start Metronome",
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (state.isPlaying) "Stop" else "Start",
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
