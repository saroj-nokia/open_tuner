@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.opentuner.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opentuner.app.music.ChordType
import com.opentuner.app.music.ChordVoicing
import com.opentuner.app.music.Chords

@Composable
fun ChordScreen(
    state: ChordUiState,
    onSelectRoot: (String) -> Unit,
    onSelectType: (ChordType) -> Unit,
    onToggleLeftHanded: () -> Unit,
    onStrum: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val cs = MaterialTheme.colorScheme
    val voicing = state.currentVoicing
    val notes = state.chordNotes

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with Left-Handed toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Chord Library",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = cs.onSurface,
                )
                Text(
                    text = if (state.leftHanded) "Left-handed view" else "Right-handed view",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            FilterChip(
                selected = state.leftHanded,
                onClick = onToggleLeftHanded,
                label = { Text(if (state.leftHanded) "Left-handed" else "Right-handed") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Toggle hand view",
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.testTag("left_handed_chord_toggle"),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Root Selector Chips (C .. B)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(Chords.ROOTS) { root ->
                val selected = root == state.selectedRoot
                FilterChip(
                    selected = selected,
                    onClick = { onSelectRoot(root) },
                    label = { Text(root, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cs.primary,
                        selectedLabelColor = cs.onPrimary,
                    ),
                    modifier = Modifier.testTag("root_chip_$root"),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Type Selector Chips (maj, m, 7, maj7, m7, sus2, sus4, dim, aug)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(ChordType.entries) { type ->
                val selected = type == state.selectedType
                FilterChip(
                    selected = selected,
                    onClick = { onSelectType(type) },
                    label = { Text(type.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cs.primaryContainer,
                        selectedLabelColor = cs.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag("type_chip_${type.name}"),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Fretboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onStrum() }
                .testTag("chord_card"),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Chord Name & Formula
                Text(
                    text = voicing.chordName,
                    style = MaterialTheme.typography.displaySmallEmphasized.copy(fontSize = 38.sp),
                    color = cs.onSurface,
                )
                Text(
                    text = "${voicing.displayName}  •  ${notes.joinToString(" - ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                // Fretboard Canvas Diagram
                ChordFretboardCanvas(
                    voicing = voicing,
                    isLeftHanded = state.leftHanded,
                    isStrumming = state.isStrumming,
                    modifier = Modifier
                        .size(width = 250.dp, height = 280.dp)
                        .testTag("fretboard_canvas"),
                )

                Spacer(Modifier.height(16.dp))

                // Strum Button
                val scale by animateFloatAsState(
                    targetValue = if (state.isStrumming) 0.94f else 1.0f,
                    animationSpec = tween(durationMillis = 150),
                    label = "strum_scale",
                )
                Button(
                    onClick = onStrum,
                    modifier = Modifier
                        .scale(scale)
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                        .testTag("strum_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isStrumming) cs.tertiary else cs.primary,
                        contentColor = if (state.isStrumming) cs.onTertiary else cs.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isStrumming) "Strumming..." else "Strum Chord", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Draws an authentic 6-string guitar fretboard diagram on Compose Canvas.
 */
@Composable
fun ChordFretboardCanvas(
    voicing: ChordVoicing,
    isLeftHanded: Boolean,
    isStrumming: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val fretColor = cs.outlineVariant
    val nutColor = cs.onSurface
    val stringColor = cs.outline
    val dotColor = if (isStrumming) cs.tertiary else cs.primary
    val onDotColor = if (isStrumming) cs.onTertiary else cs.onPrimary
    val markerColor = cs.onSurfaceVariant

    val textPaint = remember(markerColor) {
        Paint().apply {
            color = markerColor.toArgb()
            textSize = with(density) { 14.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    val fingerPaint = remember(onDotColor) {
        Paint().apply {
            color = onDotColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    val baseFretPaint = remember(markerColor) {
        Paint().apply {
            color = markerColor.toArgb()
            textSize = with(density) { 13.sp.toPx() }
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftPad = 48.dp.toPx()
        val rightPad = 24.dp.toPx()
        val topPad = 36.dp.toPx()
        val bottomPad = 20.dp.toPx()

        val fretboardWidth = w - leftPad - rightPad
        val fretboardHeight = h - topPad - bottomPad

        val stringSpacing = fretboardWidth / 5f
        val numFretSpaces = 4
        val fretSpacing = fretboardHeight / numFretSpaces.toFloat()

        val nutY = topPad
        val bottomY = topPad + fretboardHeight

        // 1. Draw Nut or Top Fret
        if (voicing.baseFret == 1) {
            // Thick Nut bar
            drawRoundRect(
                color = nutColor,
                topLeft = Offset(leftPad - 2f, nutY - 5.dp.toPx()),
                size = Size(fretboardWidth + 4f, 5.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        } else {
            // Regular top fret line
            drawLine(
                color = fretColor,
                start = Offset(leftPad, nutY),
                end = Offset(leftPad + fretboardWidth, nutY),
                strokeWidth = 2.dp.toPx(),
            )
            // Draw base fret number label (e.g. "3 fr") to the left of the first fret slot
            drawContext.canvas.nativeCanvas.drawText(
                "${voicing.baseFret} fr",
                leftPad - 8.dp.toPx(),
                nutY + fretSpacing * 0.65f,
                baseFretPaint,
            )
        }

        // 2. Draw remaining horizontal fret lines
        for (f in 1..numFretSpaces) {
            val y = nutY + f * fretSpacing
            drawLine(
                color = fretColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + fretboardWidth, y),
                strokeWidth = 1.8.dp.toPx(),
            )
        }

        // 3. Draw vertical strings (from Low E to High E, adjusting line thickness)
        for (s in 0..5) {
            val x = leftPad + s * stringSpacing
            // In standard right-handed: s=0 is Low E (thickest), s=5 is High E (thinnest).
            // In left-handed: s=0 is High E (thinnest), s=5 is Low E (thickest).
            val strokeDp = if (isLeftHanded) {
                1.0f + s * 0.45f
            } else {
                3.2f - s * 0.40f
            }
            drawLine(
                color = stringColor,
                start = Offset(x, nutY),
                end = Offset(x, bottomY),
                strokeWidth = strokeDp.dp.toPx(),
            )
        }

        // 4. Draw Barre capsule if applicable
        if (voicing.barreFret != null) {
            val relFret = voicing.barreFret - voicing.baseFret + 1
            if (relFret in 1..numFretSpaces) {
                val cy = nutY + (relFret - 0.5f) * fretSpacing
                val fromX = leftPad + voicing.barreFrom * stringSpacing
                val toX = leftPad + voicing.barreTo * stringSpacing
                val minX = kotlin.math.min(fromX, toX)
                val maxX = kotlin.math.max(fromX, toX)
                val barreHeight = 16.dp.toPx()

                drawRoundRect(
                    color = dotColor.copy(alpha = 0.85f),
                    topLeft = Offset(minX - 10.dp.toPx(), cy - barreHeight / 2f),
                    size = Size(maxX - minX + 20.dp.toPx(), barreHeight),
                    cornerRadius = CornerRadius(barreHeight / 2f, barreHeight / 2f),
                )
            }
        }

        // 5. Draw Open (O), Mute (X) markers and Finger Dots
        val dotRadius = 11.5.dp.toPx()

        for (s in 0..5) {
            val fret = voicing.frets.getOrElse(s) { -1 }
            val finger = voicing.fingers.getOrNull(s) ?: -1
            val x = leftPad + s * stringSpacing

            when {
                fret == -1 -> {
                    // Muted string: 'X' above nut
                    drawContext.canvas.nativeCanvas.drawText(
                        "✕",
                        x,
                        nutY - 12.dp.toPx(),
                        textPaint,
                    )
                }
                fret == 0 -> {
                    // Open string: '○' above nut
                    drawContext.canvas.nativeCanvas.drawText(
                        "○",
                        x,
                        nutY - 12.dp.toPx(),
                        textPaint,
                    )
                }
                fret > 0 -> {
                    // Fretted note: filled circle at relative fret
                    val relFret = fret - voicing.baseFret + 1
                    if (relFret in 1..numFretSpaces) {
                        val cy = nutY + (relFret - 0.5f) * fretSpacing
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, cy),
                        )
                        // Finger number inside dot (if 1..4)
                        if (finger in 1..4) {
                            val textBaselineOffset = with(density) { 4.sp.toPx() }
                            drawContext.canvas.nativeCanvas.drawText(
                                "$finger",
                                x,
                                cy + textBaselineOffset,
                                fingerPaint,
                            )
                        }
                    }
                }
            }
        }
    }
}
