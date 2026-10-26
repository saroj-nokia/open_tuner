@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.opentuner.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val INTRO_MS = 1600

/**
 * Cold-start intro, drawn entirely in code (no video, no image files).
 *
 * A tuning peg turns, the wobbling string settles straight, a ring pulses, and the headstock gives way to a
 * Material shape. That shape then grows into the tuner's note circle at [heroBounds], so the intro never "ends":
 * it becomes the tuner. Tap anywhere to skip straight to that hand-over.
 */
@Composable
fun IntroOverlay(
    heroBounds: State<Rect?>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val settleSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val t = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }
    var finishing by remember { mutableStateOf(false) }
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(INTRO_MS, easing = LinearEasing))
        finishing = true
    }
    LaunchedEffect(finishing) {
        if (finishing) {
            settle.animateTo(1f, settleSpec)
            finished()
        }
    }

    val palette = ArtPalette(
        headstock = cs.secondaryContainer,
        neck = cs.surfaceContainerHighest,
        nut = cs.onSurfaceVariant,
        string = cs.outline,
        tunedString = cs.primary,
        peg = cs.onSecondaryContainer,
        knob = cs.tertiary,
        ring = cs.tertiary,
    )

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { finishing = true } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val tt = t.value
            val raw = settle.value // may overshoot 1 a little: that is the expressive spring
            val sp = raw.coerceIn(0f, 1f)

            drawRect(cs.surface, alpha = 1f - smooth(sp, 0.3f, 1f))

            val artAlpha = smooth(tt, 0f, 0.1f) * (1f - smooth(tt, 0.7f, 0.88f)) * (1f - smooth(sp, 0f, 0.35f))
            if (artAlpha > 0f) drawGuitar(tt, artAlpha, palette)

            // The hand-over shape: grows at the centre, then travels to the tuner's note circle.
            val appear = smooth(tt, 0.78f, 0.95f)
            val hero = heroBounds.value
            val startSize = size.minDimension * 0.5f
            val startCenter = Offset(size.width / 2f, size.height / 2f)
            val grown = startSize * (0.15f + 0.85f * appear)
            val targetSize = hero?.width ?: grown
            val targetCenter = hero?.center ?: startCenter
            val shapeSize = grown + (targetSize - grown) * raw
            val center = Offset(
                startCenter.x + (targetCenter.x - startCenter.x) * raw,
                startCenter.y + (targetCenter.y - startCenter.y) * raw,
            )
            val shapeAlpha = if (hero == null) appear * (1f - sp) else maxOf(appear, sp)
            if (shapeAlpha > 0f && shapeSize > 0f) {
                // Cookie12 -> Cookie9: the same shape the tuner's note circle starts as.
                val path = HeroMorphs.idleToListening.toPath(1f - sp)
                withTransform({
                    translate(center.x - shapeSize / 2f, center.y - shapeSize / 2f)
                    scale(shapeSize, shapeSize, Offset.Zero)
                }) {
                    drawPath(path, lerp(cs.primaryContainer, cs.surfaceContainerHigh, sp), alpha = shapeAlpha)
                }
            }
        }
        Text(
            "Open Tuner",
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = cs.onSurface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .graphicsLayer {
                    val tt = t.value
                    alpha = smooth(tt, 0.05f, 0.2f) * (1f - smooth(tt, 0.7f, 0.85f)) * (1f - settle.value.coerceIn(0f, 1f))
                },
        )
    }
}

private class ArtPalette(
    val headstock: Color,
    val neck: Color,
    val nut: Color,
    val string: Color,
    val tunedString: Color,
    val peg: Color,
    val knob: Color,
    val ring: Color,
)

private fun smooth(x: Float, from: Float, to: Float): Float {
    val k = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

/** [progress] 0 = first shape of the morph, 1 = second. The result lives in 0..1 space (shapes are normalized). */
private fun Morph.toPath(progress: Float): Path {
    val cubics = asCubics(progress)
    return Path().apply {
        moveTo(cubics.first().anchor0X, cubics.first().anchor0Y)
        for (c in cubics) cubicTo(c.control0X, c.control0Y, c.control1X, c.control1Y, c.anchor1X, c.anchor1Y)
        close()
    }
}

/** A flat headstock: one peg turns twice while its wobbling string settles straight, then a ring pulses. */
private fun DrawScope.drawGuitar(t: Float, alpha: Float, p: ArtPalette) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val top = cy - s * 0.36f
    val headW = s * 0.30f
    val headH = s * 0.44f
    val nutY = top + headH
    val neckBottom = cy + s * 0.46f
    val pop = 0.92f + 0.08f * smooth(t, 0f, 0.12f)

    // 0 -> 1 while the peg turns; the string's wobble is scaled by (1 - turn).
    val turn = smooth(t, 0.08f, 0.6f)

    scale(pop, pop, Offset(cx, cy)) {
        drawRect(p.neck, Offset(cx - s * 0.10f, nutY), Size(s * 0.20f, neckBottom - nutY), alpha = alpha)
        drawRoundRect(
            p.headstock, Offset(cx - headW / 2f, top), Size(headW, headH),
            cornerRadius = CornerRadius(s * 0.09f), alpha = alpha,
        )
        drawRect(p.nut, Offset(cx - s * 0.10f, nutY - s * 0.012f), Size(s * 0.20f, s * 0.02f), alpha = alpha)

        val y0 = top + s * 0.06f
        for (i in 0..5) {
            val x0 = cx + (i - 2.5f) * s * 0.032f
            if (i != 1) {
                drawLine(p.string, Offset(x0, y0), Offset(x0, neckBottom), strokeWidth = s * 0.006f, alpha = alpha)
                continue
            }
            val amp = s * 0.035f * (1f - turn)
            val phase = t * 70f
            val path = Path()
            val steps = 48
            for (k in 0..steps) {
                val f = k / steps.toFloat()
                val x = x0 + amp * sin(3f * PI.toFloat() * f) * cos(phase)
                val y = y0 + (neckBottom - y0) * f
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, p.tunedString, alpha = alpha, style = Stroke(width = s * 0.009f, cap = StrokeCap.Round))
        }

        for (side in 0..1) {
            for (j in 0..2) {
                val px = cx + (if (side == 0) -1f else 1f) * (headW / 2f + s * 0.03f)
                val py = top + s * 0.09f + j * s * 0.12f
                drawCircle(p.peg, s * 0.024f, Offset(px, py), alpha = alpha)
                val angle = if (side == 0 && j == 0) 720f * turn else 25f * (side * 3 + j)
                rotate(angle, Offset(px, py)) {
                    drawRoundRect(
                        p.knob, Offset(px - s * 0.03f, py - s * 0.011f), Size(s * 0.06f, s * 0.022f),
                        cornerRadius = CornerRadius(s * 0.011f), alpha = alpha,
                    )
                }
            }
        }

        val pulse = ((t - 0.6f) / 0.18f).coerceIn(0f, 1f)
        if (pulse in 0.001f..0.999f) {
            drawCircle(
                p.ring, s * (0.10f + 0.30f * pulse), Offset(cx, nutY),
                alpha = alpha * (1f - pulse), style = Stroke(width = s * 0.008f),
            )
        }
    }
}
