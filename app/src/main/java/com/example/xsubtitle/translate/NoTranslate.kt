package com.example.xsubtitle.translate

/**
 * 纯离线实现：不做翻译，原样返回原文。
 * 满足“零依赖、不联网、不暴露内容”的诉求。
 */
class NoTranslate : Translator {
    override suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        return text
    }
}
