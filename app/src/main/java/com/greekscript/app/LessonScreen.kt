package com.greekscript.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Lets the active exercise tell the lesson screen what the Check button should do. */
@Stable
class Controller {
    var canCheck by mutableStateOf(false)
    var locked by mutableStateOf(false)
    var checkLabel by mutableStateOf("Check")
    var skipFeedback by mutableStateOf(false)
    var check: () -> Boolean = { true }
    var answerText: String = ""
    var mistake: () -> Unit = {}
}

@Composable
fun LessonScreen(
    lesson: Lesson,
    exercises: List<Exercise>,
    speaker: Speaker,
    onFinished: (xp: Int, perfect: Boolean) -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    val queue = remember { mutableStateListOf<Exercise>().apply { addAll(exercises) } }
    val retried = remember { HashSet<Exercise>() }
    var index by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var correct by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var showExit by remember { mutableStateOf(false) }
    var outOfHearts by remember { mutableStateOf(false) }

    BackHandler { showExit = true }

    if (outOfHearts) {
        OutOfHearts(onRetry = onRetry, onExit = onExit)
    } else if (index >= queue.size) {
        LaunchedEffect(Unit) { onFinished(10 + correct * 2, mistakes == 0) }
        Box(Modifier.fillMaxSize().background(Pal.Marble))
    } else {
        val current = queue[index]
        val ctrl = remember(index) { Controller() }
        var result by remember(index) { mutableStateOf<Boolean?>(null) }
        ctrl.mistake = {
            hearts -= 1
            mistakes += 1
            if (hearts <= 0) outOfHearts = true
        }

        Column(Modifier.fillMaxSize().background(Pal.Marble)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✕",
                    color = Pal.Muted,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showExit = true }
                        .padding(10.dp),
                )
                Spacer(Modifier.width(6.dp))
                ProgressBar(
                    fraction = index.toFloat() / queue.size.toFloat(),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "♥ $hearts",
                    color = Pal.Coral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (current) {
                    is Exercise.Intro -> IntroView(current.glyph, ctrl, speaker)
                    is Exercise.Pick -> PickView(current, ctrl, speaker)
                    is Exercise.Match -> MatchView(current, ctrl)
                    is Exercise.Build -> BuildView(current, ctrl)
                }
            }

            val res = result
            val tint = when (res) {
                true -> Pal.OliveTint
                false -> Pal.CoralTint
                null -> Pal.Marble
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(tint)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (res != null) {
                    Text(
                        text = if (res) "Correct!" else "Not quite",
                        color = if (res) Pal.OliveDark else Pal.CoralDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!res) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = ctrl.answerText,
                            color = Pal.CoralDark,
                            fontSize = 18.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ChunkyButton(
                        text = "Continue",
                        face = if (res) Pal.Olive else Pal.Coral,
                        edge = if (res) Pal.OliveDark else Pal.CoralDark,
                        onClick = {
                            if (hearts <= 0) outOfHearts = true else index += 1
                        },
                    )
                } else {
                    ChunkyButton(
                        text = ctrl.checkLabel,
                        enabled = ctrl.canCheck,
                        onClick = {
                            if (ctrl.skipFeedback) {
                                index += 1
                            } else {
                                val ok = ctrl.check()
                                ctrl.locked = true
                                result = ok
                                if (ok) {
                                    correct += 1
                                } else {
                                    hearts -= 1
                                    mistakes += 1
                                    if (retried.add(current)) queue.add(current)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text("Leave this lesson?") },
            text = { Text("Your progress in this lesson will be lost.") },
            confirmButton = { TextButton(onClick = onExit) { Text("Leave", color = Pal.Coral) } },
            dismissButton = { TextButton(onClick = { showExit = false }) { Text("Keep going") } },
        )
    }
}

@Composable
private fun OutOfHearts(onRetry: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Pal.Marble).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("♥", fontSize = 72.sp, color = Pal.Coral)
        Spacer(Modifier.height(12.dp))
        Text("Out of hearts", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Pal.Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No problem. New letters take a few tries to stick. Start this lesson again.",
            color = Pal.Muted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        ChunkyButton(text = "Try again", onClick = onRetry)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onExit) {
            Text("Back to lessons", color = Pal.Muted, fontSize = 16.sp)
        }
    }
}

// ------------------------------------------------------------------ views

@Composable
private fun IntroView(g: Glyph, ctrl: Controller, speaker: Speaker) {
    SideEffect {
        ctrl.canCheck = true
        ctrl.skipFeedback = true
        ctrl.checkLabel = "Got it"
    }
    val shown = if (g.upper == g.lower) g.lower else "${g.upper} ${g.lower}"
    val card = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (g.isPair) "A new letter pair" else "A new letter",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(card)
                .background(Color.White)
                .border(2.dp, Pal.SkyLine, card)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = shown,
                fontFamily = GreekFont,
                fontSize = 84.sp,
                color = Pal.Deep,
                textAlign = TextAlign.Center,
            )
            Text(text = g.name, color = Pal.Muted, fontSize = 18.sp)
            Spacer(Modifier.height(14.dp))
            SpeakerButton(onClick = { speaker.speak(g.say) }, enabled = speaker.ready)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Says “${g.sound}”",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.Ink,
        )
        Text(
            text = "like ${g.example}",
            fontSize = 17.sp,
            color = Pal.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Pal.Sky)
                .padding(16.dp),
        ) {
            Text(text = g.hint, fontSize = 16.sp, color = Pal.Ink)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PickView(ex: Exercise.Pick, ctrl: Controller, speaker: Speaker) {
    var selected by remember(ex) { mutableStateOf<String?>(null) }
    SideEffect {
        ctrl.canCheck = selected != null
        ctrl.check = { selected == ex.answer }
        ctrl.answerText = ex.reveal ?: ex.answer
    }
    LaunchedEffect(ex) {
        val s = ex.speak
        if (ex.autoPlay && s != null) {
            delay(300)
            speaker.speak(s)
        }
    }

    val card = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = ex.prompt,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        val speakText = ex.speak
        if (ex.big.isNotEmpty() || speakText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(card)
                    .background(Color.White)
                    .border(2.dp, Pal.SkyLine, card)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (ex.big.isNotEmpty()) {
                        Text(
                            text = ex.big,
                            fontFamily = if (ex.bigGreek) GreekFont else FontFamily.Default,
                            fontSize = if (ex.bigGreek) greekSizeFor(ex.big).sp else 52.sp,
                            fontWeight = if (ex.bigGreek) FontWeight.Normal else FontWeight.Bold,
                            color = Pal.Deep,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (speakText != null) {
                        SpeakerButton(
                            onClick = { speaker.speak(speakText) },
                            enabled = speaker.ready,
                            size = if (ex.big.isEmpty()) 88.dp else 52.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        val shortOptions = ex.options.all { it.length <= 4 }
        val rows = if (shortOptions) ex.options.chunked(2) else ex.options.map { listOf(it) }
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (opt in row) {
                    OptionCard(
                        text = opt,
                        greek = ex.optionsGreek,
                        selected = selected == opt,
                        locked = ctrl.locked,
                        isAnswer = opt == ex.answer,
                        onClick = { selected = opt },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MatchView(ex: Exercise.Match, ctrl: Controller) {
    val n = ex.pairs.size
    val leftOrder = remember(ex) { (0 until n).shuffled() }
    val rightOrder = remember(ex) { (0 until n).shuffled() }
    val matched = remember(ex) { mutableStateListOf<Int>() }
    var sel by remember(ex) { mutableStateOf<Int?>(null) }
    var flash by remember(ex) { mutableStateOf<Pair<Int, Int>?>(null) }

    SideEffect {
        ctrl.canCheck = matched.size == n
        ctrl.skipFeedback = true
        ctrl.checkLabel = "Continue"
    }
    LaunchedEffect(flash) {
        if (flash != null) {
            delay(500)
            flash = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = ex.prompt,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (i in leftOrder) {
                    val done = i in matched
                    val f = flash
                    val bad = f != null && f.first == i
                    Tile(
                        text = ex.pairs[i].first,
                        greek = ex.leftGreek,
                        fill = when {
                            done -> Pal.OliveTint
                            bad -> Pal.CoralTint
                            sel == i -> Pal.Sky
                            else -> Color.White
                        },
                        outline = when {
                            done -> Pal.Olive
                            bad -> Pal.Coral
                            sel == i -> Pal.Aegean
                            else -> Pal.SkyLine
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !done,
                        onClick = { sel = i },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (j in rightOrder) {
                    val done = j in matched
                    val f = flash
                    val bad = f != null && f.second == j
                    Tile(
                        text = ex.pairs[j].second,
                        greek = ex.rightGreek,
                        fill = when {
                            done -> Pal.OliveTint
                            bad -> Pal.CoralTint
                            else -> Color.White
                        },
                        outline = when {
                            done -> Pal.Olive
                            bad -> Pal.Coral
                            else -> Pal.SkyLine
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !done,
                        onClick = {
                            val s = sel
                            if (s != null) {
                                if (s == j) {
                                    matched.add(j)
                                } else {
                                    flash = Pair(s, j)
                                    ctrl.mistake()
                                }
                                sel = null
                            }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BuildView(ex: Exercise.Build, ctrl: Controller) {
    val chosen = remember(ex) { mutableStateListOf<Int>() }
    val target = ex.tokens.joinToString("")
    SideEffect {
        ctrl.canCheck = chosen.isNotEmpty()
        ctrl.check = { chosen.joinToString("") { ex.tiles[it] } == target }
        ctrl.answerText = target
    }

    val built = chosen.joinToString("") { ex.tiles[it] }
    val verdict = when {
        !ctrl.locked -> 0
        built == target -> 1
        else -> 2
    }
    val box = RoundedCornerShape(18.dp)
    val remaining = ex.tiles.indices.filter { it !in chosen }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = ex.prompt,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .clip(box)
                .background(
                    when (verdict) {
                        1 -> Pal.OliveTint
                        2 -> Pal.CoralTint
                        else -> Color.White
                    }
                )
                .border(
                    2.dp,
                    when (verdict) {
                        1 -> Pal.Olive
                        2 -> Pal.Coral
                        else -> Pal.SkyLine
                    },
                    box,
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (chosen.isEmpty()) {
                Text("Tap the letters below", color = Pal.Muted, fontSize = 16.sp)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (row in chosen.toList().chunked(5)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (idx in row) {
                                Tile(
                                    text = ex.tiles[idx],
                                    greek = true,
                                    fill = Pal.Sky,
                                    outline = Pal.Aegean,
                                    enabled = !ctrl.locked,
                                    onClick = { chosen.remove(idx) },
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (row in remaining.chunked(4)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (idx in row) {
                        Tile(
                            text = ex.tiles[idx],
                            greek = true,
                            fill = Color.White,
                            outline = Pal.SkyLine,
                            enabled = !ctrl.locked,
                            onClick = { chosen.add(idx) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
