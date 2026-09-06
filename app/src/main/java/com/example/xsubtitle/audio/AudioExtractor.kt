package com.example.xsubtitle.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileDescriptor

/**
 * 从视频 Uri 提取音轨，解码为 16bit PCM，重采样到 16kHz 单声道后分块回调。
 * Vosk 要求输入为 16kHz、单声道、16bit 小端 PCM。
 */
class AudioExtractor(private val context: Context) {

    fun interface PcmSink {
        fun onPcm(pcm16Mono: ByteArray)
    }

    suspend fun extract(uri: Uri, targetSampleRate: Int = 16000, sink: PcmSink) =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) {
                extractor.release()
                return@withContext
            }
            pfd.use { fd: FileDescriptor ->
                extractor.setDataSource(fd)
                var audioTrack = -1
                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        audioTrack = i
                        break
                    }
                }
                if (audioTrack < 0) {
                    extractor.release()
                    return@use
                }

                extractor.selectTrack(audioTrack)
                val inFormat = extractor.getTrackFormat(audioTrack)
                val mime = inFormat.getString(MediaFormat.KEY_MIME)!!
                val decoder = MediaCodec.createDecoderByType(mime)

                val outFormat = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_RAW,
                    inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                )
                decoder.configure(outFormat, null, null, 0)
                decoder.start()

                val resampler = Resampler(
                    inSampleRate = inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    inChannels = inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                    outSampleRate = targetSampleRate
                )

                val info = MediaCodec.BufferInfo()
                var sawInputEOS = false
                var sawOutputEOS = false
                val timeoutUs = 10000L

                while (!sawOutputEOS) {
                    if (!sawInputEOS) {
                        val inIdx = decoder.dequeueInputBuffer(timeoutUs)
                        if (inIdx >= 0) {
                            val buf = decoder.getInputBuffer(inIdx)!!
                            val sampleSize = extractor.readSampleData(buf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                sawInputEOS = true
                            } else {
                                decoder.queueInputBuffer(
                                    inIdx, 0, sampleSize,
                                    extractor.sampleTime, 0
                                )
                                extractor.advance()
                            }
                        }
                    }

                    val outIdx = decoder.dequeueOutputBuffer(info, timeoutUs)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* 下一轮重试 */ }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* 忽略 */ }
                        outIdx >= 0 -> {
                            val buf = decoder.getOutputBuffer(outIdx)!!
                            val chunk = ByteArray(info.size)
                            buf.get(chunk)
                            decoder.releaseOutputBuffer(outIdx, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                sawOutputEOS = true
                            }
                            val out = resampler.process(chunk)
                            if (out.isNotEmpty()) sink.onPcm(out)
                        }
                    }
                }
                decoder.stop()
                decoder.release()
            }
            extractor.release()
        }
}
