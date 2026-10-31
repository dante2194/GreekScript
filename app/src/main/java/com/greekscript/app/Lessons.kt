package com.greekscript.app

import kotlin.math.abs
import kotlin.random.Random

sealed interface Exercise {

    /** "Meet the letter" card. No score. */
    data class Intro(val glyph: Glyph) : Exercise

    /** Multiple choice. [big] is the thing being asked about (can be empty for listening). */
    data class Pick(
        val prompt: String,
        val big: String,
        val bigGreek: Boolean,
        val options: List<String>,
        val answer: String,
        val optionsGreek: Boolean,
        val speak: String? = null,
        val autoPlay: Boolean = false,
        val reveal: String? = null,
    ) : Exercise

    /** Tap a left tile, then its partner on the right. */
    data class Match(
        val prompt: String,
        val pairs: List<Pair<String, String>>,
        val leftGreek: Boolean,
        val rightGreek: Boolean,
    ) : Exercise

    /** Spell an English word in Greek by tapping tiles. */
    data class Build(
        val prompt: String,
        val tokens: List<String>,
        val tiles: List<String>,
        val caps: Boolean,
    ) : Exercise
}

object LessonBuilder {

    fun learnedKeys(upToLessonId: Int): Set<String> =
        ALL_LESSONS.filter { it.id <= upToLessonId }.flatMap { it.glyphs }.toSet()

    fun build(lesson: Lesson, listenOk: Boolean, rnd: Random = Random.Default): List<Exercise> {
        val learned = learnedKeys(lesson.id)
        val pool = GLYPHS.filter { it.key in learned }
        return when (lesson.kind) {
            LessonKind.LETTERS -> lettersLesson(lesson, learned, pool, listenOk, rnd)
            LessonKind.WORDS -> wordsLesson(lesson, learned, pool, listenOk, rnd, caps = false)
            LessonKind.CAPS -> wordsLesson(lesson, learned, pool, listenOk, rnd, caps = true)
        }
    }

    // ---------------------------------------------------------------- lessons

    private fun lettersLesson(
        lesson: Lesson,
        learned: Set<String>,
        pool: List<Glyph>,
        listenOk: Boolean,
        rnd: Random,
    ): List<Exercise> {
        val fresh = lesson.glyphs.mapNotNull { GLYPH_BY_KEY[it] }
        val out = mutableListOf<Exercise>()

        for (g in fresh) {
            out += Exercise.Intro(g)
            out += letterToSound(g, pool, rnd)
        }
        for (g in fresh.shuffled(rnd)) {
            out += soundToLetter(g, pool, rnd)
        }

        val older = pool.filter { p -> fresh.none { it.key == p.key } }.shuffled(rnd).take(2)
        for (g in older) out += letterToSound(g, pool, rnd)

        matchCase(fresh + older)?.let { out += it }
        matchSound(fresh + older)?.let { out += it }

        val usable = wordsFor(learned, caps = false)
        val words = usable
            .filter { w -> w.tokens.any { t -> fresh.any { f -> f.key == baseKey(t) } } }
            .shuffled(rnd)

        for (w in words.take(3)) out += wordToEnglish(w, rnd)
        for (w in words.drop(1).take(2)) buildEx(w, pool, rnd)?.let { out += it }
        if (usable.size >= 4) {
            words.firstOrNull()?.let { w -> englishToGreek(w, usable, rnd)?.let { out += it } }
            if (listenOk) {
                words.drop(1).firstOrNull()?.let { w -> listen(w, usable, rnd)?.let { out += it } }
            }
        }
        return out
    }

    private fun wordsLesson(
        lesson: Lesson,
        learned: Set<String>,
        pool: List<Glyph>,
        listenOk: Boolean,
        rnd: Random,
        caps: Boolean,
    ): List<Exercise> {
        val section = SECTIONS.first { it.index == lesson.section }
        val sectionKeys = section.lessons.flatMap { it.glyphs }.toSet()
        val usable = wordsFor(learned, caps)
        val focus = usable.filter { w -> w.tokens.any { baseKey(it) in sectionKeys } }
        val base = if (focus.size >= 6) focus else usable
        val picks = base.shuffled(rnd).take(8)

        val out = mutableListOf<Exercise>()
        for (w in picks.take(5)) out += wordToEnglish(w, rnd)
        if (picks.size >= 3) {
            out += Exercise.Match(
                prompt = "Match each word with its English",
                pairs = picks.take(4).map { it.greek to it.english },
                leftGreek = true,
                rightGreek = false,
            )
        }
        for (w in picks.drop(2).take(2)) buildEx(w, pool, rnd)?.let { out += it }
        if (usable.size >= 4) {
            for (w in picks.drop(1).take(2)) englishToGreek(w, usable, rnd)?.let { out += it }
            if (listenOk) {
                for (w in picks.drop(3).take(2)) listen(w, usable, rnd)?.let { out += it }
            }
        }
        val sectionGlyphs = sectionKeys.mapNotNull { GLYPH_BY_KEY[it] }.shuffled(rnd).take(3)
        for (g in sectionGlyphs) out += letterToSound(g, pool, rnd)
        return out.shuffled(rnd)
    }

    // ---------------------------------------------------------------- helpers

    private fun wordsFor(learned: Set<String>, caps: Boolean): List<Word> =
        WORDS.filter { w -> w.caps == caps && w.tokens.all { baseKey(it) in learned } }

    private fun soundOptions(target: Glyph, pool: List<Glyph>, rnd: Random): List<String> {
        val first = pool.filter { it.sound != target.sound }.shuffled(rnd)
        val second = GLYPHS.filter { it.sound != target.sound }.shuffled(rnd)
        val others = (first + second).map { it.sound }.distinct().take(3)
        return (others + target.sound).shuffled(rnd)
    }

    private fun glyphOptions(target: Glyph, pool: List<Glyph>, rnd: Random): List<Glyph> {
        val first = pool.filter { it.sound != target.sound }.shuffled(rnd)
        val second = GLYPHS.filter { it.sound != target.sound }.shuffled(rnd)
        val others = (first + second).distinctBy { it.sound }.take(3)
        return (others + target).shuffled(rnd)
    }

    private fun letterToSound(g: Glyph, pool: List<Glyph>, rnd: Random): Exercise.Pick {
        val shown = if (g.upper != g.lower && rnd.nextBoolean()) g.upper else g.lower
        return Exercise.Pick(
            prompt = "What sound does this make?",
            big = shown,
            bigGreek = true,
            options = soundOptions(g, pool, rnd),
            answer = g.sound,
            optionsGreek = false,
            speak = g.say,
            reveal = "$shown says “${g.sound}”",
        )
    }

    private fun soundToLetter(g: Glyph, pool: List<Glyph>, rnd: Random): Exercise.Pick {
        val useUpper = g.upper != g.lower && rnd.nextBoolean()
        fun show(x: Glyph): String = if (useUpper) x.upper else x.lower
        return Exercise.Pick(
            prompt = "Which one says “${g.sound}”?",
            big = g.sound,
            bigGreek = false,
            options = glyphOptions(g, pool, rnd).map { show(it) },
            answer = show(g),
            optionsGreek = true,
            speak = null,
            reveal = "${show(g)} says “${g.sound}”",
        )
    }

    private fun matchCase(gs: List<Glyph>): Exercise.Match? {
        val cand = gs.filter { it.upper != it.lower }.distinctBy { it.upper }.take(5)
        if (cand.size < 2) return null
        return Exercise.Match(
            prompt = "Match each capital with its small letter",
            pairs = cand.map { it.upper to it.lower },
            leftGreek = true,
            rightGreek = true,
        )
    }

    private fun matchSound(gs: List<Glyph>): Exercise.Match? {
        val cand = gs.distinctBy { it.sound }.take(5)
        if (cand.size < 2) return null
        return Exercise.Match(
            prompt = "Match each letter with its sound",
            pairs = cand.map { it.lower to it.sound },
            leftGreek = true,
            rightGreek = false,
        )
    }

    private fun wordToEnglish(w: Word, rnd: Random): Exercise.Pick {
        val others = WORDS.map { it.english }
            .distinct()
            .filter { it != w.english }
            .shuffled(rnd)
            .take(3)
        return Exercise.Pick(
            prompt = "Read it. What English word is this?",
            big = w.greek,
            bigGreek = true,
            options = (others + w.english).shuffled(rnd),
            answer = w.english,
            optionsGreek = false,
            speak = w.greek,
            reveal = "${w.greek} = ${w.english}",
        )
    }

    private fun englishToGreek(w: Word, usable: List<Word>, rnd: Random): Exercise.Pick? {
        val others = usable
            .filter { it.greek != w.greek }
            .sortedBy { abs(it.tokens.size - w.tokens.size) }
            .take(6)
            .shuffled(rnd)
            .take(3)
        if (others.size < 3) return null
        return Exercise.Pick(
            prompt = "Which one spells “${w.english}”?",
            big = w.english,
            bigGreek = false,
            options = (others.map { it.greek } + w.greek).shuffled(rnd),
            answer = w.greek,
            optionsGreek = true,
            speak = null,
            reveal = "${w.greek} = ${w.english}",
        )
    }

    private fun listen(w: Word, usable: List<Word>, rnd: Random): Exercise.Pick? {
        val others = usable
            .filter { it.greek != w.greek }
            .sortedBy { abs(it.tokens.size - w.tokens.size) }
            .take(6)
            .shuffled(rnd)
            .take(3)
        if (others.size < 3) return null
        return Exercise.Pick(
            prompt = "Tap what you hear",
            big = "",
            bigGreek = false,
            options = (others.map { it.greek } + w.greek).shuffled(rnd),
            answer = w.greek,
            optionsGreek = true,
            speak = w.greek,
            autoPlay = true,
            reveal = "${w.greek} = ${w.english}",
        )
    }

    private fun buildEx(w: Word, pool: List<Glyph>, rnd: Random): Exercise.Build? {
        if (w.tokens.size !in 2..8) return null
        val have = w.tokens.map { baseKey(it) }.toSet()
        val extras = pool
            .filter { it.key !in have && it.key != "ς" }
            .shuffled(rnd)
            .take(2)
            .map { if (w.caps) it.upper else it.lower }
        return Exercise.Build(
            prompt = "Write “${w.english}” in Greek",
            tokens = w.tokens,
            tiles = (w.tokens + extras).shuffled(rnd),
            caps = w.caps,
        )
    }
}
