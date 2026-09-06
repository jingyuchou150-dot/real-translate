package com.example.xsubtitle

import android.content.Context
import android.content.res.AssetManager
import java.io.File

/**
 * 把打包在 assets/vosk_models/<name> 下的离线识别模型，
 * 首次启动时拷贝到应用私有存储，供 Vosk 加载。
 * 这样用户无需手动下载/选择模型，实现“傻瓜式”开箱即用。
 */
object AssetModelInstaller {

    fun ensureModel(context: Context, modelName: String): String {
        val target = File(context.filesDir, "vosk_models/$modelName")
        if (target.exists() && target.listFiles()?.isNotEmpty() == true) {
            return target.absolutePath
        }
        target.deleteRecursively()
        target.mkdirs()
        copyAssets(context.assets, "vosk_models/$modelName", target)
        return target.absolutePath
    }

    private fun copyAssets(am: AssetManager, assetPath: String, dst: File) {
        val entries = am.list(assetPath) ?: return
        if (entries.isEmpty()) {
            // 叶子节点：文件
            am.open(assetPath).use { ins ->
                dst.outputStream().use { outs -> ins.copyTo(outs) }
            }
            return
        }
        dst.mkdirs()
        for (name in entries) {
            copyAssets(am, "$assetPath/$name", File(dst, name))
        }
    }
}
