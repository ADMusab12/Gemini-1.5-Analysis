package com.trino.dietplanai.activities

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Spanned
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.trino.dietplanai.databinding.ActivityAudioAnalyzeBinding
import com.trino.dietplanai.util.Constant.CLUB_SUITE
import com.trino.dietplanai.util.Constant.STAR_OF_DAVID
import com.trino.dietplanai.util.Extension.showMessage
import com.trino.dietplanai.viewmodel.DietPlanViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AudioAnalyzeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioAnalyzeBinding
    private lateinit var pickAudioButton: Button
    private lateinit var pickAudioLauncher: ActivityResultLauncher<String>
    private val viewModel: DietPlanViewModel by viewModels()
    private lateinit var mediaPlayer: MediaPlayer
    private val handler = Handler(Looper.getMainLooper())
    private var isAudioPlaying = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioAnalyzeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pickAudioButton = binding.buttonPickAudio

        setupLauncher()
        clickListeners()
        observeViewModel()
    }

    private fun setupLauncher() {
        pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                playAudio(uri)
                viewModel.fetchAudioAnalysis(this@AudioAnalyzeActivity, uri)
                binding.progress.isVisible = true
                showMessage("Audio selected")
            } else {
                showMessage("Failed to pick a audio")
            }
        }
    }

    private fun clickListeners() {
        pickAudioButton.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        binding.seekAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    binding.textStart.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacksAndMessages(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateSeekBar()
            }
        })
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun playAudio(uri: Uri) {
        // Release existing MediaPlayer if any
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        // Initialize MediaPlayer with the selected audio URI
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer.setOnPreparedListener {
            val duration = mediaPlayer.duration
            binding.seekAudio.max = duration
            binding.textEnd.text = formatTime(duration)
            binding.textStart.text = formatTime(0)
            isAudioPlaying = true
            mediaPlayer.start()
            updateSeekBar()
        }

        mediaPlayer.setOnCompletionListener {
            handler.removeCallbacksAndMessages(null)
            isAudioPlaying = false
            binding.seekAudio.progress = 0
            binding.textStart.text = formatTime(0)
        }
    }

    private fun updateSeekBar() {
        handler.post(object : Runnable {
            override fun run() {
                if (isAudioPlaying) {
                    val currentPosition = mediaPlayer.currentPosition
                    binding.seekAudio.progress = currentPosition
                    binding.textStart.text = formatTime(currentPosition)
                    handler.postDelayed(this, 500)
                }
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.dietPlanState.collect { state ->
                when (state) {
                    is DietPlanViewModel.UiState.Loading -> {
                        binding.textResult.text = "Generating..."
                    }

                    is DietPlanViewModel.UiState.Success -> {
                        binding.progress.isVisible = false
                        binding.textScroll.isVisible = true
                        binding.textResult.text = enhanceDietPlanText(state.dietPlan)
                    }

                    is DietPlanViewModel.UiState.Error -> {
                        binding.textResult.text = "No description available"
                    }

                    is DietPlanViewModel.UiState.Initial -> {

                    }
                }
            }
        }
    }


    private fun enhanceDietPlanText(apiResponse: String): Spanned {
        val enhancedText = apiResponse
            .replace("**", "") // Remove bold markers entirely
            .replace("* ", "$STAR_OF_DAVID ") // Replace list indicators with bullets
            .replace("##", "") // Remove heading markers
            .replace(">", "$STAR_OF_DAVID ") // Remove heading markers
            .replace("\n", "<br>")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(enhancedText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            HtmlCompat.fromHtml(enhancedText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        handler.removeCallbacksAndMessages(null)
    }
}