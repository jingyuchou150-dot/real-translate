package com.example.xsubtitle.translate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * 在线翻译实现（LibreTranslate 协议）。
 *
 * 为尽量做到“傻瓜式开箱即用”，内置一组公共实例地址按顺序尝试（[FALLBACK_BASE_URLS]）。
 * 任意一个可用即返回译文；全部失败则降级为原文，绝不中断字幕生成。
 *
 * 若你有自建/稳定的实例或 API Key，构造时传入 [baseUrl]/[apiKey] 即可覆盖内置列表：
 *     LibreTranslateClient(baseUrl = "http://你的IP:5000", apiKey = "xxxx")
 * 自建（Docker 一行）：docker run -p 5000:5000 libretranslate/libretranslate
 */
class LibreTranslateClient(
    private val baseUrl: String? = null,
    private val apiKey: String? = null
) : Translator {

    private val client = OkHttpClient()

    // 公共实例顺序尝试；不同实例对语种支持与限流策略不同，多留几个提高命中率。
    private val endpoints: List<String>
        get() = if (baseUrl != null) listOf(baseUrl)
        else FALLBACK_BASE_URLS

    override suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (text.isBlank()) return text
        return withContext(Dispatchers.IO) {
            var lastErr: Exception? = null
            for (endpoint in endpoints) {
                try {
                    val result = doTranslate(endpoint, text, sourceLang, targetLang)
                    if (result != null) return@withContext result
                } catch (e: Exception) {
                    lastErr = e
                }
            }
            // 全部失败：降级返回原文
            lastErr?.printStackTrace()
            text
        }
    }

    private fun doTranslate(
        base: String,
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? {
        val bodyJson = JSONObject().apply {
            put("q", text)
            put("source", sourceLang)
            put("target", targetLang)
            put("format", "text")
            if (!apiKey.isNullOrBlank()) put("api_key", apiKey)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val req = Request.Builder()
            .url("$base/translate")
            .post(bodyJson.toString().toRequestBody(mediaType))
            .build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) return null
        val out = JSONObject(body).optString("translatedText", "")
        return out.takeIf { it.isNotBlank() }
    }

    companion object {
        // 公共实例（稳定性不保证，仅供开箱即用兜底；生产建议自建）
        private val FALLBACK_BASE_URLS = listOf(
            "https://libretranslate.de",
            "https://translate.argosopentech.com",
            "https://lt.earthly.dev"
        )
    }
}
