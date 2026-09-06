package com.example.xsubtitle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.xsubtitle.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var videoUri: Uri? = null

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        videoUri = uri
        binding.tvSelected.text = "已选择视频，点击「生成字幕」"
        binding.btnStart.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickVideo.setOnClickListener { pickVideo.launch(arrayOf("video/*")) }

        binding.btnStart.setOnClickListener {
            val v = videoUri ?: return@setOnClickListener
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("videoUri", v.toString())
                putExtra("translate", binding.switchTranslate.isChecked)
            }
            startActivity(intent)
        }
    }
}
