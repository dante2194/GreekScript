package com.greekscript.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A button with a solid "lip" underneath that presses down when tapped. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    face: Color = Pal.Aegean,
    edge: Color = Pal.Deep,
    textColor: Color = Color.White,
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift = if (pressed && enabled) 1.dp else 5.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) edge else Pal.LockEdge)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = lift)
                .clip(shape)
                .background(if (enabled) face else Pal.Lock),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) textColor else Pal.Muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Pal.SkyLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(f)
                .clip(RoundedCornerShape(7.dp))
                .background(Pal.Olive),
        )
    }
}

@Composable
fun SpeakerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 52.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) Pal.Sky else Pal.Lock)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("🔊", fontSize = (size.value * 0.42f).sp)
    }
}

/** A running Greek-key (meander) border. */
@Composable
fun MeanderBand(
    modifier: Modifier = Modifier,
    color: Color = Pal.Gold,
    cell: Dp = 14.dp,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(cell)) {
        val s = cell.toPx()
        val w = (s * 0.14f).coerceAtLeast(1.5f)
        val step = s * 1.33f
        val count = (size.width / step).toInt() + 1
        val path = Path()
        path.moveTo(0f, s)
        path.lineTo(size.width, s)
        for (k in 0 until count) {
            val x0 = k * step
            path.moveTo(x0, s)
            path.lineTo(x0, 0f)
            path.lineTo(x0 + s, 0f)
            path.lineTo(x0 + s, s * 0.72f)
            path.lineTo(x0 + s * 0.33f, s * 0.72f)
            path.lineTo(x0 + s * 0.33f, s * 0.34f)
            path.lineTo(x0 + s * 0.66f, s * 0.34f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = w, cap = StrokeCap.Square, join = StrokeJoin.Miter),
        )
    }
}

/** Greek text scales down as words get longer so nothing overflows a phone screen. */
fun greekSizeFor(text: String): Float = when {
    text.length <= 2 -> 88f
    text.length <= 4 -> 64f
    text.length <= 7 -> 48f
    text.length <= 10 -> 38f
    else -> 30f
}

@Composable
fun OptionCard(
    text: String,
    greek: Boolean,
    selected: Boolean,
    locked: Boolean,
    isAnswer: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val bg: Color
    val line: Color
    when {
        locked && isAnswer -> { bg = Pal.OliveTint; line = Pal.Olive }
        locked && selected -> { bg = Pal.CoralTint; line = Pal.Coral }
        selected -> { bg = Pal.Sky; line = Pal.Aegean }
        else -> { bg = Color.White; line = Pal.SkyLine }
    }
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(bg)
            .border(2.dp, line, shape)
            .clickable(enabled = !locked, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Pal.Ink,
            fontFamily = if (greek) GreekFont else FontFamily.Default,
            fontSize = if (greek) 28.sp else 20.sp,
            fontWeight = if (greek) FontWeight.Medium else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

/** Small rounded tile used by the "build the word" and "match" exercises. */
@Composable
fun Tile(
    text: String,
    greek: Boolean,
    fill: Color,
    outline: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(fill)
            .border(2.dp, outline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Pal.Ink,
            fontFamily = if (greek) GreekFont else FontFamily.Default,
            fontSize = if (greek) 26.sp else 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StatPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(2.dp, Pal.SkyLine, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
