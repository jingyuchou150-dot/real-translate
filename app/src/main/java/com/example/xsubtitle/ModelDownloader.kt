package com.example.xsubtitle

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 离线识别模型（日语）的就绪工具。
 *
 * 优先级：
 * 1) 已解压过 -> 直接返回；
 * 2) APK 内置模型包（assets/vosk-model-ja.zip）— 纯离线解压，不消耗流量；
 * 3) 内置包不存在时才回退到网络下载（旧版构建或未打包模型的情况）。
 */
object ModelDownloader {

    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip"
    private const val MODEL_DIR = "ja"
    private const val ASSET_ZIP = "vosk-model-ja.zip"

    suspend fun ensure(
        context: Context,
        onProgress: (percent: Int, status: String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val target = File(context.filesDir, "vosk_models/$MODEL_DIR")
        if (target.exists() && target.listFiles()?.isNotEmpty() == true) {
            return@withContext target.absolutePath
        }

        val fromAssets =
            runCatching { unpackFromAssets(context, target, onProgress) }.getOrDefault(false)
        if (fromAssets) {
            return@withContext target.absolutePath
        }

        onProgress(0, "正在下载日语识别模型…")
        val zipFile = File(context.cacheDir, "ja_model.zip")
        download(MODEL_URL, zipFile, onProgress)
        onProgress(99, "正在解压模型…")
        unzip(zipFile, target)
        zipFile.delete()
        target.absolutePath
    }

    private fun unpackFromAssets(
        context: Context,
        target: File,
        onProgress: (percent: Int, status: String) -> Unit
    ): Boolean {
        val input = try {
            context.assets.open(ASSET_ZIP)
        } catch (e: Exception) {
            return false
        }
        target.deleteRecursively()
        target.mkdirs()
        var extracted = 0L
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val rel = entry.name.substringAfter('/')
                if (rel.isNotEmpty()) {
                    val outFile = File(target, rel)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { os -> zis.copyTo(os) }
                        extracted += entry.size
                        onProgress(
                            (extracted / 500_000L).coerceAtMost(99L).toInt(),
                            "正在解压内置日语模型…"
                        )
                    }
                }
                entry = zis.nextEntry
            }
        }
        return target.listFiles()?.isNotEmpty() == true
    }

    private suspend fun download(
        url: String,
        out: File,
        onProgress: (percent: Int, status: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("模型下载失败: HTTP ${resp.code}")
            val body = resp.body ?: throw RuntimeException("空响应")
            val total = if (body.contentLength() > 0) body.contentLength() else -1L
            out.outputStream().use { fos ->
                val src = body.byteStream()
                val buf = ByteArray(8192)
                var read: Int
                var downloaded = 0L
                while (src.read(buf).also { read = it } != -1) {
                    fos.write(buf, 0, read)
                    downloaded += read
                    if (total > 0) {
                        onProgress(
                            (downloaded * 100 / total).toInt(),
                            "正在下载日语识别模型…"
                        )
                    }
                }
            }
        }
    }

    private fun unzip(zip: File, target: File) {
        target.deleteRecursively()
        target.mkdirs()
        ZipInputStream(zip.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val rel = entry.name.substringAfter('/')
                if (rel.isNotEmpty()) {
                    val outFile = File(target, rel)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { os -> zis.copyTo(os) }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
