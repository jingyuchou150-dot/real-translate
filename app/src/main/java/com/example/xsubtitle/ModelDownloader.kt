package com.example.xsubtitle

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 首次使用时自动下载并解压离线识别模型（日语音频模型）。
 * 这样 APK 体积小、用户无需手动放置模型，实现“傻瓜式”开箱即用。
 *
 * 下载地址为官方 Vosk 模型（在真机正常网络下可用）；
 * 若需要，可把 MODEL_URL 换成你自己的镜像/自建地址。
 */
object ModelDownloader {

    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip"
    private const val MODEL_DIR = "ja"

    /**
     * 确保模型已就绪，返回 Vosk 可加载的目录路径。
     * onProgress(percent, status) 用于更新进度 UI。
     */
    suspend fun ensure(
        context: Context,
        onProgress: (percent: Int, status: String) -> Unit
    ): String {
        val target = File(context.filesDir, "vosk_models/$MODEL_DIR")
        if (target.exists() && target.listFiles()?.isNotEmpty() == true) {
            return target.absolutePath
        }
        onProgress(0, "正在下载日语识别模型…")
        val zipFile = File(context.cacheDir, "ja_model.zip")
        download(MODEL_URL, zipFile, onProgress)
        onProgress(99, "正在解压模型…")
        unzip(zipFile, target)
        zipFile.delete()
        return target.absolutePath
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
                // zip 顶层目录形如 vosk-model-small-ja-0.22/，去掉这一层
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
