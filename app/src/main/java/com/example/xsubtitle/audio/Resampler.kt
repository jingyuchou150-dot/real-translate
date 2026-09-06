package com.example.xsubtitle.audio

/**
 * 极简 PCM 处理器：
 * 1) 多声道混合为单声道；
 * 2) 线性插值重采样到目标采样率（默认 16kHz）。
 * 输入/输出均为 16bit 小端 PCM。仅在分块边界存在极小不连续，对 ASR 影响可忽略。
 */
class Resampler(
    private val inSampleRate: Int,
    private val inChannels: Int,
    private val outSampleRate: Int
) {
    fun process(pcm: ByteArray): ByteArray {
        if (pcm.size < 2) return ByteArray(0)
        val totalSamples = pcm.size / 2
        val shorts = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val lo = pcm[i * 2].toInt() and 0xFF
            val hi = pcm[i * 2 + 1].toInt()
            shorts[i] = (lo or (hi shl 8)).toShort()
        }

        // 声道混合 -> 单声道
        val mono = ShortArray(totalSamples / inChannels)
        for (i in mono.indices) {
            var sum = 0
            for (c in 0 until inChannels) sum += shorts[i * inChannels + c]
            mono[i] = (sum / inChannels).toShort()
        }

        // 重采样（线性插值）
        val ratio = outSampleRate.toDouble() / inSampleRate
        val outLen = (mono.size * ratio).toInt()
        val out = ShortArray(outLen)
        for (i in 0 until outLen) {
            val pos = i / ratio
            val i0 = pos.toInt().coerceAtMost(mono.lastIndex)
            val i1 = (i0 + 1).coerceAtMost(mono.lastIndex)
            val frac = pos - i0
            val v0 = mono[i0].toDouble()
            val v1 = mono[i1].toDouble()
            out[i] = (v0 + (v1 - v0) * frac).toInt()
                .coerceIn(-32768, 32767).toShort()
        }

        // 转回 byte 小端
        val bytes = ByteArray(out.size * 2)
        for (i in out.indices) {
            val v = out[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = (v shr 8).toByte()
        }
        return bytes
    }
}
