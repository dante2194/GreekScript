package com.greekscript.app

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.util.Locale

/** Saved progress: finished lessons, XP and the daily streak. */
class AppState(private val prefs: SharedPreferences) {

    var xp by mutableIntStateOf(prefs.getInt("xp", 0))
        private set

    private var streak by mutableIntStateOf(prefs.getInt("streak", 0))
    private var lastDay by mutableStateOf(prefs.getString("lastDay", "") ?: "")

    var done by mutableStateOf(
        (prefs.getStringSet("done", emptySet()) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    )
        private set

    /** The streak as it stands today: it lapses if a whole day was skipped. */
    fun currentStreak(): Int {
        val today = LocalDate.now()
        val last = try {
            if (lastDay.isEmpty()) null else LocalDate.parse(lastDay)
        } catch (e: Exception) {
            null
        }
        return if (last != null && (last == today || last == today.minusDays(1))) streak else 0
    }

    fun isUnlocked(lesson: Lesson): Boolean = lesson.id == 0 || (lesson.id - 1) in done

    fun learnedKeys(): Set<String> =
        ALL_LESSONS.filter { it.id in done }.flatMap { it.glyphs }.toSet()

    fun completeLesson(lesson: Lesson, gainedXp: Int) {
        done = done + lesson.id
        xp += gainedXp
        val today = LocalDate.now()
        val last = try {
            if (lastDay.isEmpty()) null else LocalDate.parse(lastDay)
        } catch (e: Exception) {
            null
        }
        if (last == null || last != today) {
            streak = if (last != null && last == today.minusDays(1)) streak + 1 else 1
            lastDay = today.toString()
        }
        prefs.edit()
            .putInt("xp", xp)
            .putInt("streak", streak)
            .putString("lastDay", lastDay)
            .putStringSet("done", done.map { it.toString() }.toSet())
            .apply()
    }
}

/**
 * Plays bundled clips from res/raw when they exist (see make-audio.sh),
 * otherwise falls back to the phone's Greek text-to-speech voice.
 */
class Speaker(context: Context) {
    private val ctx = context.applicationContext
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null

    var ttsReady by mutableStateOf(false)
        private set

    val hasClips: Boolean = clipId(AUDIO_CLIPS.values.firstOrNull()) != 0
    val ready: Boolean get() = hasClips || ttsReady

    init {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("el", "GR")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    private fun clipId(name: String?): Int =
        if (name == null) 0 else ctx.resources.getIdentifier(name, "raw", ctx.packageName)

    fun speak(text: String) {
        val id = clipId(AUDIO_CLIPS[text.lowercase(Locale.ROOT)])
        if (id != 0) {
            play(id)
        } else if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gramma")
        }
    }

    private fun play(id: Int) {
        try {
            player?.release()
            val p = MediaPlayer.create(ctx, id) ?: return
            player = p
            p.setOnCompletionListener { mp ->
                mp.release()
                if (player === mp) player = null
            }
            p.start()
        } catch (e: Exception) {
            player = null
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        player?.release()
        player = null
    }
}
