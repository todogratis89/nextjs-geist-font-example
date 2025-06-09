package com.example.rtmpstreamer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var m3u8UrlInput: EditText
    private lateinit var rtmpUrlInput: EditText
    private lateinit var streamKeyInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var titleText: TextView

    private var streamingJob: Job? = null
    private var isStreaming = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m3u8UrlInput = findViewById(R.id.m3u8UrlInput)
        rtmpUrlInput = findViewById(R.id.rtmpUrlInput)
        streamKeyInput = findViewById(R.id.streamKeyInput)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        titleText = findViewById(R.id.titleText)

        startButton.setOnClickListener {
            if (!isStreaming) {
                val m3u8Url = m3u8UrlInput.text.toString().trim()
                val rtmpUrl = rtmpUrlInput.text.toString().trim()
                val streamKey = streamKeyInput.text.toString().trim()

                if (m3u8Url.isEmpty() || rtmpUrl.isEmpty() || streamKey.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    startStreaming(m3u8Url, rtmpUrl, streamKey)
                }
            }
        }

        stopButton.setOnClickListener {
            if (isStreaming) {
                stopStreaming()
            }
        }
    }

    private fun startStreaming(m3u8Url: String, rtmpUrl: String, streamKey: String) {
        isStreaming = true
        startButton.isEnabled = false
        stopButton.isEnabled = true
        Toast.makeText(this, "Starting stream...", Toast.LENGTH_SHORT).show()

        val fullRtmpUrl = "$rtmpUrl/$streamKey"

        streamingJob = CoroutineScope(Dispatchers.IO).launch {
            val ffmpegCommand = arrayOf(
                "-re",
                "-i", m3u8Url,
                "-c", "copy",
                "-f", "flv",
                fullRtmpUrl
            )

            val rc = FFmpeg.execute(ffmpegCommand)

            withContext(Dispatchers.Main) {
                if (rc == Config.RETURN_CODE_SUCCESS) {
                    Toast.makeText(this@MainActivity, "Stream ended successfully", Toast.LENGTH_SHORT).show()
                } else if (rc == Config.RETURN_CODE_CANCEL) {
                    Toast.makeText(this@MainActivity, "Stream stopped", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Stream failed with rc=$rc", Toast.LENGTH_LONG).show()
                }
                isStreaming = false
                startButton.isEnabled = true
                stopButton.isEnabled = false
            }
        }
    }

    private fun stopStreaming() {
        if (isStreaming) {
            FFmpeg.cancel()
            streamingJob?.cancel()
            isStreaming = false
            startButton.isEnabled = true
            stopButton.isEnabled = false
            Toast.makeText(this, "Stopping stream...", Toast.LENGTH_SHORT).show()
        }
    }
}
