package com.example.xsubtitle

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.xsubtitle.audio.AudioExtractor
import com.example.xsubtitle.asr.VoskRecognizerEngine
import com.example.xsubtitle.databinding.ActivityPlayerBinding
import com.example.xsubtitle.model.SubtitleCue
import com.example.xsubtitle.subtitle.SrtWriter
import com.example.xsubtitle.translate.LibreTranslateClient
import com.example.xsubtitle.translate.NoTranslate
import com.example.xsubtitle.translate.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var cues: List<SubtitleCue> = emptyList()

    // 翻译语言对：日文 Vosk 模型 -> 中文
    private val SOURCE_LANG = "ja"
    private val TARGET_LANG = "zh"

    private val exportSrt = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.openOutputStream(uri)?.use { os ->
                os.writer(Charsets.UTF_8).use { w -> SrtWriter.writeTo(cues, w) }
            }
            Toast.makeText(this, "SRT 已导出", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val subtitleRunnable = object : Runnable {
        override fun run() {
            val pos = player?.currentPosition ?: 0
            val cue = cues.firstOrNull { pos in it.startMs..it.endMs }
            binding.tvSubtitle.text = cue?.displayText ?: ""
            binding.tvSubtitle.postDelayed(this, 80)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoUri = Uri.parse(intent.getStringExtra("videoUri"))
        val translate = intent.getBooleanExtra("translate", true)

        binding.fabExport.setOnClickListener { exportSrt.launch("subtitle.srt") }

        lifecycleScope.launch {
            try {
                binding.tvRecognizeStatus.text = "正在准备离线识别模型…"
                val modelPath = ModelDownloader.ensure(this@PlayerActivity) { p, s ->
                    runOnUiThread { binding.tvRecognizeStatus.text = "$s $p%" }
                }

                binding.tvRecognizeStatus.text = getString(R.string.recognizing)
                val engine = VoskRecognizerEngine(modelPath)
                val collected = mutableListOf<SubtitleCue>()
                withContext(Dispatchers.IO) {
                    AudioExtractor(this@PlayerActivity).extract(videoUri) { pcm ->
                        val added = engine.feed(pcm)
                        if (added.isNotEmpty()) collected.addAll(added)
                    }
                    collected.addAll(engine.finish())
                    engine.close()
                }

                if (translate) {
                    binding.tvRecognizeStatus.text = "正在翻译（日→中）…"
                    val tr: Translator = LibreTranslateClient()
                    withContext(Dispatchers.IO) {
                        collected.forEach { cue ->
                            cue.translatedText = tr.translate(cue.sourceText, SOURCE_LANG, TARGET_LANG)
                        }
                    }
                } else {
                    val noop = NoTranslate()
                    collected.forEach { it.translatedText = noop.translate(it.sourceText, SOURCE_LANG, TARGET_LANG) }
                }

                cues = collected
                binding.recognizeOverlay.visibility = View.GONE
                setupPlayer(videoUri)
            } catch (e: Exception) {
                binding.tvRecognizeStatus.text = "出错: ${e.message}"
                Toast.makeText(this@PlayerActivity, "处理失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupPlayer(videoUri: Uri) {
        player = ExoPlayer.Builder(this).build().also {
            binding.playerView.player = it
            it.setMediaItem(MediaItem.fromUri(videoUri))
            it.prepare()
            it.playWhenReady = true
        }
        binding.tvSubtitle.post(subtitleRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.tvSubtitle.removeCallbacks(subtitleRunnable)
        player?.release()
        player = null
    }
}
