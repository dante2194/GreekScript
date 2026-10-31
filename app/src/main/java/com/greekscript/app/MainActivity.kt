package com.greekscript.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var speaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = Speaker(this)
        val state = AppState(getSharedPreferences("gramma", MODE_PRIVATE))
        setContent {
            GreekTheme {
                App(state, speaker)
            }
        }
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}

private sealed interface Screen {
    data object Main : Screen
    data class InLesson(val lesson: Lesson, val attempt: Int) : Screen
    data class Finished(
        val lesson: Lesson,
        val xp: Int,
        val perfect: Boolean,
        val streak: Int,
    ) : Screen
}

@Composable
private fun App(state: AppState, speaker: Speaker) {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }

    when (val s = screen) {
        is Screen.Main -> MainShell(
            state = state,
            speaker = speaker,
            onStartLesson = { lesson -> screen = Screen.InLesson(lesson, 0) },
        )

        is Screen.InLesson -> {
            val exercises = remember(s) {
                LessonBuilder.build(s.lesson, speaker.ready, Random.Default)
            }
            LessonScreen(
                lesson = s.lesson,
                exercises = exercises,
                speaker = speaker,
                onFinished = { xp, perfect ->
                    state.completeLesson(s.lesson, xp)
                    screen = Screen.Finished(s.lesson, xp, perfect, state.currentStreak())
                },
                onRetry = { screen = Screen.InLesson(s.lesson, s.attempt + 1) },
                onExit = { screen = Screen.Main },
            )
        }

        is Screen.Finished -> ResultScreen(
            lesson = s.lesson,
            xp = s.xp,
            perfect = s.perfect,
            streak = s.streak,
            onContinue = { screen = Screen.Main },
        )
    }
}
