package com.greekscript.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class NodeState { Done, Current, Locked }

private val ZIGZAG = listOf(0, 42, 64, 42, 0, -42, -64, -42)

private fun openVoiceSettings(context: Context) {
    val tries = listOf(
        Intent("com.android.settings.TTS_SETTINGS"),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in tries) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // try the next one
        }
    }
}

/** Learn / Alphabet tabs. */
@Composable
fun MainShell(
    state: AppState,
    speaker: Speaker,
    onStartLesson: (Lesson) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Pal.Marble)) {
        Box(Modifier.weight(1f)) {
            if (tab == 0) {
                LearnTab(state, speaker, onStartLesson)
            } else {
                AlphabetTab(state, speaker)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Pal.SkyLine),
        ) {
            BottomTab("Learn", tab == 0, Modifier.weight(1f)) { tab = 0 }
            BottomTab("Alphabet", tab == 1, Modifier.weight(1f)) { tab = 1 }
        }
    }
}

@Composable
private fun BottomTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Pal.Aegean else Pal.Muted,
        )
    }
}

// ------------------------------------------------------------------ learn

@Composable
private fun LearnTab(state: AppState, speaker: Speaker, onStartLesson: (Lesson) -> Unit) {
    val context = LocalContext.current
    val nextLessonId = ALL_LESSONS.firstOrNull { it.id !in state.done }?.id ?: -1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "top") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Γράμμα",
                        fontFamily = GreekFont,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pal.Deep,
                    )
                    Text(text = "Learn to read the Greek alphabet", fontSize = 14.sp, color = Pal.Muted)
                }
                StatPill("🔥 ${state.currentStreak()}", Pal.GoldDeepText)
                Spacer(Modifier.width(8.dp))
                StatPill("${state.xp} XP", Pal.Aegean)
            }
        }

        if (!speaker.ready) {
            item(key = "voice") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Pal.Sky)
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "No Greek voice found yet. Letter sounds and listening exercises need one.",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = Pal.Ink,
                    )
                    TextButton(onClick = { openVoiceSettings(context) }) {
                        Text("Set up", color = Pal.Aegean, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        for (section in SECTIONS) {
            item(key = "section-${section.index}") {
                SectionHeader(section, state)
            }
            items(section.lessons, key = { it.id }) { lesson ->
                val nodeState = when {
                    lesson.id in state.done -> NodeState.Done
                    lesson.id == nextLessonId -> NodeState.Current
                    else -> NodeState.Locked
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.offset(x = ZIGZAG[lesson.id % ZIGZAG.size].dp)) {
                        LessonNode(
                            label = nodeLabel(lesson),
                            state = nodeState,
                            onClick = { onStartLesson(lesson) },
                        )
                    }
                    Text(
                        text = lesson.title,
                        modifier = Modifier
                            .offset(x = ZIGZAG[lesson.id % ZIGZAG.size].dp)
                            .padding(top = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (nodeState == NodeState.Locked) Pal.Muted else Pal.Ink,
                        fontFamily = if (lesson.kind == LessonKind.LETTERS) GreekFont else FontFamily.Default,
                    )
                }
            }
        }
    }
}

private fun nodeLabel(lesson: Lesson): String {
    if (lesson.kind != LessonKind.LETTERS) return "★"
    val g = GLYPH_BY_KEY[lesson.glyphs.first()] ?: return "★"
    return g.upper
}

@Composable
private fun SectionHeader(section: Section, state: AppState) {
    val finished = section.lessons.count { it.id in state.done }
    val letters = section.lessons
        .flatMap { it.glyphs }
        .mapNotNull { GLYPH_BY_KEY[it] }
        .joinToString("  ") { if (it.upper == it.lower) it.lower else "${it.upper}${it.lower}" }
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp)
            .clip(shape)
            .background(Pal.Deep)
            .padding(18.dp),
    ) {
        Text(
            text = section.title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = section.subtitle, color = Pal.SkyLine, fontSize = 14.sp)
        if (letters.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = letters,
                color = Color.White,
                fontFamily = GreekFont,
                fontSize = 20.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        MeanderBand(color = Pal.Gold, cell = 12.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$finished of ${section.lessons.size} lessons done",
            color = Pal.SkyLine,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun LessonNode(label: String, state: NodeState, onClick: () -> Unit) {
    val face = when (state) {
        NodeState.Done -> Pal.Olive
        NodeState.Current -> Pal.Aegean
        NodeState.Locked -> Pal.Lock
    }
    val edge = when (state) {
        NodeState.Done -> Pal.OliveDark
        NodeState.Current -> Pal.Deep
        NodeState.Locked -> Pal.LockEdge
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 86.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = state != NodeState.Locked,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(78.dp)
                .clip(CircleShape)
                .background(edge),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(78.dp)
                .clip(CircleShape)
                .background(face)
                .then(
                    if (state == NodeState.Current) {
                        Modifier.border(4.dp, Pal.Gold, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (state == NodeState.Done) "✓" else label,
                color = if (state == NodeState.Locked) Pal.Muted else Color.White,
                fontFamily = GreekFont,
                fontWeight = FontWeight.Bold,
                fontSize = if (label.length > 1) 26.sp else 34.sp,
            )
        }
    }
}

// ------------------------------------------------------------------ alphabet

@Composable
private fun AlphabetTab(state: AppState, speaker: Speaker) {
    val learned = state.learnedKeys()
    val singles = GLYPHS.filter { !it.isPair }
    val pairs = GLYPHS.filter { it.isPair }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "Alphabet",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Deep,
                )
                Text(
                    text = "Tap a row to hear it. Letters you haven't met yet are faded.",
                    fontSize = 14.sp,
                    color = Pal.Muted,
                )
            }
        }
        items(singles, key = { "s-" + it.key }) { g ->
            GlyphRow(g, g.key in learned, speaker)
        }
        item(key = "pairs-title") {
            Text(
                text = "Letter pairs",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.Deep,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        items(pairs, key = { "p-" + it.key }) { g ->
            GlyphRow(g, g.key in learned, speaker)
        }
    }
}

@Composable
private fun GlyphRow(g: Glyph, learned: Boolean, speaker: Speaker) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (learned) 1f else 0.45f)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, Pal.SkyLine, shape)
            .clickable { speaker.speak(g.say) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (g.upper == g.lower) g.lower else "${g.upper} ${g.lower}",
            fontFamily = GreekFont,
            fontSize = 34.sp,
            color = Pal.Deep,
            modifier = Modifier.width(110.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(text = g.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
            Text(text = "says “${g.sound}”", fontSize = 14.sp, color = Pal.Muted)
        }
        Text("🔊", fontSize = 18.sp)
    }
}

// ------------------------------------------------------------------ result

@Composable
fun ResultScreen(
    lesson: Lesson,
    xp: Int,
    perfect: Boolean,
    streak: Int,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Pal.Marble)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MeanderBand(color = Pal.Gold, cell = 16.dp, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Lesson complete",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.Deep,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (perfect) "No mistakes. Perfect." else lesson.title,
            fontSize = 18.sp,
            color = Pal.Muted,
            fontFamily = if (perfect) FontFamily.Default else GreekFont,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultStat("+$xp", "XP earned", Pal.Aegean)
            ResultStat("$streak", "day streak", Pal.GoldDeepText)
        }
        Spacer(Modifier.height(36.dp))
        ChunkyButton(text = "Continue", onClick = onContinue)
        Spacer(Modifier.height(28.dp))
        MeanderBand(color = Pal.Gold, cell = 16.dp, modifier = Modifier.padding(horizontal = 32.dp))
    }
}

@Composable
private fun ResultStat(value: String, label: String, color: Color) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(Color.White)
            .border(2.dp, Pal.SkyLine, shape)
            .padding(horizontal = 26.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 13.sp, color = Pal.Muted)
    }
}
