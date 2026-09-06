package com.example.xsubtitle.translate

/**
 * 翻译层抽象。ASR 用 Vosk 离线完成；翻译是可插拔的：
 * - NoTranslate：纯离线，返回原文（不消耗流量、无需 Key）；
 * - LibreTranslateClient：在线翻译，可指向自建/公共实例。
 * sourceLang / targetLang 使用 ISO 639-1 代码，如 "en"、"zh"。
 */
interface Translator {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String
}
