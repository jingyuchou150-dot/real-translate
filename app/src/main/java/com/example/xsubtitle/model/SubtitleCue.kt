package com.example.xsubtitle.model

/**
 * 一条字幕 cue。
 * sourceText 为离线识别出的原始语种文本；
 * translatedText 为（可选）翻译结果，为空时显示原文。
 */
data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val sourceText: String,
    var translatedText: String? = null
) {
    val displayText: String
        get() = if (!translatedText.isNullOrBlank()) translatedText!! else sourceText
}
