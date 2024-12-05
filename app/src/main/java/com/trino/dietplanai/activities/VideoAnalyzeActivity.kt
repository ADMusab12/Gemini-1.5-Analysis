package com.trino.dietplanai.activities

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.widget.Button
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.trino.dietplanai.databinding.ActivityVideoAnalyzeBinding
import com.trino.dietplanai.util.Constant.CLUB_SUITE
import com.trino.dietplanai.util.Extension.showMessage
import com.trino.dietplanai.viewmodel.DietPlanViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

class VideoAnalyzeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoAnalyzeBinding
    private lateinit var videoView: VideoView
    private lateinit var pickVideoButton: Button
    private lateinit var pickVideoLauncher: ActivityResultLauncher<String>
    private val viewModel: DietPlanViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoAnalyzeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoView = binding.videoView
        pickVideoButton = binding.buttonPickVideo

        setupLauncher()
        clickListeners()
        observeViewModel()
    }

    private fun setupLauncher() {
        pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                videoView.setVideoURI(uri)
                videoView.start()
                viewModel.fetchVideoAnalysis(this@VideoAnalyzeActivity, uri)
                binding.progress.isVisible = true
                showMessage("Video selected")
            } else {
                showMessage("Failed to pick a video")
            }
        }
    }

    private fun clickListeners() {
        pickVideoButton.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
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
                        val parse = extractMainText(state.dietPlan)
                        binding.textResult.text = enhanceDietPlanText(parse)
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

    private fun extractMainText(response: String): String {
        return try {
            val jsonObject = JSONObject(response)
            val candidates = jsonObject.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    parts.getJSONObject(0).getString("text")
                } else {
                    "No description available."
                }
            } else {
                "No candidates found."
            }
        } catch (e: Exception) {
            "Failed to parse response: ${e.message}"
        }
    }

    private fun enhanceDietPlanText(apiResponse: String): Spanned {
        val enhancedText = apiResponse
            .replace("**", "") // Remove bold markers entirely
            .replace("* ", "$CLUB_SUITE ") // Replace list indicators with bullets
            .replace("##", "") // Remove heading markers
            .replace(">", "$CLUB_SUITE ") // Remove heading markers
            .replace("\n", "<br>")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(enhancedText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            HtmlCompat.fromHtml(enhancedText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    }
}