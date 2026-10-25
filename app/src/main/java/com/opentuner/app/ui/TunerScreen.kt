@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.opentuner.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import com.opentuner.app.metronome.MetronomeViewModel
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.mutableIntStateOf
import com.opentuner.app.music.CustomTuningValidator
import com.opentuner.app.music.Pitch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opentuner.app.music.Note
import com.opentuner.app.music.Tuning
import com.opentuner.app.music.Tunings
import java.util.Locale
import kotlin.math.roundToInt

enum class AppScreen {
    TUNER,
    METRONOME,
}

@Composable
fun TunerRoute(
    vm: TunerViewModel = viewModel(),
    metronomeVm: MetronomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.TUNER) }
    val state by vm.state.collectAsStateWithLifecycle()
    val metronomeState by metronomeVm.state.collectAsStateWithLifecycle()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    LifecycleResumeEffect(granted, currentScreen) {
        if (granted && currentScreen == AppScreen.TUNER) vm.startListening()
        onPauseOrDispose { vm.stopListening() }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.TUNER,
                    onClick = {
                        if (currentScreen != AppScreen.TUNER) {
                            metronomeVm.stop()
                            currentScreen = AppScreen.TUNER
                        }
                    },
                    icon = { Icon(Icons.Rounded.Build, contentDescription = "Tuner") },
                    label = { Text("Tuner") },
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.METRONOME,
                    onClick = {
                        if (currentScreen != AppScreen.METRONOME) {
                            vm.stopListening()
                            currentScreen = AppScreen.METRONOME
                        }
                    },
                    icon = { Icon(Icons.Rounded.Refresh, contentDescription = "Metronome") },
                    label = { Text("Metronome") },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (currentScreen) {
                AppScreen.TUNER -> {
                    if (granted) {
                        TunerScreen(
                            state = state,
                            onString = vm::selectString,
                            onTuning = vm::setTuning,
                            onA4 = vm::setA4,
                            onLeftHanded = vm::setLeftHanded,
                            onAddCustomTuning = vm::addCustomTuning,
                            onDeleteCustomTuning = vm::deleteCustomTuning,
                            onPreviewPitch = vm::playPreview,
                        )
                    } else {
                        PermissionScreen(
                            onAllow = { ask.launch(Manifest.permission.RECORD_AUDIO) },
                            onSettings = {
                                context.startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            },
                        )
                    }
                }
                AppScreen.METRONOME -> {
                    MetronomeScreen(
                        state = metronomeState,
                        onTogglePlay = metronomeVm::togglePlay,
                        onBpmChange = metronomeVm::setBpm,
                        onIncrementBpm = metronomeVm::incrementBpm,
                        onTimeSignatureChange = metronomeVm::setTimeSignature,
                        onAccentChange = metronomeVm::setAccent,
                        onTapTempo = metronomeVm::tapTempo,
                        onBack = { currentScreen = AppScreen.TUNER },
                    )
                }
            }
        }
    }
}

@Composable
private fun TunerScreen(
    state: TunerUiState,
    onString: (Int) -> Unit,
    onTuning: (Tuning) -> Unit,
    onA4: (Float) -> Unit,
    onLeftHanded: (Boolean) -> Unit,
    onAddCustomTuning: (String, List<Int>) -> Tuning?,
    onDeleteCustomTuning: (String) -> Unit,
    onPreviewPitch: (Int) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }
    var tuningToDelete by remember { mutableStateOf<Tuning?>(null) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.inTune) {
        if (state.inTune) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Open Tuner", style = MaterialTheme.typography.headlineMediumEmphasized)
                    Text(
                        "A4 = ${state.a4.roundToInt()} Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(onClick = { showSettings = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }

            NoteHero(state, Modifier.weight(1f).fillMaxWidth())
            Status(state)
            CentsMeter(state.cents, state.hasSignal, state.inTune)
            StringRow(state, onString)

            var selectedInstrument by remember(state.tuning.id) {
                mutableStateOf(state.tuning.instrument)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Tunings.instruments) { inst ->
                        FilterChip(
                            selected = inst == selectedInstrument,
                            onClick = {
                                selectedInstrument = inst
                                if (inst == "Custom") {
                                    if (state.customTunings.isNotEmpty() && state.tuning.instrument != "Custom") {
                                        onTuning(state.customTunings.first())
                                    }
                                } else {
                                    val tunings = Tunings.forInstrument(inst, state.customTunings)
                                    if (tunings.isNotEmpty() && tunings.none { it.id == state.tuning.id }) {
                                        onTuning(tunings.first())
                                    }
                                }
                            },
                            leadingIcon = if (inst == "Custom") {
                                { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(inst) },
                        )
                    }
                }

                if (selectedInstrument == "Custom") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            AssistChip(
                                onClick = { showCustomSheet = true },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = "New custom tuning", modifier = Modifier.size(16.dp)) },
                                label = { Text("New Tuning") },
                            )
                        }
                        items(state.customTunings) { t ->
                            FilterChip(
                                selected = t.id == state.tuning.id,
                                onClick = { onTuning(t) },
                                label = { Text(t.name) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { tuningToDelete = t },
                                        modifier = Modifier.size(20.dp),
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Delete ${t.name}", modifier = Modifier.size(14.dp))
                                    }
                                },
                            )
                        }
                    }
                } else {
                    val currentTunings = remember(selectedInstrument, state.customTunings) {
                        Tunings.forInstrument(selectedInstrument, state.customTunings)
                    }
                    if (currentTunings.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentTunings) { t ->
                                FilterChip(
                                    selected = t.id == state.tuning.id,
                                    onClick = { onTuning(t) },
                                    label = { Text(t.name) },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

    if (showSettings) SettingsSheet(state.a4, state.leftHanded, onA4, onLeftHanded) { showSettings = false }

    if (showCustomSheet) {
        CustomTuningSheet(
            a4 = state.a4,
            onPreview = onPreviewPitch,
            onSave = { name, strings ->
                onAddCustomTuning(name, strings)
            },
            onDismiss = { showCustomSheet = false },
        )
    }

    if (tuningToDelete != null) {
        AlertDialog(
            onDismissRequest = { tuningToDelete = null },
            title = { Text("Delete tuning") },
            text = { Text("Delete \"${tuningToDelete?.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tuningToDelete?.id?.let { onDeleteCustomTuning(it) }
                        tuningToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tuningToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ---- Hero: a Material shape that morphs idle -> listening -> in tune -------------------------------------------

/** Draws a [Morph] (from graphics-shapes) as a Compose shape. Polygons must be normalized to 0..1. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cubics = morph.asCubics(progress)
        val path = Path()
        path.moveTo(cubics.first().anchor0X * size.width, cubics.first().anchor0Y * size.height)
        for (c in cubics) {
            path.cubicTo(
                c.control0X * size.width, c.control0Y * size.height,
                c.control1X * size.width, c.control1Y * size.height,
                c.anchor1X * size.width, c.anchor1Y * size.height,
            )
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
private fun NoteHero(s: TunerUiState, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val idleToListening = remember {
        Morph(MaterialShapes.Cookie9Sided.normalized(), MaterialShapes.Cookie12Sided.normalized())
    }
    val listeningToTuned = remember {
        Morph(MaterialShapes.Cookie12Sided.normalized(), MaterialShapes.Sunny.normalized())
    }

    val tuned = s.hasSignal && s.inTune
    val stage by animateFloatAsState(
        targetValue = if (tuned) 2f else if (s.hasSignal) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "stage",
    )
    val container by animateColorAsState(
        targetValue = if (tuned) cs.tertiaryContainer else if (s.hasSignal) cs.primaryContainer else cs.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "container",
    )
    val onContainer = if (tuned) cs.onTertiaryContainer else if (s.hasSignal) cs.onPrimaryContainer else cs.onSurfaceVariant
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing)),
        label = "spin",
    )
    val shape = if (stage <= 1f) MorphShape(idleToListening, stage) else MorphShape(listeningToTuned, stage - 1f)
    val locked = s.lockedString?.let { Note(s.tuning.strings[it]).label }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(minOf(maxWidth, maxHeight)), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }.clip(shape).background(container))
            val note = s.note
            when {
                s.toneActive -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Playing", style = MaterialTheme.typography.titleLargeEmphasized, color = onContainer)
                    Text(locked.orEmpty(), style = MaterialTheme.typography.displayMediumEmphasized, color = onContainer)
                }
                s.hasSignal && note != null -> Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.name,
                        style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = 112.sp, lineHeight = 112.sp),
                        color = onContainer,
                    )
                    Text(
                        note.octave.toString(),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = onContainer,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
                else -> Text(
                    "Pluck a\nstring",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = onContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Status(s: TunerUiState) {
    val text = when {
        s.toneActive -> "Reference tone"
        !s.hasSignal -> "Listening…"
        s.inTune -> "In tune"
        s.cents < 0 -> "Too low — tune up"
        else -> "Too high — tune down"
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.titleLargeEmphasized)
        Text(
            if (s.hasSignal) "%.1f Hz  ·  %+.0f cents".format(Locale.US, s.frequency, s.cents) else " ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Cents meter -----------------------------------------------------------------------------------------------

@Composable
private fun CentsMeter(cents: Float, active: Boolean, inTune: Boolean) {
    val cs = MaterialTheme.colorScheme
    val pos by animateFloatAsState(
        targetValue = if (active) cents.coerceIn(-50f, 50f) else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "cents",
    )
    val pill by animateColorAsState(
        targetValue = if (!active) cs.outlineVariant else if (inTune) cs.tertiary else cs.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "pill",
    )
    Column {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val barH = 40.dp.toPx()
            val top = (size.height - barH) / 2
            val half = size.width / 2 - barH / 2
            fun x(c: Float) = size.width / 2 + c / 50f * half

            drawRoundRect(cs.surfaceContainerHighest, Offset(0f, top), Size(size.width, barH), CornerRadius(barH / 2))
            drawRoundRect(cs.tertiaryContainer, Offset(x(-5f), top), Size(x(5f) - x(-5f), barH), CornerRadius(barH / 2))
            for (c in -40..40 step 10) {
                if (c != 0) drawCircle(cs.outline, 2.dp.toPx(), Offset(x(c.toFloat()), size.height / 2))
            }
            val w = 14.dp.toPx()
            drawRoundRect(pill, Offset(x(pos) - w / 2, top - 8.dp.toPx()), Size(w, barH + 16.dp.toPx()), CornerRadius(w / 2))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("♭", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
            Text("♯", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
        }
    }
}

// ---- Strings ---------------------------------------------------------------------------------------------------

@Composable
private fun StringRow(s: TunerUiState, onString: (Int) -> Unit) {
    val hint = when {
        s.tuning.isChromatic -> "Chromatic — play any note"
        s.lockedString == null -> "Auto-detect · tap a string to lock it and hear it"
        else -> "Locked to ${Note(s.tuning.strings[s.lockedString]).label} · tap again to release"
    }
    val count = s.tuning.strings.size
    val spacing = if (count > 6) 4.dp else 6.dp
    val textStyle = if (count > 6) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!s.tuning.isChromatic) {
            val indices = if (s.leftHanded) s.tuning.strings.indices.reversed() else s.tuning.strings.indices
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                indices.forEach { i ->
                    val midi = s.tuning.strings[i]
                    ToggleButton(
                        checked = s.activeString == i,
                        onCheckedChange = { onString(i) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(Note(midi).name, maxLines = 1, style = textStyle) }
                }
            }
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

// ---- Settings --------------------------------------------------------------------------------------------------

@Composable
private fun SettingsSheet(
    a4: Float,
    leftHanded: Boolean,
    onA4: (Float) -> Unit,
    onLeftHanded: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMediumEmphasized)

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Left-handed mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Reverses string button order for left-handed view",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = leftHanded,
                    onCheckedChange = onLeftHanded,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("Reference pitch", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Slider(
                    value = a4,
                    onValueChange = { onA4(it.roundToInt().toFloat()) },
                    valueRange = 415f..466f,
                    steps = 50,
                    modifier = Modifier.weight(1f),
                )
                Text("${a4.roundToInt()} Hz", style = MaterialTheme.typography.titleLargeEmphasized)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(432, 440, 442).forEach { hz ->
                    ToggleButton(
                        checked = a4.roundToInt() == hz,
                        onCheckedChange = { onA4(hz.toFloat()) },
                    ) { Text("$hz") }
                }
            }
        }
    }
}

// ---- Custom Tuning Sheet ---------------------------------------------------------------------------------------

@Composable
private fun CustomTuningSheet(
    a4: Float,
    onPreview: (Int) -> Unit,
    onSave: (String, List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var stringCount by remember { mutableIntStateOf(6) }
    var strings by remember { mutableStateOf(listOf(40, 45, 50, 55, 59, 64)) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("New custom tuning", style = MaterialTheme.typography.headlineMediumEmphasized)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tuning name") },
                placeholder = { Text("e.g. Open C, Drop B, Modal D") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Number of strings", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (4..8).forEach { count ->
                    FilterChip(
                        selected = stringCount == count,
                        onClick = {
                            if (count > strings.size) {
                                val added = (strings.size until count).map {
                                    val last = strings.lastOrNull() ?: 40
                                    (last + 5).coerceIn(CustomTuningValidator.MIN_MIDI, CustomTuningValidator.MAX_MIDI)
                                }
                                strings = strings + added
                            } else if (count < strings.size) {
                                strings = strings.take(count)
                            }
                            stringCount = count
                        },
                        label = { Text("$count") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text("Tune strings (lowest to highest)", style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                strings.forEachIndexed { i, midi ->
                    val note = Note(midi)
                    val freq = Pitch.freqOf(midi, a4.toDouble())
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "String ${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(note.label, style = MaterialTheme.typography.titleLargeEmphasized)
                                    Text(
                                        "%.1f Hz".format(Locale.US, freq),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    val updated = strings.toMutableList()
                                    updated[i] = (updated[i] - 1).coerceAtLeast(CustomTuningValidator.MIN_MIDI)
                                    strings = updated
                                    onPreview(updated[i])
                                },
                                enabled = midi > CustomTuningValidator.MIN_MIDI,
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Text("−", style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(Modifier.width(8.dp))

                            FilledTonalIconButton(
                                onClick = {
                                    val updated = strings.toMutableList()
                                    updated[i] = (updated[i] + 1).coerceAtMost(CustomTuningValidator.MAX_MIDI)
                                    strings = updated
                                    onPreview(updated[i])
                                },
                                enabled = midi < CustomTuningValidator.MAX_MIDI,
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            val isValid = CustomTuningValidator.validate(name, strings) == null
            Button(
                onClick = {
                    onSave(name, strings)
                    onDismiss()
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save custom tuning")
            }
        }
    }
}

// ---- Permission ------------------------------------------------------------------------------------------------

@Composable
private fun PermissionScreen(onAllow: () -> Unit, onSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = remember {
        val p = MaterialShapes.Cookie12Sided.normalized()
        MorphShape(Morph(p, p), 0f)
    }
    Column(
        Modifier.fillMaxSize().background(cs.surface).systemBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(180.dp).clip(shape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
            Text("♪", style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = 88.sp), color = cs.onPrimaryContainer)
        }
        Spacer(Modifier.height(32.dp))
        Text("Microphone access", style = MaterialTheme.typography.headlineMediumEmphasized)
        Spacer(Modifier.height(8.dp))
        Text(
            "Open Tuner listens to your instrument to find the pitch. Audio is analysed on this device and is never recorded or sent anywhere.",
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAllow, shapes = ButtonDefaults.shapes()) { Text("Allow microphone") }
        TextButton(onClick = onSettings) { Text("Open app settings") }
    }
}
