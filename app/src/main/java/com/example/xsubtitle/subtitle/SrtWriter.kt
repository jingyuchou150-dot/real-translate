package com.example.xsubtitle.subtitle

import com.example.xsubtitle.model.SubtitleCue
import java.io.File
import java.io.Writer

object SrtWriter {

    /** 生成标准 SRT 文本。 */
    fun build(cues: List<SubtitleCue>): String = buildString {
        cues.forEachIndexed { i, cue ->
            appendLine((i + 1).toString())
            appendLine("${fmt(cue.startMs)} --> ${fmt(cue.endMs)}")
            appendLine(cue.displayText)
            appendLine()
        }
    }

    /** 直接写入文件（UTF-8）。 */
    fun write(cues: List<SubtitleCue>, file: File) {
        file.bufferedWriter(Charsets.UTF_8).use { w -> w.write(build(cues)) }
    }

    /** 写入任意 Writer（如 SAF 返回的 OutputStream）。 */
    fun writeTo(cues: List<SubtitleCue>, writer: Writer) {
        writer.write(build(cues))
    }

    private fun fmt(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val millis = ms % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, millis)
    }
}
