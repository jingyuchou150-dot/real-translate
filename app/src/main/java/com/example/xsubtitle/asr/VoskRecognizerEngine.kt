package com.example.xsubtitle.asr

import com.example.xsubtitle.model.SubtitleCue
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.Closeable

/**
 * 基于 Vosk 的离线语音识别引擎。
 * 流式接收 16k 单声道 PCM，按词的时间戳聚合成带时间轴的字幕 cue。
 * 全部推理在本地完成，不依赖网络。
 */
class VoskRecognizerEngine(modelDir: String) : Closeable {

    private val model = Model(modelDir)
    private val recognizer = Recognizer(model, 16000.0f)

    private val cues = mutableListOf<SubtitleCue>()
    private var pendingWords = mutableListOf<Word>()

    private data class Word(
        val startMs: Long,
        val endMs: Long,
        val text: String
    )

    /** 喂入一块 PCM，返回本次“已确定(final)”的新增字幕 cue（可能为空）。 */
    fun feed(pcm: ByteArray): List<SubtitleCue> {
        return if (recognizer.acceptWaveForm(pcm, pcm.size)) {
            flushFinal(recognizer.result)
        } else {
            emptyList()
        }
    }

    /** 全部音频喂完后调用，得到完整字幕列表。 */
    fun finish(): List<SubtitleCue> {
        flushFinal(recognizer.finalResult)
        if (pendingWords.isNotEmpty()) {
            cues.add(buildCue(pendingWords))
            pendingWords.clear()
        }
        return cues.toList()
    }

    private fun flushFinal(json: String): List<SubtitleCue> {
        val obj = JSONObject(json)
        val arr = obj.optJSONArray("result") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val w = arr.getJSONObject(i)
            pendingWords.add(
                Word(
                    (w.getDouble("start") * 1000).toLong(),
                    (w.getDouble("end") * 1000).toLong(),
                    w.getString("word")
                )
            )
        }
        val added = mutableListOf<SubtitleCue>()
        while (pendingWords.isNotEmpty() && pendingWords.any { endsWithPunct(it.text) }) {
            val seg = takeSentence()
            if (seg.isNotEmpty()) {
                val cue = buildCue(seg)
                cues.add(cue)
                added.add(cue)
            }
        }
        return added
    }

    private fun takeSentence(): List<Word> {
        val idx = pendingWords.indexOfFirst { endsWithPunct(it.text) }
        return if (idx >= 0) {
            val seg = pendingWords.take(idx + 1)
            pendingWords = pendingWords.drop(idx + 1).toMutableList()
            seg
        } else {
            val seg = pendingWords.toList()
            pendingWords.clear()
            seg
        }
    }

    private fun buildCue(words: List<Word>): SubtitleCue {
        val text = words.joinToString(" ") { it.text }.trim()
        return SubtitleCue(
            startMs = words.first().startMs,
            endMs = words.last().endMs,
            sourceText = text
        )
    }

    private fun endsWithPunct(s: String): Boolean =
        s.matches(".*[.!?,;:。！？，；：]$".toRegex())

    override fun close() {
        recognizer.close()
        model.close()
    }
}
