@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.opentuner.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material3.rememberSliderState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.Morph
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.opentuner.app.music.AudioSignalMath
import com.opentuner.app.music.StrobeMath
import com.opentuner.app.music.Note
import com.opentuner.app.music.Tuning
import com.opentuner.app.music.Tunings
import java.util.Locale
import kotlin.math.roundToInt

enum class AppScreen {
    TUNER,
    METRONOME,
    CHORDS,
}

@Composable
fun TunerRoute(
    vm: TunerViewModel = viewModel(),
    metronomeVm: MetronomeViewModel = viewModel(),
    chordVm: ChordViewModel = viewModel(),
) {
    val context = LocalContext.current
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.TUNER) }
    // Plays on every cold start; saved so a rotation doesn't replay it.
    var introDone by rememberSaveable { mutableStateOf(false) }
    val heroBounds = remember { mutableStateOf<Rect?>(null) }
    val state by vm.state.collectAsStateWithLifecycle()
    val metronomeState by metronomeVm.state.collectAsStateWithLifecycle()
    val chordState by chordVm.state.collectAsStateWithLifecycle()

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

    // No foreground service, so Android may silence a backgrounded metronome at random; stop it cleanly instead.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { metronomeVm.stop() }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(Modifier.fillMaxSize()) {
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
                            currentScreen = AppScreen.METRONOME
                        }
                    },
                    icon = { Icon(Icons.Rounded.Refresh, contentDescription = "Metronome") },
                    label = { Text("Metronome") },
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.CHORDS,
                    onClick = {
                        if (currentScreen != AppScreen.CHORDS) {
                            metronomeVm.stop()
                            currentScreen = AppScreen.CHORDS
                        }
                    },
                    icon = { ChordNavIcon(selected = currentScreen == AppScreen.CHORDS) },
                    label = { Text("Chords") },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val motion = MaterialTheme.motionScheme
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
                label = "screens",
            ) { screen ->
            when (screen) {
                AppScreen.TUNER -> {
                    if (granted) {
                        TunerScreen(
                            state = state,
                            onString = vm::selectString,
                            onTuning = vm::setTuning,
                            onA4 = vm::setA4,
                            onLeftHanded = { enabled ->
                                vm.setLeftHanded(enabled)
                                chordVm.setLeftHanded(enabled)
                            },
                            onDisplayMode = vm::setDisplayMode,
                            onNoiseGateDb = vm::setNoiseGateDb,
                            onInTuneChime = vm::setInTuneChimeEnabled,
                            onInTuneHaptic = vm::setInTuneHapticEnabled,
                            onAddCustomTuning = vm::addCustomTuning,
                            onDeleteCustomTuning = vm::deleteCustomTuning,
                            onPreviewPitch = vm::playPreview,
                            heroHidden = !introDone,
                            onHeroBounds = { heroBounds.value = it },
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
                AppScreen.CHORDS -> {
                    ChordScreen(
                        state = chordState,
                        onSelectRoot = chordVm::selectRoot,
                        onSelectType = chordVm::selectType,
                        onToggleLeftHanded = chordVm::toggleLeftHanded,
                        onStrum = chordVm::strum,
                        onBack = { currentScreen = AppScreen.TUNER },
                    )
                }
            }
            }
        }
    }
    if (!introDone) IntroOverlay(heroBounds = heroBounds, onFinished = { introDone = true })
    }
}

@Composable
private fun ChordNavIcon(selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        for (i in 0..2) {
            val x = w * (0.22f + i * 0.28f)
            drawLine(color, Offset(x, 2.dp.toPx()), Offset(x, h - 2.dp.toPx()), strokeWidth = stroke)
        }
        for (j in 0..2) {
            val y = h * (0.22f + j * 0.28f)
            drawLine(color, Offset(w * 0.22f, y), Offset(w * 0.78f, y), strokeWidth = stroke)
        }
        drawCircle(color, radius = 2.4.dp.toPx(), center = Offset(w * 0.22f, h * 0.36f))
        drawCircle(color, radius = 2.4.dp.toPx(), center = Offset(w * 0.50f, h * 0.64f))
    }
}

@Composable
private fun TunerScreen(
    state: TunerUiState,
    onString: (Int) -> Unit,
    onTuning: (Tuning) -> Unit,
    onA4: (Float) -> Unit,
    onLeftHanded: (Boolean) -> Unit,
    onDisplayMode: (TunerDisplayMode) -> Unit,
    onNoiseGateDb: (Float) -> Unit,
    onInTuneChime: (Boolean) -> Unit,
    onInTuneHaptic: (Boolean) -> Unit,
    onAddCustomTuning: (String, List<Int>) -> Tuning?,
    onDeleteCustomTuning: (String) -> Unit,
    onPreviewPitch: (Int) -> Unit,
    heroHidden: Boolean,
    onHeroBounds: (Rect) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }
    var tuningToDelete by remember { mutableStateOf<Tuning?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.affirmationTrigger) {
        if (state.affirmationTrigger > 0L && state.inTuneHapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
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

            AudioInputMeter(
                levelNormalized = state.inputLevelNormalized,
                db = state.inputLevelDb,
                aboveGate = state.inputAboveGate,
                gateDb = state.noiseGateDb,
            )

            NoteHero(state, heroHidden, onHeroBounds, Modifier.weight(1f).fillMaxWidth())
            Status(state)
            if (state.displayMode == TunerDisplayMode.STROBE) {
                StrobeMeter(state.cents, state.hasSignal, state.inTune)
            } else {
                CentsMeter(state.cents, state.hasSignal, state.inTune)
            }
            StringRow(state.tuning, state.activeString, state.lockedString, state.leftHanded, onString)

            TuningPicker(
                tuning = state.tuning,
                customTunings = state.customTunings,
                onTuning = onTuning,
                onNewCustom = { showCustomSheet = true },
                onDeleteRequest = { tuningToDelete = it },
            )
            Spacer(Modifier.height(4.dp))
        }

    if (showSettings) {
        SettingsSheet(
            a4 = state.a4,
            leftHanded = state.leftHanded,
            displayMode = state.displayMode,
            noiseGateDb = state.noiseGateDb,
            inTuneChimeEnabled = state.inTuneChimeEnabled,
            inTuneHapticEnabled = state.inTuneHapticEnabled,
            onA4 = onA4,
            onLeftHanded = onLeftHanded,
            onDisplayMode = onDisplayMode,
            onNoiseGateDb = onNoiseGateDb,
            onInTuneChime = onInTuneChime,
            onInTuneHaptic = onInTuneHaptic,
            onDismiss = { showSettings = false },
        )
    }

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

/** Split out of TunerScreen so the chips don't recompose on every audio frame. */
@Composable
private fun TuningPicker(
    tuning: Tuning,
    customTunings: List<Tuning>,
    onTuning: (Tuning) -> Unit,
    onNewCustom: () -> Unit,
    onDeleteRequest: (Tuning) -> Unit,
) {
    var selectedInstrument by remember(tuning.id) {
        mutableStateOf(tuning.instrument)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Tunings.instruments) { inst ->
                FilterChip(
                    selected = inst == selectedInstrument,
                    onClick = {
                        selectedInstrument = inst
                        if (inst == "Custom") {
                            if (customTunings.isNotEmpty() && tuning.instrument != "Custom") {
                                onTuning(customTunings.first())
                            }
                        } else {
                            val tunings = Tunings.forInstrument(inst, customTunings)
                            if (tunings.isNotEmpty() && tunings.none { it.id == tuning.id }) {
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
                        onClick = { onNewCustom() },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = "New custom tuning", modifier = Modifier.size(16.dp)) },
                        label = { Text("New Tuning") },
                    )
                }
                items(customTunings) { t ->
                    FilterChip(
                        selected = t.id == tuning.id,
                        onClick = { onTuning(t) },
                        label = { Text(t.name) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onDeleteRequest(t) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Delete ${t.name}", modifier = Modifier.size(14.dp))
                            }
                        },
                    )
                }
            }
        } else {
            val currentTunings = remember(selectedInstrument, customTunings) {
                Tunings.forInstrument(selectedInstrument, customTunings)
            }
            if (currentTunings.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentTunings) { t ->
                        FilterChip(
                            selected = t.id == tuning.id,
                            onClick = { onTuning(t) },
                            label = { Text(t.name) },
                        )
                    }
                }
            }
        }
    }
}

// ---- Hero: a Material shape that morphs idle -> listening -> in tune -------------------------------------------

/** Draws a [Morph] (from graphics-shapes) as a Compose shape. Polygons must be normalized to 0..1. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    // Equal shapes let Compose skip re-clipping on frames where the morph hasn't moved.
    override fun equals(other: Any?) = other is MorphShape && other.morph === morph && other.progress == progress
    override fun hashCode() = 31 * System.identityHashCode(morph) + progress.hashCode()

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

/** Built once per process; rebuilding a Morph on every visit to the tuner tab is wasted work. */
internal object HeroMorphs {
    val idleToListening: Morph by lazy {
        Morph(MaterialShapes.Cookie9Sided.normalized(), MaterialShapes.Cookie12Sided.normalized())
    }
    val listeningToTuned: Morph by lazy {
        Morph(MaterialShapes.Cookie12Sided.normalized(), MaterialShapes.Sunny.normalized())
    }
}

@Composable
private fun NoteHero(
    s: TunerUiState,
    heroHidden: Boolean,
    onBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val idleToListening = HeroMorphs.idleToListening
    val listeningToTuned = HeroMorphs.listeningToTuned

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
    // Starts turning only once the intro hands over, from 0 degrees, so the intro's shape and this one line up exactly.
    val spin = remember { Animatable(0f) }
    LaunchedEffect(heroHidden) {
        if (!heroHidden) spin.animateTo(360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)))
    }
    val shape = if (stage <= 1f) MorphShape(idleToListening, stage) else MorphShape(listeningToTuned, stage - 1f)
    val locked = s.lockedString?.let { Note(s.tuning.strings[it]).label }

    BoxWithConstraints(
        modifier.graphicsLayer { alpha = if (heroHidden) 0f else 1f }, // hidden while the intro's own copy of the shape is on screen
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(minOf(maxWidth, maxHeight))
                .onGloballyPositioned { onBounds(it.boundsInRoot()) },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin.value }.clip(shape).background(container))
            if (s.inTuneHoldProgress > 0f) {
                val isLocked = s.inTuneHoldProgress >= 1f
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .testTag("in_tune_lock_indicator"),
                ) {
                    val strokeW = 4.dp.toPx()
                    val diameter = size.minDimension - strokeW
                    val sweep = s.inTuneHoldProgress * 360f
                    drawArc(
                        color = if (isLocked) cs.tertiary else cs.primary,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2),
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    )
                }
            }
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
        !s.hasSignal && !s.inputAboveGate && s.inputLevelDb > -70f -> "Signal below noise gate"
        !s.hasSignal -> "Listening…"
        s.inTuneHoldProgress >= 1f -> "In tune · Locked ✓"
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

// ---- Strobe meter ----------------------------------------------------------------------------------------------

@Composable
private fun StrobeMeter(cents: Float, active: Boolean, inTune: Boolean) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current.density
    val currentSpeed by rememberUpdatedState(
        StrobeMath.calculateDriftSpeed(cents = cents, hasSignal = active, inTune = inTune),
    )

    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos > 0L) {
                    val dt = ((frameNanos - lastNanos).coerceIn(0L, 100_000_000L)) / 1_000_000_000f
                    val speed = currentSpeed
                    if (speed != 0f) {
                        phase += speed * dt
                    }
                }
                lastNanos = frameNanos
            }
        }
    }

    val inTuneGlow by animateColorAsState(
        targetValue = if (active && inTune) cs.tertiary else if (active) cs.primary else cs.outlineVariant,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "strobeGlow",
    )

    Column(Modifier.testTag("strobe_meter")) {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val totalW = size.width
            val totalH = size.height
            val barH = 40.dp.toPx()
            val top = (totalH - barH) / 2f
            val cornerRadius = CornerRadius(barH / 2f)

            // Outer bezel
            drawRoundRect(
                color = cs.surfaceContainerHighest,
                topLeft = Offset(0f, top),
                size = Size(totalW, barH),
                cornerRadius = cornerRadius,
            )

            // Clip within the rounded track
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = top,
                        right = totalW,
                        bottom = top + barH,
                        cornerRadius = cornerRadius,
                    ),
                )
            }

            clipPath(path) {
                val halfBarH = barH / 2f
                val period1 = 36.dp.toPx()
                val period2 = 18.dp.toPx()

                val phasePx1 = StrobeMath.wrapPhase(phase * density, period1)
                val phasePx2 = StrobeMath.wrapPhase(phase * density * 1.5f, period2)

                val barColor = if (!active) {
                    cs.onSurfaceVariant.copy(alpha = 0.18f)
                } else if (inTune) {
                    cs.tertiary.copy(alpha = 0.90f)
                } else {
                    cs.primary.copy(alpha = 0.85f)
                }

                // Top track (Fundamental pitch band)
                val startX1 = -period1 * 2 + phasePx1
                var x1 = startX1
                val blockW1 = period1 / 2f
                while (x1 < totalW + period1) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x1, top),
                        size = Size(blockW1, halfBarH),
                    )
                    x1 += period1
                }

                // Track divider line
                drawLine(
                    color = cs.surfaceContainerLowest.copy(alpha = 0.5f),
                    start = Offset(0f, top + halfBarH),
                    end = Offset(totalW, top + halfBarH),
                    strokeWidth = 1.dp.toPx(),
                )

                // Bottom track (2nd harmonic octave band)
                val startX2 = -period2 * 2 + phasePx2
                var x2 = startX2
                val blockW2 = period2 / 2f
                while (x2 < totalW + period2) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x2, top + halfBarH),
                        size = Size(blockW2, halfBarH),
                    )
                    x2 += period2
                }

                // When in tune: Draw central emerald highlight glow behind the reticle
                if (active && inTune) {
                    val glowW = 48.dp.toPx()
                    drawRect(
                        color = cs.tertiary.copy(alpha = 0.30f),
                        topLeft = Offset(totalW / 2f - glowW / 2f, top),
                        size = Size(glowW, barH),
                    )
                }
            }

            // Central reticle (Center target line & pointer notches)
            val centerX = totalW / 2f
            val reticleColor = if (!active) cs.outlineVariant else inTuneGlow
            val reticleWidth = if (active && inTune) 3.dp.toPx() else 2.dp.toPx()

            // Vertical crosshair
            drawLine(
                color = reticleColor,
                start = Offset(centerX, top - 3.dp.toPx()),
                end = Offset(centerX, top + barH + 3.dp.toPx()),
                strokeWidth = reticleWidth,
            )

            // Top pointer triangle
            val notchSize = 6.dp.toPx()
            val topTriangle = Path().apply {
                moveTo(centerX - notchSize, top - 6.dp.toPx())
                lineTo(centerX + notchSize, top - 6.dp.toPx())
                lineTo(centerX, top + 1.dp.toPx())
                close()
            }
            drawPath(topTriangle, color = reticleColor)

            // Bottom pointer triangle
            val bottomTriangle = Path().apply {
                moveTo(centerX - notchSize, top + barH + 6.dp.toPx())
                lineTo(centerX + notchSize, top + barH + 6.dp.toPx())
                lineTo(centerX, top + barH - 1.dp.toPx())
                close()
            }
            drawPath(bottomTriangle, color = reticleColor)
        }

        // Subtitle indicators below strobe band
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "◄ ♭ FLAT",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
            Text(
                when {
                    !active -> "STROBE STANDBY"
                    inTune -> "● IN TUNE · LOCKED"
                    cents < 0 -> "DRIFTING LEFT"
                    else -> "DRIFTING RIGHT"
                },
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = if (active && inTune) cs.tertiary else cs.onSurfaceVariant,
            )
            Text(
                "SHARP ♯ ►",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
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
    Column(Modifier.testTag("cents_meter")) {
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
private fun StringRow(
    tuning: Tuning,
    activeString: Int?,
    lockedString: Int?,
    leftHanded: Boolean,
    onString: (Int) -> Unit,
) {
    val hint = when {
        tuning.isChromatic -> "Chromatic — play any note"
        lockedString == null -> "Auto-detect · tap a string to lock it and hear it"
        else -> "Locked to ${Note(tuning.strings[lockedString]).label} · tap again to release"
    }
    val count = tuning.strings.size
    val spacing = if (count > 6) 4.dp else 6.dp
    val textStyle = if (count > 6) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!tuning.isChromatic) {
            val indices = if (leftHanded) tuning.strings.indices.reversed() else tuning.strings.indices
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                indices.forEach { i ->
                    val midi = tuning.strings[i]
                    ToggleButton(
                        checked = activeString == i,
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

// ---- Audio Input Meter -----------------------------------------------------------------------------------------

@Composable
private fun AudioInputMeter(
    levelNormalized: Float,
    db: Float,
    aboveGate: Boolean,
    gateDb: Float,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val animatedLevel by animateFloatAsState(
        targetValue = levelNormalized,
        animationSpec = tween(60),
        label = "vu_level",
    )
    val gatePos = remember(gateDb) {
        AudioSignalMath.normalizeDb(gateDb)
    }

    Row(
        modifier = modifier
            .testTag("audio_input_meter")
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(cs.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "MIC",
            style = MaterialTheme.typography.labelSmall,
            color = if (aboveGate) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.6f),
        )

        Canvas(
            Modifier
                .weight(1f)
                .height(8.dp)
        ) {
            val barW = size.width
            val barH = size.height
            val radius = CornerRadius(barH / 2, barH / 2)

            // Background track
            drawRoundRect(
                color = cs.outlineVariant.copy(alpha = 0.4f),
                size = Size(barW, barH),
                cornerRadius = radius,
            )

            // Active level fill
            val fillW = (barW * animatedLevel).coerceIn(0f, barW)
            if (fillW > 0f) {
                drawRoundRect(
                    color = if (aboveGate) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.45f),
                    size = Size(fillW, barH),
                    cornerRadius = radius,
                )
            }

            // Noise gate threshold tick marker
            val gateX = (barW * gatePos).coerceIn(0f, barW)
            drawLine(
                color = if (aboveGate) cs.tertiary else cs.error,
                start = Offset(gateX, -2.dp.toPx()),
                end = Offset(gateX, barH + 2.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
            )
        }

        Text(
            if (db <= -70f) "— dB" else "%.0f dB".format(Locale.US, db),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End,
        )
    }
}

// ---- Settings --------------------------------------------------------------------------------------------------

@Composable
private fun SettingsSheet(
    a4: Float,
    leftHanded: Boolean,
    displayMode: TunerDisplayMode,
    noiseGateDb: Float,
    inTuneChimeEnabled: Boolean,
    inTuneHapticEnabled: Boolean,
    onA4: (Float) -> Unit,
    onLeftHanded: (Boolean) -> Unit,
    onDisplayMode: (TunerDisplayMode) -> Unit,
    onNoiseGateDb: (Float) -> Unit,
    onInTuneChime: (Boolean) -> Unit,
    onInTuneHaptic: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMediumEmphasized)

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Strobe tuner display", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Replaces the cents needle with an animated stroboscopic drift band",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = displayMode == TunerDisplayMode.STROBE,
                    onCheckedChange = { checked ->
                        onDisplayMode(if (checked) TunerDisplayMode.STROBE else TunerDisplayMode.NEEDLE)
                    },
                    modifier = Modifier.testTag("strobe_display_switch"),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("In-tune audio chime", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Plays a crystalline bell ping when the string locks in tune",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = inTuneChimeEnabled,
                    onCheckedChange = onInTuneChime,
                    modifier = Modifier.testTag("in_tune_chime_switch"),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("In-tune haptic vibration", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Vibrates device when pitch is held in-tune",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = inTuneHapticEnabled,
                    onCheckedChange = onInTuneHaptic,
                    modifier = Modifier.testTag("in_tune_haptic_switch"),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Noise gate threshold", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Rejects background noise below threshold to prevent false pitch detection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Slider(
                        state = rememberSliderState(noiseGateDb, 35, trackRange = -60f..-24f),
                        onValueChange = { onNoiseGateDb(it.roundToInt().toFloat()) },
                        modifier = Modifier.weight(1f).testTag("noise_gate_slider"),
                    )
                    Text(
                        "%.0f dB".format(Locale.US, noiseGateDb),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = noiseGateDb.roundToInt() == AudioSignalMath.GATE_PRESET_QUIET.roundToInt(),
                        onClick = { onNoiseGateDb(AudioSignalMath.GATE_PRESET_QUIET) },
                        label = { Text("Quiet Room") },
                    )
                    FilterChip(
                        selected = noiseGateDb.roundToInt() == AudioSignalMath.GATE_PRESET_NORMAL.roundToInt(),
                        onClick = { onNoiseGateDb(AudioSignalMath.GATE_PRESET_NORMAL) },
                        label = { Text("Normal") },
                    )
                    FilterChip(
                        selected = noiseGateDb.roundToInt() == AudioSignalMath.GATE_PRESET_REHEARSAL.roundToInt(),
                        onClick = { onNoiseGateDb(AudioSignalMath.GATE_PRESET_REHEARSAL) },
                        label = { Text("Rehearsal") },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                    state = rememberSliderState(a4, 50, trackRange = 415f..466f),
                    onValueChange = { onA4(it.roundToInt().toFloat()) },
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
